package gavinx.fea.api;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.Direction;

/**
 * Documentation-by-code for how this API is intended to be used.
 *
 * <p>Provider side (your BlockEntity): register a lookup in your mod init.
 * <pre>{@code
 * FEApi.STORAGE.registerForBlockEntities((be, side) -> ((MyBE) be).getEnergyStorage(side), MY_BE_TYPE);
 * }
 * </pre>
 *
 * <p>Consumer side:
 * <pre>{@code
 * FEStorage storage = FEApi.STORAGE.find(world, pos, side);
 * if (storage != null) {
 *   long received = FETransactions.insert(storage, 1000, false);
 * }
 * }
 * </pre>
 */
public final class FEApiUsage {
	private FEApiUsage() {}

	/** Example signature for a BE method you might implement. */
	public static FEStorage getEnergyStorage(BlockEntity blockEntity, Direction side) {
		return null;
	}
}
