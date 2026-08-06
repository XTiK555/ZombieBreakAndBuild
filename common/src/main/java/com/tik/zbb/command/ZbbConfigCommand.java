package com.tik.zbb.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.config.edit.ConfigEditOperation;
import com.tik.zbb.config.edit.ConfigEditRequest;
import com.tik.zbb.config.edit.ConfigEditResult;
import com.tik.zbb.config.edit.ConfigEditService;
import com.tik.zbb.config.edit.ConfigWriteMode;
import com.tik.zbb.config.schema.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

import java.util.*;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class ZbbConfigCommand
{
    private static final String PATH_ARGUMENT = "path";
    private static final String VALUE_ARGUMENT = "value";
    private static final String ENTRY_ARGUMENT = "entry";
    private static final SuggestionProvider<CommandSourceStack> LEAF_PATH_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(ConfigSchema.descriptors().stream()
                    .map(descriptor -> descriptor.path().value()), builder);

    private static final SuggestionProvider<CommandSourceStack> PATH_OR_SECTION_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(pathAndSectionSuggestions(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(literal("zbb")
                .then(literal("config")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(literal("list")
                                .executes(context -> list(context, null))
                                .then(argument(PATH_ARGUMENT, StringArgumentType.word())
                                        .suggests(PATH_OR_SECTION_SUGGESTIONS)
                                        .executes(context -> list(context, readPath(context)))))
                        .then(literal("get")
                                .then(argument(PATH_ARGUMENT, StringArgumentType.word())
                                        .suggests(LEAF_PATH_SUGGESTIONS)
                                        .executes(ZbbConfigCommand::get)))
                        .then(literal("runtime_overrides")
                                .executes(ZbbConfigCommand::runtimeOverrides))
                        .then(literal("set")
                                .then(argument(PATH_ARGUMENT, StringArgumentType.word())
                                        .suggests(LEAF_PATH_SUGGESTIONS)
                                        .then(modeValue(ConfigWriteMode.PERSISTENT, VALUE_ARGUMENT, context -> set(context, ConfigWriteMode.PERSISTENT)))
                                        .then(modeValue(ConfigWriteMode.RUNTIME_ONLY, VALUE_ARGUMENT, context -> set(context, ConfigWriteMode.RUNTIME_ONLY)))
                                        .then(argument(VALUE_ARGUMENT, StringArgumentType.greedyString())
                                                .executes(context -> set(context, ConfigWriteMode.PERSISTENT)))))
                        .then(literal("add")
                                .then(argument(PATH_ARGUMENT, StringArgumentType.word())
                                        .suggests(LEAF_PATH_SUGGESTIONS)
                                        .then(modeValue(ConfigWriteMode.PERSISTENT, ENTRY_ARGUMENT, context -> add(context, ConfigWriteMode.PERSISTENT)))
                                        .then(modeValue(ConfigWriteMode.RUNTIME_ONLY, ENTRY_ARGUMENT, context -> add(context, ConfigWriteMode.RUNTIME_ONLY)))
                                        .then(argument(ENTRY_ARGUMENT, StringArgumentType.greedyString())
                                                .executes(context -> add(context, ConfigWriteMode.PERSISTENT)))))
                        .then(literal("remove")
                                .then(argument(PATH_ARGUMENT, StringArgumentType.word())
                                        .suggests(LEAF_PATH_SUGGESTIONS)
                                        .then(modeValue(ConfigWriteMode.PERSISTENT, ENTRY_ARGUMENT, context -> remove(context, ConfigWriteMode.PERSISTENT)))
                                        .then(modeValue(ConfigWriteMode.RUNTIME_ONLY, ENTRY_ARGUMENT, context -> remove(context, ConfigWriteMode.RUNTIME_ONLY)))
                                        .then(argument(ENTRY_ARGUMENT, StringArgumentType.greedyString())
                                                .executes(context -> remove(context, ConfigWriteMode.PERSISTENT)))))
                        .then(literal("clear")
                                .then(argument(PATH_ARGUMENT, StringArgumentType.word())
                                        .suggests(LEAF_PATH_SUGGESTIONS)
                                        .executes(context -> edit(context, ConfigEditRequest.clear(readPath(context), ConfigWriteMode.PERSISTENT)))
                                        .then(literal(ConfigWriteMode.PERSISTENT.commandName())
                                                .executes(context -> edit(context, ConfigEditRequest.clear(readPath(context), ConfigWriteMode.PERSISTENT))))
                                        .then(literal(ConfigWriteMode.RUNTIME_ONLY.commandName())
                                                .executes(context -> edit(context, ConfigEditRequest.clear(readPath(context), ConfigWriteMode.RUNTIME_ONLY))))))
                        .then(literal("reset")
                                .then(literal("all")
                                        .executes(context -> edit(context, ConfigEditRequest.resetAll(ConfigWriteMode.PERSISTENT)))
                                        .then(literal(ConfigWriteMode.PERSISTENT.commandName())
                                                .executes(context -> edit(context, ConfigEditRequest.resetAll(ConfigWriteMode.PERSISTENT))))
                                        .then(literal(ConfigWriteMode.RUNTIME_ONLY.commandName())
                                                .executes(context -> edit(context, ConfigEditRequest.resetAll(ConfigWriteMode.RUNTIME_ONLY)))))
                                .then(argument(PATH_ARGUMENT, StringArgumentType.word())
                                        .suggests(LEAF_PATH_SUGGESTIONS)
                                        .executes(context -> edit(context, ConfigEditRequest.reset(readPath(context), ConfigWriteMode.PERSISTENT)))
                                        .then(literal(ConfigWriteMode.PERSISTENT.commandName())
                                                .executes(context -> edit(context, ConfigEditRequest.reset(readPath(context), ConfigWriteMode.PERSISTENT))))
                                        .then(literal(ConfigWriteMode.RUNTIME_ONLY.commandName())
                                                .executes(context -> edit(context, ConfigEditRequest.reset(readPath(context), ConfigWriteMode.RUNTIME_ONLY))))))
                        .then(literal("discard_runtime_overrides")
                                .executes(context -> edit(context, ConfigEditRequest.discardAll()))
                                .then(literal("all")
                                        .executes(context -> edit(context, ConfigEditRequest.discardAll())))
                                .then(argument(PATH_ARGUMENT, StringArgumentType.word())
                                        .suggests(PATH_OR_SECTION_SUGGESTIONS)
                                        .executes(context -> edit(context, ConfigEditRequest.discard(readPath(context))))))
                        .then(literal("reload")
                                .executes(ZbbConfigCommand::reload))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> modeValue(
            ConfigWriteMode mode,
            String valueArgument,
            Command<CommandSourceStack> command)
    {
        return literal(mode.commandName())
                .then(argument(valueArgument, StringArgumentType.greedyString()).executes(command));
    }

    private static int list(CommandContext<CommandSourceStack> context, ConfigPath path)
    {
        Collection<ConfigFieldDescriptor> descriptors = path == null ? ConfigSchema.descriptors() : ConfigSchema.listUnder(path);
        if (descriptors.isEmpty())
        {
            return fail(context, "Unknown config path or section: " + path);
        }

        success(context, "list " + (path == null ? "all" : path) + " (" + descriptors.size() + ")");
        for (ConfigFieldDescriptor descriptor : descriptors)
        {
            success(context, descriptor.path() + " = " + format(ConfigManager.getEffectiveValue(descriptor)));
        }
        return descriptors.size();
    }

    private static int get(CommandContext<CommandSourceStack> context)
    {
        ConfigPath path = readPath(context);
        ConfigFieldDescriptor descriptor = ConfigSchema.find(path).orElse(null);
        if (descriptor == null)
        {
            return fail(context, "Unknown config path: " + path);
        }

        success(context, descriptor.path() + " = " + format(ConfigManager.getEffectiveValue(descriptor)));
        return 1;
    }

    private static int runtimeOverrides(CommandContext<CommandSourceStack> context)
    {
        Map<ConfigPath, Object> overrides = ConfigManager.getRuntimeOverrides();
        success(context, "runtime overrides (" + overrides.size() + ")");
        for (Map.Entry<ConfigPath, Object> entry : overrides.entrySet())
        {
            success(context, entry.getKey() + " = " + format(entry.getValue()));
        }
        return overrides.size();
    }

    private static int set(CommandContext<CommandSourceStack> context, ConfigWriteMode writeMode)
    {
        return editRaw(context, ConfigEditRequest.set(
                readPath(context),
                readRawValue(context, VALUE_ARGUMENT),
                writeMode
        ));
    }

    private static int add(CommandContext<CommandSourceStack> context, ConfigWriteMode writeMode)
    {
        return editRaw(context, ConfigEditRequest.add(
                readPath(context),
                readRawValue(context, ENTRY_ARGUMENT),
                writeMode
        ));
    }

    private static int remove(CommandContext<CommandSourceStack> context, ConfigWriteMode writeMode)
    {
        return editRaw(context, ConfigEditRequest.remove(
                readPath(context),
                readRawValue(context, ENTRY_ARGUMENT),
                writeMode
        ));
    }

    private static int edit(CommandContext<CommandSourceStack> context, ConfigEditRequest request)
    {
        ConfigEditResult result = ConfigManager.edit(request);
        if (!result.success())
        {
            return fail(context, result.message());
        }

        String valueSuffix = result.effectiveValue() == null ? "" : " = " + formatResultValue(result);
        success(context, operationName(result.operation()) + " " + (result.path() == null ? "all" : result.path()) + valueSuffix + ", changed=" + result.affectedCount() + ", mode=" + result.writeMode().commandName() + ", persisted=" + result.persisted());
        return Math.max(1, result.affectedCount());
    }

    private static int editRaw(CommandContext<CommandSourceStack> context, ConfigEditRequest request)
    {
        ConfigEditResult result = ConfigManager.editRaw(request);
        if (!result.success())
        {
            return fail(context, result.message());
        }

        String valueSuffix = result.effectiveValue() == null ? "" : " = " + formatResultValue(result);
        success(context, operationName(result.operation()) + " " + (result.path() == null ? "all" : result.path()) + valueSuffix + ", changed=" + result.affectedCount() + ", mode=" + result.writeMode().commandName() + ", persisted=" + result.persisted());
        return Math.max(1, result.affectedCount());
    }

    private static String operationName(ConfigEditOperation operation)
    {
        return switch (operation)
        {
            case RESET_TO_DEFAULT -> "reset_to_default";
            case RESET_ALL_TO_DEFAULTS -> "reset_all_to_defaults";
            case REVERT_TO_PERSISTED -> "revert_to_persisted";
            case DISCARD_ALL_OVERRIDES -> "discard_all_overrides";
            default -> operation.name().toLowerCase();
        };
    }

    private static String formatResultValue(ConfigEditResult result)
    {
        if (result.path() == null) return String.valueOf(result.effectiveValue());
        return ConfigSchema.find(result.path())
                .map(descriptor -> format(result.effectiveValue()))
                .orElse(String.valueOf(result.effectiveValue()));
    }

    private static String format(Object value)
    {
        if (value instanceof List<?> list)
        {
            List<String> entries = new ArrayList<>();
            for (Object entry : list)
            {
                entries.add(String.valueOf(entry));
            }
            return "[" + String.join(", ", entries) + "]";
        }
        if (value instanceof java.util.Map<?, ?> map)
        {
            List<String> entries = new ArrayList<>();
            for (java.util.Map.Entry<?, ?> entry : map.entrySet())
            {
                entries.add(entry.getKey() + "=" + entry.getValue());
            }
            return "{" + String.join(", ", entries) + "}";
        }
        return String.valueOf(value);
    }

    private static int reload(CommandContext<CommandSourceStack> context)
    {
        ConfigEditService.ConfigReloadResult result = ConfigManager.reload();
        if (!result.success())
        {
            return fail(context, result.message());
        }
        success(context, result.message() + " and discarded temporary values");
        return 1;
    }

    private static ConfigPath readPath(CommandContext<CommandSourceStack> context)
    {
        return new ConfigPath(StringArgumentType.getString(context, PATH_ARGUMENT));
    }

    private static String readRawValue(CommandContext<CommandSourceStack> context, String argument)
    {
        return StringArgumentType.getString(context, argument).trim();
    }

    private static int fail(CommandContext<CommandSourceStack> context, String message)
    {
        context.getSource().sendFailure(Component.literal("ZBB config: " + message));
        return 0;
    }

    private static void success(CommandContext<CommandSourceStack> context, String message)
    {
        context.getSource().sendSuccess(() -> Component.literal("ZBB config: " + message), false);
    }

    private static Set<String> pathAndSectionSuggestions()
    {
        Set<String> paths = new LinkedHashSet<>();
        for (ConfigFieldDescriptor descriptor : ConfigSchema.descriptors())
        {
            ConfigPath path = descriptor.path();
            paths.add(path.value());

            ConfigPath parent = path.parent();
            while (parent != null)
            {
                paths.add(parent.value());
                parent = parent.parent();
            }
        }
        return paths;
    }
}
