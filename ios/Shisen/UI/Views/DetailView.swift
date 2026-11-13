import SwiftUI

public struct DetailView: View {
    public let anime: Anime
    @State private var episodes: [Episode] = []
    @State private var isLoadingEpisodes: Bool = true
    @State private var selectedEpisode: Episode? = nil
    
    public init(anime: Anime) {
        self.anime = anime
    }
    
    public var body: some View {
        ZStack {
            ShisenTheme.background.ignoresSafeArea()
            
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Banner & Poster
                    ZStack(alignment: .bottomLeading) {
                        AsyncImage(url: URL(string: anime.bannerImage ?? anime.coverImage ?? "")) { phase in
                            if let image = phase.image {
                                image
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                            } else {
                                Rectangle().fill(ShisenTheme.surfaceElevated)
                            }
                        }
                        .frame(height: 220)
                        .clipped()
                        
                        LinearGradient(
                            colors: [.clear, ShisenTheme.background],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        .frame(height: 120)
                        
                        HStack(alignment: .bottom, spacing: 16) {
                            AsyncImage(url: URL(string: anime.coverImage ?? "")) { phase in
                                if let image = phase.image {
                                    image
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                } else {
                                    Rectangle().fill(ShisenTheme.surfaceElevated)
                                }
                            }
                            .frame(width: 100, height: 150)
                            .cornerRadius(10)
                            .shadow(radius: 8)
                            
                            VStack(alignment: .leading, spacing: 6) {
                                Text(anime.displayTitle)
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(ShisenTheme.textPrimary)
                                    .lineLimit(2)
                                
                                if let status = anime.status {
                                    Text(status.capitalized)
                                        .font(.caption)
                                        .foregroundColor(ShisenTheme.accentCyan)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 3)
                                        .background(ShisenTheme.accentCyan.opacity(0.15))
                                        .cornerRadius(6)
                                }
                            }
                        }
                        .padding(.horizontal)
                        .offset(y: 20)
                    }
                    
                    VStack(alignment: .leading, spacing: 16) {
                        // Genres
                        if !anime.genres.isEmpty {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    ForEach(anime.genres, id: \.self) { genre in
                                        Text(genre)
                                            .font(.caption)
                                            .foregroundColor(ShisenTheme.textSecondary)
                                            .padding(.horizontal, 10)
                                            .padding(.vertical, 5)
                                            .background(ShisenTheme.surfaceElevated)
                                            .cornerRadius(8)
                                    }
                                }
                            }
                        }
                        
                        // Synopsis
                        if let desc = anime.description {
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Sinopsis")
                                    .font(.headline)
                                    .foregroundColor(ShisenTheme.textPrimary)
                                Text(desc.replacingOccurrences(of: "<[^>]+>", with: "", options: .regularExpression))
                                    .font(.subheadline)
                                    .foregroundColor(ShisenTheme.textSecondary)
                                    .lineSpacing(4)
                            }
                        }
                        
                        // Episode List
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Text("Daftar Episode")
                                    .font(.headline)
                                    .foregroundColor(ShisenTheme.textPrimary)
                                Spacer()
                                if let epCount = anime.episodes {
                                    Text("\(epCount) Episode")
                                        .font(.caption)
                                        .foregroundColor(ShisenTheme.textMuted)
                                }
                            }
                            
                            if isLoadingEpisodes {
                                HStack {
                                    Spacer()
                                    ProgressView().tint(ShisenTheme.accent)
                                    Spacer()
                                }
                                .padding(.vertical, 20)
                            } else if episodes.isEmpty {
                                Text("Tidak ada episode yang tersedia.")
                                    .font(.subheadline)
                                    .foregroundColor(ShisenTheme.textMuted)
                                    .padding(.vertical, 12)
                            } else {
                                LazyVStack(spacing: 10) {
                                    ForEach(episodes) { ep in
                                        Button(action: {
                                            selectedEpisode = ep
                                        }) {
                                            HStack {
                                                Image(systemName: "play.circle.fill")
                                                    .font(.title3)
                                                    .foregroundColor(ShisenTheme.accent)
                                                Text(ep.title ?? "Episode \(ep.number)")
                                                    .font(.system(size: 14, weight: .medium))
                                                    .foregroundColor(ShisenTheme.textPrimary)
                                                Spacer()
                                                Text(ep.source.uppercased())
                                                    .font(.caption2)
                                                    .foregroundColor(ShisenTheme.textMuted)
                                                    .padding(.horizontal, 6)
                                                    .padding(.vertical, 2)
                                                    .background(ShisenTheme.surfaceElevated)
                                                    .cornerRadius(4)
                                            }
                                            .padding(14)
                                            .background(ShisenTheme.surface)
                                            .cornerRadius(12)
                                        }
                                        .buttonStyle(.plain)
                                    }
                                }
                            }
                        }
                    }
                    .padding(.horizontal)
                    .padding(.top, 24)
                }
                .padding(.bottom, 32)
            }
        }
        .fullScreenCover(item: $selectedEpisode) { ep in
            PlayerView(anime: anime, episode: ep)
        }
        .task {
            await loadEpisodes()
        }
        .navigationBarTitleDisplayMode(.inline)
    }
    
    private func loadEpisodes() async {
        isLoadingEpisodes = true
        // Default: generate standard episode sequence or query HiAnime
        let total = anime.episodes ?? anime.nextAiringEpisode.map { $0 - 1 } ?? 12
        var list: [Episode] = []
        for i in 1...max(1, total) {
            list.append(Episode(number: i, title: "Episode \(i)", source: "hianime", sourceId: "\(anime.id)-\(i)"))
        }
        self.episodes = list
        isLoadingEpisodes = false
    }
}
