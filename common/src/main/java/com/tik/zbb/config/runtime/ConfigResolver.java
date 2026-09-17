package com.tik.zbb.config.runtime;

import com.tik.zbb.config.ConfigDocument;
import com.tik.zbb.config.edit.ConfigSemanticValidator;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigSchema;
import com.tik.zbb.config.schema.ConfigValidationException;
import com.tik.zbb.utilities.ConfigUtilities;

import java.util.*;

final class ConfigResolver
{
    private final ConfigSemanticValidator validator;

    ConfigResolver(ConfigSemanticValidator validator)
    {
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    Resolution resolve(ConfigDocument fileDocument)
    {
        ConfigDocument runtimeDocument = ConfigUtilities.copyConfig(fileDocument);
        ConfigAvailabilityReport report = new ConfigAvailabilityReport();

        for (ConfigFieldDescriptor descriptor : ConfigSchema.descriptors())
        {
            Object fileValue = descriptor.getValue(runtimeDocument);
            Object runtimeValue = validator.resolveValue(descriptor, fileValue, descriptor.defaultValue(), report);
            if (!Objects.equals(fileValue, runtimeValue))
            {
                descriptor.setValue(runtimeDocument, runtimeValue);
            }
        }

        return new Resolution(runtimeDocument, report);
    }

    void validate(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
    {
        validator.validate(descriptor, value);
    }

    Object preserveUnavailable(Object fileValue, Object runtimeValue, Object updatedValue)
    {
        if (fileValue instanceof List<?> fileList
                && runtimeValue instanceof List<?> runtimeList
                && updatedValue instanceof List<?> updatedList)
        {
            List<Object> merged = new ArrayList<>(fileList);
            merged.removeIf(entry -> runtimeList.contains(entry) && !updatedList.contains(entry));
            for (Object entry : updatedList)
            {
                if (!merged.contains(entry)) merged.add(entry);
            }
            return merged;
        }

        if (fileValue instanceof Map<?, ?> fileMap
                && runtimeValue instanceof Map<?, ?> runtimeMap
                && updatedValue instanceof Map<?, ?> updatedMap)
        {
            Map<Object, Object> merged = new LinkedHashMap<>(fileMap);
            merged.keySet().removeIf(key -> runtimeMap.containsKey(key) && !updatedMap.containsKey(key));
            merged.putAll(updatedMap);
            return merged;
        }

        return updatedValue;
    }

    record Resolution(ConfigDocument document, ConfigAvailabilityReport report) {}
}
