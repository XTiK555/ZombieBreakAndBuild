package com.tik.zbb.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.config.edit.ConfigEditRequest;
import com.tik.zbb.config.edit.ConfigEditResult;
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
    private static final String MODE_ARGUMENT = "mode";
    private static final String SOURCE = "command";

    private static final SuggestionProvider<CommandSourceStack> LEAF_PATH_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(ConfigSchema.descriptors().stream()
                    .map(descriptor -> descriptor.path().value()), builder);

    private static final SuggestionProvider<CommandSourceStack> PATH_OR_SECTION_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(pathAndSectionSuggestions(), builder);

    private static final SuggestionProvider<CommandSourceStack> MODE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(Set.of(
                    ConfigWriteMode.PERSISTENT.commandName(),
                    ConfigWriteMode.RUNTIME_ONLY.commandName()
            ), builder);

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
                        .then(literal("set")
                                .then(argument(PATH_ARGUMENT, StringArgumentType.word())
                                        .suggests(LEAF_PATH_SUGGESTIONS)
                                        .then(argument(VALUE_ARGUMENT, StringArgumentType.word())
                                                .executes(context -> set(context, ConfigWriteMode.PERSISTENT))
                                                .then(argument(MODE_ARGUMENT, StringArgumentType.word())
                                                        .suggests(MODE_SUGGESTIONS)
                                                        .executes(context -> set(context, readMode(context)))))))
                        .then(literal("add")
                                .then(argument(PATH_ARGUMENT, StringArgumentType.word())
                                        .suggests(LEAF_PATH_SUGGESTIONS)
                                        .then(argument(ENTRY_ARGUMENT, StringArgumentType.word())
                                                .executes(context -> add(context, ConfigWriteMode.PERSISTENT))
                                                .then(argument(MODE_ARGUMENT, StringArgumentType.word())
                                                        .suggests(MODE_SUGGESTIONS)
                                                        .executes(context -> add(context, readMode(context)))))))
                        .then(literal("remove")
                                .then(argument(PATH_ARGUMENT, StringArgumentType.word())
                                        .suggests(LEAF_PATH_SUGGESTIONS)
                                        .then(argument(ENTRY_ARGUMENT, StringArgumentType.word())
                                                .executes(context -> remove(context, ConfigWriteMode.PERSISTENT))
                                                .then(argument(MODE_ARGUMENT, StringArgumentType.word())
                                                        .suggests(MODE_SUGGESTIONS)
                                                        .executes(context -> remove(context, readMode(context)))))))
                        .then(literal("clear")
                                .then(argument(PATH_ARGUMENT, StringArgumentType.word())
                                        .suggests(LEAF_PATH_SUGGESTIONS)
                                        .executes(context -> edit(context, ConfigEditRequest.clear(readPath(context), ConfigWriteMode.PERSISTENT, SOURCE)))
                                        .then(argument(MODE_ARGUMENT, StringArgumentType.word())
                                                .suggests(MODE_SUGGESTIONS)
                                                .executes(context -> edit(context, ConfigEditRequest.clear(readPath(context), readMode(context), SOURCE))))))
                        .then(literal("reset")
                                .then(literal("all")
                                        .executes(context -> edit(context, ConfigEditRequest.resetAll(ConfigWriteMode.PERSISTENT, SOURCE)))
                                        .then(argument(MODE_ARGUMENT, StringArgumentType.word())
                                                .suggests(MODE_SUGGESTIONS)
                                                .executes(context -> edit(context, ConfigEditRequest.resetAll(readMode(context), SOURCE)))))
                                .then(argument(PATH_ARGUMENT, StringArgumentType.word())
                                        .suggests(LEAF_PATH_SUGGESTIONS)
                                        .executes(context -> edit(context, ConfigEditRequest.reset(readPath(context), ConfigWriteMode.PERSISTENT, SOURCE)))
                                        .then(argument(MODE_ARGUMENT, StringArgumentType.word())
                                                .suggests(MODE_SUGGESTIONS)
                                                .executes(context -> edit(context, ConfigEditRequest.reset(readPath(context), readMode(context), SOURCE))))))
                        .then(literal("discard")
                                .executes(context -> edit(context, ConfigEditRequest.discardAll(SOURCE)))
                                .then(literal("all")
                                        .executes(context -> edit(context, ConfigEditRequest.discardAll(SOURCE))))
                                .then(argument(PATH_ARGUMENT, StringArgumentType.word())
                                        .suggests(PATH_OR_SECTION_SUGGESTIONS)
                                        .executes(context -> edit(context, ConfigEditRequest.discard(readPath(context), SOURCE)))))
                        .then(literal("reload")
                                .executes(ZbbConfigCommand::reload))));
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

    private static int set(CommandContext<CommandSourceStack> context, ConfigWriteMode writeMode)
    {
        ConfigFieldDescriptor descriptor = readDescriptor(context);
        if (descriptor == null) return 0;

        try
        {
            Object value = descriptor.codec().parseText(descriptor, StringArgumentType.getString(context, VALUE_ARGUMENT));
            return edit(context, ConfigEditRequest.set(descriptor.path(), value, writeMode, SOURCE));
        }
        catch (ConfigValidationException e)
        {
            return fail(context, descriptor.path() + ": " + e.getMessage());
        }
    }

    private static int add(CommandContext<CommandSourceStack> context, ConfigWriteMode writeMode)
    {
        ConfigFieldDescriptor descriptor = readListDescriptor(context);
        if (descriptor == null) return 0;

        try
        {
            String entry = parseSingleListEntry(descriptor, StringArgumentType.getString(context, ENTRY_ARGUMENT));
            return edit(context, ConfigEditRequest.add(descriptor.path(), entry, writeMode, SOURCE));
        }
        catch (ConfigValidationException e)
        {
            return fail(context, descriptor.path() + ": " + e.getMessage());
        }
    }

    private static int remove(CommandContext<CommandSourceStack> context, ConfigWriteMode writeMode)
    {
        ConfigFieldDescriptor descriptor = readListDescriptor(context);
        if (descriptor == null) return 0;

        try
        {
            String entry = parseSingleListEntry(descriptor, StringArgumentType.getString(context, ENTRY_ARGUMENT));
            return edit(context, ConfigEditRequest.remove(descriptor.path(), entry, writeMode, SOURCE));
        }
        catch (ConfigValidationException e)
        {
            return fail(context, descriptor.path() + ": " + e.getMessage());
        }
    }

    private static int edit(CommandContext<CommandSourceStack> context, ConfigEditRequest request)
    {
        if (request.writeMode() == null)
        {
            return fail(context, "Expected mode persistent or runtime_only");
        }

        ConfigEditResult result = ConfigManager.edit(request);
        if (!result.success())
        {
            return fail(context, result.message());
        }

        String valueSuffix = result.effectiveValue() == null ? "" : " = " + formatResultValue(result);
        success(context, result.operation().name().toLowerCase() + " " + (result.path() == null ? "all" : result.path()) + valueSuffix + ", mode=" + result.writeMode().commandName() + ", persisted=" + result.persisted());
        return Math.max(1, result.affectedCount());
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
        return String.valueOf(value);
    }

    private static ConfigFieldDescriptor readDescriptor(CommandContext<CommandSourceStack> context)
    {
        ConfigPath path = readPath(context);
        ConfigFieldDescriptor descriptor = ConfigSchema.find(path).orElse(null);
        if (descriptor == null)
        {
            fail(context, "Unknown config path: " + path);
        }
        return descriptor;
    }

    private static ConfigFieldDescriptor readListDescriptor(CommandContext<CommandSourceStack> context)
    {
        ConfigFieldDescriptor descriptor = readDescriptor(context);
        if (descriptor == null) return null;

        if (descriptor.kind() != ConfigValueKind.STRING_LIST)
        {
            fail(context, descriptor.path() + " is not a list");
            return null;
        }

        return descriptor;
    }

    @SuppressWarnings("unchecked")
    private static String parseSingleListEntry(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
    {
        List<String> values = (List<String>) descriptor.codec().parseText(descriptor, rawValue);
        if (values.size() != 1)
        {
            throw new ConfigValidationException("Expected one list entry");
        }
        return values.get(0);
    }

    private static int reload(CommandContext<CommandSourceStack> context)
    {
        ConfigManager.reload();
        success(context, "reloaded config and discarded temporary values");
        return 1;
    }

    private static ConfigPath readPath(CommandContext<CommandSourceStack> context)
    {
        return new ConfigPath(StringArgumentType.getString(context, PATH_ARGUMENT));
    }

    private static ConfigWriteMode readMode(CommandContext<CommandSourceStack> context)
    {
        return ConfigWriteMode.parse(StringArgumentType.getString(context, MODE_ARGUMENT)).orElse(null);
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
