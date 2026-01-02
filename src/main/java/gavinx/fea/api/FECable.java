package gavinx.fea.api;

/**
 * Capability exposed by cable blocks.
 *
 * <p>Values are defined by modders:
 * - transfer capacity is in FE per transfer operation (typically per-tick)
 * - resistance is a percent [0..100]; 0 = no loss, 100 = no transfer
 *
 * <p>When traversing a path, capacity is treated as a bottleneck (minimum across segments) and
 * resistance is treated as additive across segments, clamped to {@code 0..100}. A transfer that
 * sends {@code sent} FE across total resistance {@code r} delivers:
 *
 * <pre>
 * delivered = floor(sent * (100 - r) / 100)
 * </pre>
 */
public interface FECable {
	/** Max FE that can traverse this cable (typically per-tick). */
	long getTransferCapacityFE();

	/** Percent loss along this cable, clamped to [0..100]. */
	int getResistancePercent();

	default int getResistancePercentClamped() {
		int r = getResistancePercent();
		if (r < 0) return 0;
		if (r > 100) return 100;
		return r;
	}
}
