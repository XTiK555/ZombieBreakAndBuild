package com.tik.zbb.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.tik.zbb.config.ConfigManager;
import com.tik.zbb.config.annotations.Range;
import com.tik.zbb.config.annotations.ResourceLocationRegistry;
import com.tik.zbb.config.annotations.ResourceLocationSemantics;
import com.tik.zbb.config.edit.*;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigPath;
import com.tik.zbb.config.schema.ConfigSchema;
import com.tik.zbb.config.schema.ConfigValueKind;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.Permissions;

import java.util.*;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class ZbbConfigCommand
{
    private static final String VALUE_ARGUMENT = "value";
    private static final String ENTRY_ARGUMENT = "entry";
    private static final String KEY_ARGUMENT = "key";
    private static final SimpleCommandExceptionType EXPECTED_VALUE =
            new SimpleCommandExceptionType(Component.literal("Expected value"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(literal("zbb")
                .then(literal("config")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(listCommand())
                        .then(getCommand())
                        .then(runtimeOverridesCommand())
                        .then(editCommand(ConfigEditOperation.SET))
                        .then(editCommand(ConfigEditOperation.ADD))
                        .then(editCommand(ConfigEditOperation.REMOVE))
                        .then(clearCommand())
                        .then(resetCommand())
                        .then(literal("reload")
                                .executes(ZbbConfigCommand::reload))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> listCommand()
    {
        LiteralArgumentBuilder<CommandSourceStack> command = literal("list")
                .executes(context -> list(context, null));

        for (String path : pathAndSectionSuggestions())
        {
            ConfigPath configPath = new ConfigPath(path);
            command.then(literal(path)
                    .executes(context -> list(context, configPath)));
        }

        return command;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> getCommand()
    {
        LiteralArgumentBuilder<CommandSourceStack> command = literal("get");

        for (ConfigFieldDescriptor descriptor : ConfigSchema.descriptors())
        {
            ConfigPath path = descriptor.path();
            command.then(literal(path.value())
                    .executes(context -> get(context, path)));
        }

        return command;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> runtimeOverridesCommand()
    {
        LiteralArgumentBuilder<CommandSourceStack> discard = literal("discard")
                .executes(context -> edit(context, ConfigEditRequest.discardAll()));

        for (String path : pathAndSectionSuggestions())
        {
            ConfigPath configPath = new ConfigPath(path);
            discard.then(literal(path)
                    .executes(context -> edit(context, ConfigEditRequest.discard(configPath))));
        }

        return literal("runtime_overrides")
                .then(literal("get")
                        .executes(ZbbConfigCommand::getRuntimeOverrides))
                .then(discard);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> clearCommand()
    {
        LiteralArgumentBuilder<CommandSourceStack> command = literal("clear");

        for (ConfigFieldDescriptor descriptor : ConfigSchema.descriptors())
        {
            if (!isCollection(descriptor.kind())) continue;

            ConfigPath path = descriptor.path();
            command.then(literal(path.value())
                    .then(literal(ConfigWriteMode.PERSISTENT.commandName())
                            .executes(context -> edit(context, ConfigEditRequest.clear(path, ConfigWriteMode.PERSISTENT))))
                    .then(literal(ConfigWriteMode.RUNTIME_ONLY.commandName())
                            .executes(context -> edit(context, ConfigEditRequest.clear(path, ConfigWriteMode.RUNTIME_ONLY)))));
        }

        return command;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> resetCommand()
    {
        LiteralArgumentBuilder<CommandSourceStack> command = literal("reset")
                .then(literal("all")
                        .then(literal(ConfigWriteMode.PERSISTENT.commandName())
                                .executes(context -> edit(context, ConfigEditRequest.resetAll(ConfigWriteMode.PERSISTENT))))
                        .then(literal(ConfigWriteMode.RUNTIME_ONLY.commandName())
                                .executes(context -> edit(context, ConfigEditRequest.resetAll(ConfigWriteMode.RUNTIME_ONLY)))));

        for (ConfigFieldDescriptor descriptor : ConfigSchema.descriptors())
        {
            ConfigPath path = descriptor.path();
            command.then(literal(path.value())
                    .then(literal(ConfigWriteMode.PERSISTENT.commandName())
                            .executes(context -> edit(context, ConfigEditRequest.reset(path, ConfigWriteMode.PERSISTENT))))
                    .then(literal(ConfigWriteMode.RUNTIME_ONLY.commandName())
                            .executes(context -> edit(context, ConfigEditRequest.reset(path, ConfigWriteMode.RUNTIME_ONLY)))));
        }

        return command;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> editCommand(ConfigEditOperation operation)
    {
        LiteralArgumentBuilder<CommandSourceStack> command = literal(operationName(operation));

        for (ConfigFieldDescriptor descriptor : ConfigSchema.descriptors())
        {
            if (operation != ConfigEditOperation.SET && !isCollection(descriptor.kind())) continue;
            if (operation == ConfigEditOperation.SET && isCollection(descriptor.kind()) && !isMap(descriptor.kind())) continue;

            command.then(literal(descriptor.path().value())
                    .then(editMode(descriptor, ConfigWriteMode.PERSISTENT, operation))
                    .then(editMode(descriptor, ConfigWriteMode.RUNTIME_ONLY, operation)));
        }

        return command;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> editMode(ConfigFieldDescriptor descriptor, ConfigWriteMode writeMode, ConfigEditOperation operation)
    {
        var mode = literal(writeMode.commandName());

        return switch (operation)
        {
            case SET -> mode.then(setValueArgument(descriptor, writeMode));
            case ADD -> mode.then(addEntryArgument(descriptor, writeMode));
            case REMOVE -> mode.then(removeEntryArgument(descriptor, writeMode));
            default -> throw new IllegalArgumentException("Unsupported operation: " + operation);
        };
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> removeEntryArgument(ConfigFieldDescriptor descriptor, ConfigWriteMode writeMode)
    {
        if (isMap(descriptor.kind()))
        {
            return argument(KEY_ARGUMENT, identifierArgument(keyRegistry(descriptor)))
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                            collectionEntries(ConfigManager.getValueForMode(descriptor, writeMode)),
                            builder))
                    .executes(context -> remove(
                            context,
                            descriptor.path(),
                            readArgument(context, KEY_ARGUMENT),
                            writeMode));
        }

        RequiredArgumentBuilder<CommandSourceStack, String> entry = argument(ENTRY_ARGUMENT, StringArgumentType.greedyString());

        entry.suggests((context, builder) ->
                SharedSuggestionProvider.suggest(
                        collectionEntries(ConfigManager.getValueForMode(descriptor, writeMode)),
                        builder));

        return entry.executes(context -> remove(context, descriptor.path(), writeMode));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> addEntryArgument(ConfigFieldDescriptor descriptor, ConfigWriteMode writeMode)
    {
        if (isMap(descriptor.kind()))
        {
            return mapAddEntryArgument(descriptor, writeMode);
        }
        if (descriptor.kind() == ConfigValueKind.RESOURCE_LOCATION_PATTERN_LIST)
        {
            return patternAddEntryArgument(descriptor, writeMode);
        }

        ResourceLocationSemantics semantics = descriptor.resourceLocationSemantics();
        if (semantics != null && semantics.element() != ResourceLocationRegistry.NONE)
        {
            return argument(ENTRY_ARGUMENT, identifierArgument(semantics.element()))
                    .executes(context -> add(
                            context,
                            descriptor.path(),
                            readArgument(context, ENTRY_ARGUMENT),
                            writeMode));
        }

        return argument(ENTRY_ARGUMENT, StringArgumentType.greedyString())
                .executes(context -> add(
                        context,
                        descriptor.path(),
                        readRawValue(context, ENTRY_ARGUMENT),
                        writeMode));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> patternAddEntryArgument(ConfigFieldDescriptor descriptor, ConfigWriteMode writeMode)
    {
        ResourceLocationSemantics semantics = descriptor.resourceLocationSemantics();
        ArgumentType<?> elementType = identifierArgument(semantics == null
                ? ResourceLocationRegistry.NONE
                : semantics.element());

        return argument(ENTRY_ARGUMENT, StringArgumentType.greedyString())
                .suggests((context, builder) ->
                {
                    int offset = patternValueOffset(builder.getRemaining(), 0);
                    String remaining = builder.getRemaining().substring(offset);
                    if (remaining.indexOf('*') >= 0 || remaining.indexOf(',') >= 0)
                    {
                        return builder.buildFuture();
                    }

                    return elementType.listSuggestions(
                            context,
                            builder.createOffset(builder.getStart() + offset));
                })
                .executes(context -> add(
                        context,
                        descriptor.path(),
                        readSinglePattern(context, ENTRY_ARGUMENT, elementType),
                        writeMode));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> mapAddEntryArgument(ConfigFieldDescriptor descriptor, ConfigWriteMode writeMode)
    {
        String valueName = mapValueArgumentTypeName(descriptor.kind());

        return argument(KEY_ARGUMENT, identifierArgument(keyRegistry(descriptor)))
                .then(argument(valueName, mapValueArgumentType(descriptor))
                        .executes(context -> add(
                                context,
                                descriptor.path(),
                                readArgument(context, KEY_ARGUMENT)
                                        + "="
                                        + readArgument(context, valueName),
                                writeMode)));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> setValueArgument(ConfigFieldDescriptor descriptor, ConfigWriteMode writeMode)
    {
        Range range = descriptor.range();
        return switch (descriptor.kind())
        {
            case BOOLEAN -> scalarSetValueArgument("boolean", BoolArgumentType.bool(), descriptor, writeMode);
            case INT -> scalarSetValueArgument("integer", range == null
                    ? IntegerArgumentType.integer()
                    : IntegerArgumentType.integer((int) Math.ceil(range.min()), (int) Math.floor(range.max())), descriptor, writeMode);
            case DOUBLE -> scalarSetValueArgument("number", range == null
                    ? DoubleArgumentType.doubleArg()
                    : DoubleArgumentType.doubleArg(range.min(), range.max()), descriptor, writeMode);
            case FLOAT -> scalarSetValueArgument("number", range == null
                    ? FloatArgumentType.floatArg()
                    : FloatArgumentType.floatArg((float) range.min(), (float) range.max()), descriptor, writeMode);
            case RESOURCE_LOCATION -> scalarSetValueArgument("id", identifierArgument(valueRegistry(descriptor)), descriptor, writeMode);
            case STRING -> scalarSetValueArgument(VALUE_ARGUMENT, StringArgumentType.greedyString(), descriptor, writeMode);
            case STRING_LIST, RESOURCE_LOCATION_PATTERN_LIST ->
                    throw new IllegalArgumentException("SET is not supported for list fields");
            case RESOURCE_LOCATION_PAIR_MAP, RESOURCE_LOCATION_INT_PAIR_MAP -> mapSetValueArgument(descriptor, writeMode);
        };
    }

    private static <T> RequiredArgumentBuilder<CommandSourceStack, T> scalarSetValueArgument
            (String name, ArgumentType<T> type, ConfigFieldDescriptor descriptor, ConfigWriteMode writeMode)
    {
        return argument(name, type).executes(context -> set(context, descriptor.path(), readArgument(context, name), writeMode));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> mapSetValueArgument(ConfigFieldDescriptor descriptor, ConfigWriteMode writeMode)
    {
        String valueName = mapValueArgumentTypeName(descriptor.kind());

        return argument(KEY_ARGUMENT, identifierArgument(keyRegistry(descriptor)))
                .then(argument(valueName, mapValueArgumentType(descriptor))
                        .executes(context -> setMapEntry(
                                context,
                                descriptor,
                                readArgument(context, KEY_ARGUMENT),
                                readMapValue(context, valueName),
                                writeMode)));
    }

    private static String readSinglePattern(CommandContext<CommandSourceStack> context, String argument, ArgumentType<?> elementType) throws CommandSyntaxException
    {
        String value = readRawValue(context, argument);
        validatePattern(value, elementType);
        return value;
    }

    private static void validatePattern(String value, ArgumentType<?> elementType) throws CommandSyntaxException
    {
        int prefixLength = 0;
        while (prefixLength < value.length() && isPatternModifier(value.charAt(prefixLength)))
        {
            prefixLength++;
        }

        if (prefixLength == value.length())
        {
            throw EXPECTED_VALUE.create();
        }

        String prefix = value.substring(0, prefixLength)
                .replace("@", "")
                .replace("!", "")
                .replace("*", "x");

        String pattern = value.substring(prefixLength)
                .replace('*', 'x');

        StringReader validationReader = new StringReader(prefix + pattern);
        readArgument(validationReader, elementType);

        if (validationReader.canRead())
        {
            throw EXPECTED_VALUE.createWithContext(validationReader);
        }
    }

    private static String readArgument(StringReader reader, ArgumentType<?> type) throws CommandSyntaxException
    {
        Object value = type.parse(reader);
        return value instanceof ResourceKey<?> key ? key.identifier().toString() : String.valueOf(value).trim();
    }

    private static ArgumentType<?> identifierArgument(ResourceLocationRegistry registry)
    {
        return switch (registry)
        {
            case BLOCK -> ResourceKeyArgument.key(Registries.BLOCK);
            case ENTITY -> ResourceKeyArgument.key(Registries.ENTITY_TYPE);
            case DIMENSION -> ResourceKeyArgument.key(Registries.DIMENSION);
            case NONE -> IdentifierArgument.id();
        };
    }

    private static ResourceLocationRegistry keyRegistry(ConfigFieldDescriptor descriptor)
    {
        ResourceLocationSemantics semantics = descriptor.resourceLocationSemantics();
        return semantics == null ? ResourceLocationRegistry.NONE : semantics.key();
    }

    private static ResourceLocationRegistry valueRegistry(ConfigFieldDescriptor descriptor)
    {
        ResourceLocationSemantics semantics = descriptor.resourceLocationSemantics();
        return semantics == null ? ResourceLocationRegistry.NONE : semantics.value();
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

    private static int get(CommandContext<CommandSourceStack> context, ConfigPath path)
    {
        ConfigFieldDescriptor descriptor = ConfigSchema.find(path).orElse(null);
        if (descriptor == null)
        {
            return fail(context, "Unknown config path: " + path);
        }

        success(context, descriptor.path() + " = " + format(ConfigManager.getEffectiveValue(descriptor)));
        return 1;
    }

    private static int getRuntimeOverrides(CommandContext<CommandSourceStack> context)
    {
        Map<ConfigPath, Object> overrides = ConfigManager.getRuntimeOverrides();
        success(context, "runtime overrides (" + overrides.size() + ")");
        for (Map.Entry<ConfigPath, Object> entry : overrides.entrySet())
        {
            success(context, entry.getKey() + " = " + format(entry.getValue()));
        }
        return overrides.size();
    }

    private static int set(CommandContext<CommandSourceStack> context, ConfigPath path, String rawValue, ConfigWriteMode writeMode)
    {
        return editRaw(context, ConfigEditRequest.set(path, rawValue, writeMode));
    }

    private static int setMapEntry(CommandContext<CommandSourceStack> context, ConfigFieldDescriptor descriptor, String key, Object value,
                                   ConfigWriteMode writeMode)
    {
        Object currentValue = ConfigManager.getValueForMode(descriptor, writeMode);
        if (!(currentValue instanceof java.util.Map<?, ?> currentMap))
        {
            return fail(context, descriptor.path() + " is not a map");
        }

        return edit(context, ConfigEditRequest.set(
                descriptor.path(),
                mapWithEntry(currentMap, key, value),
                writeMode));
    }

    static java.util.Map<String, Object> mapWithEntry(java.util.Map<?, ?> currentMap, String key, Object value)
    {
        java.util.Map<String, Object> updated = new LinkedHashMap<>();
        currentMap.forEach((currentKey, currentValue) -> updated.put(String.valueOf(currentKey), currentValue));
        updated.put(key, value);
        return updated;
    }
    {
        return editRaw(context, ConfigEditRequest.remove(
                path,
                readRawValue(context, ENTRY_ARGUMENT),
    private static int remove(CommandContext<CommandSourceStack> context, ConfigPath path, String entry, ConfigWriteMode writeMode)
    {
        return editRaw(context, ConfigEditRequest.remove(path, entry, writeMode));
    }

    private static int edit(CommandContext<CommandSourceStack> context, ConfigEditRequest request)
    {
        return handleEditResult(context, ConfigManager.edit(request));
    }

    private static int editRaw(CommandContext<CommandSourceStack> context, ConfigEditRequest request)
    {
        return handleEditResult(context, ConfigManager.editRaw(request));
    }

    private static int handleEditResult(CommandContext<CommandSourceStack> context, ConfigEditResult result)
    {
        if (!result.success())
        {
            return fail(context, result.message());
        }

        String valueSuffix = result.effectiveValue() == null
                ? ""
                : " = " + formatResultValue(result);

        String modeSuffix = result.writeMode() == null
                ? ""
                : ", mode=" + result.writeMode().commandName();

        String pathSuffix = result.path() == null
                ? result.operation() == ConfigEditOperation.DISCARD_ALL_OVERRIDES ? "" : " all"
                : " " + result.path();

        success(context,
                operationName(result.operation())
                        + pathSuffix
                        + valueSuffix
                        + ", changed=" + result.affectedCount()
                        + modeSuffix);

        return result.affectedCount();
    }

    private static String operationName(ConfigEditOperation operation)
    {
        return switch (operation)
        {
            case RESET_TO_DEFAULT, RESET_ALL_TO_DEFAULTS -> "reset";
            case REVERT_TO_PERSISTED, DISCARD_ALL_OVERRIDES -> "runtime_overrides discard";
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

    private static String readRawValue(CommandContext<CommandSourceStack> context, String argument)
    {
        return StringArgumentType.getString(context, argument).trim();
    }

    private static String readArgument(CommandContext<CommandSourceStack> context, String argument)
    {
        Object value = context.getArgument(argument, Object.class);
        return value instanceof ResourceKey<?> key ? key.identifier().toString() : String.valueOf(value).trim();
    }

    private static Object readMapValue(CommandContext<CommandSourceStack> context, String argument)
    {
        Object value = context.getArgument(argument, Object.class);
        return value instanceof ResourceKey<?> key ? key.identifier().toString() : value;
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

    private static int patternValueOffset(String value, int offset)
    {
        while (offset < value.length() && Character.isWhitespace(value.charAt(offset)))
        {
            offset++;
        }
        while (offset < value.length() && isPatternModifier(value.charAt(offset)))
        {
            offset++;
        }
        return offset;
    }

    private static boolean isPatternModifier(char value)
    {
        return value == '@' || value == '!' || value == '*';
    }

    private static ArgumentType<?> mapValueArgumentType(ConfigFieldDescriptor descriptor)
    {
        return descriptor.kind() == ConfigValueKind.RESOURCE_LOCATION_INT_PAIR_MAP
                ? IntegerArgumentType.integer(0)
                : identifierArgument(valueRegistry(descriptor));
    }

    private static String mapValueArgumentTypeName(ConfigValueKind kind)
    {
        return kind == ConfigValueKind.RESOURCE_LOCATION_INT_PAIR_MAP ? "integer" : "id";
    }

    private static Collection<String> collectionEntries(Object value)
    {
        if (value instanceof List<?> list) return list.stream().map(String::valueOf).toList();
        if (value instanceof java.util.Map<?, ?> map) return map.keySet().stream().map(String::valueOf).toList();
        return List.of();
    }

    private static boolean isCollection(ConfigValueKind kind)
    {
        return kind == ConfigValueKind.STRING_LIST
                || kind == ConfigValueKind.RESOURCE_LOCATION_PATTERN_LIST
                || isMap(kind);
    }

    private static boolean isMap(ConfigValueKind kind)
    {
        return kind == ConfigValueKind.RESOURCE_LOCATION_PAIR_MAP
                || kind == ConfigValueKind.RESOURCE_LOCATION_INT_PAIR_MAP;
    }
}