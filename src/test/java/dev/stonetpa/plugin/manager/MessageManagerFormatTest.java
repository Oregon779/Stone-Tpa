package dev.stonetpa.plugin.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * format() never touches the "plugin" field (only getRaw()/getRawList()/
 * load() do), so it can be tested completely standalone - no Bukkit server,
 * no plugin instance, no MockBukkit needed.
 */
class MessageManagerFormatTest {

    private final MessageManager mm = new MessageManager(null);

    @Test
    void convertsLegacyColorCode() {
        Component result = mm.format("&aHello", null);

        assertEquals(NamedTextColor.GREEN, result.color());
        assertEquals("Hello", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void convertsLegacyFormattingCode() {
        Component result = mm.format("&lBold", null);

        assertEquals(TextDecoration.State.TRUE, result.decoration(TextDecoration.BOLD));
    }

    @Test
    void convertsLegacyHexColorCode() {
        Component result = mm.format("&#00B4D8Water", null);

        assertEquals(0x00B4D8, result.color().value());
        assertEquals("Water", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void substitutesPlaceholders() {
        Component result = mm.format("Hello {player}, {seconds}s left", Map.of("player", "Steve", "seconds", "5"));

        assertEquals("Hello Steve, 5s left", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void aLoneTrailingAmpersandIsKeptLiteralAndDoesNotCrash() {
        Component result = mm.format("weird&", null);

        assertEquals("weird&", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void anUnknownLegacyCodeIsKeptLiteral() {
        Component result = mm.format("&znothing special", null);

        assertEquals("&znothing special", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void placeholderValueCannotInjectMiniMessageMarkup() {
        // Regression test for the MiniMessage-injection bug: a placeholder
        // VALUE containing tag syntax (e.g. untrusted data such as a Modrinth
        // API version string) must render as inert literal text, never as an
        // actual formatted/interactive component.
        Component result = mm.format("Update: {version}",
                Map.of("version", "<bold><click:run_command:/op hacker>1.0.0</click></bold>"));

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertEquals("Update: <bold><click:run_command:/op hacker>1.0.0</click></bold>", plain);
        assertFalse(result.decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE,
                "the injected <bold> tag must not have actually been applied as formatting");
    }

    @Test
    void legacyConversionStillWorksAlongsideAnEscapedPlaceholder() {
        // The template's own legacy color codes must still work normally
        // even though a placeholder value in the same message got escaped.
        Component result = mm.format("&aStatus: {status}", Map.of("status", "<red>bad</red>"));

        assertEquals(NamedTextColor.GREEN, result.color());
        assertTrue(PlainTextComponentSerializer.plainText().serialize(result).contains("<red>bad</red>"));
    }
}
