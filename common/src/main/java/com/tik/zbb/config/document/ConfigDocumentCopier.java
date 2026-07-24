package com.tik.zbb.config.document;

import com.tik.zbb.config.ConfigDocument;
import com.tik.zbb.utilities.ConfigUtilities;

public final class ConfigDocumentCopier
{
    public static ConfigDocument copy(ConfigDocument data)
    {
        return (ConfigDocument) ConfigUtilities.deepCopyConfigValue(data);
    }

    private ConfigDocumentCopier() {}
}
