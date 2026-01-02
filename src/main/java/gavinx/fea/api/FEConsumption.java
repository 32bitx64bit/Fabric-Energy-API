package gavinx.fea.api;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Helpers for consuming (spending) FE.
 *
 * <p>Use these when a machine action has a fixed energy cost and should only run if the full cost can
 * be paid.
 */
public final class FEConsumption {
	private FEConsumption() {}

	/**
	 * Consume up to {@code maxAmount} FE from the given storage.
	 *
	 * <p>This is equivalent to {@link FETransactions#extract(FEStorage, long, boolean)}.
	 */
	public static long consumeUpTo(FEStorage storage, long maxAmount, boolean simulate) {
		if (storage == null) return 0;
		return FETransactions.extract(storage, maxAmount, simulate);
	}

	/**
	 * Consume exactly {@code amount} FE, or consume nothing.
	 *
	 * @return true if the full amount could be consumed
	 */
	public static boolean consumeExact(FEStorage storage, long amount, boolean simulate) {
		if (storage == null) return false;
		if (amount <= 0) return false;
		if (!storage.supportsExtraction()) return false;

		try (Transaction transaction = Transaction.openOuter()) {
			long extracted = storage.extract(amount, transaction);
			if (extracted != amount) {
				return false;
			}
			if (!simulate) {
				transaction.commit();
			}
			return true;
		}
	}

	public static long consumeUpTo(World world, BlockPos pos, Direction side, long maxAmount, boolean simulate) {
		return consumeUpTo(FE.find(world, pos, side), maxAmount, simulate);
	}

	public static boolean consumeExact(World world, BlockPos pos, Direction side, long amount, boolean simulate) {
		return consumeExact(FE.find(world, pos, side), amount, simulate);
	}

	public static long consumeUpTo(ItemStack stack, long maxAmount, boolean simulate) {
		return consumeUpTo(FE.find(stack), maxAmount, simulate);
	}

	public static boolean consumeExact(ItemStack stack, long amount, boolean simulate) {
		return consumeExact(FE.find(stack), amount, simulate);
	}
}
