package dev.stonetpa.plugin.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the recursive config-migration merge that runs on every plugin
 * start/reload. This is the exact mechanism relied on to add new config.yml
 * / messages.yml keys to existing installs without ever touching a value an
 * admin already changed - getting it wrong either loses admin edits or fails
 * to add new options silently.
 */
class ConfigUpdaterTest {

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
    void addsMissingTopLevelKey() {
        YamlConfiguration defaults = yaml("""
                language: en
                new-option: 42
                """);
        YamlConfiguration current = yaml("""
                language: de
                """);

        int added = ConfigUpdater.mergeSection(defaults, current);

        assertEquals(1, added);
        assertEquals(42, current.getInt("new-option"));
        // Existing admin-chosen value must survive untouched.
        assertEquals("de", current.getString("language"));
    }

    @Test
    void neverOverwritesAnExistingValue() {
        YamlConfiguration defaults = yaml("""
                request:
                  cooldown-seconds: 10
                """);
        YamlConfiguration current = yaml("""
                request:
                  cooldown-seconds: 999
                """);

        int added = ConfigUpdater.mergeSection(defaults, current);

        assertEquals(0, added);
        assertEquals(999, current.getInt("request.cooldown-seconds"));
    }

    @Test
    void addsNewKeyInsideAnExistingNestedSection() {
        YamlConfiguration defaults = yaml("""
                teleport:
                  delay-seconds: 5
                  cancel-on-move: true
                """);
        YamlConfiguration current = yaml("""
                teleport:
                  delay-seconds: 20
                """);

        int added = ConfigUpdater.mergeSection(defaults, current);

        assertEquals(1, added);
        assertEquals(20, current.getInt("teleport.delay-seconds"));
        assertTrue(current.getBoolean("teleport.cancel-on-move"));
    }

    @Test
    void addsAWholeNewNestedSection() {
        YamlConfiguration defaults = yaml("""
                update-checker:
                  enabled: true
                  check-interval-minutes: 60
                """);
        YamlConfiguration current = yaml("""
                language: en
                """);

        int added = ConfigUpdater.mergeSection(defaults, current);

        // A whole missing section is copied over in one shot (its own key
        // is missing entirely, so the loop never recurses into its
        // children) - it counts as ONE added key, not one per leaf inside it.
        assertEquals(1, added);
        assertTrue(current.getBoolean("update-checker.enabled"));
        assertEquals(60, current.getInt("update-checker.check-interval-minutes"));
    }

    @Test
    void doesNotRecurseWhenExistingKeyIsWrongType() {
        // An admin (or a bad manual edit) turned what should be a section
        // into a plain scalar - this must not crash and must not silently
        // reinterpret the scalar as a section.
        YamlConfiguration defaults = yaml("""
                teleport:
                  delay-seconds: 5
                """);
        YamlConfiguration current = yaml("""
                teleport: "not a section"
                """);

        int added = ConfigUpdater.mergeSection(defaults, current);

        assertEquals(0, added);
        assertEquals("not a section", current.getString("teleport"));
    }

    @Test
    void reportsUnchangedWhenNothingIsMissing() {
        YamlConfiguration defaults = yaml("language: en");
        YamlConfiguration current = yaml("language: de");

        int added = ConfigUpdater.mergeSection(defaults, current);

        assertFalse(added > 0);
    }
}
