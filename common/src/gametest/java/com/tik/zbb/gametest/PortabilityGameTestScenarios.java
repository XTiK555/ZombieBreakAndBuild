package com.tik.zbb.gametest;

import com.tik.zbb.Constants;
import com.tik.zbb.gametest.capability.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Difficulty;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class PortabilityGameTestScenarios
{
    public static final String TEST_MOD_ID = Constants.MOD_ID + "_portability_test";
    public static final String EMPTY_STRUCTURE_ID = Constants.MOD_ID + ":tests_empty";
    public static final String REPORT_FILE_PROPERTY = Constants.MOD_ID + ".portability.reportFile";
    public static final String SUITE_PROPERTY = Constants.MOD_ID + ".portability.suite";
    private static final Properties SUITE_IDS = loadSuiteIds();

    public static final Suite MIXIN_COMPATIBILITY = sharedEnvironmentSuite(suiteId("mixinCompatibility"), Difficulty.HARD, List.of(
            scenario("level_chunk_block_change_event", 40, MixinCompatibilityGameTests::levelChunkBlockChangeEvent),
            scenario("same_block_type_state_change_does_not_publish_block_changed_event", 40, MixinCompatibilityGameTests::sameBlockTypeStateChangeDoesNotPublishBlockChangedEvent),
            scenario("mob_accessors", 40, MixinCompatibilityGameTests::mobAccessors),
            scenario("targeting_conditions_accessor", 40, MixinCompatibilityGameTests::targetingConditionsAccessor),
            scenario("display_accessors", 40, MixinCompatibilityGameTests::displayAccessors),
            scenario("falling_block", 120, MixinCompatibilityGameTests::fallingBlock),
            dedicated(scenario("nearest_target_through_wall", 40, MixinCompatibilityGameTests::nearestTargetThroughWall)),
            dedicated(scenario("nearest_target_through_wall_preserves_original_selector", 40, MixinCompatibilityGameTests::nearestTargetThroughWallPreservesOriginalSelector)),
            dedicated(scenario("continue_target_through_wall", 40, MixinCompatibilityGameTests::continueTargetThroughWall))
    ));

    public static final Suite FUNCTIONAL = dedicatedEnvironmentSuite(suiteId("functional"), Difficulty.HARD, List.of(
            scenario("platform_and_loader_wiring_smoke", 160, PlatformWiringGameTests::platformAndLoaderWiringSmoke),
            scenario("through_wall_targeting_does_not_apply_to_unaffected_mob", 100, TargetingGameTests::throughWallTargetingDoesNotApplyToUnaffectedMob),
            scenario("always_see_nearest_player_respects_toggle_and_chooses_nearest_valid_player", 100, TargetingGameTests::alwaysSeeNearestPlayerRespectsToggleAndChoosesNearestValidPlayer),
            scenario("always_see_nearest_player_does_not_aggro_non_angry_neutral_mob", 100, TargetingGameTests::alwaysSeeNearestPlayerDoesNotAggroNonAngryNeutralMob),
            scenario("break_action_respects_override_and_hardness_cap", 240, ActionGameTests::breakActionRespectsOverrideAndHardnessCap),
            scenario("break_action_accumulates_damage_breaks_and_enforces_cooldown", 140, ActionGameTests::breakActionAccumulatesDamageBreaksAndEnforcesCooldown),
            scenario("ignore_break_mob_does_not_break_obstacle", 100, ActionGameTests::ignoreBreakMobDoesNotBreakObstacle),
            scenario("mob_built_block_is_temporarily_protected", 300, ActionGameTests::mobBuiltBlockIsTemporarilyProtected),
            scenario("effective_tool_breaks_block_before_baseline_mob", 140, ActionGameTests::effectiveToolBreaksBlockBeforeBaselineMob),
            scenario("build_action_places_blocks_and_enforces_cooldown", 300, ActionGameTests::buildActionPlacesBlocksAndEnforcesCooldown),
            scenario("build_action_rejects_non_replaceable", 100, ActionGameTests::buildActionRejectsNonReplaceable),
            scenario("ignore_build_mob_does_not_build_bridge", 100, ActionGameTests::ignoreBuildMobDoesNotBuildBridge),
            scenario("mob_uses_configured_build_block", 180, ActionGameTests::mobUsesConfiguredBuildBlock),
            scenario("mob_breaks_wall_to_reach_target", 360, GoalAndPathingGameTests::mobBreaksWallToReachTarget),
            scenario("mob_bridges_gap_to_reach_target", 240, GoalAndPathingGameTests::mobBridgesGapToReachTarget),
            scenario("mob_climbs_toward_higher_target", 360, GoalAndPathingGameTests::mobClimbsTowardHigherTarget),
            scenario("mitigate_dangerous_blocks_covers_or_breaks_danger", 180, GoalAndPathingGameTests::mitigateDangerousBlocksCoversOrBreaksDanger),
            scenario("reachable_path_does_not_modify_world", 160, GoalAndPathingGameTests::reachablePathDoesNotModifyWorld),
            scenario("mob_without_target_does_not_modify_world", 160, GoalAndPathingGameTests::mobWithoutTargetDoesNotModifyWorld),
            scenario("built_block_disappears_and_restores_previous_state", 160, BlockStorageGameTests::builtBlockDisappearsAndRestoresPreviousState),
            scenario("built_block_restores_after_chunk_unload_without_stale_visual", 6000, BlockStorageGameTests::builtBlockRestoresAfterChunkUnloadWithoutStaleVisual),
            scenario("external_replacement_cancels_built_block_restoration_without_overwrite", 160, BlockStorageGameTests::externalReplacementCancelsBuiltBlockRestorationWithoutOverwrite),
            scenario("broken_block_restores_after_ttl_without_loot", 160, BlockStorageGameTests::brokenBlockRestoresAfterTtlWithoutLoot),
            scenario("broken_container_restores_block_entity_nbt", 160, BlockStorageGameTests::brokenContainerRestoresBlockEntityNbt),
            scenario("occupied_broken_position_preserves_replacement_and_drops_stored_item", 160, BlockStorageGameTests::occupiedBrokenPositionPreservesReplacementAndDropsStoredItem),
            scenario("falling_build_disappear_moves_tracking_and_restores_landing_old_state", 220, BlockStorageGameTests::fallingBuildDisappearMovesTrackingAndRestoresLandingOldState),
            scenario("broken_blocks_restoring_disabled_keeps_normal_loot", 180, BlockStorageGameTests::brokenBlocksRestoringDisabledKeepsNormalLoot),
            scenario("built_blocks_disappearing_disabled_keeps_mob_block", 180, BlockStorageGameTests::builtBlocksDisappearingDisabledKeepsMobBlock),
            scenario("real_config_command_hot_reloads_existing_mob_behavior", 180, PlatformWiringGameTests::realConfigCommandHotReloadsExistingMobBehavior),
            scenario("nearest_target_through_wall_respects_solid_block_limit", 240, TargetingGameTests::nearestTargetThroughWallRespectsSolidBlockLimit),
            scenario("continue_target_through_wall_respects_solid_block_limit", 600, TargetingGameTests::continueTargetThroughWallRespectsSolidBlockLimit),
            scenario("mob_filter_config_changes_targeting_behavior", 120, TargetingGameTests::mobFilterConfigChangesTargetingBehavior),
            scenario("through_wall_targeting_disabled_uses_vanilla_visibility", 600, TargetingGameTests::throughWallTargetingDisabledUsesVanillaVisibility),
            scenario("build_block_config_hot_reload_affects_existing_mob", 220, ActionGameTests::buildBlockConfigHotReloadAffectsExistingMob)
    ));
    public static final List<Suite> SUITES = List.of(FUNCTIONAL, MIXIN_COMPATIBILITY);

    static
    {
        if (SUITES.stream().map(Suite::id).distinct().count() != SUITES.size())
            throw new IllegalStateException("Duplicate portability GameTest suite id");
        if (SUITES.stream().map(Suite::normalizedId).distinct().count() != SUITES.size())
            throw new IllegalStateException("Duplicate normalized portability GameTest suite id");
        List<Scenario> scenarios = SUITES.stream().flatMap(suite -> suite.scenarios().stream()).toList();
        if (scenarios.stream().map(Scenario::id).distinct().count() != scenarios.size())
            throw new IllegalStateException("Duplicate portability GameTest scenario id");
    }

    private PortabilityGameTestScenarios() {}

    private static Properties loadSuiteIds()
    {
        try (var input = PortabilityGameTestScenarios.class.getResourceAsStream("/portability-gametest-suites.properties"))
        {
            if (input == null) throw new IllegalStateException("Missing portability-gametest-suites.properties");
            Properties properties = new Properties();
            properties.load(input);
            return properties;
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Could not load portability GameTest suite ids", exception);
        }
    }

    private static SuiteId suiteId(String name)
    {
        String value = Objects.requireNonNull(SUITE_IDS.getProperty(name), "Missing GameTest suite id: " + name);
        return new SuiteId(value);
    }

    private static Scenario scenario(String id, int maxTicks, Consumer<GameTestHelper> test)
    {
        return new Scenario(id, maxTicks, test, false);
    }

    private static Scenario dedicated(Scenario scenario)
    {
        return scenario.withDedicatedEnvironment();
    }

    public static Suite selectedSuite()
    {
        return suite(System.getProperty(SUITE_PROPERTY, FUNCTIONAL.id()));
    }

    public static Suite suite(String id)
    {
        return SUITES.stream().filter(suite -> suite.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown GameTest suite: " + id));
    }

    private static Suite sharedEnvironmentSuite(SuiteId id, Difficulty difficulty, List<Scenario> scenarios)
    {
        return new Suite(id, difficulty, scenarios, EnvironmentSharing.SHARED);
    }

    private static Suite dedicatedEnvironmentSuite(SuiteId id, Difficulty difficulty, List<Scenario> scenarios)
    {
        return new Suite(id, difficulty, scenarios, EnvironmentSharing.DEDICATED);
    }

    public enum EnvironmentSharing
    {SHARED, DEDICATED}

    private record SuiteId(String external)
    {
        private String normalized() {return external.replace('-', '_');}
    }

    public static final class Suite
    {
        private static final Pattern VALID_ID = Pattern.compile("[a-z][a-z0-9]*(?:-[a-z0-9]+)*");
        private static final Pattern VALID_NORMALIZED_ID = Pattern.compile("[a-z][a-z0-9]*(?:_[a-z0-9]+)*");

        private final String id;
        private final String normalizedId;
        private final String testNamespace;
        private final Difficulty difficulty;
        private final List<Scenario> scenarios;
        private final Map<String, Scenario> scenariosById;
        private final EnvironmentSharing environmentSharing;

        private Suite(SuiteId suiteId, Difficulty difficulty, List<Scenario> scenarios,
                      EnvironmentSharing environmentSharing)
        {
            String id = suiteId.external();
            if (!VALID_ID.matcher(Objects.requireNonNull(id, "id")).matches())
                throw new IllegalArgumentException("Invalid portability GameTest suite id: " + id);
            String normalizedId = suiteId.normalized();
            if (!VALID_NORMALIZED_ID.matcher(Objects.requireNonNull(normalizedId, "normalizedId")).matches())
                throw new IllegalArgumentException("Invalid normalized portability GameTest suite id: " + normalizedId);
            this.id = id;
            this.normalizedId = normalizedId;
            this.testNamespace = Constants.MOD_ID + "_" + normalizedId;
            this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
            this.scenarios = List.copyOf(scenarios);
            this.environmentSharing = Objects.requireNonNull(environmentSharing, "environmentSharing");
            Map<String, Scenario> byId = new LinkedHashMap<>();
            for (Scenario scenario : scenarios)
                if (byId.put(scenario.id(), scenario) != null)
                    throw new IllegalStateException("Duplicate scenario id in " + id + ": " + scenario.id());
            this.scenariosById = Map.copyOf(byId);
        }

        public String id() {return id;}

        public String normalizedId() {return normalizedId;}

        public String testNamespace() {return testNamespace;}

        public Difficulty difficulty() {return difficulty;}

        public String environmentName(Scenario scenario)
        {
            return environmentSharing == EnvironmentSharing.SHARED && !scenario.dedicatedEnvironment()
                    ? "parallel"
                    : scenario.id();
        }

        public String environmentId(Scenario scenario)
        {
            return testNamespace + ":" + environmentName(scenario);
        }

        public List<String> reportNames(Scenario scenario)
        {
            return List.of(testNamespace + ":" + scenario.id(),
                    TEST_MOD_ID + ":" + normalizedId + "_" + scenario.id(),
                    normalizedId + "." + scenario.id(),
                    normalizedId.replace("_", "") + "." + scenario.id());
        }

        public List<Scenario> scenarios() {return scenarios;}

        public void run(Scenario scenario, GameTestHelper helper)
        {
            PortabilityGameTestLifecycle.ensureDifficulty(helper, difficulty);
            scenario.test().accept(helper);
        }

        public Scenario scenario(String id)
        {
            Scenario scenario = scenariosById.get(id);
            if (scenario == null) throw new IllegalArgumentException("Unknown scenario in " + this.id + ": " + id);
            return scenario;
        }
    }

    public record Scenario(String id, int maxTicks, Consumer<GameTestHelper> test, boolean dedicatedEnvironment)
    {
        private static final Pattern VALID_ID = Pattern.compile("[a-z][a-z0-9]*(?:_[a-z0-9]+)*");

        public Scenario
        {
            if (!VALID_ID.matcher(Objects.requireNonNull(id, "id")).matches())
                throw new IllegalArgumentException("Invalid portability GameTest scenario id: " + id);
            if (maxTicks <= 0) throw new IllegalArgumentException("maxTicks must be positive: " + maxTicks);
            Objects.requireNonNull(test, "test");
        }

        private Scenario withDedicatedEnvironment()
        {
            return new Scenario(id, maxTicks, test, true);
        }
    }
}
