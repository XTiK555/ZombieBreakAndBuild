package com.tik.zbb.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;
import com.tik.zbb.config.document.ConfigDocumentCopier;
import com.tik.zbb.config.edit.*;
import com.tik.zbb.config.io.*;
import com.tik.zbb.config.runtime.ConfigRepository;
import com.tik.zbb.config.schema.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConfigEditServiceTest
{
    private static final ObjectSerializer SERIALIZER = ObjectSerializer.standard();

    @TempDir
    Path tempDir;

    @BeforeAll
    static void bootstrapMinecraft()
    {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void codecStrictCommandsRejectValuesThatFileRepairCanFix() throws Exception
    {
        ConfigFieldDescriptor descriptor = descriptor("balance.breakBuildActivationDistance");

        assertThrows(ConfigValidationException.class, () -> descriptor.codec().parseText(descriptor, "0"));

        Object fixed = new ConfigDocumentNormalizer().normalize(configWithRaw("balance", "breakBuildActivationDistance", 0))
                .document()
                .balance
                .breakBuildActivationDistance;

        assertEquals(6, fixed);
    }

    @Test
    void numericCodecsRejectNonFiniteValues()
    {
        ConfigFieldDescriptor descriptor = descriptor("balance.blockDamage.blockHardnessExponent");

        assertThrows(ConfigValidationException.class, () -> descriptor.codec().parseText(descriptor, "NaN"));
        assertThrows(ConfigValidationException.class, () -> descriptor.codec().parseText(descriptor, "Infinity"));
    }

    @Test
    void integerCodecRejectsFractionsAndOverflowBeforeRangeValidation()
    {
        ConfigFieldDescriptor descriptor = descriptor("balance.breakBuildActivationDistance");

        assertThrows(ConfigValidationException.class, () -> descriptor.codec().decodeDocumentValue(descriptor, 1.5D));
        assertThrows(ConfigValidationException.class, () -> descriptor.codec().decodeDocumentValue(descriptor, Double.NaN));
        assertThrows(ConfigValidationException.class, () -> descriptor.codec().decodeDocumentValue(descriptor, 2_147_483_648L));
        assertThrows(ConfigValidationException.class, () -> descriptor.codec().decodeDocumentValue(
                descriptor,
                new java.math.BigDecimal("1.0000000000000000001")
        ));
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
    void floatCodecAcceptsNumericValues()
    {
        ConfigEditService service =
                service(tempDir.resolve("zbb.toml"));

        ConfigEditResult result = service.edit(
                ConfigEditRequest.set(
                        new ConfigPath(
                                "balance.blockDamage.blockHardnessExponent"
                        ),
                        0.5D
                )
        );

        assertTrue(result.success());
        assertEquals(
                0.5f,
                service.snapshot()
                        .document()
                        .balance
                        .blockDamage
                        .blockHardnessExponent
        );
    }

    @Test
    void fileNormalizerRepairsAndReportsInvalidValues()
    {
        CommentedConfig raw = defaultsConfig();
        CommentedConfig ai = raw.get("ai");
        ai.set("alwaysSeeNearestPlayer", "not-a-boolean");

        ConfigDocumentNormalizer.NormalizedConfig normalized = normalizer().normalize(raw);

        assertFalse(normalized.document().ai.alwaysSeeNearestPlayer);
        assertTrue(normalized.repairReport().entries().stream()
                .anyMatch(entry -> entry.contains("ai.alwaysSeeNearestPlayer")));
    }

    @Test
    void strictCommandFailureDoesNotPublishState()
    {
        ConfigEditService service = service(tempDir.resolve("zbb.toml"));

        ConfigEditResult result = service.edit(ConfigEditRequest.set(
                new ConfigPath("balance.breakBuildActivationDistance"),
                0
        ));

        assertFalse(result.success());
        assertEquals(6, service.snapshot().document().balance.breakBuildActivationDistance);
    }

    @Test
    void saveFailureRollsBackEffectiveState()
    {
        ConfigEditService service = service(new FailingConfigFileStore(tempDir.resolve("zbb.toml")));

        ConfigEditResult result = service.edit(ConfigEditRequest.set(
                new ConfigPath("ai.alwaysSeeNearestPlayer"),
                true
        ));

        assertFalse(result.success());
        assertFalse(service.snapshot().document().ai.alwaysSeeNearestPlayer);
    }

    @Test
    void editsSerializeReadSaveAndPublish() throws Exception
    {
        BlockingSaveStorage storage = new BlockingSaveStorage();
        ConfigEditService service = service(storage);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try
        {
            Future<ConfigEditResult> first = executor.submit(() -> service.edit(ConfigEditRequest.set(
                    new ConfigPath("ai.alwaysSeeNearestPlayer"),
                    true
            )));
            assertTrue(storage.firstSaveStarted.await(1, TimeUnit.SECONDS));

            Future<ConfigEditResult> second = executor.submit(() -> service.edit(ConfigEditRequest.set(
                    new ConfigPath("ai.alwaysSeeNearestPlayer"),
                    false
            )));

            assertFalse(storage.secondSaveStarted.await(200, TimeUnit.MILLISECONDS));
            storage.allowFirstSave.countDown();

            assertTrue(first.get(1, TimeUnit.SECONDS).success());
            assertTrue(second.get(1, TimeUnit.SECONDS).success());
        }
        finally
        {
            executor.shutdownNow();
        }

        assertFalse(service.snapshot().document().ai.alwaysSeeNearestPlayer);
    }

    @Test
    void reloadSaveFailureKeepsPreviousInMemoryState()
    {
        ConfigDocument initial = new ConfigDocument();
        initial.ai.alwaysSeeNearestPlayer = true;
        ConfigDocument loaded = new ConfigDocument();
        ConfigRepairReport repairReport = new ConfigRepairReport();
        repairReport.repaired(new ConfigPath("ai.alwaysSeeNearestPlayer"), "invalid", false, "test repair");
        ConfigEditService service = new ConfigEditService(
                new ConfigRepository(initial),
                new FailingNormalizedSaveStorage(loaded, repairReport)
        );

        ConfigEditService.ConfigReloadResult result = service.reloadFromFile();

        assertFalse(result.success());
        assertFalse(result.saved());
        assertTrue(service.snapshot().document().ai.alwaysSeeNearestPlayer);
    }

    @Test
    void reloadRecoveryFailureKeepsPreviousInMemoryState()
    {
        ConfigDocument initial = new ConfigDocument();
        initial.ai.alwaysSeeNearestPlayer = true;
        ConfigEditService service = new ConfigEditService(
                new ConfigRepository(initial),
                new FailingRecoveryStorage()
        );

        ConfigEditService.ConfigReloadResult result = service.reloadFromFile();

        assertFalse(result.success());
        assertFalse(result.saved());
        assertTrue(service.snapshot().document().ai.alwaysSeeNearestPlayer);
    }

    @Test
    void reloadDoesNotSaveAnUnchangedDocument()
    {
        LoadedDocumentStorage storage = new LoadedDocumentStorage(new ConfigDocument(), new ConfigRepairReport());
        ConfigEditService service = service(storage);

        ConfigEditService.ConfigReloadResult result = service.reloadFromFile();

        assertTrue(result.success());
        assertFalse(result.saved());
        assertEquals(0, storage.saveCount);
    }

    @Test
    void editRequestRejectsImpossibleStates()
    {
        assertThrows(IllegalArgumentException.class, () -> new ConfigEditRequest(
                ConfigEditOperation.SET,
                null,
                true
        ));
        assertThrows(IllegalArgumentException.class, () -> new ConfigEditRequest(
                ConfigEditOperation.RESET_ALL_TO_DEFAULTS,
                new ConfigPath("ai"),
                null
        ));
    }

    @Test
    void resetAllPersistsDefaults()
    {
        ConfigEditService service = service(tempDir.resolve("zbb.toml"));

        assertTrue(service.edit(ConfigEditRequest.set(
                new ConfigPath("ai.alwaysSeeNearestPlayer"),
                true
        )).success());
        assertTrue(service.edit(ConfigEditRequest.set(
                new ConfigPath("balance.breakBuildActivationDistance"),
                10
        )).success());

        ConfigEditResult result = service.edit(ConfigEditRequest.resetAll());

        assertTrue(result.success());
        assertEquals(2, result.affectedCount());
        assertFalse(service.snapshot().document().ai.alwaysSeeNearestPlayer);
        assertEquals(6, service.snapshot().document().balance.breakBuildActivationDistance);
        assertTrue(service.reloadFromFile().success());
        assertFalse(service.snapshot().document().ai.alwaysSeeNearestPlayer);
        assertEquals(6, service.snapshot().document().balance.breakBuildActivationDistance);

        ConfigEditResult repeated = service.edit(ConfigEditRequest.resetAll());
        assertTrue(repeated.success());
        assertEquals(0, repeated.affectedCount());
    }

    @Test
    void unchangedEditsReportZeroAffectedValuesAndAvoidPersistence()
    {
        LoadedDocumentStorage storage = new LoadedDocumentStorage(new ConfigDocument(), new ConfigRepairReport());
        ConfigEditService service = service(storage);

        ConfigEditResult set = service.edit(ConfigEditRequest.set(
                new ConfigPath("ai.alwaysSeeNearestPlayer"),
                false
        ));
        ConfigEditResult removeMissing = service.edit(ConfigEditRequest.remove(
                new ConfigPath("ai.ignoreBreakEntityIdList"),
                "minecraft:zombie"
        ));
        ConfigEditResult clearEmpty = service.edit(ConfigEditRequest.clear(
                new ConfigPath("balance.blockDamage.blockHealthOverrideMap")
        ));
        ConfigEditResult addDuplicate = service.edit(ConfigEditRequest.add(
                new ConfigPath("ai.affectedEntityIdList"),
                "@MONSTER"
        ));

        assertAll(
                () -> assertTrue(set.success()),
                () -> assertEquals(0, set.affectedCount()),
                () -> assertEquals("updated 0 elements", set.message()),
                () -> assertTrue(removeMissing.success()),
                () -> assertEquals(0, removeMissing.affectedCount()),
                () -> assertTrue(clearEmpty.success()),
                () -> assertEquals(0, clearEmpty.affectedCount()),
                () -> assertTrue(addDuplicate.success()),
                () -> assertEquals(0, addDuplicate.affectedCount()),
                () -> assertEquals(0, storage.saveCount)
        );
    }

    @Test
    void snapshotDataIsDefensiveCopy()
    {
        ConfigEditService service = service(tempDir.resolve("zbb.toml"));
        ConfigSnapshot snapshot = service.snapshot();

        ConfigDocument copy = snapshot.document();
        copy.ai.alwaysSeeNearestPlayer = true;

        assertFalse(snapshot.document().ai.alwaysSeeNearestPlayer);
        assertFalse(service.snapshot().document().ai.alwaysSeeNearestPlayer);
    }

    @Test
    void snapshotGameIsImmutableRuntimeView()
    {
        ConfigEditService service = service(tempDir.resolve("zbb.toml"));

        assertTrue(service.edit(ConfigEditRequest.set(
                new ConfigPath("ai.alwaysSeeNearestPlayer"),
                true
        )).success());

        ConfigSnapshot snapshot = service.snapshot();

        assertSame(snapshot.game(), snapshot.game());
        assertTrue(snapshot.game().ai().alwaysSeeNearestPlayer());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.game().blocks().dimensionPlaceBlockMap().clear());
    }

    @Test
    void rawEditConvertsJsonLikeValueWithoutCallerDescriptorLookup()
    {
        ConfigEditService service = service(tempDir.resolve("zbb.toml"));

        ConfigEditResult result = service.editRaw(ConfigEditRequest.add(
                new ConfigPath("blocks.mobPlaceBlockIdOverrideMap"),
                java.util.Map.of("minecraft:zombie", "minecraft:dirt")
        ));

        assertTrue(result.success(), result.message());
        assertEquals("minecraft:dirt", service.snapshot().document().blocks.mobPlaceBlockIdOverrideMap.get("minecraft:zombie"));
    }

    @Test
    void mapEditStoresTheNormalizedIntegerMap()
    {
        ConfigEditService service = service(tempDir.resolve("zbb.toml"));

        ConfigEditResult result = service.edit(ConfigEditRequest.set(
                new ConfigPath("balance.blockDamage.blockHealthOverrideMap"),
                java.util.Map.of("minecraft:dirt", "5")
        ));

        assertTrue(result.success(), result.message());
        assertEquals(5, service.snapshot().document().balance.blockDamage.blockHealthOverrideMap.get("minecraft:dirt"));
        assertTrue(service.reloadFromFile().success());
        assertEquals(5, service.snapshot().document().balance.blockDamage.blockHealthOverrideMap.get("minecraft:dirt"));
    }

    @Test
    void integerMapRejectsFractionsOverflowAndEntriesWithoutValues()
    {
        ConfigEditService service = service(tempDir.resolve("zbb.toml"));
        ConfigPath path = new ConfigPath("balance.blockDamage.blockHealthOverrideMap");

        assertFalse(service.edit(ConfigEditRequest.set(
                path,
                java.util.Map.of("minecraft:dirt", 1.5D)
        )).success());
        assertFalse(service.edit(ConfigEditRequest.set(
                path,
                java.util.Map.of("minecraft:dirt", 2_147_483_648L)
        )).success());
        assertFalse(service.editRaw(ConfigEditRequest.add(
                path,
                "minecraft:dirt"
        )).success());
    }

    @Test
    void rawMapRemovalStillAcceptsAKeyWithoutEquals()
    {
        ConfigEditService service = service(tempDir.resolve("zbb.toml"));
        ConfigPath path = new ConfigPath("balance.blockDamage.blockHealthOverrideMap");
        assertTrue(service.edit(ConfigEditRequest.set(
                path,
                java.util.Map.of("minecraft:dirt", 5)
        )).success());

        ConfigEditResult result = service.editRaw(ConfigEditRequest.remove(
                path,
                "minecraft:dirt"
        ));

        assertTrue(result.success(), result.message());
        assertTrue(service.snapshot().document().balance.blockDamage.blockHealthOverrideMap.isEmpty());
    }

    @Test
    void semanticValidatorRejectsUnknownBlockIdWithoutPublishingState()
    {
        ConfigEditService service = new ConfigEditService(
                new ConfigRepository(new ConfigDocument()),
                new ConfigFileStore(tempDir.resolve("zbb.toml")),
                (descriptor, value) ->
                {
                    if (descriptor.path().value().equals("blocks.fallbackPlaceBlockId")
                            && value.equals("minecraft:not_a_real_block"))
                    {
                        throw new ConfigValidationException("Unknown block id: " + value);
                    }
                }
        );

        ConfigEditResult result = service.edit(ConfigEditRequest.set(
                new ConfigPath("blocks.fallbackPlaceBlockId"),
                "minecraft:not_a_real_block"
        ));

        assertFalse(result.success());
        assertEquals("minecraft:stone", service.snapshot().document().blocks.fallbackPlaceBlockId);
    }

    @Test
    void semanticRepairRemovesOnlyInvalidListEntriesOnReload()
    {
        ConfigDocument loaded = new ConfigDocument();
        loaded.ai.affectedEntityIdList = new java.util.ArrayList<>(java.util.List.of(
                "minecraft:zombie",
                "minecraft:not_a_real_entity",
                "minecraft:*"
        ));
        ConfigRepairReport repairReport = new ConfigRepairReport();

        ConfigEditService service = new ConfigEditService(
                new ConfigRepository(new ConfigDocument()),
                new LoadedDocumentStorage(loaded, repairReport),
                new RemovingListSemanticValidator()
        );

        ConfigEditService.ConfigReloadResult result = service.reloadFromFile();

        assertTrue(result.success());
        assertTrue(result.saved());
        assertEquals(
                java.util.List.of("minecraft:zombie", "minecraft:*"),
                service.snapshot().document().ai.affectedEntityIdList
        );
        assertTrue(repairReport.entries().stream()
                .anyMatch(entry -> entry.contains("minecraft:not_a_real_entity") && entry.contains("<removed>")));
    }

    @Test
    void bootstrapDefersRegistryBackedValidationAndResolutionUntilRuntimeStart()
    {
        ConfigDocument loaded = new ConfigDocument();
        loaded.ai.affectedEntityIdList = new java.util.ArrayList<>(java.util.List.of(
                "minecraft:zombie",
                "minecraft:not_a_real_entity"
        ));
        ConfigRepairReport repairReport = new ConfigRepairReport();
        LoadedDocumentStorage storage = new LoadedDocumentStorage(loaded, repairReport);
        ConfigEditService service = new ConfigEditService(
                new ConfigRepository(new ConfigDocument()),
                storage,
                new RemovingListSemanticValidator()
        );

        ConfigEditService.ConfigReloadResult bootstrapResult = service.bootstrapFromFile();

        assertTrue(bootstrapResult.success());
        assertFalse(bootstrapResult.saved());
        assertEquals(0, storage.saveCount);
        assertEquals(
                java.util.List.of("minecraft:zombie", "minecraft:not_a_real_entity"),
                service.snapshot().document().ai.affectedEntityIdList
        );
        assertTrue(repairReport.entries().isEmpty());

        AtomicInteger blockResolutions = new AtomicInteger();
        ConfigEditService.ConfigReloadResult reloadResult = service.startRuntime((rawValue, defaultBlock) ->
        {
            blockResolutions.incrementAndGet();
            return defaultBlock;
        });

        assertTrue(reloadResult.success());
        assertTrue(reloadResult.saved());
        assertEquals(1, storage.saveCount);
        assertTrue(blockResolutions.get() > 0);
        assertEquals(
                java.util.List.of("minecraft:zombie"),
                service.snapshot().document().ai.affectedEntityIdList
        );
    }

    @Test
    void configDataCopierDeepCopiesLists()
    {
        ConfigDocument original = new ConfigDocument();
        ConfigDocument copy = ConfigDocumentCopier.copy(original);

        copy.blocks.dangerousBlockIdList.clear();
        copy.ai.affectedEntityIdList.add("minecraft:test");
        copy.ai.ignoreBuildEntityIdList.add("minecraft:test");
        copy.balance.blockDamage.blockHealthOverrideMap.put("minecraft:dirt", 5);

        assertFalse(original.blocks.dangerousBlockIdList.isEmpty());
        assertFalse(original.ai.affectedEntityIdList.contains("minecraft:test"));
        assertFalse(original.ai.ignoreBuildEntityIdList.contains("minecraft:test"));
        assertTrue(original.balance.blockDamage.blockHealthOverrideMap.isEmpty());
    }

    @Test
    void normalizerIgnoresLegacyPairLists()
    {
        CommentedConfig raw = defaultsConfig();
        raw.set("blocks.dimensionPlaceBlockIdList", java.util.List.of("minecraft:overworld=minecraft:dirt"));
        raw.set("balance.blockDamage.blockHealthOverrideList", java.util.List.of("minecraft:dirt=5"));

        ConfigDocumentNormalizer.NormalizedConfig normalized = normalizer().normalize(raw);

        assertEquals(new ConfigDocument().blocks.dimensionPlaceBlockIdMap, normalized.document().blocks.dimensionPlaceBlockIdMap);
        assertTrue(normalized.document().balance.blockDamage.blockHealthOverrideMap.isEmpty());
        assertTrue(normalized.repairReport().entries().isEmpty());
    }

    private static ConfigFieldDescriptor descriptor(String path)
    {
        return ConfigSchema.find(new ConfigPath(path)).orElseThrow();
    }

    private static CommentedConfig defaultsConfig()
    {
        CommentedConfig config = CommentedConfig.inMemory();
        SERIALIZER.serializeFields(new ConfigDocument(), config);
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
        return new ConfigDocumentNormalizer();
    }

    private static ConfigEditService service(Path path)
    {
        return service(new ConfigFileStore(path));
    }

    private static ConfigEditService service(ConfigStorage storage)
    {
        return new ConfigEditService(
                new ConfigRepository(new ConfigDocument()),
                storage
        );
    }

    private static final class FailingConfigFileStore implements ConfigStorage
    {
        private FailingConfigFileStore(Path path)
        {
        }

        @Override
        public LoadedConfig load() throws ConfigStorageException
        {
            return new LoadedConfig(new ConfigDocument(), new ConfigRepairReport());
        }

        @Override
        public void save(ConfigDocument data) throws ConfigStorageException
        {
            throw new ConfigPersistenceException("expected test failure", new RuntimeException("expected"));
        }

        @Override
        public RecoveryResult recoverAfterLoadFailure(ConfigDocument fallbackDocument)
        {
            return new RecoveryResult(true, "test");
        }
    }

    private static final class FailingRecoveryStorage implements ConfigStorage
    {
        @Override
        public LoadedConfig load() throws ConfigStorageException
        {
            throw new ConfigPersistenceException("expected load failure", new RuntimeException("expected"));
        }

        @Override
        public void save(ConfigDocument data)
        {
            fail("recovery should own fallback saving");
        }

        @Override
        public RecoveryResult recoverAfterLoadFailure(ConfigDocument fallbackDocument) throws ConfigStorageException
        {
            throw new ConfigPersistenceException("expected recovery failure", new RuntimeException("expected"));
        }
    }

    private static final class FailingNormalizedSaveStorage implements ConfigStorage
    {
        private final ConfigDocument document;
        private final ConfigRepairReport repairReport;

        private FailingNormalizedSaveStorage(ConfigDocument document, ConfigRepairReport repairReport)
        {
            this.document = document;
            this.repairReport = repairReport;
        }

        @Override
        public LoadedConfig load()
        {
            return new LoadedConfig(document, repairReport);
        }

        @Override
        public void save(ConfigDocument data) throws ConfigStorageException
        {
            throw new ConfigPersistenceException("expected normalized save failure", new RuntimeException("expected"));
        }

        @Override
        public RecoveryResult recoverAfterLoadFailure(ConfigDocument fallbackDocument)
        {
            fail("recovery should not run after a successful load");
            return null;
        }
    }

    private static final class BlockingSaveStorage implements ConfigStorage
    {
        private final CountDownLatch firstSaveStarted = new CountDownLatch(1);
        private final CountDownLatch allowFirstSave = new CountDownLatch(1);
        private final CountDownLatch secondSaveStarted = new CountDownLatch(1);
        private int saveCount;

        @Override
        public LoadedConfig load()
        {
            return new LoadedConfig(new ConfigDocument(), new ConfigRepairReport());
        }

        @Override
        public void save(ConfigDocument data) throws ConfigStorageException
        {
            int currentSave;
            synchronized (this)
            {
                currentSave = ++saveCount;
            }

            if (currentSave == 1)
            {
                firstSaveStarted.countDown();
                try
                {
                    if (!allowFirstSave.await(1, TimeUnit.SECONDS))
                    {
                        throw new ConfigPersistenceException("timed out waiting to finish first save", new RuntimeException("timeout"));
                    }
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    throw new ConfigPersistenceException("interrupted while saving", e);
                }
            }
            else
            {
                secondSaveStarted.countDown();
            }
        }

        @Override
        public RecoveryResult recoverAfterLoadFailure(ConfigDocument fallbackDocument)
        {
            fail("recovery should not run during edits");
            return null;
        }
    }

    private static final class LoadedDocumentStorage implements ConfigStorage
    {
        private final ConfigDocument document;
        private final ConfigRepairReport repairReport;
        private int saveCount;

        private LoadedDocumentStorage(ConfigDocument document, ConfigRepairReport repairReport)
        {
            this.document = document;
            this.repairReport = repairReport;
        }

        @Override
        public LoadedConfig load()
        {
            return new LoadedConfig(document, repairReport);
        }

        @Override
        public void save(ConfigDocument data)
        {
            saveCount++;
        }

        @Override
        public RecoveryResult recoverAfterLoadFailure(ConfigDocument fallbackDocument)
        {
            return new RecoveryResult(true, "test");
        }
    }

    private static final class RemovingListSemanticValidator implements ConfigSemanticValidator
    {
        @Override
        public void validate(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
        {
            if (value instanceof java.util.List<?> list && list.contains("minecraft:not_a_real_entity"))
            {
                throw new ConfigValidationException("Unknown entity id: minecraft:not_a_real_entity");
            }
        }

        @Override
        public Object repairValue(
                ConfigFieldDescriptor descriptor,
                Object value,
                Object defaultValue,
                ConfigRepairReport report
        )
        {
            if (!(value instanceof java.util.List<?> list) || !list.contains("minecraft:not_a_real_entity"))
            {
                return value;
            }

            java.util.List<Object> repaired = new java.util.ArrayList<>(list);
            repaired.remove("minecraft:not_a_real_entity");
            report.repaired(descriptor.path(), "minecraft:not_a_real_entity", "<removed>", "Unknown entity id: minecraft:not_a_real_entity");
            report.repaired(descriptor.path(), value, repaired, "Repaired list entries");
            return repaired;
        }
    }
}
