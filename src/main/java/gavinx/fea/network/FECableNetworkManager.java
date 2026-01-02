package gavinx.fea.network;

import gavinx.fea.api.FECable;
import gavinx.fea.api.FEApi;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.ArrayDeque;
import java.util.Objects;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Per-world cable network cache.
 *
 * <p>Maintains connected components of cable blocks and invalidates locally on block changes.
 */
public final class FECableNetworkManager {
	private final ServerWorld world;

	// pos -> networkId
	private final Long2IntOpenHashMap posToNetwork = new Long2IntOpenHashMap();
	private final Long2ObjectOpenHashMap<FECableNetwork> networks = new Long2ObjectOpenHashMap<>();
	private int nextNetworkId = 1;

	public FECableNetworkManager(ServerWorld world) {
		this.world = Objects.requireNonNull(world, "world");
		posToNetwork.defaultReturnValue(0);
	}

	public void markDirty(BlockPos pos) {
		long p = pos.asLong();
		invalidateAt(p);
		for (Direction dir : Direction.values()) {
			invalidateAt(BlockPos.offset(p, dir));
		}
	}

	public FECableNetwork getNetworkContaining(BlockPos cablePos) {
		long start = cablePos.asLong();
		if (!isCableAnySide(start)) return null;

		int id = posToNetwork.get(start);
		if (id != 0) {
			FECableNetwork existing = networks.get(id);
			if (existing != null) return existing;
			posToNetwork.remove(start);
		}

		FECableNetwork built = buildNetworkFrom(start);
		if (built == null) return null;
		return built;
	}

	private void invalidateAt(long pos) {
		int id = posToNetwork.get(pos);
		if (id == 0) return;
		FECableNetwork net = networks.remove(id);
		if (net == null) return;
		for (long cable : net.cables) {
			posToNetwork.remove(cable);
		}
	}

	private boolean isCableAnySide(long pos) {
		BlockPos bp = BlockPos.fromLong(pos);
		for (Direction dir : Direction.values()) {
			FECable cable = FEApi.CABLE.find(world, bp, dir);
			if (cable != null) return true;
		}
		return false;
	}

	private FECableNetwork buildNetworkFrom(long startPos) {
		if (!isCableAnySide(startPos)) return null;

		int networkId = nextNetworkId++;
		LongOpenHashSet cables = new LongOpenHashSet();
		ArrayDeque<Long> queue = new ArrayDeque<>();

		cables.add(startPos);
		queue.add(startPos);

		while (!queue.isEmpty()) {
			long p = queue.removeFirst();
			BlockPos bp = BlockPos.fromLong(p);

			for (Direction dir : Direction.values()) {
				long neighbor = BlockPos.offset(p, dir);
				if (cables.contains(neighbor)) continue;

				// Connection requires both sides to expose a cable.
				if (FEApi.CABLE.find(world, bp, dir) == null) continue;
				BlockPos nbp = BlockPos.fromLong(neighbor);
				if (FEApi.CABLE.find(world, nbp, dir.getOpposite()) == null) continue;

				cables.add(neighbor);
				queue.add(neighbor);
			}
		}

		FECableNetwork network = new FECableNetwork(networkId, cables);
		networks.put(networkId, network);
		for (long p : cables) {
			posToNetwork.put(p, networkId);
		}
		return network;
	}

	public static final class FECableNetwork {
		public final int id;
		public final LongOpenHashSet cables;

		private FECableNetwork(int id, LongOpenHashSet cables) {
			this.id = id;
			this.cables = cables;
		}
	}
}
