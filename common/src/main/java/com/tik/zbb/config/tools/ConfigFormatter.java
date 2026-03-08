package com.tik.zbb.config.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ConfigFormatter
{
    public static void splitBlocks(Path path)
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

                String next = i + 1 < lines.size() ? lines.get(i + 1).trim() : "";
                if (!next.isEmpty() && !next.startsWith("["))
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
}
