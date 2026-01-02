package gavinx.fea.api;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Convenience helpers for finding and interacting with {@link FEStorage}. */
public final class FE {
	private FE() {}

	public static FEStorage find(World world, BlockPos pos, Direction side) {
		return FEApi.STORAGE.find(world, pos, side);
	}

	public static FEStorage find(ItemStack stack) {
		return FEApi.find(stack);
	}

	public static long insert(World world, BlockPos pos, Direction side, long maxAmount, boolean simulate) {
		FEStorage storage = find(world, pos, side);
		if (storage == null) return 0;
		return FETransactions.insert(storage, maxAmount, simulate);
	}

	public static long extract(World world, BlockPos pos, Direction side, long maxAmount, boolean simulate) {
		FEStorage storage = find(world, pos, side);
		if (storage == null) return 0;
		return FETransactions.extract(storage, maxAmount, simulate);
	}

	/**
	 * Consume exactly {@code amount} FE from the storage, or consume nothing.
	 *
	 * @return true if the full amount could be consumed
	 */
	public static boolean consumeExact(World world, BlockPos pos, Direction side, long amount, boolean simulate) {
		return FEConsumption.consumeExact(world, pos, side, amount, simulate);
	}

	public static long insert(ItemStack stack, long maxAmount, boolean simulate) {
		FEStorage storage = find(stack);
		if (storage == null) return 0;
		return FETransactions.insert(storage, maxAmount, simulate);
	}

	public static long extract(ItemStack stack, long maxAmount, boolean simulate) {
		FEStorage storage = find(stack);
		if (storage == null) return 0;
		return FETransactions.extract(storage, maxAmount, simulate);
	}

	/**
	 * Consume exactly {@code amount} FE from the storage, or consume nothing.
	 *
	 * @return true if the full amount could be consumed
	 */
	public static boolean consumeExact(ItemStack stack, long amount, boolean simulate) {
		return FEConsumption.consumeExact(stack, amount, simulate);
	}
}
