import SwiftUI

public struct HomeView: View {
    @State private var trending: [Anime] = []
    @State private var popular: [Anime] = []
    @State private var searchResults: [Anime] = []
    @State private var searchText: String = ""
    @State private var isLoading: Bool = true
    @State private var errorMessage: String? = nil
    @State private var selectedAnime: Anime? = nil
    
    public init() {}
    
    public var body: some View {
        NavigationStack {
            ZStack {
                ShisenTheme.background.ignoresSafeArea()
                
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        // Header
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("視線 • SHISEN")
                                    .font(.system(size: 24, weight: .black, design: .rounded))
                                    .foregroundStyle(
                                        LinearGradient(
                                            colors: [ShisenTheme.accent, ShisenTheme.accentCyan],
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        )
                                    )
                                Text("Anime Streaming Bebas Iklan")
                                    .font(.caption)
                                    .foregroundColor(ShisenTheme.textMuted)
                            }
                            Spacer()
                            NavigationLink(destination: SettingsView()) {
                                Image(systemName: "gearshape.fill")
                                    .font(.title3)
                                    .foregroundColor(ShisenTheme.textSecondary)
                                    .padding(10)
                                    .background(ShisenTheme.surfaceElevated)
                                    .clipShape(Circle())
                            }
                        }
                        .padding(.horizontal)
                        .padding(.top, 8)
                        
                        // Search Bar
                        HStack {
                            Image(systemName: "magnifyingglass")
                                .foregroundColor(ShisenTheme.textMuted)
                            TextField("Cari anime...", text: $searchText)
                                .foregroundColor(ShisenTheme.textPrimary)
                                .onSubmit {
                                    performSearch()
                                }
                            if !searchText.isEmpty {
                                Button(action: {
                                    searchText = ""
                                    searchResults = []
                                }) {
                                    Image(systemName: "xmark.circle.fill")
                                        .foregroundColor(ShisenTheme.textMuted)
                                }
                            }
                        }
                        .padding(12)
                        .background(ShisenTheme.surfaceElevated)
                        .cornerRadius(12)
                        .padding(.horizontal)
                        
                        if !searchResults.isEmpty {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Hasil Pencarian")
                                    .font(.headline)
                                    .foregroundColor(ShisenTheme.textPrimary)
                                    .padding(.horizontal)
                                
                                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                                    ForEach(searchResults) { anime in
                                        NavigationLink(value: anime) {
                                            AnimeCardContent(anime: anime)
                                        }
                                        .buttonStyle(.plain)
                                    }
                                }
                                .padding(.horizontal)
                            }
                        } else if isLoading {
                            HStack {
                                Spacer()
                                ProgressView()
                                    .tint(ShisenTheme.accent)
                                Spacer()
                            }
                            .padding(.top, 40)
                        } else if let err = errorMessage, trending.isEmpty && popular.isEmpty {
                            VStack(spacing: 12) {
                                Image(systemName: "wifi.exclamationmark")
                                    .font(.largeTitle)
                                    .foregroundColor(ShisenTheme.accentRed)
                                Text(err)
                                    .font(.subheadline)
                                    .foregroundColor(ShisenTheme.textSecondary)
                                    .multilineTextAlignment(.center)
                                Button("Coba Lagi") {
                                    Task { await loadHomeData() }
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(ShisenTheme.accent)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.top, 40)
                            .padding(.horizontal)
                        } else {
                            // Trending Section
                            if !trending.isEmpty {
                                VStack(alignment: .leading, spacing: 12) {
                                    Text("Sedang Trending")
                                        .font(.headline)
                                        .foregroundColor(ShisenTheme.textPrimary)
                                        .padding(.horizontal)
                                    
                                    ScrollView(.horizontal, showsIndicators: false) {
                                        LazyHStack(spacing: 14) {
                                            ForEach(trending) { anime in
                                                NavigationLink(value: anime) {
                                                    AnimeCardContent(anime: anime, isHorizontal: true)
                                                }
                                                .buttonStyle(.plain)
                                            }
                                        }
                                        .padding(.horizontal)
                                    }
                                }
                            }
                            
                            // Popular Section
                            if !popular.isEmpty {
                                VStack(alignment: .leading, spacing: 12) {
                                    Text("Paling Populer")
                                        .font(.headline)
                                        .foregroundColor(ShisenTheme.textPrimary)
                                        .padding(.horizontal)
                                    
                                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                                        ForEach(popular) { anime in
                                            NavigationLink(value: anime) {
                                                AnimeCardContent(anime: anime)
                                            }
                                            .buttonStyle(.plain)
                                        }
                                    }
                                    .padding(.horizontal)
                                }
                            }
                        }
                    }
                    .padding(.bottom, 32)
                }
            }
            .navigationDestination(for: Anime.self) { anime in
                DetailView(anime: anime)
            }
            .task {
                await loadHomeData()
            }
        }
    }
    
    private func loadHomeData() async {
        isLoading = true
        errorMessage = nil
        do {
            async let trendingTask = AniListService.shared.fetchTrending(page: 1, perPage: 10)
            async let popularTask = AniListService.shared.fetchPopular(page: 1, perPage: 16)
            let (t, p) = try await (trendingTask, popularTask)
            self.trending = t
            self.popular = p
        } catch {
            self.errorMessage = "Gagal memuat katalog anime. Periksa koneksi internet."
        }
        isLoading = false
    }
    
    private func performSearch() {
        guard !searchText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        Task {
            isLoading = true
            do {
                self.searchResults = try await AniListService.shared.searchAnime(query: searchText)
            } catch {
                self.searchResults = []
            }
            isLoading = false
        }
    }
}

public struct AnimeCardContent: View {
    public let anime: Anime
    public var isHorizontal: Bool = false
    
    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ZStack(alignment: .topTrailing) {
                AsyncImage(url: URL(string: anime.coverImage ?? "")) { phase in
                    if let image = phase.image {
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    } else {
                        Rectangle()
                            .fill(ShisenTheme.surfaceElevated)
                    }
                }
                .frame(width: isHorizontal ? 140 : nil, height: isHorizontal ? 200 : 230)
                .clipped()
                .cornerRadius(12)
                
                if let score = anime.averageScore {
                    Text("\(score)%")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 3)
                        .background(Color.black.opacity(0.75))
                        .cornerRadius(6)
                        .padding(6)
                }
            }
            
            Text(anime.displayTitle)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(ShisenTheme.textPrimary)
                .lineLimit(2)
                .frame(width: isHorizontal ? 140 : nil, alignment: .leading)
        }
    }
}
