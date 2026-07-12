package com.tik.zbb.config.schema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceLocationPatternParserTest
{
    @Test
    void normalizeEntryAcceptsExactAndWildcardForms() throws Exception
    {
        assertEquals("minecraft:zombie", ResourceLocationPatternParser.normalizeEntry("minecraft:zombie"));
        assertEquals("minecraft:*", ResourceLocationPatternParser.normalizeEntry("minecraft:*"));
        assertEquals("*:zombie", ResourceLocationPatternParser.normalizeEntry("*:zombie"));
        assertEquals("*:*", ResourceLocationPatternParser.normalizeEntry("*:*"));
        assertEquals("!minecraft:zombie", ResourceLocationPatternParser.normalizeEntry("!minecraft:zombie"));
        assertEquals("!minecraft:*", ResourceLocationPatternParser.normalizeEntry("!minecraft:*"));
        assertEquals("!*:zombie", ResourceLocationPatternParser.normalizeEntry("!*:zombie"));
        assertEquals("!*:*", ResourceLocationPatternParser.normalizeEntry("!*:*"));
    }

    @Test
    void normalizeEntryRejectsUnsupportedForms()
    {
        assertThrows(ConfigValidationException.class, () -> ResourceLocationPatternParser.normalizeEntry(""));
        assertThrows(ConfigValidationException.class, () -> ResourceLocationPatternParser.normalizeEntry("minecraft:!zombie"));
        assertThrows(ConfigValidationException.class, () -> ResourceLocationPatternParser.normalizeEntry("minecraft:zombie*"));
        assertThrows(ConfigValidationException.class, () -> ResourceLocationPatternParser.normalizeEntry("mine craft:zombie"));
        assertThrows(ConfigValidationException.class, () -> ResourceLocationPatternParser.normalizeEntry("minecraft:"));
    }
}
