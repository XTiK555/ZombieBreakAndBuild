package com.tik.zbb.config.document;

import com.tik.zbb.config.ConfigDocument;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigSchema;
import com.tik.zbb.utilities.ConfigUtilities;

import java.lang.reflect.Field;

public final class ConfigDocumentCopier
{
    public static ConfigDocument copy(ConfigDocument data)
    {
        ConfigDocument copy = new ConfigDocument();
        copyNestedSections(data, copy);

        for (ConfigFieldDescriptor descriptor : ConfigSchema.descriptors())
        {
            descriptor.setValue(copy, descriptor.getValue(data));
        }

        return copy;
    }

    private static void copyNestedSections(Object source, Object target)
    {
        for (Field field : ConfigUtilities.getConfigFields(source.getClass()))
        {
            if (!ConfigUtilities.isNestedConfigField(field)) continue;

            try
            {
                Object nestedSource = field.get(source);
                Object nestedTarget = nestedSource == null ? null : nestedSource.getClass().getDeclaredConstructor().newInstance();
                field.set(target, nestedTarget);
                if (nestedSource != null)
                {
                    copyNestedSections(nestedSource, nestedTarget);
                }
            }
            catch (ReflectiveOperationException e)
            {
                throw new IllegalStateException("Failed to copy config section " + field.getName(), e);
            }
        }
    }

    private ConfigDocumentCopier() {}
}
