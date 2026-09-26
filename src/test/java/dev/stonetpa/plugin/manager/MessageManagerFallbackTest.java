package dev.stonetpa.plugin.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the getRaw()/getRawList() English-fallback rule:
 * falling back must depend on whether the key is SET in the active
 * language, not on whether its current value happens to be empty/blank.
 */
class MessageManagerFallbackTest {

    private YamlConfiguration yaml(String content) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(content);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return config;
    }

    @Test
    void usesActiveLanguageWhenKeyIsPresent() {
        YamlConfiguration active = yaml("prefix: '[DE] '");
        YamlConfiguration fallback = yaml("prefix: '[EN] '");

        assertEquals("[DE] ", MessageManager.resolveString(active, fallback, "prefix"));
    }

    @Test
    void fallsBackToEnglishWhenKeyIsMissingEntirely() {
        YamlConfiguration active = yaml("other-key: foo");
        YamlConfiguration fallback = yaml("prefix: '[EN] '");

        assertEquals("[EN] ", MessageManager.resolveString(active, fallback, "prefix"));
    }

    @Test
    void anIntentionallyEmptyStringIsNotTreatedAsMissing() {
        YamlConfiguration active = yaml("prefix: ''");
        YamlConfiguration fallback = yaml("prefix: '[EN] '");

        assertEquals("", MessageManager.resolveString(active, fallback, "prefix"));
    }

    @Test
    void anIntentionallyEmptyListIsNotTreatedAsMissing() {
        YamlConfiguration active = yaml("lore: []");
        YamlConfiguration fallback = yaml("""
                lore:
                  - "This should NOT show up"
                """);

        List<String> result = MessageManager.resolveList(active, fallback, "lore");

        assertTrue(result.isEmpty(), "an admin-configured empty list must stay empty, not fall back to English");
    }

    @Test
    void fallsBackToEnglishListWhenKeyIsMissingEntirely() {
        YamlConfiguration active = yaml("other-key: foo");
        YamlConfiguration fallback = yaml("""
                lore:
                  - "line one"
                  - "line two"
                """);

        List<String> result = MessageManager.resolveList(active, fallback, "lore");

        assertEquals(List.of("line one", "line two"), result);
    }

    @Test
    void missingEverywhereReturnsEmptyListNotNull() {
        YamlConfiguration active = yaml("other-key: foo");
        YamlConfiguration fallback = yaml("other-key: bar");

        List<String> result = MessageManager.resolveList(active, fallback, "lore");

        assertTrue(result.isEmpty());
    }
}
