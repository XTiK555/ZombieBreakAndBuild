package com.tik.zbb.config;

import java.util.ArrayList;

public final class ConfigDataCopier
{
    public static ConfigData copy(ConfigData data)
    {
        ConfigData copy = new ConfigData();

        copy.blocks.dimensionPlaceBlockIdList = new ArrayList<>(data.blocks.dimensionPlaceBlockIdList);
        copy.blocks.mobPlaceBlockIdOverrideList = new ArrayList<>(data.blocks.mobPlaceBlockIdOverrideList);
        copy.blocks.fallbackPlaceBlockId = data.blocks.fallbackPlaceBlockId;
        copy.blocks.dangerousBlockIdList = new ArrayList<>(data.blocks.dangerousBlockIdList);

        copy.ai.alwaysSeeNearestPlayer = data.ai.alwaysSeeNearestPlayer;
        copy.ai.canNoticeTargetsThroughBlocks = data.ai.canNoticeTargetsThroughBlocks;
        copy.ai.noticeTargetsThroughBlocksLimit = data.ai.noticeTargetsThroughBlocksLimit;
        copy.ai.canContinueSeeingTargetsThroughBlocks = data.ai.canContinueSeeingTargetsThroughBlocks;
        copy.ai.continueSeeingTargetsThroughBlocksLimit = data.ai.continueSeeingTargetsThroughBlocksLimit;
        copy.ai.applyToAllMonsters = data.ai.applyToAllMonsters;
        copy.ai.additionalEntityIdList = new ArrayList<>(data.ai.additionalEntityIdList);
        copy.ai.ignoreBuildEntityIdList = new ArrayList<>(data.ai.ignoreBuildEntityIdList);
        copy.ai.ignoreBreakEntityIdList = new ArrayList<>(data.ai.ignoreBreakEntityIdList);

        copy.balance.builtBlocksProtectionTime = data.balance.builtBlocksProtectionTime;
        copy.balance.dangerousBlocksSearchRadius = data.balance.dangerousBlocksSearchRadius;
        copy.balance.pathEndBreakBuildDistance = data.balance.pathEndBreakBuildDistance;
        copy.balance.damageStoreTime = data.balance.damageStoreTime;

        copy.balance.blockDamage.damageToBlocks = data.balance.blockDamage.damageToBlocks;
        copy.balance.blockDamage.blockHardnessContrast = data.balance.blockDamage.blockHardnessContrast;
        copy.balance.blockDamage.blockHardnessMultiplier = data.balance.blockDamage.blockHardnessMultiplier;
        copy.balance.blockDamage.blockHealthOverrideList = new ArrayList<>(data.balance.blockDamage.blockHealthOverrideList);
        copy.balance.blockDamage.itemDamageMultiplierStrength = data.balance.blockDamage.itemDamageMultiplierStrength;
        copy.balance.blockDamage.hitboxSizeMultiplierStrength = data.balance.blockDamage.hitboxSizeMultiplierStrength;

        copy.balance.cooldowns.breakCooldown = data.balance.cooldowns.breakCooldown;
        copy.balance.cooldowns.buildCooldown = data.balance.cooldowns.buildCooldown;
        copy.balance.cooldowns.searchDangerousBlocksCooldown = data.balance.cooldowns.searchDangerousBlocksCooldown;

        copy.blockRestoration.builtBlocksDisappearing = data.blockRestoration.builtBlocksDisappearing;
        copy.blockRestoration.builtBlocksDisappearTime = data.blockRestoration.builtBlocksDisappearTime;
        copy.blockRestoration.brokenBlocksRestoring = data.blockRestoration.brokenBlocksRestoring;
        copy.blockRestoration.brokenBlocksRestoreTime = data.blockRestoration.brokenBlocksRestoreTime;

        copy.visualEffects.breakMobSwing = data.visualEffects.breakMobSwing;
        copy.visualEffects.buildBlockSound = data.visualEffects.buildBlockSound;
        copy.visualEffects.brokenReappearParticles = data.visualEffects.brokenReappearParticles;
        copy.visualEffects.brokenReappearChargeSound = data.visualEffects.brokenReappearChargeSound;
        copy.visualEffects.brokenReappearSound = data.visualEffects.brokenReappearSound;
        copy.visualEffects.builtDisappearBlockDisplay = data.visualEffects.builtDisappearBlockDisplay;
        copy.visualEffects.builtDisappearShrinkSound = data.visualEffects.builtDisappearShrinkSound;
        copy.visualEffects.builtDisappearSound = data.visualEffects.builtDisappearSound;

        return copy;
    }
}
