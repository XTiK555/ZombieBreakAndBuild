package com.tik.zbb.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void valueCommandsAcceptModesWithoutModeLiteral()
    {
        assertParses("zbb config set ai.alwaysSeeNearestPlayer true");
        assertParses("zbb config set ai.alwaysSeeNearestPlayer persistent true");
        assertParses("zbb config set ai.alwaysSeeNearestPlayer runtime_only true");
        assertParses("zbb config add ai.affectedEntityIdList runtime_only minecraft:zombie");
        assertParses("zbb config remove ai.affectedEntityIdList persistent minecraft:zombie");
    }

    @Test
    void commandsWithoutValuesAcceptModesWithoutModeLiteral()
    {
        assertParses("zbb config clear ai.affectedEntityIdList runtime_only");
        assertParses("zbb config reset all persistent");
        assertParses("zbb config reset ai.alwaysSeeNearestPlayer runtime_only");
    }

    @Test
    void obsoleteModeLiteralIsNotPartOfTheCommandGrammar()
    {
        assertDoesNotParse("zbb config clear ai.affectedEntityIdList mode runtime_only");
        assertDoesNotParse("zbb config reset all mode persistent");
        assertNoModeLiteralUnderValueCommand("set");
        assertNoModeLiteralUnderValueCommand("add");
        assertNoModeLiteralUnderValueCommand("remove");
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

    private void assertNoModeLiteralUnderValueCommand(String command)
    {
        CommandNode<CommandSourceStack> pathNode = dispatcher.getRoot()
                .getChild("zbb")
                .getChild("config")
                .getChild(command)
                .getChild("path");

        assertNotNull(pathNode);
        assertTrue(pathNode.getChildren().stream().noneMatch(child -> child.getName().equals("mode")));
    }
}
