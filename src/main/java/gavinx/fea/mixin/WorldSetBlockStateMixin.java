package gavinx.fea.mixin;

import gavinx.fea.network.FECableNetworks;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldSetBlockStateMixin {
	@Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", at = @At("RETURN"))
	private void fea$onSetBlockState(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ()) return;
		Object self = this;
		if (self instanceof ServerWorld serverWorld) {
			FECableNetworks.get(serverWorld).markDirty(pos);
		}
	}

	@Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z", at = @At("RETURN"))
	private void fea$onSetBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth,
			CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ()) return;
		Object self = this;
		if (self instanceof ServerWorld serverWorld) {
			FECableNetworks.get(serverWorld).markDirty(pos);
		}
	}
}
