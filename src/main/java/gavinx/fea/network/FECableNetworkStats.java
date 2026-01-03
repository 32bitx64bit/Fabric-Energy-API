package gavinx.fea.network;

import gavinx.fea.api.FEApi;
import gavinx.fea.api.FEBlockEnergy;
import gavinx.fea.api.FEStorage;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Utilities for inspecting a cable network.
 *
 * <p>Intended for use-cases like generators that should stop producing when the connected network
 * has no remaining insertable space.
 */
public final class FECableNetworkStats {
	private FECableNetworkStats() {}

	public record Stats(long amount, long capacity) {
		public long space() {
			if (capacity <= 0) return 0;
			long remaining = capacity - amount;
			return remaining <= 0 ? 0 : remaining;
		}

		public boolean isFull() {
			return space() == 0;
		}
	}

	private static final Stats EMPTY = new Stats(0L, 0L);

	/**
	 * Compute total insertable FE amount/capacity for the cable network containing {@code anyCablePos}.
	 *
	 * <p>Counts storages adjacent to cables where the storage exists for the cable-facing side and
	 * insertion is allowed (supportsInsertion + optional {@link FEBlockEnergy} sided rules).
	 */
	public static Stats getInsertableTotals(ServerWorld world, BlockPos anyCablePos) {
		FECableNetworkManager.FECableNetwork network = FECableNetworks.get(world).getNetworkContaining(anyCablePos);
		if (network == null) return EMPTY;
		return getInsertableTotals(world, network);
	}

	/**
	 * Convenience: compute insertable totals for the cable network directly adjacent to a block side.
	 *
	 * <p>This matches the conventions used by {@link FECableTransfer#distributeFrom}:
	 * the network is discovered starting at {@code sourcePos.offset(sourceSide)}.
	 */
	public static Stats getInsertableTotalsFrom(ServerWorld world, BlockPos sourcePos, Direction sourceSide) {
		BlockPos startCablePos = sourcePos.offset(sourceSide);
		return getInsertableTotals(world, startCablePos);
	}

	private static Stats getInsertableTotals(ServerWorld world, FECableNetworkManager.FECableNetwork network) {
		LongOpenHashSet[] seenBySide = new LongOpenHashSet[6];
		for (int i = 0; i < 6; i++) {
			seenBySide[i] = new LongOpenHashSet();
		}

		long totalAmount = 0L;
		long totalCapacity = 0L;

		for (long cableLong : network.cables) {
			BlockPos cablePos = BlockPos.fromLong(cableLong);

			for (Direction dir : Direction.values()) {
				BlockPos neighborPos = cablePos.offset(dir);
				Direction neighborSide = dir.getOpposite();
				int sideId = neighborSide.getId();

				long neighborLong = neighborPos.asLong();
				if (!seenBySide[sideId].add(neighborLong)) continue;

				// Skip other cables; only count storages attached to the network.
				if (FEApi.CABLE.find(world, neighborPos, neighborSide) != null) continue;

				FEBlockEnergy def = FEApi.BLOCK_ENERGY.find(world, neighborPos, null);
				if (def != null && !def.getSideMode(neighborSide).canInsert()) continue;

				FEStorage storage = FEApi.STORAGE.find(world, neighborPos, neighborSide);
				if (storage == null || !storage.supportsInsertion()) continue;

				long cap = storage.getCapacity();
				long amt = storage.getAmount();
				if (cap <= 0) continue;
				if (amt < 0) amt = 0;
				if (amt > cap) amt = cap;

				totalCapacity = satAdd(totalCapacity, cap);
				totalAmount = satAdd(totalAmount, amt);
			}
		}

		return new Stats(totalAmount, totalCapacity);
	}

	private static long satAdd(long a, long b) {
		if (a <= 0) return Math.max(0L, b);
		if (b <= 0) return a;
		long r = a + b;
		if (r < 0 || r < a) return Long.MAX_VALUE;
		return r;
	}
}
