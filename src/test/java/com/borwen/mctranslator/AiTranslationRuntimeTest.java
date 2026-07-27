package com.borwen.mctranslator;

import com.borwen.mctranslator.config.TranslatorConfig;
import com.borwen.mctranslator.translate.AiTranslationRuntime;
import com.borwen.mctranslator.translate.HttpTransport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiTranslationRuntimeTest {

    @Test
    void routesKeylessApiWithoutStartingCodex(@TempDir Path directory) throws Exception {
        TranslatorConfig config = new TranslatorConfig();
        config.aiUseCodex = false;
        config.aiBaseUrl = "http://127.0.0.1:11434/v1";
        config.aiModel = "local-model";
        config.aiApiKeys.clear();
        config.requestCooldownMs = 0;

        AtomicInteger calls = new AtomicInteger();
        HttpTransport transport = new HttpTransport() {
            @Override
            public String get(String url) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String post(String url, String body, Map<String, String> headers) {
                calls.incrementAndGet();
                assertTrue(headers.isEmpty());
                return "{\"choices\":[{\"message\":{\"content\":\"86001本地翻譯86002\"}}]}";
            }
        };

        try (AiTranslationRuntime runtime = new AiTranslationRuntime(
                config, transport, directory.resolve("home"), directory.resolve("workspace"))) {
            assertTrue(runtime.isConfigured());
            assertEquals("本地翻譯", runtime.translate("Local", "zh-TW").translatedText());
            assertEquals(1, calls.get());

            config.aiUseCodex = true;
            assertFalse(runtime.isConfigured(), "Codex requires this runtime's own signed-in session");
        }
    }
}
