package com.tik.zbb.config.schema.codecs;

import com.tik.zbb.config.annotations.Range;
import com.tik.zbb.config.schema.ConfigFieldDescriptor;
import com.tik.zbb.config.schema.ConfigValidationException;
import com.tik.zbb.config.schema.ConfigValueCodec;

import java.math.BigDecimal;

abstract class NumberValueCodec implements ConfigValueCodec
{
    protected static int exactInt(Number number, String error) throws ConfigValidationException
    {
        if ((number instanceof Double d && !Double.isFinite(d))
                || (number instanceof Float f && !Float.isFinite(f)))
        {
            throw new ConfigValidationException(error);
        }

        try
        {
            return new BigDecimal(number.toString()).intValueExact();
        }
        catch (NumberFormatException | ArithmeticException e)
        {
            throw new ConfigValidationException(error);
        }
    }

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
