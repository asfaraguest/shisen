import Foundation

public final class AiSubtitleService {
    public static let shared = AiSubtitleService()
    
    public enum Provider: String, CaseIterable, Identifiable {
        case cerebras = "Cerebras (Fast)"
        case gemini = "Google Gemini"
        case groq = "Groq Cloud"
        case nvidiaNim = "NVIDIA NIM"
        case openRouter = "OpenRouter"
        case openAi = "OpenAI"
        
        public var id: String { rawValue }
        
        public static func detect(key: String) -> Provider {
            let trimmed = key.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed.hasPrefix("csk-") { return .cerebras }
            if trimmed.hasPrefix("gsk_") { return .groq }
            if trimmed.hasPrefix("AQ.") || trimmed.hasPrefix("AIza") || trimmed.hasPrefix("AQ") { return .gemini }
            if trimmed.hasPrefix("sk-or-") { return .openRouter }
            if trimmed.hasPrefix("nvapi-") || trimmed.hasPrefix("nvapi_") { return .nvidiaNim }
            if trimmed.hasPrefix("sk-") { return .openAi }
            return .gemini
        }
    }
    
    public static func getDefaultModel(for provider: Provider) -> String {
        switch provider {
        case .cerebras: return "llama3.3-70b"
        case .gemini: return "gemini-3.8-flash"
        case .groq: return "openai/gpt-oss-120b"
        case .nvidiaNim: return "meta/llama-3.3-70b-instruct"
        case .openRouter: return "google/gemma-4-31b-it:free"
        case .openAi: return "gpt-4o-mini"
        }
    }
    
    public static func getAvailableModels(for provider: Provider) -> [String] {
        switch provider {
        case .cerebras:
            return ["llama3.3-70b", "llama3.1-8b"]
        case .gemini:
            return [
                "gemini-3.8-flash", "gemini-4-argon", "gemini-2.0-flash",
                "gemini-3.1-flash-lite", "gemini-3.5-flash", "gemini-3.5-flash-lite", "gemini-flash-lite-latest"
            ]
        case .groq:
            return [
                "openai/gpt-oss-120b", "openai/gpt-oss-20b", "qwen/qwen3.8-27b",
                "llama-3.3-70b-versatile", "llama-3.1-8b-instant", "gemma2-9b-it"
            ]
        case .nvidiaNim:
            return ["meta/llama-3.3-70b-instruct", "nvidia/llama-3.1-nemotron-70b-instruct", "deepseek-ai/deepseek-r1"]
        case .openRouter:
            return [
                "google/gemma-4-31b-it:free", "google/gemma-4-26b-a4b-it:free", "nvidia/nemotron-3.5-lightning:free",
                "nvidia/nemotron-3-ultra-550b-a55b:free", "google/gemini-2.0-flash-exp:free",
                "meta-llama/llama-3.3-70b-instruct:free", "deepseek/deepseek-r1:free",
                "qwen/qwen-2.5-coder-32b-instruct:free", "mistralai/mistral-7b-instruct:free"
            ]
        case .openAi:
            return ["gpt-4o-mini", "gpt-4o", "o3-mini"]
        }
    }
    
    private init() {}
    
    public func translateBatch(
        cues: [String],
        provider: Provider,
        apiKey: String,
        model: String? = nil,
        contextTitle: String = "Anime"
    ) async throws -> [String] {
        guard !cues.isEmpty else { return [] }
        guard !apiKey.isEmpty else {
            return await GtxTranslator.shared.translateBatch(texts: cues, targetLang: "id")
        }
        
        let selectedModel = model?.isEmpty == false ? model! : Self.getDefaultModel(for: provider)
        let prompt = """
        You are an expert anime subtitle translator from Japanese/English to natural, engaging Indonesian (Fansub style).
        Title: \(contextTitle)
        
        Translate these subtitle lines into Indonesian while adhering strictly to:
        1. Keep casual, expressive tone suitable for anime dialog.
        2. Preserve line count exactly. Each input line must correspond to exactly one output line.
        3. Do NOT add notes, explanations, or numbers. Return ONLY translated lines separated by newlines.
        
        Lines to translate:
        \(cues.joined(separator: "\n"))
        """
        
        return try await executeChatCompletion(
            provider: provider,
            apiKey: apiKey,
            model: selectedModel,
            prompt: prompt,
            expectedCount: cues.count
        )
    }
    
    private func executeChatCompletion(
        provider: Provider,
        apiKey: String,
        model: String,
        prompt: String,
        expectedCount: Int
    ) async throws -> [String] {
        let endpointUrl: URL
        switch provider {
        case .cerebras:
            endpointUrl = URL(string: "https://api.cerebras.ai/v1/chat/completions")!
        case .groq:
            endpointUrl = URL(string: "https://api.groq.com/openai/v1/chat/completions")!
        case .nvidiaNim:
            endpointUrl = URL(string: "https://integrate.api.nvidia.com/v1/chat/completions")!
        case .openAi:
            endpointUrl = URL(string: "https://api.openai.com/v1/chat/completions")!
        case .openRouter:
            endpointUrl = URL(string: "https://openrouter.ai/api/v1/chat/completions")!
        case .gemini:
            endpointUrl = URL(string: "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions")!
        }
        
        let body: [String: Any] = [
            "model": model,
            "messages": [
                ["role": "system", "content": "You are a professional anime subtitle translator."],
                ["role": "user", "content": prompt]
            ],
            "temperature": 0.3
        ]
        
        let jsonData = try JSONSerialization.data(withJSONObject: body)
        var headers: [String: String] = [
            "Content-Type": "application/json",
            "Authorization": "Bearer \(apiKey)"
        ]
        if provider == .openRouter {
            headers["HTTP-Referer"] = "https://shisen.app"
            headers["X-Title"] = "Shisen iOS"
        }
        
        let (data, response) = try await NetworkClient.shared.post(url: endpointUrl, body: jsonData, headers: headers)
        guard response.statusCode == 200 else {
            throw NSError(domain: "AiSubtitleService", code: response.statusCode, userInfo: [NSLocalizedDescriptionKey: "AI Subtitle request failed with code \(response.statusCode)"])
        }
        
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let choices = json["choices"] as? [[String: Any]],
              let firstChoice = choices.first,
              let message = firstChoice["message"] as? [String: Any],
              let content = message["content"] as? String else {
            throw NSError(domain: "AiSubtitleService", code: -1, userInfo: [NSLocalizedDescriptionKey: "Malformed AI response"])
        }
        
        let lines = content.components(separatedBy: .newlines).map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
        if lines.count == expectedCount {
            return lines
        }
        return lines
    }
}
