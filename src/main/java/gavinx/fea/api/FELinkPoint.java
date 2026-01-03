package gavinx.fea.api;

/**
 * Optional capability exposed by blocks that want to advertise attach points (“sockets”) for
 * non-block connections (ropes, wires, wireless relays, etc).
 *
 * <p>This API does not implement the wire/rope system itself. Instead, a wire mod can:
 * <ol>
 *   <li>Query {@link FEApi#LINK_POINT} to discover whether a side is a valid endpoint and what its
 *   per-port limits are.</li>
 *   <li>Query {@link FEApi#STORAGE} on the same position/side to actually insert/extract FE.</li>
 * </ol>
 */
public interface FELinkPoint {
	/**
	 * Whether this port is allowed to insert/extract FE via links.
	 *
	 * <p>Default is {@link FESideMode#BOTH}.
	 */
	default FESideMode getMode() {
		return FESideMode.BOTH;
	}

	/**
	 * Maximum FE this port allows to traverse per tick (FE/t).
	 *
	 * <p>Return 0 to effectively disable transfer, or {@link Long#MAX_VALUE} for “no port limit”.
	 */
	long getTransferLimitFE();

	/**
	 * Optional percent loss for traversing this port, in the range {@code 0..100}.
	 *
	 * <p>Wire mods may treat this as additive with other losses along a path.
	 */
	default int getResistancePercent() {
		return 0;
	}

	default int getResistancePercentClamped() {
		int r = getResistancePercent();
		if (r < 0) return 0;
		if (r > 100) return 100;
		return r;
	}
}
