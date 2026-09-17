package com.tik.zbb.config;

import com.tik.zbb.config.edit.MinecraftConfigSemanticValidator;
import com.tik.zbb.config.runtime.ConfigAvailabilityReport;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigPath;
import com.tik.zbb.config.schema.ConfigSchema;
import com.tik.zbb.config.schema.ConfigValidationException;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftConfigSemanticValidatorTest
{
    private static MinecraftConfigSemanticValidator validator;

    @BeforeAll
    static void bootstrapMinecraft()
    {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        validator = new MinecraftConfigSemanticValidator(
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
        );
    }

    @Test
    void categoriesAreAcceptedOnlyForEntityPatternLists()
    {
        ConfigFieldDescriptor entities = ConfigSchema.find(
                new ConfigPath("ai.affectedEntityIdList")
        ).orElseThrow();
        ConfigFieldDescriptor blocks = ConfigSchema.find(
                new ConfigPath("blocks.dangerousBlockIdList")
        ).orElseThrow();

        assertDoesNotThrow(() -> validator.validate(entities, List.of("@monster", "!@creature")));
        assertThrows(ConfigValidationException.class, () -> validator.validate(blocks, List.of("@monster")));
    }

    @Test
    void resolutionIgnoresIdsFromUnavailableMods()
    {
        ConfigFieldDescriptor descriptor = ConfigSchema.find(
                new ConfigPath("ai.affectedEntityIdList")
        ).orElseThrow();
        List<String> ids = List.of("othermod:mob", "minecraft:zombie");

        ConfigAvailabilityReport availability = new ConfigAvailabilityReport();
        Object resolved = validator.resolveValue(
                descriptor, ids, descriptor.defaultValue(), availability
        );

        assertEquals(List.of("minecraft:zombie"), resolved);
        assertTrue(availability.entries().stream().anyMatch(entry -> entry.contains("othermod:mob")));

        ConfigFieldDescriptor mapDescriptor = ConfigSchema.find(
                new ConfigPath("blocks.mobPlaceBlockIdOverrideMap")
        ).orElseThrow();
        Map<String, String> overrides = Map.of("othermod:mob", "othermod:block");

        Object resolvedOverrides = validator.resolveValue(
                mapDescriptor, overrides, mapDescriptor.defaultValue(), new ConfigAvailabilityReport()
        );

        assertEquals(Map.of(), resolvedOverrides);
    }
}
