package com.tik.zbb.config.io.format;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ConfigFormatter
{
    public static void format(Path path)
    {
        splitBlocks(path);
        addHeader(path);
    }

    private static void splitBlocks(Path path)
    {
        try
        {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            StringBuilder out = new StringBuilder();

            for (int i = 0; i < lines.size(); i++)
            {
                String line = lines.get(i);
                String trimmed = line.trim();

                out.append(line).append('\n');

                if (!trimmed.contains("=")) continue;
                if (isQuotedTableEntry(trimmed)) continue;

                String next = i + 1 < lines.size() ? lines.get(i + 1).trim() : "";
                if (!next.isEmpty() && !next.startsWith("[") && !trimmed.startsWith("#"))
                {
                    out.append('\n');
                }
            }

            Files.writeString(path, out.toString(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to format config", e);
        }
    }

    private static boolean isQuotedTableEntry(String trimmed)
    {
        return trimmed.startsWith("\"") || trimmed.startsWith("'");
    }

    private static void addHeader(Path path)
    {
        try
        {
            String header = String.join("\n",
                    "# -----------------------------------------------------------------------------------",
                    "# Zombies Break & Build config",
                    "#",
                    "# This config can be reloaded with /zbb config reload,",
                    "# and can be changed with `/zbb config ...` commands.",
                    "# You do NOT need to restart the server for changes.",
                    "#",
                    "# Pattern lists support the following special prefixes/wildcards:",
                    "#   !  Excludes a matching entry. Exclusions always take priority over inclusions.",
                    "#      Example: !minecraft:creeper",
                    "#",
                    "#   *  Wildcard for an entire namespace or path part.",
                    "#      minecraft:*  = every ID from the minecraft namespace",
                    "#      *:zombie      = any ID whose path is exactly 'zombie'",
                    "#      *:*           = every ID",
                    "#",
                    "#   @  Matches a mob category instead of a specific entity ID.",
                    "#      Example: @monster",
                    "#      Prefix it with ! to exclude a category, e.g. !@monster",
                    "#",
                    "# Examples can be combined in the same list:",
                    "#   [\"*:*\", \"!minecraft:creeper\", \"!@monster\"]",
                    "# -----------------------------------------------------------------------------------",
                    "",
                    ""
            );

            String original = Files.readString(path, StandardCharsets.UTF_8);

            if (!original.contains("# Zombies Break & Build config"))
            {
                original = header + original;
                Files.writeString(path, original, StandardCharsets.UTF_8);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to format config", e);
        }
    }
}
