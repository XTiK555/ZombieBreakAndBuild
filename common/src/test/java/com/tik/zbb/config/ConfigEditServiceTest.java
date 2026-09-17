package com.tik.zbb.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;
import com.tik.zbb.config.edit.*;
import com.tik.zbb.config.io.*;
import com.tik.zbb.config.runtime.ConfigAvailabilityReport;
import com.tik.zbb.config.runtime.ConfigLoadResult;
import com.tik.zbb.config.runtime.ConfigRepository;
import com.tik.zbb.config.schema.*;
import com.tik.zbb.utilities.ConfigUtilities;
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
    private static final ObjectDeserializer DESERIALIZER = ObjectDeserializer.standard();

    @TempDir
    Path tempDir;

    @Test
    void codecStrictCommandsRejectValuesThatFileRepairCanFix() throws Exception
    {
        ConfigFieldDescriptor descriptor = descriptor("balance.breakBuildActivationDistance");

        assertThrows(ConfigValidationException.class, () -> descriptor.codec().parseText(descriptor, "0"));

        Object fixed = new ConfigDocumentNormalizer(DESERIALIZER).normalize(configWithRaw("balance", "breakBuildActivationDistance", 0))
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
    void resourceLocationSemanticsSelectsCodecFromFieldShape()
    {
        assertEquals(ConfigValueKind.RESOURCE_LOCATION, descriptor("blocks.fallbackPlaceBlockId").kind());
        assertEquals(ConfigValueKind.RESOURCE_LOCATION_PATTERN_LIST, descriptor("blocks.dangerousBlockIdList").kind());
        assertEquals(ConfigValueKind.RESOURCE_LOCATION_PAIR_MAP, descriptor("blocks.dimensionPlaceBlockIdMap").kind());
        assertEquals(
                ConfigValueKind.RESOURCE_LOCATION_INT_PAIR_MAP,
                descriptor("balance.blockDamage.blockHealthOverrideMap").kind()
        );
    }

    @Test
    void floatCodecAcceptsNumericValues()
    {
        TestConfig service =
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
    void fileNormalizerReportsMissingAndInvalidValuesSeparately()
    {
        CommentedConfig raw = defaultsConfig();
        CommentedConfig ai = raw.get("ai");
        ai.set("alwaysSeeNearestPlayer", "not-a-boolean");
        ai.remove("affectedEntityIdList");

        ConfigDocumentNormalizer.NormalizedConfig normalized = normalizer().normalize(raw);

        assertFalse(normalized.document().ai.alwaysSeeNearestPlayer);
        assertTrue(normalized.fileReport().invalidValues().stream()
                .anyMatch(entry -> entry.contains("ai.alwaysSeeNearestPlayer")));
        assertTrue(normalized.fileReport().missingValues().stream()
                .anyMatch(entry -> entry.contains("ai.affectedEntityIdList")));
    }

    @Test
    void strictCommandFailureDoesNotPublishState()
    {
        TestConfig service = service(tempDir.resolve("zbb.toml"));

        ConfigEditResult result = service.edit(ConfigEditRequest.set(
                new ConfigPath("balance.breakBuildActivationDistance"),
                0
        ));

        assertFalse(result.success());
        assertEquals(6, service.snapshot().document().balance.breakBuildActivationDistance);
    }

    @Test
    void saveFailureDoesNotPublishState()
    {
        TestConfig service = service(new FailingConfigFileStore(tempDir.resolve("zbb.toml")));

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
        TestConfig service = service(storage);

        try (ExecutorService executor = Executors.newFixedThreadPool(2))
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

        assertFalse(service.snapshot().document().ai.alwaysSeeNearestPlayer);
    }

    @Test
    void reloadSaveFailureKeepsPreviousInMemoryState()
    {
        ConfigDocument initial = new ConfigDocument();
        initial.ai.alwaysSeeNearestPlayer = true;
        ConfigDocument loaded = new ConfigDocument();
        ConfigFileReport fileReport = new ConfigFileReport();
        fileReport.invalid(new ConfigPath("ai.alwaysSeeNearestPlayer"), "invalid", false, "test repair");
        TestConfig service = service(
                initial,
                new FailingNormalizedSaveStorage(loaded, fileReport),
                ConfigSemanticValidator.NONE
        );

        ConfigLoadResult result = service.reload();

        assertFalse(result.success());
        assertFalse(result.saved());
        assertTrue(service.snapshot().document().ai.alwaysSeeNearestPlayer);
    }

    @Test
    void reloadRecoveryFailureKeepsPreviousInMemoryState()
    {
        ConfigDocument initial = new ConfigDocument();
        initial.ai.alwaysSeeNearestPlayer = true;
        TestConfig service = service(
                initial,
                new FailingRecoveryStorage(),
                ConfigSemanticValidator.NONE
        );

        ConfigLoadResult result = service.reload();

        assertFalse(result.success());
        assertFalse(result.saved());
        assertTrue(service.snapshot().document().ai.alwaysSeeNearestPlayer);
    }

    @Test
    void reloadDoesNotSaveAnUnchangedDocument()
    {
        LoadedDocumentStorage storage = new LoadedDocumentStorage(new ConfigDocument(), new ConfigFileReport());
        TestConfig service = service(storage);

        ConfigLoadResult result = service.reload();

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
        TestConfig service = service(tempDir.resolve("zbb.toml"));

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
        assertTrue(service.reload().success());
        assertFalse(service.snapshot().document().ai.alwaysSeeNearestPlayer);
        assertEquals(6, service.snapshot().document().balance.breakBuildActivationDistance);

        ConfigEditResult repeated = service.edit(ConfigEditRequest.resetAll());
        assertTrue(repeated.success());
        assertEquals(0, repeated.affectedCount());
    }

    @Test
    void unchangedEditsReportZeroAffectedValuesAndAvoidPersistence()
    {
        LoadedDocumentStorage storage = new LoadedDocumentStorage(new ConfigDocument(), new ConfigFileReport());
        TestConfig service = service(storage);

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
                () -> assertFalse(addDuplicate.success()),
                () -> assertEquals(0, addDuplicate.affectedCount()),
                () -> assertEquals(0, storage.saveCount)
        );
    }

    @Test
    void snapshotDataIsDefensiveCopy()
    {
        TestConfig service = service(tempDir.resolve("zbb.toml"));
        ConfigSnapshot snapshot = service.snapshot();

        ConfigDocument copy = snapshot.document();
        copy.ai.alwaysSeeNearestPlayer = true;

        assertFalse(snapshot.document().ai.alwaysSeeNearestPlayer);
        assertFalse(service.snapshot().document().ai.alwaysSeeNearestPlayer);
    }

    @Test
    void snapshotGameIsImmutableRuntimeView()
    {
        TestConfig service = service(tempDir.resolve("zbb.toml"));

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
        TestConfig service = service(tempDir.resolve("zbb.toml"));

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
        TestConfig service = service(tempDir.resolve("zbb.toml"));

        ConfigEditResult result = service.edit(ConfigEditRequest.set(
                new ConfigPath("balance.blockDamage.blockHealthOverrideMap"),
                java.util.Map.of("minecraft:dirt", "5")
        ));

        assertTrue(result.success(), result.message());
        assertEquals(5, service.snapshot().document().balance.blockDamage.blockHealthOverrideMap.get("minecraft:dirt"));
        assertTrue(service.reload().success());
        assertEquals(5, service.snapshot().document().balance.blockDamage.blockHealthOverrideMap.get("minecraft:dirt"));
    }

    @Test
    void integerMapRejectsFractionsOverflowAndEntriesWithoutValues()
    {
        TestConfig service = service(tempDir.resolve("zbb.toml"));
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
        TestConfig service = service(tempDir.resolve("zbb.toml"));
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
        TestConfig service = service(
                new ConfigDocument(),
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
    void resolutionIgnoresUnavailableListEntriesWithoutSavingThem()
    {
        ConfigDocument loaded = new ConfigDocument();
        loaded.ai.affectedEntityIdList = new java.util.ArrayList<>(java.util.List.of(
                "minecraft:zombie",
                "minecraft:not_a_real_entity",
                "minecraft:*"
        ));
        ConfigFileReport fileReport = new ConfigFileReport();
        LoadedDocumentStorage storage = new LoadedDocumentStorage(loaded, fileReport);

        TestConfig service = unstarted(new ConfigDocument(), storage);

        ConfigLoadResult result = service.repository().activate(
                ConfigRuntime.BlockResolver.NONE,
                new RemovingListSemanticValidator()
        );

        assertTrue(result.success());
        assertFalse(result.saved());
        assertEquals(0, storage.saveCount);
        assertEquals(
                java.util.List.of("minecraft:zombie", "minecraft:*"),
                service.snapshot().document().ai.affectedEntityIdList
        );
        assertEquals(
                java.util.List.of("minecraft:zombie", "minecraft:not_a_real_entity", "minecraft:*"),
                storage.document.ai.affectedEntityIdList
        );
        assertTrue(result.availabilityReport().entries().stream()
                .anyMatch(entry -> entry.contains("minecraft:not_a_real_entity")));
        assertFalse(fileReport.changed());

        ConfigEditResult addResult = service.edit(ConfigEditRequest.add(
                new ConfigPath("ai.affectedEntityIdList"),
                "minecraft:skeleton"
        ));

        assertTrue(addResult.success(), addResult.message());
        assertEquals(1, storage.saveCount);
        assertEquals(
                java.util.List.of(
                        "minecraft:zombie",
                        "minecraft:not_a_real_entity",
                        "minecraft:*",
                        "minecraft:skeleton"
                ),
                storage.savedDocument.ai.affectedEntityIdList
        );
    }

    @Test
    void unavailableRegistryMapEntriesDoNotBlockLaterEditsOrDisappearFromFile()
    {
        ConfigDocument loaded = new ConfigDocument();
        loaded.blocks.mobPlaceBlockIdOverrideMap.put("oldmod:mob_a", "oldmod:block_a");
        loaded.blocks.mobPlaceBlockIdOverrideMap.put("oldmod:mob_b", "oldmod:block_b");
        LoadedDocumentStorage storage = new LoadedDocumentStorage(loaded, new ConfigFileReport());
        TestConfig service = unstarted(new ConfigDocument(), storage);

        ConfigLoadResult reload = service.repository().activate(
                ConfigRuntime.BlockResolver.NONE,
                new RemovingMapSemanticValidator()
        );
        assertTrue(reload.success());
        assertFalse(reload.saved());
        assertEquals(java.util.Map.of(), service.snapshot().document().blocks.mobPlaceBlockIdOverrideMap);

        ConfigEditResult add = service.editRaw(ConfigEditRequest.add(
                new ConfigPath("blocks.mobPlaceBlockIdOverrideMap"),
                "minecraft:zombie=minecraft:dirt"
        ));

        assertTrue(add.success(), add.message());
        assertEquals("oldmod:block_a", storage.savedDocument.blocks.mobPlaceBlockIdOverrideMap.get("oldmod:mob_a"));
        assertEquals("oldmod:block_b", storage.savedDocument.blocks.mobPlaceBlockIdOverrideMap.get("oldmod:mob_b"));
        assertEquals("minecraft:dirt", storage.savedDocument.blocks.mobPlaceBlockIdOverrideMap.get("minecraft:zombie"));

        ConfigEditResult set = service.edit(ConfigEditRequest.set(
                new ConfigPath("blocks.mobPlaceBlockIdOverrideMap"),
                java.util.Map.of("minecraft:skeleton", "minecraft:stone")
        ));

        assertTrue(set.success(), set.message());
        assertEquals("oldmod:block_a", storage.savedDocument.blocks.mobPlaceBlockIdOverrideMap.get("oldmod:mob_a"));
        assertEquals("oldmod:block_b", storage.savedDocument.blocks.mobPlaceBlockIdOverrideMap.get("oldmod:mob_b"));
        assertFalse(storage.savedDocument.blocks.mobPlaceBlockIdOverrideMap.containsKey("minecraft:zombie"));
        assertEquals("minecraft:stone", storage.savedDocument.blocks.mobPlaceBlockIdOverrideMap.get("minecraft:skeleton"));
    }

    @Test
    void exactReplacementOperationsRemoveUnavailableEntries()
    {
        ConfigPath path = new ConfigPath("blocks.mobPlaceBlockIdOverrideMap");
        for (ConfigEditRequest request : java.util.List.of(
                ConfigEditRequest.clear(path),
                ConfigEditRequest.reset(path),
                ConfigEditRequest.resetAll()
        ))
        {
            ConfigDocument loaded = new ConfigDocument();
            loaded.blocks.mobPlaceBlockIdOverrideMap.put("oldmod:mob", "oldmod:block");
            LoadedDocumentStorage storage = new LoadedDocumentStorage(loaded, new ConfigFileReport());
            TestConfig service = unstarted(new ConfigDocument(), storage);
            service.repository().activate(ConfigRuntime.BlockResolver.NONE, new RemovingMapSemanticValidator());

            ConfigEditResult result = service.edit(request);

            assertTrue(result.success(), result.message());
            assertEquals(1, result.affectedCount());
            assertEquals(1, storage.saveCount);
            assertTrue(storage.savedDocument.blocks.mobPlaceBlockIdOverrideMap.isEmpty());
        }
    }

    @Test
    void loadDefersRegistryBackedResolutionUntilActivation()
    {
        ConfigDocument loaded = new ConfigDocument();
        loaded.ai.affectedEntityIdList = new java.util.ArrayList<>(java.util.List.of(
                "minecraft:zombie",
                "minecraft:not_a_real_entity"
        ));
        ConfigFileReport fileReport = new ConfigFileReport();
        LoadedDocumentStorage storage = new LoadedDocumentStorage(loaded, fileReport);
        TestConfig service = unstarted(new ConfigDocument(), storage);

        ConfigLoadResult loadResult = service.repository().load();

        assertTrue(loadResult.success());
        assertFalse(loadResult.saved());
        assertEquals(0, storage.saveCount);
        assertThrows(IllegalStateException.class, service::snapshot);
        assertEquals(
                java.util.List.of("minecraft:zombie", "minecraft:not_a_real_entity"),
                service.repository().value(descriptor("ai.affectedEntityIdList"))
        );
        assertFalse(fileReport.changed());

        AtomicInteger blockResolutions = new AtomicInteger();
        ConfigLoadResult activationResult = service.repository().activate(
                (rawValue, defaultBlock) ->
                {
                    blockResolutions.incrementAndGet();
                    return defaultBlock;
                },
                new RemovingListSemanticValidator()
        );

        assertTrue(activationResult.success());
        assertFalse(activationResult.saved());
        assertEquals(0, storage.saveCount);
        assertTrue(blockResolutions.get() > 0);
        assertEquals(
                java.util.List.of("minecraft:zombie"),
                service.snapshot().document().ai.affectedEntityIdList
        );
    }

    @Test
    void runtimeStartValidatesLastLoadedDocumentWhenReloadAndRecoveryFail()
    {
        ConfigDocument loaded = new ConfigDocument();
        loaded.ai.affectedEntityIdList = new java.util.ArrayList<>(java.util.List.of(
                "minecraft:zombie",
                "minecraft:not_a_real_entity"
        ));
        BootstrapThenFailingStorage storage = new BootstrapThenFailingStorage(loaded);
        TestConfig service = unstarted(new ConfigDocument(), storage);

        assertTrue(service.repository().load().success());
        ConfigLoadResult result = service.repository().activate(
                ConfigRuntime.BlockResolver.NONE,
                new RemovingListSemanticValidator()
        );

        assertFalse(result.success());
        assertEquals(
                java.util.List.of("minecraft:zombie"),
                service.snapshot().document().ai.affectedEntityIdList
        );
        assertTrue(result.availabilityReport().hasEntries());
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
        return new ConfigDocumentNormalizer(DESERIALIZER);
    }

    private static TestConfig service(Path path)
    {
        return service(new ConfigFileStore(path));
    }

    private static TestConfig service(ConfigStorage storage)
    {
        return service(new ConfigDocument(), storage, ConfigSemanticValidator.NONE);
    }

    private static TestConfig service(
            ConfigDocument initialDocument,
            ConfigStorage storage,
            ConfigSemanticValidator semanticValidator
    )
    {
        TestConfig config = unstarted(initialDocument, storage);
        config.repository().activate(ConfigRuntime.BlockResolver.NONE, semanticValidator);
        return config;
    }

    private static TestConfig unstarted(ConfigDocument initialDocument, ConfigStorage storage)
    {
        ConfigRepository repository = new ConfigRepository(initialDocument, storage);
        return new TestConfig(repository, new ConfigEditService(repository));
    }

    private record TestConfig(ConfigRepository repository, ConfigEditService editor)
    {
        private ConfigEditResult edit(ConfigEditRequest request)
        {
            return editor.edit(request);
        }

        private ConfigEditResult editRaw(ConfigEditRequest request)
        {
            return editor.editRaw(request);
        }

        private ConfigSnapshot snapshot()
        {
            return repository.snapshot();
        }

        private ConfigLoadResult reload()
        {
            return repository.reload();
        }
    }

    private static final class FailingConfigFileStore implements ConfigStorage
    {
        private FailingConfigFileStore(Path path)
        {
        }

        @Override
        public LoadedConfig load() throws ConfigStorageException
        {
            return new LoadedConfig(new ConfigDocument(), new ConfigFileReport());
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

    private static final class BootstrapThenFailingStorage implements ConfigStorage
    {
        private final ConfigDocument document;
        private int loadCount;

        private BootstrapThenFailingStorage(ConfigDocument document)
        {
            this.document = document;
        }

        @Override
        public LoadedConfig load() throws ConfigStorageException
        {
            if (loadCount++ == 0)
            {
                return new LoadedConfig(document, new ConfigFileReport());
            }
            throw new ConfigPersistenceException("expected reload failure", new RuntimeException("expected"));
        }

        @Override
        public void save(ConfigDocument data)
        {
            fail("save should not run");
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
        private final ConfigFileReport fileReport;

        private FailingNormalizedSaveStorage(ConfigDocument document, ConfigFileReport fileReport)
        {
            this.document = document;
            this.fileReport = fileReport;
        }

        @Override
        public LoadedConfig load()
        {
            return new LoadedConfig(document, fileReport);
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
            return new LoadedConfig(new ConfigDocument(), new ConfigFileReport());
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
        private final ConfigFileReport fileReport;
        private ConfigDocument savedDocument;
        private int saveCount;

        private LoadedDocumentStorage(ConfigDocument document, ConfigFileReport fileReport)
        {
            this.document = document;
            this.fileReport = fileReport;
        }

        @Override
        public LoadedConfig load()
        {
            return new LoadedConfig(document, fileReport);
        }

        @Override
        public void save(ConfigDocument data)
        {
            saveCount++;
            savedDocument = ConfigUtilities.copyConfig(data);
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
        public Object resolveValue(
                ConfigFieldDescriptor descriptor,
                Object value,
                Object defaultValue,
                ConfigAvailabilityReport report
        )
        {
            if (!(value instanceof java.util.List<?> list) || !list.contains("minecraft:not_a_real_entity"))
            {
                return value;
            }

            java.util.List<Object> resolved = new java.util.ArrayList<>(list);
            resolved.remove("minecraft:not_a_real_entity");
            report.unavailable(descriptor.path(), "minecraft:not_a_real_entity", "Unknown entity id");
            return resolved;
        }
    }

    private static final class RemovingMapSemanticValidator implements ConfigSemanticValidator
    {
        @Override
        public void validate(ConfigFieldDescriptor descriptor, Object value)
        {
        }

        @Override
        public Object resolveValue(
                ConfigFieldDescriptor descriptor,
                Object value,
                Object defaultValue,
                ConfigAvailabilityReport report
        )
        {
            if (!(value instanceof java.util.Map<?, ?> map)
                    || map.keySet().stream().noneMatch(key -> String.valueOf(key).startsWith("oldmod:")))
            {
                return value;
            }

            java.util.Map<Object, Object> resolved = new java.util.LinkedHashMap<>(map);
            resolved.keySet().removeIf(key -> String.valueOf(key).startsWith("oldmod:"));
            return resolved;
        }
    }
}
