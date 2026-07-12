package com.tik.zbb.config.schema;

import com.tik.zbb.config.annotations.Range;
import com.tik.zbb.config.annotations.ResourceLocationIntPairList;
import com.tik.zbb.config.annotations.ResourceLocationPairList;
import com.tik.zbb.config.annotations.ResourceLocationPatternList;
import com.tik.zbb.config.annotations.ResourceLocationString;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class ConfigValueCodecs
{
    public static ConfigValueCodec forKind(ConfigValueKind kind)
    {
        return switch (kind)
        {
            case BOOLEAN -> BooleanCodec.INSTANCE;
            case INT -> IntCodec.INSTANCE;
            case DOUBLE -> DoubleCodec.INSTANCE;
            case FLOAT -> FloatCodec.INSTANCE;
            case STRING -> StringCodec.INSTANCE;
            case STRING_LIST -> StringListCodec.INSTANCE;
        };
    }

    private static final class BooleanCodec implements ConfigValueCodec
    {
        private static final BooleanCodec INSTANCE = new BooleanCodec();

        @Override
        public Object parseText(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
        {
            if ("true".equalsIgnoreCase(rawValue)) return true;
            if ("false".equalsIgnoreCase(rawValue)) return false;
            throw new ConfigValidationException("Expected true or false");
        }

        @Override
        public Object decodeDocumentValue(ConfigFieldDescriptor descriptor, Object rawValue) throws ConfigValidationException
        {
            if (rawValue instanceof Boolean) return rawValue;
            throw new ConfigValidationException("Expected boolean");
        }

        @Override
        public void validateValue(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
        {
            if (!(value instanceof Boolean))
            {
                throw new ConfigValidationException("Expected boolean");
            }
        }
    }

    private abstract static class NumberCodec implements ConfigValueCodec
    {
        protected void validateRange(ConfigFieldDescriptor descriptor, double value) throws ConfigValidationException
        {
            if (!Double.isFinite(value))
            {
                throw new ConfigValidationException("Expected finite number");
            }

            Range range = descriptor.range();
            if (range != null && (value < range.min() || value > range.max()))
            {
                throw new ConfigValidationException("Expected value between " + range.min() + " and " + range.max());
            }
        }
    }

    private static final class IntCodec extends NumberCodec
    {
        private static final IntCodec INSTANCE = new IntCodec();

        @Override
        public Object parseText(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
        {
            try
            {
                int value = Integer.parseInt(rawValue);
                validateRange(descriptor, value);
                return value;
            }
            catch (NumberFormatException e)
            {
                throw new ConfigValidationException("Expected integer");
            }
        }

        @Override
        public Object decodeDocumentValue(ConfigFieldDescriptor descriptor, Object rawValue) throws ConfigValidationException
        {
            if (!(rawValue instanceof Number n))
            {
                throw new ConfigValidationException("Expected integer");
            }

            int value = n.intValue();
            validateRange(descriptor, value);
            return value;
        }

        @Override
        public void validateValue(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
        {
            if (!(value instanceof Integer i))
            {
                throw new ConfigValidationException("Expected integer");
            }
            validateRange(descriptor, i);
        }
    }

    private static final class DoubleCodec extends NumberCodec
    {
        private static final DoubleCodec INSTANCE = new DoubleCodec();

        @Override
        public Object parseText(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
        {
            try
            {
                double value = Double.parseDouble(rawValue);
                validateRange(descriptor, value);
                return value;
            }
            catch (NumberFormatException e)
            {
                throw new ConfigValidationException("Expected decimal number");
            }
        }

        @Override
        public Object decodeDocumentValue(ConfigFieldDescriptor descriptor, Object rawValue) throws ConfigValidationException
        {
            if (!(rawValue instanceof Number n))
            {
                throw new ConfigValidationException("Expected decimal number");
            }

            double value = n.doubleValue();
            validateRange(descriptor, value);
            return value;
        }

        @Override
        public void validateValue(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
        {
            if (!(value instanceof Number n))
            {
                throw new ConfigValidationException("Expected decimal number");
            }
            validateRange(descriptor, n.doubleValue());
        }
    }

    private static final class FloatCodec extends NumberCodec
    {
        private static final FloatCodec INSTANCE = new FloatCodec();

        @Override
        public Object parseText(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
        {
            try
            {
                float value = Float.parseFloat(rawValue);
                validateRange(descriptor, value);
                return value;
            }
            catch (NumberFormatException e)
            {
                throw new ConfigValidationException("Expected decimal number");
            }
        }

        @Override
        public Object decodeDocumentValue(ConfigFieldDescriptor descriptor, Object rawValue) throws ConfigValidationException
        {
            if (!(rawValue instanceof Number n))
            {
                throw new ConfigValidationException("Expected decimal number");
            }

            float value = n.floatValue();
            validateRange(descriptor, value);
            return value;
        }

        @Override
        public void validateValue(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
        {
            if (!(value instanceof Float f))
            {
                throw new ConfigValidationException("Expected decimal number");
            }
            validateRange(descriptor, f);
        }
    }

    private static final class StringCodec implements ConfigValueCodec
    {
        private static final StringCodec INSTANCE = new StringCodec();

        @Override
        public Object parseText(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
        {
            return normalizeString(descriptor, rawValue);
        }

        @Override
        public Object decodeDocumentValue(ConfigFieldDescriptor descriptor, Object rawValue) throws ConfigValidationException
        {
            if (!(rawValue instanceof String s))
            {
                throw new ConfigValidationException("Expected string");
            }
            return normalizeString(descriptor, s);
        }

        @Override
        public void validateValue(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
        {
            if (!(value instanceof String s))
            {
                throw new ConfigValidationException("Expected string");
            }
            normalizeString(descriptor, s);
        }

        private String normalizeString(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
        {
            if (descriptor.field().isAnnotationPresent(ResourceLocationString.class))
            {
                Identifier id = Identifier.tryParse(rawValue);
                if (id == null) throw new ConfigValidationException("Expected resource location");
                return id.toString();
            }
            return rawValue;
        }
    }

    private static final class StringListCodec implements ConfigValueCodec
    {
        private static final StringListCodec INSTANCE = new StringListCodec();

        @Override
        public Object parseText(ConfigFieldDescriptor descriptor, String rawValue) throws ConfigValidationException
        {
            List<String> values = new ArrayList<>();
            if (rawValue.isBlank()) return values;

            for (String entry : rawValue.split(","))
            {
                values.add(normalizeListEntry(descriptor.field(), entry.trim()));
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
                values.add(normalizeListEntry(descriptor.field(), s));
            }
            return values;
        }

        @Override
        public void validateValue(ConfigFieldDescriptor descriptor, Object value) throws ConfigValidationException
        {
            if (!(value instanceof List<?> list))
            {
                throw new ConfigValidationException("Expected list");
            }

            for (Object entry : list)
            {
                if (!(entry instanceof String s))
                {
                    throw new ConfigValidationException("Expected string list entry");
                }
                normalizeListEntry(descriptor.field(), s);
            }
        }

        private String normalizeListEntry(Field field, String rawValue) throws ConfigValidationException
        {
            if (rawValue.isBlank()) throw new ConfigValidationException("List entry cannot be empty");

            if (field.isAnnotationPresent(ResourceLocationPatternList.class))
            {
                return ResourceLocationPatternParser.normalizeEntry(rawValue);
            }

            if (field.isAnnotationPresent(ResourceLocationPairList.class))
            {
                String[] parts = rawValue.split("=", 2);
                if (parts.length != 2) throw new ConfigValidationException("Expected key=value");

                Identifier key = Identifier.tryParse(parts[0].trim());
                Identifier value = Identifier.tryParse(parts[1].trim());
                if (key == null || value == null) throw new ConfigValidationException("Expected resource location pair");

                return key + "=" + value;
            }

            if (field.isAnnotationPresent(ResourceLocationIntPairList.class))
            {
                String[] parts = rawValue.split("=", 2);
                if (parts.length != 2) throw new ConfigValidationException("Expected key=value");

                Identifier key = Identifier.tryParse(parts[0].trim());
                if (key == null) throw new ConfigValidationException("Expected resource location key");

                int value;
                try
                {
                    value = Integer.parseInt(parts[1].trim());
                }
                catch (NumberFormatException e)
                {
                    throw new ConfigValidationException("Expected integer value");
                }

                if (value < 0) throw new ConfigValidationException("Expected non-negative integer value");
                return key + "=" + value;
            }

            return rawValue;
        }
    }
}
