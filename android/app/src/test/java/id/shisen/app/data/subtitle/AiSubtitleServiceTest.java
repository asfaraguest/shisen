package id.shisen.app.data.subtitle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.*;

public class AiSubtitleServiceTest {

    private MockWebServer server;
    private AiSubtitleService service;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        service = new AiSubtitleService();
        service.setCustomBaseUrl(server.url("").toString().replaceAll("/$", ""));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void testProviderAutoDetectionFromKeyPrefixes() {
        assertEquals(AiSubtitleService.Provider.GEMINI,
                AiSubtitleService.Provider.detect("AIzaSyD-1234567890abcdef"));

        assertEquals(AiSubtitleService.Provider.GROQ,
                AiSubtitleService.Provider.detect("gsk_abc1234567890xyz"));

        assertEquals(AiSubtitleService.Provider.OPENAI,
                AiSubtitleService.Provider.detect("sk-proj-abcdef123456"));

        assertEquals(AiSubtitleService.Provider.OPENAI,
                AiSubtitleService.Provider.detect("sk-standard-token-xyz"));

        assertEquals(AiSubtitleService.Provider.OPENROUTER,
                AiSubtitleService.Provider.detect("sk-or-v1-abcdef1234567890"));

        assertEquals(AiSubtitleService.Provider.OLLAMA,
                AiSubtitleService.Provider.detect("ollama_secret_token_123"));

        assertEquals(AiSubtitleService.Provider.OLLAMA,
                AiSubtitleService.Provider.detect("afd1045dafd1045dafd1045dafd1045d.TYA04secret"));

        assertEquals(AiSubtitleService.Provider.OLLAMA,
                AiSubtitleService.Provider.detect("http://192.168.1.50:11434"));

        assertEquals(AiSubtitleService.Provider.OLLAMA,
                AiSubtitleService.Provider.detect("ollama:http://localhost:11434"));
    }

    @Test
    public void testGeminiSubtitleTranslationWithMockWebServer() throws Exception {
        String mockResponse = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\"text\": \"Selamat datang di dunia Satisfy!\"}\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponse));

        String key = "AIzaSyTestGeminiKey123";
        String translated = service.callGemini("Welcome to the world of Satisfy!", key);

        assertEquals("Selamat datang di dunia Satisfy!", translated);

        RecordedRequest req = server.takeRequest();
        assertEquals("POST", req.getMethod());
        assertFalse(req.getPath().contains("key="));
        assertEquals(key, req.getHeader("x-goog-api-key"));
        assertTrue(req.getBody().readUtf8().contains("Welcome to the world of Satisfy!"));
    }

    @Test
    public void testGroqSubtitleTranslationWithMockWebServer() throws Exception {
        String mockResponse = "{\n" +
                "  \"choices\": [\n" +
                "    {\n" +
                "      \"message\": {\n" +
                "        \"role\": \"assistant\",\n" +
                "        \"content\": \"Sihir Zoltraak telah dilepaskan.\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponse));

        String key = "gsk_groq_production_key_456";
        String translated = service.callGroq("Zoltraak magic has been unleashed.", key);

        assertEquals("Sihir Zoltraak telah dilepaskan.", translated);

        RecordedRequest req = server.takeRequest();
        assertEquals("POST", req.getMethod());
        assertEquals("Bearer " + key, req.getHeader("Authorization"));
        String body = req.getBody().readUtf8();
        assertTrue(body.contains("llama-3.3-70b-versatile"));
        assertTrue(body.contains("Zoltraak magic has been unleashed."));
    }

    @Test
    public void testOpenAiSubtitleTranslationWithMockWebServer() throws Exception {
        String mockResponse = "{\n" +
                "  \"choices\": [\n" +
                "    {\n" +
                "      \"message\": {\n" +
                "        \"role\": \"assistant\",\n" +
                "        \"content\": \"Ayo bergegas ke tujuan berikutnya!\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponse));

        String key = "sk-proj-openai_token_789";
        String translated = service.callOpenAi("Let's hurry to the next destination!", key);

        assertEquals("Ayo bergegas ke tujuan berikutnya!", translated);

        RecordedRequest req = server.takeRequest();
        assertEquals("POST", req.getMethod());
        assertEquals("Bearer " + key, req.getHeader("Authorization"));
        assertTrue(req.getBody().readUtf8().contains("gpt-4o-mini"));
    }

    @Test
    public void testCustomPromptInjectionInGemini() throws Exception {
        String mockResponse = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\"text\": \"Gue cabut duluan ya bro!\"}\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponse));

        String key = "AIzaSyGeminiCustomKey";
        String customPrompt = "Terjemahkan dengan dialek gaul Jakarta anak muda.";
        String translated = service.callGemini("I'm heading out now!", key, customPrompt);

        assertEquals("Gue cabut duluan ya bro!", translated);

        RecordedRequest req = server.takeRequest();
        String body = req.getBody().readUtf8();
        assertTrue(body.contains(customPrompt));
        assertTrue(body.contains("I'm heading out now!"));
    }

    @Test
    public void testCustomPromptInjectionInGroqAndOpenAi() throws Exception {
        String mockResponse = "{\n" +
                "  \"choices\": [\n" +
                "    {\n" +
                "      \"message\": {\n" +
                "        \"role\": \"assistant\",\n" +
                "        \"content\": \"Yang Mulia Putri, pasukan ksatria telah tiba.\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponse));

        String key = "gsk_groq_custom_prompt_test";
        String customPrompt = "Gunakan register bahasa sopan dan formal kerajaan kuno.";
        String translated = service.callGroq("Princess, the knights have arrived.", key, customPrompt);

        assertEquals("Yang Mulia Putri, pasukan ksatria telah tiba.", translated);

        RecordedRequest req = server.takeRequest();
        String body = req.getBody().readUtf8();
        assertTrue(body.contains(customPrompt));
        assertTrue(body.contains("Princess, the knights have arrived."));
    }

    @Test
    public void testDefaultCustomPromptConstant() {
        assertNotNull(AiSubtitleService.DEFAULT_CUSTOM_PROMPT);
        assertTrue(AiSubtitleService.DEFAULT_CUSTOM_PROMPT.toLowerCase().contains("bahasa indonesia"));
        assertTrue(AiSubtitleService.DEFAULT_CUSTOM_PROMPT.contains("fansub"));
        assertTrue(AiSubtitleService.DEFAULT_CUSTOM_PROMPT.contains("honorifik"));
    }

    @Test
    public void testModelSelectorDefaultsAndAvailableList() {
        assertEquals("gemini-2.5-flash", AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.GEMINI));
        assertEquals("llama-3.3-70b-versatile", AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.GROQ));
        assertEquals("gpt-4o-mini", AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.OPENAI));
        assertEquals("google/gemini-2.0-flash-exp:free", AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.OPENROUTER));
        assertEquals("gpt-oss:20b", AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.OLLAMA));

        java.util.List<String> geminiModels = AiSubtitleService.getAvailableModels(AiSubtitleService.Provider.GEMINI);
        assertTrue(geminiModels.size() >= 3);
        assertEquals("gemini-2.5-flash", geminiModels.get(0));

        java.util.List<String> groqModels = AiSubtitleService.getAvailableModels(AiSubtitleService.Provider.GROQ);
        assertTrue(groqModels.size() >= 4);
        assertEquals("llama-3.3-70b-versatile", groqModels.get(0));

        // Effective model fallback when custom model is null or empty
        assertEquals("gemini-2.5-flash", AiSubtitleService.getEffectiveModel(AiSubtitleService.Provider.GEMINI, ""));
        assertEquals("custom-gemini-model", AiSubtitleService.getEffectiveModel(AiSubtitleService.Provider.GEMINI, "custom-gemini-model"));
    }

    @Test
    public void testCustomModelInGeminiRequestPath() throws Exception {
        String mockResponse = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\"text\": \"Terjemahan model kustom.\"}\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponse));

        String key = "AIzaSyTestKey";
        String customModel = "gemini-1.5-pro";
        String translated = service.callGemini("Custom model test", key, null, customModel);

        assertEquals("Terjemahan model kustom.", translated);

        RecordedRequest req = server.takeRequest();
        assertTrue(req.getPath().contains("/models/gemini-1.5-pro:generateContent"));
    }

    @Test
    public void testCustomModelInGroqRequestBody() throws Exception {
        String mockResponse = "{\n" +
                "  \"choices\": [\n" +
                "    {\n" +
                "      \"message\": {\n" +
                "        \"role\": \"assistant\",\n" +
                "        \"content\": \"Hasil terjemahan mixtral.\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponse));

        String key = "gsk_test_key";
        String customModel = "mixtral-8x7b-32768";
        String translated = service.callGroq("Test phrase", key, null, customModel);

        assertEquals("Hasil terjemahan mixtral.", translated);

        RecordedRequest req = server.takeRequest();
        String body = req.getBody().readUtf8();
        assertTrue(body.contains("\"model\":\"mixtral-8x7b-32768\""));
    }

    @Test
    public void testPrepLineTokenization() {
        assertEquals("Hello <br> World", AiSubtitleService.prepLine("Hello\nWorld"));
        assertEquals("First line <br> Second line", AiSubtitleService.prepLine("First line\r\nSecond line\n"));
        assertEquals("", AiSubtitleService.prepLine(null));
        assertEquals("", AiSubtitleService.prepLine("   \n   "));
    }

    @Test
    public void testParseIdLinesWithThinkTagsAndMarkdown() {
        String llmOutput = "<think>\nThinking about translation...\n</think>\n" +
                "```text\n" +
                "1|Yang Mulia, prajurit telah tiba.\n" +
                "2|Incaranmu tinggi juga ya?<br>Nibutani, kan?\n" +
                "3|Hello World|Halo Dunia!\n" +
                "```";

        java.util.Map<Integer, String> parsed = AiSubtitleService.parseIdLines(llmOutput, 5);
        assertEquals(3, parsed.size());
        assertEquals("Yang Mulia, prajurit telah tiba.", parsed.get(1));
        assertEquals("Incaranmu tinggi juga ya?\nNibutani, kan?", parsed.get(2));
        assertEquals("Halo Dunia!", parsed.get(3));
    }
}
