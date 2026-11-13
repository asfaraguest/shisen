import SwiftUI
import UIKit
import AVFoundation

private struct PlayerLayerView: UIViewRepresentable {
    let player: AVPlayer

    func makeUIView(context: Context) -> PlayerContainerView {
        let view = PlayerContainerView()
        view.playerLayer.player = player
        view.playerLayer.videoGravity = .resizeAspect
        return view
    }

    func updateUIView(_ uiView: PlayerContainerView, context: Context) {
        if uiView.playerLayer.player !== player {
            uiView.playerLayer.player = player
        }
    }

    final class PlayerContainerView: UIView {
        override class var layerClass: AnyClass { AVPlayerLayer.self }
        var playerLayer: AVPlayerLayer { layer as! AVPlayerLayer }
    }
}

public struct PlayerView: View {
    public let anime: Anime
    public let episode: Episode

    @Environment(\.dismiss) private var dismiss
    @StateObject private var subManager = SubtitleManager.shared
    @AppStorage("auto_skip_intro") private var autoSkipIntro: Bool = true
    @AppStorage("auto_skip_outro") private var autoSkipOutro: Bool = true

    @State private var player: AVPlayer? = nil
    @State private var timeObserverToken: Any? = nil
    @State private var streamSource: StreamSource? = nil
    @State private var isLoading: Bool = true
    @State private var errorMessage: String? = nil
    @State private var isPlaying: Bool = true
    @State private var currentTime: Double = 0
    @State private var duration: Double = 0
    @State private var showControls: Bool = true
    @State private var showSubtitleSheet: Bool = false
    @State private var skipIntervals: [AniSkipInterval] = []
    @State private var activeSkip: AniSkipInterval? = nil
    @State private var lastAutoSkippedKey: String? = nil
    
    public init(anime: Anime, episode: Episode) {
        self.anime = anime
        self.episode = episode
    }
    
    public var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            if let player = player {
                PlayerLayerView(player: player)
                    .ignoresSafeArea()
                    .contentShape(Rectangle())
                    .onTapGesture {
                        withAnimation { showControls.toggle() }
                    }
            }
            
            // Subtitle Display Overlay
            if let cueText = subManager.currentCueText, subManager.currentMode != .off {
                VStack {
                    Spacer()
                    Text(cueText)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 6)
                        .background(Color.black.opacity(0.75))
                        .cornerRadius(8)
                        .padding(.bottom, showControls ? 80 : 36)
                        .padding(.horizontal, 24)
                }
                .transition(.opacity)
            }
            
            // AniSkip Button
            if let skip = activeSkip {
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        Button(action: {
                            skipTo(seconds: skip.end)
                        }) {
                            HStack(spacing: 6) {
                                Image(systemName: "forward.fill")
                                Text("Lewati \(skip.type.uppercased())")
                                    .fontWeight(.bold)
                            }
                            .font(.system(size: 14))
                            .foregroundColor(.black)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 10)
                            .background(Color.white)
                            .cornerRadius(20)
                            .shadow(radius: 6)
                        }
                        .padding(.trailing, 24)
                        .padding(.bottom, showControls ? 80 : 36)
                    }
                }
            }
            
            // Overlay Controls
            if showControls {
                VStack {
                    // Top Bar
                    HStack {
                        Button(action: {
                            player?.pause()
                            dismiss()
                        }) {
                            Image(systemName: "xmark")
                                .font(.title3)
                                .foregroundColor(.white)
                                .padding(12)
                                .background(Color.black.opacity(0.6))
                                .clipShape(Circle())
                        }
                        
                        VStack(alignment: .leading, spacing: 2) {
                            Text(anime.displayTitle)
                                .font(.headline)
                                .foregroundColor(.white)
                                .lineLimit(1)
                            Text(episode.title ?? "Episode \(episode.number)")
                                .font(.caption)
                                .foregroundColor(ShisenTheme.textSecondary)
                        }
                        .padding(.leading, 8)
                        
                        Spacer()
                        
                        Button(action: {
                            showSubtitleSheet = true
                        }) {
                            HStack(spacing: 4) {
                                Image(systemName: "captions.bubble.fill")
                                Text(subManager.currentMode.rawValue)
                                    .font(.caption2)
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(Color.black.opacity(0.6))
                            .cornerRadius(14)
                        }
                    }
                    .padding()
                    
                    Spacer()
                    
                    // Center Play/Pause / Loading
                    if isLoading {
                        ProgressView().tint(ShisenTheme.accent)
                            .scaleEffect(1.5)
                    } else if let err = errorMessage {
                        VStack(spacing: 8) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .font(.largeTitle)
                                .foregroundColor(ShisenTheme.accentRed)
                            Text(err)
                                .font(.subheadline)
                                .foregroundColor(.white)
                        }
                        .padding()
                        .background(Color.black.opacity(0.8))
                        .cornerRadius(12)
                    } else if let player = player {
                        Button(action: {
                            if isPlaying {
                                player.pause()
                                isPlaying = false
                            } else {
                                player.play()
                                isPlaying = true
                            }
                        }) {
                            Image(systemName: isPlaying ? "pause.circle.fill" : "play.circle.fill")
                                .font(.system(size: 56))
                                .foregroundColor(.white.opacity(0.9))
                                .shadow(radius: 8)
                        }
                    }
                    
                    Spacer()
                    
                    // Bottom Seek Bar
                    VStack(spacing: 6) {
                        Slider(value: Binding(
                            get: { currentTime },
                            set: { newVal in
                                skipTo(seconds: newVal)
                            }
                        ), in: 0...max(1, duration))
                        .tint(ShisenTheme.accent)
                        
                        HStack {
                            Text(formatTime(currentTime))
                                .font(.caption2)
                                .foregroundColor(ShisenTheme.textSecondary)
                            Spacer()
                            Text(formatTime(duration))
                                .font(.caption2)
                                .foregroundColor(ShisenTheme.textSecondary)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 20)
                }
                .background(
                    LinearGradient(
                        colors: [Color.black.opacity(0.8), .clear, Color.black.opacity(0.8)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
            }
        }
        .sheet(isPresented: $showSubtitleSheet) {
            SubtitleSelectorSheet(streamSource: streamSource)
        }
        .task {
            await initializePlayback()
        }
        .onDisappear {
            if let token = timeObserverToken {
                player?.removeTimeObserver(token)
                timeObserverToken = nil
            }
            player?.pause()
            player = nil
        }
    }

    private func initializePlayback() async {
        isLoading = true
        errorMessage = nil

        do {
            let source = try await StreamResolver.shared.resolve(anime: anime, episode: episode)
            self.streamSource = source

            guard let videoUrl = URL(string: source.url) else {
                errorMessage = "URL video tidak valid"
                isLoading = false
                return
            }

            // Set up AVPlayer with custom headers via AVURLAsset
            let asset = AVURLAsset(url: videoUrl, options: [
                "AVURLAssetHTTPHeaderFieldsKey": source.headers
            ])
            let item = AVPlayerItem(asset: asset)
            let av = AVPlayer(playerItem: item)
            self.player = av

            // Load total duration asynchronously
            if let cmDuration = try? await asset.load(.duration) {
                let sec = CMTimeGetSeconds(cmDuration)
                if sec.isFinite && sec > 0 {
                    self.duration = sec
                }
            }

            // Observe time for subtitles and AniSkip
            self.timeObserverToken = av.addPeriodicTimeObserver(forInterval: CMTime(seconds: 0.25, preferredTimescale: 600), queue: .main) { time in
                let sec = CMTimeGetSeconds(time)
                guard sec.isFinite else { return }
                self.currentTime = sec
                if self.duration <= 0, let itemDur = av.currentItem?.duration {
                    let d = CMTimeGetSeconds(itemDur)
                    if d.isFinite && d > 0 { self.duration = d }
                }
                self.subManager.updatePlaybackPosition(ms: Int64(sec * 1000))

                // Check AniSkip + Auto-skip
                if let match = self.skipIntervals.first(where: { sec >= $0.start && sec <= $0.end }) {
                    let key = "\(match.type)_\(Int(match.start))"
                    let isOp = match.type == "op" || match.type == "mixed-op"
                    let isEd = match.type == "ed"
                    if ((isOp && self.autoSkipIntro) || (isEd && self.autoSkipOutro)) && self.lastAutoSkippedKey != key {
                        self.lastAutoSkippedKey = key
                        self.skipTo(seconds: match.end)
                        self.activeSkip = nil
                    } else {
                        self.activeSkip = match
                    }
                } else {
                    self.activeSkip = nil
                }
            }

            av.play()
            self.isPlaying = true
            self.isLoading = false

            // Load Subtitles via SubtitleManager (Smart Merge ID by default)
            await subManager.loadFromTracks(tracks: source.subtitles, contextTitle: anime.displayTitle, headers: source.headers)

            // Fetch AniSkip using MAL ID
            if let malId = anime.idMal {
                self.skipIntervals = await AniSkipClient.shared.fetchSkipTimes(
                    malId: malId,
                    episodeNumber: episode.number,
                    episodeLength: self.duration > 0 ? self.duration : 1440
                )
            }
        } catch {
            self.errorMessage = error.localizedDescription
            self.isLoading = false
        }
    }
    
    private func skipTo(seconds: Double) {
        player?.seek(to: CMTime(seconds: seconds, preferredTimescale: 600))
    }
    
    private func formatTime(_ sec: Double) -> String {
        let total = Int(sec)
        let m = total / 60
        let s = total % 60
        return String(format: "%02d:%02d", m, s)
    }
}

public struct SubtitleSelectorSheet: View {
    public let streamSource: StreamSource?
    @Environment(\.dismiss) private var dismiss
    @StateObject private var subManager = SubtitleManager.shared
    
    public var body: some View {
        NavigationStack {
            ZStack {
                ShisenTheme.surface.ignoresSafeArea()
                
                List {
                    Section(header: Text("Mode Subtitle").foregroundColor(ShisenTheme.textMuted)) {
                        ForEach(SubtitleMode.allCases) { mode in
                            Button(action: {
                                subManager.setMode(mode)
                                dismiss()
                            }) {
                                HStack {
                                    Text(mode.rawValue)
                                        .foregroundColor(ShisenTheme.textPrimary)
                                    Spacer()
                                    if subManager.currentMode == mode {
                                        Image(systemName: "checkmark")
                                            .foregroundColor(ShisenTheme.accent)
                                    }
                                }
                            }
                        }
                    }
                    
                    if let subs = streamSource?.subtitles, !subs.isEmpty {
                        Section(header: Text("Tersedia Dari Server").foregroundColor(ShisenTheme.textMuted)) {
                            ForEach(subs) { track in
                                Button(action: {
                                    if let url = URL(string: track.url) {
                                        Task {
                                            await subManager.loadRawTrack(url: url, mode: .rawEnglish)
                                            dismiss()
                                        }
                                    }
                                }) {
                                    HStack {
                                        Text(track.label)
                                            .foregroundColor(ShisenTheme.textPrimary)
                                        Spacer()
                                        Text(track.lang.uppercased())
                                            .font(.caption)
                                            .foregroundColor(ShisenTheme.textMuted)
                                    }
                                }
                            }
                        }
                    }
                }
                .scrollContentBackground(.hidden)
            }
            .navigationTitle("Pilihan Subtitle")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Tutup") { dismiss() }
                        .foregroundColor(ShisenTheme.accent)
                }
            }
        }
        .presentationDetents([.medium])
    }
}
