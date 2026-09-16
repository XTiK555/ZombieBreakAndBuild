package com.tik.zbb.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZbbConfigCommandTest
{
    private CommandDispatcher<CommandSourceStack> dispatcher;

    @BeforeAll
    static void bootstrapMinecraft()
    {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void createDispatcherWithoutMinecraftPermissionChecks()
    {
        CommandDispatcher<CommandSourceStack> productionDispatcher = new CommandDispatcher<>();
        ZbbConfigCommand.register(productionDispatcher);

        CommandNode<CommandSourceStack> productionConfig = productionDispatcher.getRoot()
                .getChild("zbb")
                .getChild("config");
        LiteralCommandNode<CommandSourceStack> testConfig = LiteralArgumentBuilder
                .<CommandSourceStack>literal("config")
                .build();
        for (CommandNode<CommandSourceStack> child : productionConfig.getChildren())
        {
            testConfig.addChild(child);
        }

        LiteralCommandNode<CommandSourceStack> testZbb = LiteralArgumentBuilder
                .<CommandSourceStack>literal("zbb")
                .then(testConfig)
                .build();
        dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot().addChild(testZbb);
    }

    @Test
    void valueCommandsTakeValuesWithoutAMode()
    {
        assertParses("zbb config set ai.alwaysSeeNearestPlayer true");
        assertParses("zbb config add ai.affectedEntityIdList minecraft:zombie");
        assertParses("zbb config remove ai.affectedEntityIdList minecraft:zombie");
        assertDoesNotParse("zbb config set ai.alwaysSeeNearestPlayer persistent true");
    }

    @Test
    void setGrammarComesFromSchemaAndUsesTypedArguments()
    {
        CommandNode<CommandSourceStack> set = configCommand("set");
        assertNull(set.getChild("path"));

        assertInstanceOf(BoolArgumentType.class, argumentType(set, "ai.alwaysSeeNearestPlayer", "boolean"));
        IntegerArgumentType radius = assertInstanceOf(IntegerArgumentType.class,
                argumentType(set, "balance.dangerousBlocksSearchRadius", "integer"));
        assertEquals(0, radius.getMinimum());
        assertEquals(16, radius.getMaximum());
        assertInstanceOf(ResourceKeyArgument.class, argumentType(set, "blocks.fallbackPlaceBlockId", "id"));
        assertTrue(dispatcher.getCompletionSuggestions(
                        dispatcher.parse("zbb config set ai.alwaysSeeNearestPlayer ", null))
                .join().getList().stream().map(suggestion -> suggestion.getText()).toList()
                .containsAll(List.of("false", "true")));
    }

    @Test
    void mapSetTakesSuggestedRegistryKeyThenTypedValue()
    {
        assertParses("zbb config set blocks.dimensionPlaceBlockIdMap minecraft:overworld minecraft:dirt");
        assertParses("zbb config set blocks.mobPlaceBlockIdOverrideMap minecraft:zombie minecraft:stone");
        assertParses("zbb config set balance.blockDamage.blockHealthOverrideMap minecraft:stone 20");
        assertDoesNotParse("zbb config set blocks.dimensionPlaceBlockIdMap minecraft:overworld=minecraft:dirt");

        CommandNode<CommandSourceStack> key = configCommand("set")
                .getChild("blocks.dimensionPlaceBlockIdMap")
                .getChild("key");
        assertInstanceOf(ResourceKeyArgument.class, ((ArgumentCommandNode<?, ?>) key).getType());
        assertInstanceOf(ResourceKeyArgument.class, ((ArgumentCommandNode<?, ?>) key.getChild("id")).getType());
    }

    @Test
    void setIsNotExposedForLists()
    {
        assertNull(configCommand("set").getChild("ai.affectedEntityIdList"));
        assertNull(configCommand("set").getChild("ai.ignoreBuildEntityIdList"));
    }

    @Test
    void collectionCommandsExposeSchemaPathsLikeSet()
    {
        assertNull(configCommand("add").getChild("path"));
        assertNull(configCommand("remove").getChild("path"));
        assertNotNull(configCommand("add").getChild("ai.affectedEntityIdList"));
        assertNotNull(configCommand("remove").getChild("blocks.dimensionPlaceBlockIdMap"));
        assertNull(configCommand("add").getChild("ai.alwaysSeeNearestPlayer"));
    }

    @Test
    void mapAddTakesSuggestedRegistryKeyThenTypedValue()
    {
        assertParses("zbb config add blocks.dimensionPlaceBlockIdMap minecraft:overworld minecraft:stone");
        assertDoesNotParse("zbb config add blocks.dimensionPlaceBlockIdMap minecraft:overworld=minecraft:stone");
    }

    @Test
    void mapRemoveAcceptsOnlyTypedKey()
    {
        assertParses("zbb config remove blocks.dimensionPlaceBlockIdMap minecraft:overworld");
        assertDoesNotParse("zbb config remove blocks.dimensionPlaceBlockIdMap minecraft:overworld=minecraft:stone");
        assertDoesNotParse("zbb config remove blocks.dimensionPlaceBlockIdMap minecraft:overworld minecraft:stone");

        Object type = argumentType(configCommand("remove"), "blocks.dimensionPlaceBlockIdMap", "key");
        assertInstanceOf(ResourceKeyArgument.class, type);
    }

    @Test
    void patternListsAcceptCodecCategoriesAndSuggestMobCategories()
    {
        assertParses("zbb config add ai.ignoreBreakEntityIdList @MONSTER");
        assertParses("zbb config remove ai.ignoreBreakEntityIdList !@CREATURE");

        List<String> suggestions = dispatcher.getCompletionSuggestions(
                        dispatcher.parse("zbb config add ai.ignoreBreakEntityIdList @m", null))
                .join().getList().stream().map(suggestion -> suggestion.getText()).toList();
        assertTrue(suggestions.contains("@monster"));
    }

    @Test
    void wildcardIsPartOfPatternInsteadOfSuggestionModifier()
    {
        assertEquals(0, ZbbConfigCommand.patternValueOffset("*:zombie", 0));
        assertEquals(1, ZbbConfigCommand.patternValueOffset("!*:zombie", 0));
        assertEquals(0, ZbbConfigCommand.patternValueOffset("@monster", 0));
    }

    @Test
    void mapEntrySetPreservesOtherKeysAndReplacesOnlyTargetValue()
    {
        java.util.Map<String, Object> updated = ZbbConfigCommand.mapWithEntry(
                java.util.Map.of("minecraft:overworld", "minecraft:stone", "minecraft:the_nether", "minecraft:netherrack"),
                "minecraft:overworld",
                "minecraft:dirt");

        assertEquals(java.util.Map.of(
                "minecraft:overworld", "minecraft:dirt",
                "minecraft:the_nether", "minecraft:netherrack"), updated);
    }

    @Test
    void commandsWithoutValuesDoNotRequireAMode()
    {
        assertParses("zbb config clear ai.affectedEntityIdList");
        assertParses("zbb config reset all");
        assertParses("zbb config reset ai.alwaysSeeNearestPlayer");
        assertNull(configCommand("clear").getChild("ai.alwaysSeeNearestPlayer"));
    }

    @Test
    void modeNamesAndRuntimeOverridesAreNotPartOfTheCommandGrammar()
    {
        assertDoesNotParse("zbb config clear ai.affectedEntityIdList runtime_only");
        assertDoesNotParse("zbb config reset all persistent");
        assertNull(configCommand("runtime_overrides"));
        assertNoModeNamesUnderValueCommand("set");
        assertNoModeNamesUnderValueCommand("add");
        assertNoModeNamesUnderValueCommand("remove");
    }

    private void assertParses(String command)
    {
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, null);

        assertFalse(result.getReader().canRead(), () -> "Unread command suffix: " + result.getReader().getRemaining());
        assertTrue(result.getExceptions().isEmpty(), () -> "Parse exceptions: " + result.getExceptions());
        assertNotNull(result.getContext().build(command).getCommand(), "Command has no executable node");
    }

    private void assertDoesNotParse(String command)
    {
        ParseResults<CommandSourceStack> result = dispatcher.parse(command, null);
        boolean completeExecutableCommand = !result.getReader().canRead()
                && result.getExceptions().isEmpty()
                && result.getContext().build(command).getCommand() != null;

        assertFalse(completeExecutableCommand, "Unexpectedly parsed: " + command);
    }

    private void assertNoModeNamesUnderValueCommand(String command)
    {
        String path = command.equals("set") ? "ai.alwaysSeeNearestPlayer" : "ai.affectedEntityIdList";
        CommandNode<CommandSourceStack> pathNode = configCommand(command).getChild(path);

        assertNotNull(pathNode);
        assertNull(pathNode.getChild("persistent"));
        assertNull(pathNode.getChild("runtime_only"));
    }

    private CommandNode<CommandSourceStack> configCommand(String command)
    {
        return dispatcher.getRoot()
                .getChild("zbb")
                .getChild("config")
                .getChild(command);
    }

    private Object argumentType(CommandNode<CommandSourceStack> parent, String path, String argument)
    {
        return ((ArgumentCommandNode<?, ?>) parent.getChild(path).getChild(argument)).getType();
    }
}
