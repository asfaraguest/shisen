import Foundation
import CryptoKit

public final class AniChanClient {
    public static let shared = AniChanClient()
    private let baseUrl = "https://anichan.to"

    private let activeK = "903089e1"
    private let activeO = "8jaT7svYhpkeirzPKSjTSshKM9gATuwNk9XgHf8mpgM="
    private let activeB = "ZM3doawsrjvonxh8mEbni0WBlgp6rYiASHUBpHLPaqY="

    private var cachedTokenN = ""
    private var tokenExpiresEpoch: TimeInterval = 0
    private var cachedCookies: [String: String] = [:]

    private init() {}

    public func fetchEpisodes(anilistId: Int) async -> [Episode] {
        guard anilistId > 0,
              let url = URL(string: "\(baseUrl)/api/watch/episodes?anilistId=\(anilistId)") else {
            return []
        }

        do {
            let (data, response) = try await NetworkClient.shared.get(
                url: url,
                headers: ["Referer": "\(baseUrl)/anime/\(anilistId)"]
            )
            guard response.statusCode == 200,
                  let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let totalEps = json["episodes"] as? Int, totalEps > 0 else {
                return []
            }

            return (1...totalEps).map {
                Episode(number: $0, title: "Episode \($0)", source: "anichan", sourceId: "\($0)")
            }
        } catch {
            return []
        }
    }

    public func resolveEpisodeStream(anilistId: Int, episodeNumber: Int) async throws -> StreamSource? {
        guard anilistId > 0 else { return nil }
        try await ensureSession()

        var serversList: [[String: Any]] = []
        for tier in ["fast", "rest"] {
            guard let url = URL(string: "\(baseUrl)/api/watch/servers?anilistId=\(anilistId)&ep=\(episodeNumber)&category=sub&tier=\(tier)") else { continue }
            var headers: [String: String] = [
                "X-Wk": activeK,
                "Referer": "\(baseUrl)/anime/\(anilistId)",
                "Origin": baseUrl
            ]
            if !cachedCookies.isEmpty {
                headers["Cookie"] = cachedCookies.map { "\($0.key)=\($0.value)" }.joined(separator: "; ")
            }

            do {
                let (data, response) = try await NetworkClient.shared.get(url: url, headers: headers)
                guard response.statusCode == 200,
                      let raw = try JSONSerialization.jsonObject(with: data) as? [String: Any] else { continue }
                let decrypted = decryptPayload(raw: raw, tokenN: cachedTokenN)
                if let srvArr = decrypted["servers"] as? [[String: Any]], !srvArr.isEmpty {
                    serversList = srvArr
                    break
                }
            } catch {
                continue
            }
        }

        guard !serversList.isEmpty else { return nil }

        let sorted = serversList.sorted {
            serverScore($0["name"] as? String ?? "") < serverScore($1["name"] as? String ?? "")
        }

        for srv in sorted {
            let type = (srv["type"] as? String ?? "").lowercased()
            let rawStreamUrl = srv["stream"] as? String ?? ""
            guard type == "hls", !rawStreamUrl.isEmpty else { continue }

            var directUrl = rawStreamUrl
            var referer = "https://megaplay.buzz/"

            if rawStreamUrl.contains("url="),
               let comps = URLComponents(string: rawStreamUrl) {
                if let upstream = comps.queryItems?.first(where: { $0.name == "url" })?.value, upstream.hasPrefix("http") {
                    directUrl = upstream
                }
                if let ref = comps.queryItems?.first(where: { $0.name == "ref" })?.value, !ref.isEmpty {
                    referer = ref
                }
            }

            var subTracks: [SubtitleTrack] = []
            if let subs = srv["subtitles"] as? [[String: Any]] {
                for sub in subs {
                    guard var sUrl = sub["url"] as? String, !sUrl.isEmpty else { continue }
                    if sUrl.hasPrefix("/") { sUrl = "\(baseUrl)\(sUrl)" }
                    let label = sub["lang"] as? String ?? "English"
                    let langCode = label.lowercased().contains("indo") ? "id" : "en"
                    subTracks.append(SubtitleTrack(url: sUrl, label: label, lang: langCode, isDefault: false))
                }
            }

            let serverName = srv["name"] as? String ?? "AniChan HLS"
            return StreamSource(
                url: directUrl,
                isHls: true,
                quality: "HD",
                serverName: "AniChan (\(serverName))",
                headers: [
                    "Referer": referer,
                    "Origin": "https://megaplay.buzz"
                ],
                subtitles: subTracks
            )
        }

        return nil
    }

    private func ensureSession() async throws {
        let now = Date().timeIntervalSince1970
        if !cachedTokenN.isEmpty && tokenExpiresEpoch > now + 60 {
            return
        }

        guard let url = URL(string: "\(baseUrl)/api/watch/session") else { return }
        let body = Data("{\"token\":\"\"}".utf8)
        let headers = [
            "Content-Type": "application/json; charset=utf-8",
            "Referer": "\(baseUrl)/",
            "Origin": baseUrl
        ]

        let (data, response) = try await NetworkClient.shared.post(url: url, body: body, headers: headers)
        guard response.statusCode == 200 else {
            throw NSError(domain: "AniChanClient", code: response.statusCode, userInfo: [NSLocalizedDescriptionKey: "Gagal sesi AniChan"])
        }

        if let setCookie = response.allHeaderFields["Set-Cookie"] as? String {
            for part in setCookie.components(separatedBy: ",") {
                let first = part.components(separatedBy: ";").first ?? ""
                let kv = first.split(separator: "=", maxSplits: 1)
                if kv.count == 2 {
                    cachedCookies[String(kv[0]).trimmingCharacters(in: .whitespaces)] = String(kv[1]).trimmingCharacters(in: .whitespaces)
                }
            }
        }

        if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] {
            cachedTokenN = json["n"] as? String ?? ""
            tokenExpiresEpoch = json["exp"] as? TimeInterval ?? (now + 7200)
        }
    }

    private func decryptPayload(raw: [String: Any], tokenN: String) -> [String: Any] {
        guard (raw["v"] as? Int) == 1,
              let ivB64 = raw["i"] as? String,
              let dataB64 = raw["d"] as? String,
              let ivData = Data(base64Encoded: ivB64),
              let combined = Data(base64Encoded: dataB64),
              combined.count > 16,
              let rawO = Data(base64Encoded: activeO),
              let rawB = Data(base64Encoded: activeB) else {
            return raw
        }

        let hmacKeyBytes = zip(rawO, rawB).map { $0 ^ $1 }
        let hmacKey = SymmetricKey(data: Data(hmacKeyBytes))
        let aesKeyData = HMAC<SHA256>.authenticationCode(for: Data(tokenN.utf8), using: hmacKey)
        let aesKey = SymmetricKey(data: Data(aesKeyData))

        let ciphertext = combined.dropLast(16)
        let tag = combined.suffix(16)

        do {
            let nonce = try AES.GCM.Nonce(data: ivData)
            let sealedBox = try AES.GCM.SealedBox(nonce: nonce, ciphertext: ciphertext, tag: tag)
            let plainData = try AES.GCM.open(sealedBox, using: aesKey)
            if let json = try JSONSerialization.jsonObject(with: plainData) as? [String: Any] {
                return json
            }
        } catch {
            // Fallback ke raw jika dekripsi gagal
        }
        return raw
    }

    private func serverScore(_ name: String) -> Int {
        let n = name.lowercased()
        if n.contains("anikoto") { return 0 }
        if n.contains("megaplay_hd1") { return 1 }
        if n.contains("megaplay_hd2") { return 2 }
        if n.contains("aniwatch") || n.contains("zoko") { return 3 }
        return 4
    }
}
