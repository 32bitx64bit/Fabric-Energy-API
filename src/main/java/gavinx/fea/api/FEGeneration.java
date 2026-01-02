package gavinx.fea.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Helpers for event-driven FE generation.
 *
 * <p>Generation rate is standardized as FE per tick (FE/t). This API does not register its own ticks;
 * a mod calls these helpers from its normal server tick (e.g. BlockEntity ticker) when its own logic decides
 * generation should happen (e.g. coal is burning).
 */
public final class FEGeneration {
	private FEGeneration() {}

	/**
	 * Insert newly generated FE into a storage at the given position.
	 *
	 * @return amount inserted
	 */
	public static long generateTo(World world, BlockPos pos, Direction side, long amount, boolean simulate) {
		return FE.insert(world, pos, side, amount, simulate);
	}

	/**
	 * Insert newly generated FE directly into a known storage.
	 *
	 * @return amount inserted
	 */
	public static long generateTo(FEStorage storage, long amount, boolean simulate) {
		return FETransactions.insert(storage, amount, simulate);
	}

	/**
	 * Standard FE/t helper: generate up to {@code fePerTick} this tick if {@code active}.
	 */
	public static long tickGenerateTo(World world, BlockPos pos, Direction side, long fePerTick, boolean active,
			boolean simulate) {
		if (!active) return 0;
		if (fePerTick <= 0) return 0;
		return generateTo(world, pos, side, fePerTick, simulate);
	}

	/**
	 * Standard FE/t helper: generate up to {@code fePerTick} this tick if {@code active}.
	 */
	public static long tickGenerateTo(FEStorage storage, long fePerTick, boolean active, boolean simulate) {
		if (!active) return 0;
		if (fePerTick <= 0) return 0;
		return generateTo(storage, fePerTick, simulate);
	}
}
