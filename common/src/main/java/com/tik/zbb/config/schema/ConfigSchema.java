package com.tik.zbb.config.schema;

import com.tik.zbb.config.ConfigDocument;
import com.tik.zbb.config.annotations.ResourceLocationIntPairMap;
import com.tik.zbb.config.annotations.ResourceLocationPairMap;
import com.tik.zbb.config.annotations.ResourceLocationPatternList;
import com.tik.zbb.config.annotations.ResourceLocationString;
import com.tik.zbb.utilities.ConfigUtilities;

import java.lang.reflect.Field;
import java.util.*;

public final class ConfigSchema
{
    private static final Map<ConfigPath, ConfigFieldDescriptor> DESCRIPTORS = buildDescriptors();

    public static Collection<ConfigFieldDescriptor> descriptors()
    {
        return DESCRIPTORS.values();
    }

    public static Optional<ConfigFieldDescriptor> find(ConfigPath path)
    {
        return Optional.ofNullable(DESCRIPTORS.get(path));
    }

    public static List<ConfigFieldDescriptor> listUnder(ConfigPath path)
    {
        List<ConfigFieldDescriptor> result = new ArrayList<>();
        for (ConfigFieldDescriptor descriptor : DESCRIPTORS.values())
        {
            if (descriptor.path().isDescendantOf(path))
            {
                result.add(descriptor);
            }
        }
        return result;
    }

    public static boolean hasPathOrSection(ConfigPath path)
    {
        if (DESCRIPTORS.containsKey(path)) return true;
        for (ConfigFieldDescriptor descriptor : DESCRIPTORS.values())
        {
            if (descriptor.path().isDescendantOf(path)) return true;
        }
        return false;
    }

    private static Map<ConfigPath, ConfigFieldDescriptor> buildDescriptors()
    {
        Map<ConfigPath, ConfigFieldDescriptor> descriptors = new LinkedHashMap<>();
        collect(ConfigDocument.class, "", List.of(), descriptors);
        return Collections.unmodifiableMap(descriptors);
    }

    private static void collect(Class<?> type, String prefix, List<Field> ownerFields, Map<ConfigPath, ConfigFieldDescriptor> descriptors)
    {
        for (Field field : ConfigUtilities.getConfigFields(type))
        {
            String path = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();

            if (ConfigUtilities.isConfigSectionField(field))
            {
                List<Field> nestedOwnerFields = new ArrayList<>(ownerFields);
                nestedOwnerFields.add(field);
                collect(field.getType(), path, nestedOwnerFields, descriptors);
                continue;
            }

            descriptors.put(new ConfigPath(path), new ConfigFieldDescriptor(
                    new ConfigPath(path),
                    ownerFields,
                    field,
                    kindOf(field)
            ));
        }
    }

    private static ConfigValueKind kindOf(Field field)
    {
        Class<?> type = field.getType();
        if (type == boolean.class || type == Boolean.class) return ConfigValueKind.BOOLEAN;
        if (type == int.class || type == Integer.class) return ConfigValueKind.INT;
        if (type == double.class || type == Double.class) return ConfigValueKind.DOUBLE;
        if (type == float.class || type == Float.class) return ConfigValueKind.FLOAT;
        if (type == String.class && field.isAnnotationPresent(ResourceLocationString.class)) return ConfigValueKind.RESOURCE_LOCATION;
        if (type == String.class) return ConfigValueKind.STRING;
        if (List.class.isAssignableFrom(type) && field.isAnnotationPresent(ResourceLocationPatternList.class)) return ConfigValueKind.RESOURCE_LOCATION_PATTERN_LIST;
        if (List.class.isAssignableFrom(type)) return ConfigValueKind.STRING_LIST;
        if (Map.class.isAssignableFrom(type) && field.isAnnotationPresent(ResourceLocationPairMap.class)) return ConfigValueKind.RESOURCE_LOCATION_PAIR_MAP;
        if (Map.class.isAssignableFrom(type) && field.isAnnotationPresent(ResourceLocationIntPairMap.class)) return ConfigValueKind.RESOURCE_LOCATION_INT_PAIR_MAP;
        throw new IllegalArgumentException("Unsupported config field type: " + field);
    }
}
