package com.tik.zbb.mixin;

import com.tik.zbb.Constants;
import com.tik.zbb.event.MixinEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin
{
    @Inject(method = "fall", at = @At("HEAD"))
    private static void zbb$onFallStarted(Level level, BlockPos pos, BlockState state,
                                          CallbackInfoReturnable<FallingBlockEntity> cir)
    {
        if (level instanceof ServerLevel serverLevel)
        {
            Constants.EVENT_BUS.post(new MixinEvents.OnFallingBlockStartedEvent(serverLevel, pos.immutable(), state));
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void zbb$onFallFinished(CallbackInfo ci)
    {
        FallingBlockEntity entity = (FallingBlockEntity) (Object) this;
        if (!entity.isRemoved() || !(entity.level() instanceof ServerLevel serverLevel)) return;

        Constants.EVENT_BUS.post(new MixinEvents.OnFallingBlockFinishedEvent(
                serverLevel,
                entity.getStartPos().immutable(),
                entity.blockPosition().immutable(),
                entity.getBlockState()
        ));
    }
}
