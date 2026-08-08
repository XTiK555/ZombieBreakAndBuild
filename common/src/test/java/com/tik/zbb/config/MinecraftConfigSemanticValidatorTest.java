package com.tik.zbb.config;

import com.tik.zbb.config.edit.MinecraftConfigSemanticValidator;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigPath;
import com.tik.zbb.config.schema.ConfigRepairReport;
import com.tik.zbb.config.schema.ConfigSchema;
import com.tik.zbb.config.schema.ConfigValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MinecraftConfigSemanticValidatorTest
{
    @Test
    void categoriesAreAcceptedOnlyForEntityPatternLists()
    {
        MinecraftConfigSemanticValidator validator = new MinecraftConfigSemanticValidator(null);
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
    void repairPreservesSyntacticallyValidIdsFromUnavailableMods()
    {
        ConfigFieldDescriptor descriptor = ConfigSchema.find(
                new ConfigPath("ai.affectedEntityIdList")
        ).orElseThrow();
        List<String> ids = List.of("othermod:mob", "minecraft:zombie");

        Object repaired = new MinecraftConfigSemanticValidator(null).repairValue(
                descriptor, ids, descriptor.defaultValue(), new ConfigRepairReport()
        );

        assertEquals(ids, repaired);

        ConfigFieldDescriptor mapDescriptor = ConfigSchema.find(
                new ConfigPath("blocks.mobPlaceBlockIdOverrideList")
        ).orElseThrow();
        Map<String, String> overrides = Map.of("othermod:mob", "othermod:block");

        Object repairedOverrides = new MinecraftConfigSemanticValidator(null).repairValue(
                mapDescriptor, overrides, mapDescriptor.defaultValue(), new ConfigRepairReport()
        );

        assertEquals(overrides, repairedOverrides);
    }
}
