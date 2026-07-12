package com.tik.zbb.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;
import com.tik.zbb.config.edit.ConfigEditRequest;
import com.tik.zbb.config.edit.ConfigEditResult;
import com.tik.zbb.config.edit.ConfigEditService;
import com.tik.zbb.config.edit.ConfigWriteMode;
import com.tik.zbb.config.io.ConfigDocumentNormalizer;
import com.tik.zbb.config.io.ConfigFileStore;
import com.tik.zbb.config.io.ConfigPersistenceException;
import com.tik.zbb.config.runtime.ConfigRepository;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigPath;
import com.tik.zbb.config.schema.ConfigSchema;
import com.tik.zbb.config.schema.ConfigValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigEditServiceTest
{
    private static final ObjectSerializer SERIALIZER = ObjectSerializer.standard();
    private static final ObjectDeserializer DESERIALIZER = ObjectDeserializer.standard();

    @TempDir
    Path tempDir;

    @Test
    void codecStrictCommandsRejectValuesThatFileRepairCanFix() throws Exception
    {
        ConfigFieldDescriptor descriptor = descriptor("balance.pathEndBreakBuildDistance");

        assertThrows(ConfigValidationException.class, () -> descriptor.codec().parseText(descriptor, "0"));

        Object fixed = new ConfigDocumentNormalizer(DESERIALIZER).normalize(configWithRaw("balance", "pathEndBreakBuildDistance", 0))
                .data()
                .balance
                .pathEndBreakBuildDistance;

        assertEquals(6, fixed);
    }

    @Test
    void numericCodecsRejectNonFiniteValues()
    {
        ConfigFieldDescriptor descriptor = descriptor("balance.blockDamage.blockHardnessContrast");

        assertThrows(ConfigValidationException.class, () -> descriptor.codec().parseText(descriptor, "NaN"));
        assertThrows(ConfigValidationException.class, () -> descriptor.codec().parseText(descriptor, "Infinity"));
    }

    @Test
    void resourceLocationPatternListCodecAcceptsSupportedPatterns() throws Exception
    {
        ConfigFieldDescriptor descriptor = descriptor("ai.affectedEntityIdList");

        assertEquals(
                java.util.List.of("minecraft:zombie", "minecraft:*", "*:zombie", "*:*", "!minecraft:zombie", "!minecraft:*", "!*:zombie", "!*:*"),
                descriptor.codec().parseText(descriptor, "minecraft:zombie,minecraft:*,*:zombie,*:*,!minecraft:zombie,!minecraft:*,!*:zombie,!*:*")
        );
    }

    @Test
    void resourceLocationPatternListCodecRejectsUnsupportedPatterns()
    {
        ConfigFieldDescriptor descriptor = descriptor("ai.affectedEntityIdList");

        assertThrows(ConfigValidationException.class, () -> descriptor.codec().parseText(descriptor, "minecraft:!zombie"));
        assertThrows(ConfigValidationException.class, () -> descriptor.codec().parseText(descriptor, "minecraft:zombie*"));
        assertThrows(ConfigValidationException.class, () -> descriptor.codec().parseText(descriptor, "mine craft:zombie"));
    }

    @Test
    void aiSchemaUsesPatternModelOnly()
    {
        assertTrue(ConfigSchema.find(new ConfigPath("ai.affectedEntityIdList")).isPresent());
        assertTrue(ConfigSchema.find(new ConfigPath("ai.ignoreBuildEntityIdList")).isPresent());
        assertTrue(ConfigSchema.find(new ConfigPath("ai.ignoreBreakEntityIdList")).isPresent());
        assertTrue(ConfigSchema.find(new ConfigPath("ai.applyToAllMonsters")).isEmpty());
        assertTrue(ConfigSchema.find(new ConfigPath("ai.additionalEntityIdList")).isEmpty());
    }

    @Test
    void floatCodecRejectsNonFloatRuntimeValuesWithoutPublishingState()
    {
        ConfigEditService service = service(tempDir.resolve("zbb.toml"));

        ConfigEditResult result = service.edit(ConfigEditRequest.set(
                new ConfigPath("balance.blockDamage.blockHardnessContrast"),
                0.5D,
                ConfigWriteMode.RUNTIME_ONLY,
                "test"
        ));

        assertFalse(result.success());
        assertEquals(0.85f, service.snapshot().data().balance.blockDamage.blockHardnessContrast);
    }

    @Test
    void fileNormalizerRepairsAndReportsInvalidValues()
    {
        CommentedConfig raw = defaultsConfig();
        CommentedConfig ai = raw.get("ai");
        ai.set("alwaysSeeNearestPlayer", "not-a-boolean");

        ConfigDocumentNormalizer.NormalizedConfig normalized = normalizer().normalize(raw);

        assertFalse(normalized.data().ai.alwaysSeeNearestPlayer);
        assertTrue(normalized.repairReport().entries().stream()
                .anyMatch(entry -> entry.contains("ai.alwaysSeeNearestPlayer")));
    }

    @Test
    void strictCommandFailureDoesNotPublishState()
    {
        ConfigEditService service = service(tempDir.resolve("zbb.toml"));

        ConfigEditResult result = service.edit(ConfigEditRequest.set(
                new ConfigPath("balance.pathEndBreakBuildDistance"),
                0,
                ConfigWriteMode.RUNTIME_ONLY,
                "test"
        ));

        assertFalse(result.success());
        assertEquals(6, service.snapshot().data().balance.pathEndBreakBuildDistance);
    }

    @Test
    void persistentSaveFailureRollsBackEffectiveState()
    {
        ConfigEditService service = service(new FailingConfigFileStore(tempDir.resolve("zbb.toml")));

        ConfigEditResult result = service.edit(ConfigEditRequest.set(
                new ConfigPath("ai.alwaysSeeNearestPlayer"),
                true,
                ConfigWriteMode.PERSISTENT,
                "test"
        ));

        assertFalse(result.success());
        assertFalse(service.snapshot().data().ai.alwaysSeeNearestPlayer);
    }

    @Test
    void runtimeOverrideDoesNotLeakIntoLaterPersistentSave()
    {
        Path path = tempDir.resolve("zbb.toml");
        ConfigEditService service = service(path);

        assertTrue(service.edit(ConfigEditRequest.set(
                new ConfigPath("ai.alwaysSeeNearestPlayer"),
                true,
                ConfigWriteMode.RUNTIME_ONLY,
                "test"
        )).success());

        assertTrue(service.edit(ConfigEditRequest.set(
                new ConfigPath("balance.pathEndBreakBuildDistance"),
                7,
                ConfigWriteMode.PERSISTENT,
                "test"
        )).success());

        assertTrue(service.snapshot().data().ai.alwaysSeeNearestPlayer);
        assertEquals(7, service.snapshot().data().balance.pathEndBreakBuildDistance);

        service.reloadFromFile();

        assertFalse(service.snapshot().data().ai.alwaysSeeNearestPlayer);
        assertEquals(7, service.snapshot().data().balance.pathEndBreakBuildDistance);
    }

    @Test
    void runtimeResetAllOnlyResetsExistingTemporaryValues()
    {
        ConfigEditService service = service(tempDir.resolve("zbb.toml"));

        assertTrue(service.edit(ConfigEditRequest.set(
                new ConfigPath("ai.alwaysSeeNearestPlayer"),
                true,
                ConfigWriteMode.PERSISTENT,
                "test"
        )).success());
        assertTrue(service.edit(ConfigEditRequest.set(
                new ConfigPath("balance.pathEndBreakBuildDistance"),
                10,
                ConfigWriteMode.RUNTIME_ONLY,
                "test"
        )).success());

        ConfigEditResult result = service.edit(ConfigEditRequest.resetAll(
                ConfigWriteMode.RUNTIME_ONLY,
                "test"
        ));

        assertTrue(result.success());
        assertEquals(1, result.affectedCount());
        assertTrue(service.snapshot().data().ai.alwaysSeeNearestPlayer);
        assertEquals(6, service.snapshot().data().balance.pathEndBreakBuildDistance);
    }

    @Test
    void snapshotDataIsDefensiveCopy()
    {
        ConfigEditService service = service(tempDir.resolve("zbb.toml"));
        ConfigSnapshot snapshot = service.snapshot();

        ConfigData copy = snapshot.data();
        copy.ai.alwaysSeeNearestPlayer = true;

        assertFalse(snapshot.data().ai.alwaysSeeNearestPlayer);
        assertFalse(service.snapshot().data().ai.alwaysSeeNearestPlayer);
    }

    @Test
    void configDataCopierDeepCopiesLists()
    {
        ConfigData original = new ConfigData();
        ConfigData copy = ConfigDataCopier.copy(original);

        copy.blocks.dangerousBlockIdList.clear();
        copy.ai.affectedEntityIdList.add("minecraft:test");
        copy.ai.ignoreBuildEntityIdList.add("minecraft:test");
        copy.balance.blockDamage.blockHealthOverrideList.add("minecraft:dirt=5");

        assertFalse(original.blocks.dangerousBlockIdList.isEmpty());
        assertFalse(original.ai.affectedEntityIdList.contains("minecraft:test"));
        assertFalse(original.ai.ignoreBuildEntityIdList.contains("minecraft:test"));
        assertTrue(original.balance.blockDamage.blockHealthOverrideList.isEmpty());
    }

    private static ConfigFieldDescriptor descriptor(String path)
    {
        return ConfigSchema.find(new ConfigPath(path)).orElseThrow();
    }

    private static CommentedConfig defaultsConfig()
    {
        CommentedConfig config = CommentedConfig.inMemory();
        SERIALIZER.serializeFields(new ConfigData(), config);
        return config;
    }

    private static CommentedConfig configWithRaw(String section, String key, Object value)
    {
        CommentedConfig config = defaultsConfig();
        CommentedConfig nested = config.get(section);
        nested.set(key, value);
        return config;
    }

    private static ConfigDocumentNormalizer normalizer()
    {
        return new ConfigDocumentNormalizer(DESERIALIZER);
    }

    private static ConfigEditService service(Path path)
    {
        return service(new ConfigFileStore(path, SERIALIZER));
    }

    private static ConfigEditService service(ConfigFileStore fileStore)
    {
        return new ConfigEditService(
                new ConfigRepository(new ConfigData()),
                fileStore,
                normalizer()
        );
    }

    private static final class FailingConfigFileStore extends ConfigFileStore
    {
        private FailingConfigFileStore(Path path)
        {
            super(path, SERIALIZER);
        }

        @Override
        public void save(ConfigData data) throws ConfigPersistenceException
        {
            throw new ConfigPersistenceException("expected test failure", new RuntimeException("expected"));
        }
    }
}
