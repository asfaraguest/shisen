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
        assertEquals("", AiSubtitleService.DEFAULT_CUSTOM_PROMPT);
        assertNotNull(AiSubtitleService.EMBEDDED_INSTANT_AI_PROMPT_ID);
        assertTrue(AiSubtitleService.EMBEDDED_INSTANT_AI_PROMPT_ID.toLowerCase().contains("indonesia"));
        assertTrue(AiSubtitleService.EMBEDDED_INSTANT_AI_PROMPT_ID.contains("fansub"));
        assertTrue(AiSubtitleService.EMBEDDED_INSTANT_AI_PROMPT_ID.contains("honorifik"));
        assertTrue(AiSubtitleService.EMBEDDED_INSTANT_AI_PROMPT_ID.contains("SEMUA nomor input N"));
        assertTrue(AiSubtitleService.DEFAULT_FANSUB_PROMPT_ID.toLowerCase().contains("bahasa indonesia"));
    }

    @Test
    public void testModelSelectorDefaultsAndAvailableList() {
        assertEquals("gemini-3.1-flash-lite", AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.GEMINI));
        assertEquals("llama-3.3-70b-versatile", AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.GROQ));
        assertEquals("gpt-4o-mini", AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.OPENAI));
        assertEquals("google/gemini-2.0-flash-exp:free", AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.OPENROUTER));
        assertEquals("meta/llama-3.3-70b-instruct", AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.NVIDIA_NIM));

        java.util.List<String> geminiModels = AiSubtitleService.getAvailableModels(AiSubtitleService.Provider.GEMINI);
        assertTrue(geminiModels.size() >= 3);
        assertEquals("gemini-3.1-flash-lite", geminiModels.get(0));
        assertTrue(geminiModels.contains("gemini-3.5-flash"));

        java.util.List<String> groqModels = AiSubtitleService.getAvailableModels(AiSubtitleService.Provider.GROQ);
        assertTrue(groqModels.size() >= 3);
        assertEquals("llama-3.3-70b-versatile", groqModels.get(0));
        assertTrue(groqModels.contains("llama-3.1-8b-instant"));

        java.util.List<String> nvidiaModels = AiSubtitleService.getAvailableModels(AiSubtitleService.Provider.NVIDIA_NIM);
        assertEquals("meta/llama-3.3-70b-instruct", nvidiaModels.get(0));
        assertTrue(nvidiaModels.contains("deepseek-ai/deepseek-r1"));

        // Effective model fallback when custom model is null or empty
        assertEquals("gemini-3.1-flash-lite", AiSubtitleService.getEffectiveModel(AiSubtitleService.Provider.GEMINI, ""));
        assertEquals("custom-gemini-model", AiSubtitleService.getEffectiveModel(AiSubtitleService.Provider.GEMINI, "custom-gemini-model"));

        // Active models adapted from Tatap
        assertEquals("llama-3.3-70b-versatile", AiSubtitleService.getEffectiveModel(AiSubtitleService.Provider.GROQ, "llama-3.3-70b-versatile"));
        assertEquals("google/gemini-2.0-flash-exp:free", AiSubtitleService.getEffectiveModel(AiSubtitleService.Provider.OPENROUTER, "google/gemini-2.0-flash-exp:free"));

        // Deprecated & legacy model auto-migration (Tatap _LEGACY_MODEL_UPGRADE)
        assertEquals("gemini-3.1-flash-lite", AiSubtitleService.getEffectiveModel(AiSubtitleService.Provider.GEMINI, "gemini-2.5-pro"));
        assertEquals("gemini-3.1-flash-lite", AiSubtitleService.getEffectiveModel(AiSubtitleService.Provider.GEMINI, "gemini-2.0-flash"));
        assertEquals("gemini-3.5-flash", AiSubtitleService.getEffectiveModel(AiSubtitleService.Provider.GEMINI, "gemini-2.5-flash"));
        assertEquals("llama-3.3-70b-versatile", AiSubtitleService.getEffectiveModel(AiSubtitleService.Provider.GROQ, "mixtral-8x7b-32768"));
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
        String primary = "gemini-3.1-flash-lite";
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
    public void testBuildTlcSystemPromptAndNoopParsing() {
        assertEquals(AiSubtitleService.EMBEDDED_INSTANT_AI_PROMPT_ID,
                AiSubtitleService.buildTlcSystemPrompt(null));
        assertEquals(AiSubtitleService.EMBEDDED_INSTANT_AI_PROMPT_ID,
                AiSubtitleService.buildTlcSystemPrompt(AiSubtitleService.DEFAULT_FANSUB_PROMPT_ID));

        String shortCustom = AiSubtitleService.buildTlcSystemPrompt("Gunakan gaya bicara tsundere.");
        assertTrue(shortCustom.startsWith(AiSubtitleService.EMBEDDED_INSTANT_AI_PROMPT_ID));
        assertTrue(shortCustom.contains("[USER CUSTOM INSTRUCTIONS (HIGH PRIORITY ADD-ON)]"));
        assertTrue(shortCustom.contains("Gunakan gaya bicara tsundere."));

        // Custom prompt panjang tetap bersifat aditif: tidak menimpa Embedded Master Prompt maupun [OUTPUT FORMAT]
        String fullCustom = AiSubtitleService.buildTlcSystemPrompt(
                "ATURAN UTAMA:\nGunakan bahasa baku kerajaan abad pertengahan dan pertahankan semua nama teknik jurus.");
        assertTrue(fullCustom.startsWith(AiSubtitleService.EMBEDDED_INSTANT_AI_PROMPT_ID));
        assertTrue(fullCustom.contains("[OUTPUT FORMAT]"));
        assertTrue(fullCustom.contains("[USER CUSTOM INSTRUCTIONS (HIGH PRIORITY ADD-ON)]"));
        assertTrue(fullCustom.contains("ATURAN UTAMA:"));

        String withGlossary = AiSubtitleService.buildTlcSystemPrompt(
                "Pakai 'aku/kamu'",
                java.util.Arrays.asList("Pagma's Swordsmanship", "Stun"));
        assertTrue(withGlossary.contains("[EPISODE PROTECTED TERMS (NEVER TRANSLATE LITERALLY)]"));
        assertTrue(withGlossary.contains("Pagma's Swordsmanship, Stun"));
        assertTrue(withGlossary.contains("Pakai 'aku/kamu'"));

        assertTrue(AiSubtitleService.isTlcNoopOk("0|OK"));
        assertTrue(AiSubtitleService.isTlcNoopOk("<think>all good</think>\nALL OK."));
        assertTrue(AiSubtitleService.isTlcNoopOk("TIDAK ADA"));
        assertFalse(AiSubtitleService.isTlcNoopOk("1|Halo"));

        String tlcOut = "1|How are you? || Kamu gimana?\n2|[FINAL] Baik.\n3|DRAFT: Ayo!\n4|-> Siap, Tuan!";
        java.util.Map<Integer, String> parsed = AiSubtitleService.parseIdLines(tlcOut, 5);
        assertEquals("Kamu gimana?", parsed.get(1));
        assertEquals("Baik.", parsed.get(2));
        assertEquals("Ayo!", parsed.get(3));
        assertEquals("Siap, Tuan!", parsed.get(4));
    }

    @Test
    public void testRefineWithTlcAsyncTatapFormat400RetryAnd503Rotation() throws Exception {
        AiSubtitleService.clearModelCooldowns();
        // 1) Model pertama gagal 400 reasoning_effort -> retry tanpa reasoning_effort lalu 503 -> rotasi ke model ke-2
        server.enqueue(new MockResponse().setResponseCode(400).setBody("Unknown field: reasoning_effort"));
        server.enqueue(new MockResponse().setResponseCode(503).setBody("Service Unavailable"));
        // 2) Model ke-2 mengoreksi baris 1 saja lewat field reasoning (baris 2 non-alfabet di-skip, baris 3 tetap draf GTX+)
        String okBody = "{\n" +
                "  \"choices\": [{\"message\": {\"role\": \"assistant\", \"content\": \"\", \"reasoning\": \"1|Yang Mulia, pasukan sudah bersiap.\"}}]\n" +
                "}";
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(okBody));

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

        service.refineWithTlcAsync(null, "test-ep-1", en, draft, null, "gemini-3.1-flash-lite", "AIzaSyTestKey", true, null,
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
        assertEquals("Ayo pergi!", out[0].get(2).text);
        assertTrue(AiSubtitleService.isModelInCooldown("gemini-3.1-flash-lite"));

        RecordedRequest req1 = server.takeRequest();
        String body1 = req1.getBody().readUtf8();
        assertTrue(body1.contains("\"reasoning_effort\":\"none\""));
        assertTrue(body1.contains("1|Your Majesty, the troops are ready. || Yang Mulia, pasukan udah siap."));
        assertTrue(body1.contains("3|Let's go! || Ayo pergi!"));
        assertFalse("Non-alphabetic cue 2 must be skipped from TLC request", body1.contains("2|..."));

        RecordedRequest req2 = server.takeRequest();
        assertFalse(req2.getBody().readUtf8().contains("reasoning_effort"));
        AiSubtitleService.clearModelCooldowns();
    }

    @Test
    public void testExtractProtectedGlossarySyncWithMockWebServer() throws Exception {
        String mockBody = "{\n" +
                "  \"choices\": [{\"message\": {\"role\": \"assistant\", \"content\": \"1. Pagma's Swordsmanship\\n- Blacksmith's Rage\\nSatisfy\"}}]\n" +
                "}";
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(mockBody));

        java.util.List<SubtitleCue> cues = java.util.Arrays.asList(
                new SubtitleCue(0, 2000, "Activate [Blacksmith's Rage] in Satisfy!"),
                new SubtitleCue(2000, 4000, "Skill: Pagma's Swordsmanship triggered Stun!"),
                new SubtitleCue(4000, 6000, "Pagma's Swordsmanship dealt huge damage!")
        );

        java.util.List<String> glossary = service.extractProtectedGlossarySync(
                null, "ep-overgeared-1", "Overgeared", cues, "llama-3.3-70b-versatile", "gsk_test_key", true, null);

        assertTrue(glossary.contains("Skill"));
        assertTrue(glossary.contains("Stun"));
        assertTrue(glossary.contains("Pagma's Swordsmanship"));
        assertTrue(glossary.contains("Blacksmith's Rage"));
        assertTrue(glossary.contains("Satisfy"));

        RecordedRequest req = server.takeRequest();
        String reqBody = req.getBody().readUtf8();
        assertTrue(reqBody.contains("Overgeared"));
        assertTrue(reqBody.contains("Blacksmith's Rage"));
    }
}
