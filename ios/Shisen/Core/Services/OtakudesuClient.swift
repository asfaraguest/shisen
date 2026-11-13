import Foundation
import CryptoKit

public final class OtakudesuClient {
    public static let shared = OtakudesuClient()
    private let baseUrl = "https://ww2.otakudesu.biz"

    private init() {}

    public func resolveEpisodeStream(animeTitle: String, episodeNumber: Int) async throws -> StreamSource? {
        let cleanTitle = animeTitle.replacingOccurrences(of: "[^\\p{L}\\p{N}\\s-]", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard let encoded = (cleanTitle.isEmpty ? animeTitle : cleanTitle).addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let searchUrl = URL(string: "\(baseUrl)/?s=\(encoded)&post_type=anime") else {
            return nil
        }

        do {
            let (data, response) = try await NetworkClient.shared.get(url: searchUrl, headers: ["Referer": "\(baseUrl)/"])
            guard response.statusCode == 200, let html = String(data: data, encoding: .utf8) else { return nil }

            guard let animeLink = extractFirstMatch(in: html, pattern: "href=[\"'](https://[^\"']*otakudesu[^\"']+/anime/[^\"']+)[\"']"),
                  let detailUrl = URL(string: animeLink) else { return nil }

            let (detailData, detailResp) = try await NetworkClient.shared.get(url: detailUrl, headers: ["Referer": "\(baseUrl)/"])
            guard detailResp.statusCode == 200, let detailHtml = String(data: detailData, encoding: .utf8) else { return nil }

            let epPattern = "href=[\"'](https://[^\"']*otakudesu[^\"']+/episode/[^\"']+)[\"'][^>]*>[^<]*Episode\\s*\(episodeNumber)\\b"
            guard let epLink = extractFirstMatch(in: detailHtml, pattern: epPattern)
                    ?? extractFirstMatch(in: detailHtml, pattern: "href=[\"'](https://[^\"']*otakudesu[^\"']+/episode/[^\"']*-episode-\(episodeNumber)[^\"']*)[\"']"),
                  let epUrl = URL(string: epLink) else { return nil }

            let (epData, epResp) = try await NetworkClient.shared.get(url: epUrl, headers: ["Referer": "\(baseUrl)/"])
            guard epResp.statusCode == 200, let epHtml = String(data: epData, encoding: .utf8) else { return nil }

            if let iframeSrc = extractFirstMatch(in: epHtml, pattern: "<iframe[^>]+src=[\"']([^\"']+)[\"']"),
               let resolved = await resolveEmbedPlayer(embedUrlStr: iframeSrc) {
                return resolved
            }
        } catch {
            // Biarkan caller menangani error
        }
        return nil
    }

    private func resolveEmbedPlayer(embedUrlStr: String) async -> StreamSource? {
        guard let embedUrl = URL(string: embedUrlStr) else { return nil }
        do {
            let (data, response) = try await NetworkClient.shared.get(url: embedUrl, headers: ["Referer": "\(baseUrl)/"])
            guard response.statusCode == 200, let html = String(data: data, encoding: .utf8) else { return nil }

            // 1. Cek Putarin window.__PX AES-256-GCM
            if let pxJsonStr = extractFirstMatch(in: html, pattern: "window\\.__PX\\s*=\\s*(\\{[^<]+?\\});"),
               let pxData = pxJsonStr.data(using: .utf8),
               let px = try? JSONSerialization.jsonObject(with: pxData) as? [String: Any],
               let nonce = px["n"] as? String,
               let cipherB64 = px["d"] as? String,
               let host = embedUrl.host,
               let scheme = embedUrl.scheme,
               let encNonce = nonce.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
               let keyUrl = URL(string: "\(scheme)://\(host)/api/pk?n=\(encNonce)") {

                let (keyData, keyResp) = try await NetworkClient.shared.get(url: keyUrl, headers: ["Referer": embedUrlStr])
                if keyResp.statusCode == 200,
                   let keyHex = String(data: keyData, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines),
                   keyHex.count >= 64,
                   let keyBytes = hexToData(String(keyHex.prefix(64))),
                   let rawData = Data(base64Encoded: cipherB64),
                   rawData.count >= 28 {

                    let iv = rawData.prefix(12)
                    let bodyAndTag = rawData.dropFirst(12)
                    let ciphertext = bodyAndTag.dropLast(16)
                    let tag = bodyAndTag.suffix(16)

                    let nonceGcm = try AES.GCM.Nonce(data: iv)
                    let sealedBox = try AES.GCM.SealedBox(nonce: nonceGcm, ciphertext: ciphertext, tag: tag)
                    let plain = try AES.GCM.open(sealedBox, using: SymmetricKey(data: keyBytes))

                    if let cfg = try JSONSerialization.jsonObject(with: plain) as? [String: Any] {
                        let fileUrl = (cfg["file"] as? String)
                            ?? (cfg["url"] as? String)
                            ?? ((cfg["sources"] as? [[String: Any]])?.first?["file"] as? String)
                        if let fileUrl, !fileUrl.isEmpty {
                            return StreamSource(
                                url: fileUrl,
                                isHls: fileUrl.contains(".m3u8"),
                                quality: "HD (Hardsub ID)",
                                serverName: "Otakudesu (Putarin)",
                                headers: [
                                    "Referer": embedUrlStr,
                                    "Origin": "\(scheme)://\(host)"
                                ]
                            )
                        }
                    }
                }
            }

            // 2. Direct .m3u8 / .mp4 regex (jangan kembalikan URL HTML iframe)
            if let directUrl = extractFirstMatch(in: html, pattern: "(?:file|source|src|video)\\s*:\\s*[\"']([^\"']+\\.(?:m3u8|mp4)[^\"']*)[\"']") {
                return StreamSource(
                    url: directUrl,
                    isHls: directUrl.contains(".m3u8"),
                    quality: "720p (Hardsub ID)",
                    serverName: "Otakudesu (Tier 3)",
                    headers: ["Referer": embedUrlStr]
                )
            }
        } catch {
            // Embed gagal di-extract
        }
        return nil
    }

    private func hexToData(_ hex: String) -> Data? {
        var data = Data(capacity: hex.count / 2)
        var idx = hex.startIndex
        while idx < hex.endIndex {
            let next = hex.index(idx, offsetBy: 2, limitedBy: hex.endIndex) ?? hex.endIndex
            guard next <= hex.endIndex, let byte = UInt8(hex[idx..<next], radix: 16) else { return nil }
            data.append(byte)
            idx = next
        }
        return data
    }

    private func extractFirstMatch(in text: String, pattern: String) -> String? {
        guard let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]) else { return nil }
        let nsString = text as NSString
        if let match = regex.firstMatch(in: text, range: NSRange(location: 0, length: nsString.length)),
           match.numberOfRanges > 1 {
            return nsString.substring(with: match.range(at: 1))
        }
        return nil
    }
}
