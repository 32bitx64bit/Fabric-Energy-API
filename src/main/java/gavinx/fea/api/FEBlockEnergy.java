package gavinx.fea.api;

import net.minecraft.util.math.Direction;

/**
 * Optional block definition capability for FE.
 *
 * <p>This is metadata/control for your block:
 * - storage capacity (if the block stores FE)
 * - per-side IO modes (IN/OUT/BOTH/NONE)
 * - optional generation rate reporting (for UI/introspection)
 *
 * <p>Generation itself is event-driven: the producing mod calls into its own logic and inserts FE into storage.
 */
public interface FEBlockEnergy {
	/**
	 * Declared storage capacity, or 0 if the block does not store FE.
	 *
	 * <p>This is metadata; actual storage is still exposed via {@link FEStorage}.
	 */
	long getStorageCapacityFE();

	/**
	 * Side mode for automation/cable transfer.
	 *
	 * <p>By default, returns {@link FESideMode#BOTH}.
	 */
	default FESideMode getSideMode(Direction side) {
		return FESideMode.BOTH;
	}

	/**
	 * Optional: how much FE the block is currently producing, in FE per tick (FE/t). Default 0.
	 *
	 * <p>This is reporting only and does not automatically add FE.
	 * The producing mod should call generation logic (typically from the server tick) when appropriate.
	 */
	default long getGenerationRateFE() {
		return 0;
	}
}
