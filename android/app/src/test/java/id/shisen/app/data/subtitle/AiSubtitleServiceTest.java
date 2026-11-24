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

        assertEquals(AiSubtitleService.Provider.CEREBRAS,
                AiSubtitleService.Provider.detect("csk-abc1234567890xyz"));

        assertEquals(AiSubtitleService.Provider.OPENAI,
                AiSubtitleService.Provider.detect("sk-proj-abcdef123456"));

        assertEquals(AiSubtitleService.Provider.OPENAI,
                AiSubtitleService.Provider.detect("sk-standard-token-xyz"));

        assertEquals(AiSubtitleService.Provider.OPENROUTER,
                AiSubtitleService.Provider.detect("sk-or-v1-abcdef1234567890"));

        assertEquals(AiSubtitleService.Provider.NVIDIA_NIM,
                AiSubtitleService.Provider.detect("nvapi-secret-key-123456789"));

        assertEquals(AiSubtitleService.Provider.NVIDIA_NIM,
                AiSubtitleService.Provider.detect("nvapi_secret_token_123"));

        assertEquals(AiSubtitleService.Provider.NVIDIA_NIM,
                AiSubtitleService.Provider.detect("http://192.168.1.50:11434"));

        assertEquals(AiSubtitleService.Provider.NVIDIA_NIM,
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
        assertTrue(body.contains("openai/gpt-oss-120b"));
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
        assertEquals("gemini-3.8-flash", AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.GEMINI));
        assertEquals("openai/gpt-oss-120b", AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.GROQ));
        assertEquals("gpt-4o-mini", AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.OPENAI));
        assertEquals("google/gemma-4-31b-it:free", AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.OPENROUTER));
        assertEquals("meta/llama-3.3-70b-instruct", AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.NVIDIA_NIM));

        java.util.List<String> geminiModels = AiSubtitleService.getAvailableModels(AiSubtitleService.Provider.GEMINI);
        assertTrue(geminiModels.size() >= 3);
        assertEquals("gemini-3.8-flash", geminiModels.get(0));
        assertTrue(geminiModels.contains("gemini-4-argon"));

        java.util.List<String> groqModels = AiSubtitleService.getAvailableModels(AiSubtitleService.Provider.GROQ);
        assertTrue(groqModels.size() >= 3);
        assertEquals("openai/gpt-oss-120b", groqModels.get(0));
        assertTrue(groqModels.contains("openai/gpt-oss-20b"));

        java.util.List<String> nvidiaModels = AiSubtitleService.getAvailableModels(AiSubtitleService.Provider.NVIDIA_NIM);
        assertEquals("meta/llama-3.3-70b-instruct", nvidiaModels.get(0));
        assertTrue(nvidiaModels.contains("deepseek-ai/deepseek-r1"));

        // Effective model fallback when custom model is null or empty
        assertEquals("gemini-3.8-flash", AiSubtitleService.getEffectiveModel(AiSubtitleService.Provider.GEMINI, ""));
        assertEquals("custom-gemini-model", AiSubtitleService.getEffectiveModel(AiSubtitleService.Provider.GEMINI, "custom-gemini-model"));

        // Active models adapted from Tatap
        assertEquals("llama-3.3-70b-versatile", AiSubtitleService.getEffectiveModel(AiSubtitleService.Provider.GROQ, "llama-3.3-70b-versatile"));
        assertEquals("google/gemini-2.0-flash-exp:free", AiSubtitleService.getEffectiveModel(AiSubtitleService.Provider.OPENROUTER, "google/gemini-2.0-flash-exp:free"));

        // Deprecated model auto-migration
        assertEquals("gemini-3.8-flash", AiSubtitleService.getEffectiveModel(AiSubtitleService.Provider.GEMINI, "gemini-2.5-pro"));
        assertEquals("openai/gpt-oss-120b", AiSubtitleService.getEffectiveModel(AiSubtitleService.Provider.GROQ, "mixtral-8x7b-32768"));
    }

    @Test
    public void testNvidiaNimSubtitleTranslationWithMockWebServer() throws Exception {
        String mockResponse = "{\n" +
                "  \"choices\": [\n" +
                "    {\n" +
                "      \"message\": {\n" +
                "        \"role\": \"assistant\",\n" +
                "        \"content\": \"Terjemahan NVIDIA NIM berhasil.\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponse));

        String key = "nvapi-test-token-123456789";
        String translated = service.callNvidiaNim("English text", key);

        assertEquals("Terjemahan NVIDIA NIM berhasil.", translated);

        RecordedRequest req = server.takeRequest();
        assertEquals("POST", req.getMethod());
        assertEquals("Bearer " + key, req.getHeader("Authorization"));
        String body = req.getBody().readUtf8();
        assertTrue(body.contains("meta/llama-3.3-70b-instruct"));
        assertTrue(body.contains("English text"));
    }

    @Test
    public void testGemmaModelBypassesSystemRole() throws Exception {
        String mockResponse = "{\n" +
                "  \"choices\": [\n" +
                "    {\n" +
                "      \"message\": {\n" +
                "        \"role\": \"assistant\",\n" +
                "        \"content\": \"Hasil terjemahan gemma.\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponse));

        String key = "gsk_test_key";
        String customModel = "google/gemma-4-31b-it:free";
        String translated = service.callGroq("Test gemma phrase", key, "Custom Fansub Rule", customModel);

        assertEquals("Hasil terjemahan gemma.", translated);

        RecordedRequest req = server.takeRequest();
        String body = req.getBody().readUtf8();
        assertTrue(body.contains("\"model\":\"google/gemma-4-31b-it:free\""));
        assertFalse("Gemma payload must not contain system role message", body.contains("\"role\":\"system\""));
        assertTrue("Gemma payload must merge instructions into user message", body.contains("Custom Fansub Rule"));
        assertTrue(body.contains("Test gemma phrase"));
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
        String customModel = "models/gemma-2-9b-it";
        String translated = service.callGemini("Custom model test", key, null, customModel);

        assertEquals("Terjemahan model kustom.", translated);

        RecordedRequest req = server.takeRequest();
        assertTrue(req.getPath().contains("/models/gemma-2-9b-it:generateContent"));
        assertFalse(req.getPath().contains("/models/models/"));
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
        String customModel = "qwen/qwen3.8-27b";
        String translated = service.callGroq("Test phrase", key, null, customModel);

        assertEquals("Hasil terjemahan mixtral.", translated);

        RecordedRequest req = server.takeRequest();
        String body = req.getBody().readUtf8();
        assertTrue(body.contains("\"model\":\"qwen/qwen3.8-27b\""));
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

    @Test
    public void testModelCooldownSkipsFailingModelOnNextCall() {
        AiSubtitleService.clearModelCooldowns();
        String primary = "gemini-3.8-flash";
        java.util.List<String> initialChain = AiSubtitleService.buildModelRotationChain(
                AiSubtitleService.Provider.GEMINI, primary);
        assertEquals(primary, initialChain.get(0));

        AiSubtitleService.markModelCooldown(primary, 60_000L);
        assertTrue(AiSubtitleService.isModelInCooldown(primary));

        java.util.List<String> rotatedChain = AiSubtitleService.buildModelRotationChain(
                AiSubtitleService.Provider.GEMINI, primary);
        assertNotEquals(primary, rotatedChain.get(0));
        assertEquals(primary, rotatedChain.get(rotatedChain.size() - 1));
        AiSubtitleService.clearModelCooldowns();
    }

    @Test
    public void testRefineWithTlcAsyncUsesTatapFullPromptRetriesMissingAndRotatesOn503() throws Exception {
        AiSubtitleService.clearModelCooldowns();
        // Simulasikan model pertama gagal 503, model cadangan hanya balas baris 1 lalu retry balas baris 2
        server.enqueue(new MockResponse().setResponseCode(503).setBody("Service Unavailable"));
        String partialBody = "{\n" +
                "  \"choices\": [{\"message\": {\"role\": \"assistant\", \"content\": \"1|Yang Mulia, pasukan sudah bersiap.\"}}]\n" +
                "}";
        String retryBody = "{\n" +
                "  \"choices\": [{\"message\": {\"role\": \"assistant\", \"content\": \"2|Ayo kita berangkat!\"}}]\n" +
                "}";
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(partialBody));
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(retryBody));

        java.util.List<SubtitleCue> en = java.util.Arrays.asList(
                new SubtitleCue(0, 2000, "Your Majesty, the troops are ready."),
                new SubtitleCue(2000, 4000, "..."),
                new SubtitleCue(4000, 6000, "Let's go!")
        );
        java.util.List<SubtitleCue> draft = java.util.Arrays.asList(
                new SubtitleCue(0, 2000, "Yang Mulia, pasukan udah siap."),
                new SubtitleCue(2000, 4000, "..."),
                new SubtitleCue(4000, 6000, "Ayo pergi!")
        );

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final java.util.List<SubtitleCue>[] out = new java.util.List[1];
        final String[] errOut = new String[1];
        final int[] lastProgress = new int[]{-1, -1};

        service.refineWithTlcAsync(null, "test-ep-1", en, draft, null, "gemini-3.8-flash", "AIzaSyTestKey", true, null,
                new AiSubtitleService.TranslateCuesCallback() {
                    @Override public void onProgress(int progress, int total) {
                        lastProgress[0] = progress;
                        lastProgress[1] = total;
                    }
                    @Override public void onSuccess(java.util.List<SubtitleCue> cues) {
                        out[0] = cues;
                        latch.countDown();
                    }
                    @Override public void onError(String error) {
                        errOut[0] = error;
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(10, java.util.concurrent.TimeUnit.SECONDS));
        assertNull("Expected no error, got: " + errOut[0], errOut[0]);
        assertEquals(3, lastProgress[0]);
        assertEquals(3, lastProgress[1]);
        assertNotNull(out[0]);
        assertEquals(3, out[0].size());
        assertEquals("Yang Mulia, pasukan sudah bersiap.", out[0].get(0).text);
        assertEquals("...", out[0].get(1).text);
        assertEquals("Ayo kita berangkat!", out[0].get(2).text);
        assertTrue(AiSubtitleService.isModelInCooldown("gemini-3.8-flash"));

        // Pastikan request memakai DEFAULT_FANSUB_PROMPT_ID & format N|English murni (tanpa || draf_gtx)
        RecordedRequest req1 = server.takeRequest();
        String body1 = req1.getBody().readUtf8();
        assertTrue(body1.contains("penerjemah fansub anime Indonesia berpengalaman"));
        assertTrue(body1.contains("1|Your Majesty, the troops are ready."));
        assertFalse(body1.contains("||"));
        AiSubtitleService.clearModelCooldowns();
    }
}
