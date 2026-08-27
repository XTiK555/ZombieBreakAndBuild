package com.tik.zbb.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tik.zbb.Constants;
import com.tik.zbb.event.MixinEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin
{
    @Unique
    private BlockState zbb$landingOldState;

    @Unique
    private CompoundTag zbb$landingOldNbt;

    @Inject(method = "fall", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void zbb$onFallStarted(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<FallingBlockEntity> cir, FallingBlockEntity entity)
    {
        if (level instanceof ServerLevel serverLevel)
        {
            Constants.EVENT_BUS.post(new MixinEvents.OnFallingBlockStartedEvent(serverLevel, entity));
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void zbb$onFallFinished(CallbackInfo ci)
    {
        FallingBlockEntity entity = (FallingBlockEntity) (Object) this;
        if (!entity.isRemoved() || !(entity.level() instanceof ServerLevel serverLevel)) return;

        Constants.EVENT_BUS.post(new MixinEvents.OnFallingBlockFinishedEvent(
                serverLevel,
                entity,
                zbb$landingOldState,
                zbb$landingOldNbt
        ));
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean zbb$captureLandingOldState(Level level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original)
    {
        BlockState oldState = level.getBlockState(pos);
        BlockEntity oldBlockEntity = level.getBlockEntity(pos);
        CompoundTag oldNbt = oldBlockEntity != null ? oldBlockEntity.saveWithFullMetadata(level.registryAccess()) : null;

        boolean result = original.call(level, pos, state, flags);

        if (result)
        {
            zbb$landingOldState = oldState;
            zbb$landingOldNbt = oldNbt;
        }

        return result;
    }
}
