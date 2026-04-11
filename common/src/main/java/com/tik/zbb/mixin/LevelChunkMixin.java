package com.tik.zbb.mixin;

import com.tik.zbb.Constants;
import com.tik.zbb.event.MixinEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin
{
    @Shadow
    @Final
    Level level;

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void zbb$clearStoredDamageOnBlockChange(BlockPos pos, BlockState newState, int flags, CallbackInfoReturnable<@Nullable BlockState> cir)
    {
        if (!(this.level instanceof ServerLevel serverLevel)) return;

        BlockState oldState = cir.getReturnValue();

        if (oldState == null) return;

        if (!oldState.is(newState.getBlock()))
        {
            Constants.EVENT_BUS.post(new MixinEvents.OnLevelChunkBlockChangedEvent(serverLevel, pos.immutable(), oldState, newState));
        }
    }
}