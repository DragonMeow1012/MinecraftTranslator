package com.borwen.mctranslator;

import com.borwen.mctranslator.translate.DispatchingTranslator;
import com.borwen.mctranslator.translate.TranslationException;
import com.borwen.mctranslator.translate.TranslationResult;
import com.borwen.mctranslator.translate.Translator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DispatchingTranslatorTest {

    private static Translator tagging(String tag) {
        return new Translator() {
            @Override public TranslationResult translate(String text, String targetLang) {
                return new TranslationResult(tag + ":" + text, null);
            }
        };
    }

    private static Translator failing() {
        return new Translator() {
            @Override public TranslationResult translate(String text, String targetLang) throws TranslationException {
                throw new TranslationException("boom");
            }
        };
    }

    @Test
    void usesPrimaryWhenEnabled() throws Exception {
        DispatchingTranslator d = new DispatchingTranslator(tagging("AI"), tagging("G"), () -> true);
        assertEquals("AI:Hi", d.translate("Hi", "zh-TW").translatedText());
    }

    @Test
    void usesFallbackWhenDisabled() throws Exception {
        DispatchingTranslator d = new DispatchingTranslator(tagging("AI"), tagging("G"), () -> false);
        assertEquals("G:Hi", d.translate("Hi", "zh-TW").translatedText());
    }

    @Test
    void fallsBackWhenPrimaryFails() throws Exception {
        DispatchingTranslator d = new DispatchingTranslator(failing(), tagging("G"), () -> true);
        assertEquals("G:Hi", d.translate("Hi", "zh-TW").translatedText());
    }

    @Test
    void batchHonoursToggleAndFallback() throws Exception {
        AtomicBoolean useAi = new AtomicBoolean(true);
        DispatchingTranslator d = new DispatchingTranslator(tagging("AI"), tagging("G"), useAi::get);
        assertEquals("AI:x", d.translateBatch(List.of("x"), "zh-TW").get(0).translatedText());
        useAi.set(false);
        assertEquals("G:x", d.translateBatch(List.of("x"), "zh-TW").get(0).translatedText());
    }
}
