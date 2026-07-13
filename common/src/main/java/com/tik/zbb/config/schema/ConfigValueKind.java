package com.tik.zbb.config.schema;

import com.tik.zbb.config.schema.codecs.BooleanValueCodec;
import com.tik.zbb.config.schema.codecs.DoubleValueCodec;
import com.tik.zbb.config.schema.codecs.FloatValueCodec;
import com.tik.zbb.config.schema.codecs.IntValueCodec;
import com.tik.zbb.config.schema.codecs.ResourceLocationIntPairMapCodec;
import com.tik.zbb.config.schema.codecs.ResourceLocationPairMapCodec;
import com.tik.zbb.config.schema.codecs.ResourceLocationPatternListCodec;
import com.tik.zbb.config.schema.codecs.ResourceLocationValueCodec;
import com.tik.zbb.config.schema.codecs.StringListValueCodec;
import com.tik.zbb.config.schema.codecs.StringValueCodec;

public enum ConfigValueKind
{
    BOOLEAN(BooleanValueCodec.INSTANCE),
    INT(IntValueCodec.INSTANCE),
    DOUBLE(DoubleValueCodec.INSTANCE),
    FLOAT(FloatValueCodec.INSTANCE),
    STRING(StringValueCodec.INSTANCE),
    RESOURCE_LOCATION(ResourceLocationValueCodec.INSTANCE),
    STRING_LIST(StringListValueCodec.INSTANCE),
    RESOURCE_LOCATION_PATTERN_LIST(ResourceLocationPatternListCodec.INSTANCE),
    RESOURCE_LOCATION_PAIR_MAP(ResourceLocationPairMapCodec.INSTANCE),
    RESOURCE_LOCATION_INT_PAIR_MAP(ResourceLocationIntPairMapCodec.INSTANCE);

    private final ConfigValueCodec codec;

    ConfigValueKind(ConfigValueCodec codec)
    {
        this.codec = codec;
    }

    public ConfigValueCodec codec()
    {
        return codec;
    }
}
