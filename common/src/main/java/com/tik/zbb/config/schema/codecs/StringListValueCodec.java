package com.tik.zbb.config.schema.codecs;

import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigRepairReport;
import com.tik.zbb.config.schema.ConfigValidationException;
import com.tik.zbb.config.schema.ConfigValueCodec;

import java.util.ArrayList;
import java.util.List;

public class StringListValueCodec implements ConfigValueCodec
{
    public static final StringListValueCodec INSTANCE = new StringListValueCodec();

    protected StringListValueCodec() {}

    @Override
    public Object parseText(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
    {
        List<String> values = new ArrayList<>();
        if (rawValue.isBlank()) return values;

        for (String entry : rawValue.split(","))
        {
            values.add(normalizeEntry(entry.trim()));
        }
        return values;
    }

    @Override
    public Object decodeDocumentValue(ConfigFieldDescriptor descriptor, Object rawValue) throws ConfigValidationException
    {
        if (!(rawValue instanceof List<?> list))
        {
            throw new ConfigValidationException("Expected list");
        }

        List<String> values = new ArrayList<>();
        for (Object entry : list)
        {
            if (!(entry instanceof String s))
            {
                throw new ConfigValidationException("Expected string list entry");
            }
            values.add(normalizeEntry(s));
        }
        return values;
    }

    @Override
    public Object repairDocumentValue(ConfigFieldDescriptor descriptor, Object rawValue, Object defaultValue, ConfigRepairReport report)
    {
        if (!(rawValue instanceof List<?> list))
        {
            Object fixedValue = descriptor.copyValue(defaultValue);
            report.repaired(descriptor.path(), rawValue, fixedValue, "Expected list");
            return fixedValue;
        }

        List<String> cleaned = new ArrayList<>();
        boolean repaired = false;
        for (Object entry : list)
        {
            try
            {
                if (!(entry instanceof String s))
                {
                    throw new ConfigValidationException("Expected string list entry");
                }

                String decodedEntry = normalizeEntry(s);
                cleaned.add(decodedEntry);
                if (!entry.equals(decodedEntry)) repaired = true;
            }
            catch (ConfigValidationException e)
            {
                repaired = true;
                report.repaired(descriptor.path(), entry, "<removed>", e.getMessage());
            }
        }

        if (repaired)
        {
            report.repaired(descriptor.path(), rawValue, cleaned, "Repaired list entries");
        }

        return cleaned;
    }

    @Override
    public Object normalizeValue(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
    {
        return decodeDocumentValue(descriptor, value);
    }

    @Override
    public boolean supportsCollectionEdits()
    {
        return true;
    }

    @Override
    public Object emptyValue(ConfigFieldDescriptor descriptor)
    {
        return new ArrayList<String>();
    }

    @Override
    public Object parseEntry(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
    {
        return normalizeEntry(rawValue);
    }

    @Override
    public Object addEntry(ConfigFieldDescriptor descriptor, Object value, Object entry) throws ConfigValidationException
    {
        List<String> values = copyList(value);
        if (!(entry instanceof String s))
        {
            throw new ConfigValidationException("Expected string list entry");
        }
        String normalizedEntry = normalizeEntry(s);
        if (!values.contains(normalizedEntry)) values.add(normalizedEntry);
        return values;
    }

    @Override
    public Object removeEntry(ConfigFieldDescriptor descriptor, Object value, Object entry) throws ConfigValidationException
    {
        List<String> values = copyList(value);
        if (!(entry instanceof String s))
        {
            throw new ConfigValidationException("Expected string list entry");
        }
        String normalizedEntry = normalizeEntry(s);
        values.removeIf(normalizedEntry::equals);
        return values;
    }

    protected String normalizeEntry(String rawValue) throws ConfigValidationException
    {
        if (rawValue.isBlank()) throw new ConfigValidationException("List entry cannot be empty");
        return rawValue;
    }

    private static List<String> copyList(Object value) throws ConfigValidationException
    {
        if (!(value instanceof List<?> list))
        {
            throw new ConfigValidationException("Expected list");
        }

        List<String> values = new ArrayList<>();
        for (Object entry : list)
        {
            if (!(entry instanceof String s))
            {
                throw new ConfigValidationException("Expected string list entry");
            }
            values.add(s);
        }
        return values;
    }
}
