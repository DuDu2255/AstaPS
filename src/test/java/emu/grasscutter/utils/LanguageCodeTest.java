package emu.grasscutter.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonParser;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class LanguageCodeTest {
    @Test
    void resolvesGsonChineseLocalesToBundledTranslationFiles() {
        Locale configLocale = JsonUtils.decode(JsonParser.parseString("\"zh-cn\""), Locale.class);
        Locale bareConfigLocale = JsonUtils.decode(JsonParser.parseString("\"zh\""), Locale.class);
        assertEquals("zh-CN", Utils.getLanguageCode(Locale.forLanguageTag("zh")));
        assertEquals("zh-CN", Utils.getLanguageCode(configLocale));
        assertEquals("zh-CN", Utils.getLanguageCode(bareConfigLocale));
        assertEquals("zh-TW", Utils.getLanguageCode(Locale.forLanguageTag("zh-Hant")));

        try (var translation =
                getClass()
                        .getResourceAsStream(
                                "/languages/"
                                        + Utils.getLanguageCode(bareConfigLocale)
                                        + ".json")) {
            assertNotNull(translation);
        } catch (java.io.IOException exception) {
            throw new AssertionError("Could not close the Chinese translation bundle", exception);
        }
    }

    @Test
    void preservesRegionalLocalesAndDoesNotAppendAnEmptyCountry() {
        assertEquals("zh-TW", Utils.getLanguageCode(Locale.TAIWAN));
        assertEquals("en-US", Utils.getLanguageCode(Locale.US));
        assertEquals("fr", Utils.getLanguageCode(Locale.forLanguageTag("fr")));
    }
}
