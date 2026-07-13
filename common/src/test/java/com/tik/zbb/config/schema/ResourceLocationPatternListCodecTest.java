package com.tik.zbb.config.schema;

import com.tik.zbb.config.schema.codecs.ResourceLocationPatternListCodec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceLocationPatternListCodecTest
{
    @Test
    void normalizePatternAcceptsExactAndWildcardForms() throws Exception
    {
        assertEquals("minecraft:zombie", ResourceLocationPatternListCodec.normalizePattern("minecraft:zombie"));
        assertEquals("minecraft:*", ResourceLocationPatternListCodec.normalizePattern("minecraft:*"));
        assertEquals("*:zombie", ResourceLocationPatternListCodec.normalizePattern("*:zombie"));
        assertEquals("*:*", ResourceLocationPatternListCodec.normalizePattern("*:*"));
        assertEquals("!minecraft:zombie", ResourceLocationPatternListCodec.normalizePattern("!minecraft:zombie"));
        assertEquals("!minecraft:*", ResourceLocationPatternListCodec.normalizePattern("!minecraft:*"));
        assertEquals("!*:zombie", ResourceLocationPatternListCodec.normalizePattern("!*:zombie"));
        assertEquals("!*:*", ResourceLocationPatternListCodec.normalizePattern("!*:*"));
    }

    @Test
    void normalizePatternRejectsUnsupportedForms()
    {
        assertThrows(ConfigValidationException.class, () -> ResourceLocationPatternListCodec.normalizePattern(""));
        assertThrows(ConfigValidationException.class, () -> ResourceLocationPatternListCodec.normalizePattern("minecraft:!zombie"));
        assertThrows(ConfigValidationException.class, () -> ResourceLocationPatternListCodec.normalizePattern("minecraft:zombie*"));
        assertThrows(ConfigValidationException.class, () -> ResourceLocationPatternListCodec.normalizePattern("mine craft:zombie"));
        assertThrows(ConfigValidationException.class, () -> ResourceLocationPatternListCodec.normalizePattern("minecraft:"));
    }
}
