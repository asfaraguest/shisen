import SwiftUI

public struct SettingsView: View {
    @AppStorage("ai_provider") private var selectedProvider: String = AiSubtitleService.Provider.cerebras.rawValue
    @AppStorage("ai_api_key") private var apiKey: String = ""
    @AppStorage("ai_selected_model") private var selectedModel: String = ""
    @AppStorage("auto_skip_intro") private var autoSkipIntro: Bool = true
    @AppStorage("auto_skip_outro") private var autoSkipOutro: Bool = true
    
    public init() {}
    
    private var currentProviderEnum: AiSubtitleService.Provider {
        AiSubtitleService.Provider(rawValue: selectedProvider) ?? .cerebras
    }
    
    public var body: some View {
        ZStack {
            ShisenTheme.background.ignoresSafeArea()
            
            Form {
                // Section: AI Subtitle
                Section(header: Text("AI Subtitle & Terjemahan").foregroundColor(ShisenTheme.textMuted)) {
                    Picker("Provider", selection: $selectedProvider) {
                        ForEach(AiSubtitleService.Provider.allCases) { provider in
                            Text(provider.rawValue).tag(provider.rawValue)
                        }
                    }
                    .foregroundColor(ShisenTheme.textPrimary)
                    .onChange(of: selectedProvider) { _ in
                        selectedModel = AiSubtitleService.getDefaultModel(for: currentProviderEnum)
                    }
                    
                    SecureField("API Key (\(currentProviderEnum.rawValue))", text: $apiKey)
                        .foregroundColor(ShisenTheme.textPrimary)
                    
                    Picker("Model", selection: $selectedModel) {
                        let models = AiSubtitleService.getAvailableModels(for: currentProviderEnum)
                        ForEach(models, id: \.self) { model in
                            Text(model).tag(model)
                        }
                    }
                    .foregroundColor(ShisenTheme.textPrimary)
                }
                .listRowBackground(ShisenTheme.surface)
                
                // Section: Playback
                Section(header: Text("Pemutaran Video").foregroundColor(ShisenTheme.textMuted)) {
                    Toggle("Otomatis Lewati Opening (AniSkip)", isOn: $autoSkipIntro)
                        .tint(ShisenTheme.accent)
                    Toggle("Otomatis Lewati Ending (AniSkip)", isOn: $autoSkipOutro)
                        .tint(ShisenTheme.accent)
                }
                .listRowBackground(ShisenTheme.surface)
                
                // Section: About
                Section(header: Text("Tentang Aplikasi").foregroundColor(ShisenTheme.textMuted)) {
                    HStack {
                        Text("Versi")
                            .foregroundColor(ShisenTheme.textPrimary)
                        Spacer()
                        Text("v1.1.1 (iOS Edition)")
                            .foregroundColor(ShisenTheme.textMuted)
                    }
                    HStack {
                        Text("Arsitektur")
                            .foregroundColor(ShisenTheme.textPrimary)
                        Spacer()
                        Text("Pure Native Swift & SwiftUI")
                            .foregroundColor(ShisenTheme.textMuted)
                    }
                    HStack {
                        Text("Tipe Akun")
                            .foregroundColor(ShisenTheme.textPrimary)
                        Spacer()
                        Text("Guest-Only (Bebas Iklan)")
                            .foregroundColor(ShisenTheme.accentCyan)
                    }
                }
                .listRowBackground(ShisenTheme.surface)
            }
            .scrollContentBackground(.hidden)
        }
        .navigationTitle("Pengaturan")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            if selectedModel.isEmpty {
                selectedModel = AiSubtitleService.getDefaultModel(for: currentProviderEnum)
            }
        }
    }
}
