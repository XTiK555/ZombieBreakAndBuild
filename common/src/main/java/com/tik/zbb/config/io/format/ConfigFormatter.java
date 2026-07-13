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
                    "# This config can be reloaded with /reload",
                    "# You do NOT need to restart the server for MOST changes",
                    "# Existing mobs may keep already-added AI goals until they respawn,",
                    "# but config values themselves are reloaded by /reload",
                    "# -----------------------------------------------------------------------------------",
                    "",
                    ""
            );

            String original = Files.readString(path, StandardCharsets.UTF_8);

            if (!original.contains("# This config can be reloaded with /reload"))
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
