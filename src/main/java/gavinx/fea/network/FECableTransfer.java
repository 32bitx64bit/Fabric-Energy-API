package gavinx.fea.network;

import gavinx.fea.api.FECable;
import gavinx.fea.api.FEApi;
import gavinx.fea.api.FEBlockEnergy;
import gavinx.fea.api.FEStorage;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Pathfinds through connected cables and transfers FE from a source to reachable consumers.
 *
 * <p>Semantics:
 * - path capacity is treated as a bottleneck (minimum segment capacity)
 * - path resistance is additive and clamped to {@code 0..100}
 * - delivered energy is {@code floor(sent * (100 - resistance) / 100)}
 * - targets are attempted in ascending resistance, then descending capacity
 */
public final class FECableTransfer {
	private FECableTransfer() {}

	private static final class State {
		final long pos;
		final Direction enterSide;
		final int resistance;
		final long capacity;

		State(long pos, Direction enterSide, int resistance, long capacity) {
			this.pos = pos;
			this.enterSide = enterSide;
			this.resistance = resistance;
			this.capacity = capacity;
		}
	}

	private static final class Target {
		final BlockPos pos;
		final Direction side;
		final int resistance;
		final long capacity;

		Target(BlockPos pos, Direction side, int resistance, long capacity) {
			this.pos = pos;
			this.side = side;
			this.resistance = resistance;
			this.capacity = capacity;
		}
	}

	/**
	 * Push energy from the given source side into any consumers reachable through cables.
	 *
	 * @param maxExtractFE max FE to extract from source (pre-loss)
	 * @param simulate if true, do not commit (no lasting side effects)
	 * @return FE extracted from source (pre-loss)
	 */
	public static long distributeFrom(ServerWorld world, BlockPos sourcePos, Direction sourceSide, long maxExtractFE,
			boolean simulate) {
		if (maxExtractFE <= 0) return 0;

		FEBlockEnergy sourceDef = FEApi.BLOCK_ENERGY.find(world, sourcePos, null);
		if (sourceDef != null && !sourceDef.getSideMode(sourceSide).canExtract()) return 0;

		FEStorage source = FEApi.STORAGE.find(world, sourcePos, sourceSide);
		if (source == null || !source.supportsExtraction()) return 0;

		BlockPos startCablePos = sourcePos.offset(sourceSide);
		Direction enterSide = sourceSide.getOpposite();
		FECable startCable = FEApi.CABLE.find(world, startCablePos, enterSide);
		if (startCable == null) return 0;

		FECableNetworkManager.FECableNetwork network = FECableNetworks.get(world).getNetworkContaining(startCablePos);
		if (network == null) return 0;

		ArrayList<Target> targets = findTargets(world, network, startCablePos, enterSide);
		if (targets.isEmpty()) return 0;

		targets.sort(Comparator.comparingInt((Target t) -> t.resistance).thenComparingLong(t -> -t.capacity));

		long remaining = maxExtractFE;
		long extractedTotal = 0;

		try (Transaction outer = Transaction.openOuter()) {
			for (Target target : targets) {
				if (remaining <= 0) break;

				FEBlockEnergy consumerDef = FEApi.BLOCK_ENERGY.find(world, target.pos, null);
				if (consumerDef != null && !consumerDef.getSideMode(target.side).canInsert()) continue;

				FEStorage consumer = FEApi.STORAGE.find(world, target.pos, target.side);
				if (consumer == null || !consumer.supportsInsertion()) continue;

				long pathCap = Math.min(remaining, target.capacity);
				if (pathCap <= 0) continue;

				// Simulate both ends to pick a safe amount, then perform an atomic nested commit.
				long sendMax;
				try (Transaction sim = Transaction.openNested(outer)) {
					sendMax = source.extract(pathCap, sim);
				}
				if (sendMax <= 0) continue;

				long deliveredMax = applyResistance(sendMax, target.resistance);
				if (deliveredMax <= 0) continue;

				long acceptedMax;
				try (Transaction sim = Transaction.openNested(outer)) {
					acceptedMax = consumer.insert(deliveredMax, sim);
				}
				if (acceptedMax <= 0) continue;

				long send = inverseResistanceCeil(acceptedMax, target.resistance);
				send = Math.min(send, pathCap);
				if (send <= 0) continue;

				try (Transaction step = Transaction.openNested(outer)) {
					long extracted = source.extract(send, step);
					if (extracted != send) {
						continue;
					}

					long delivered = applyResistance(extracted, target.resistance);
					if (delivered != acceptedMax) {
						// Something changed between simulation and execution; abort this target.
						continue;
					}

					long inserted = consumer.insert(delivered, step);
					if (inserted != delivered) {
						continue;
					}

					step.commit();
					extractedTotal += extracted;
					remaining -= extracted;
				}
			}

			if (!simulate) {
				outer.commit();
			}
		}

		return extractedTotal;
	}

	private static ArrayList<Target> findTargets(ServerWorld world, FECableNetworkManager.FECableNetwork network,
			BlockPos startPos, Direction startEnterSide) {
		PriorityQueue<State> pq = new PriorityQueue<>(Comparator.comparingInt((State s) -> s.resistance)
				.thenComparingLong(s -> -s.capacity));

		Long2IntOpenHashMap[] bestRes = new Long2IntOpenHashMap[6];
		Long2LongOpenHashMap[] bestCap = new Long2LongOpenHashMap[6];
		for (int i = 0; i < 6; i++) {
			bestRes[i] = new Long2IntOpenHashMap();
			bestRes[i].defaultReturnValue(Integer.MAX_VALUE);
			bestCap[i] = new Long2LongOpenHashMap();
			bestCap[i].defaultReturnValue(0L);
		}

		long startLong = startPos.asLong();
		FECable startCable = FEApi.CABLE.find(world, startPos, startEnterSide);
		int r0 = startCable == null ? 100 : startCable.getResistancePercentClamped();
		long c0 = startCable == null ? 0 : Math.max(0L, startCable.getTransferCapacityFE());

		int startIdx = startEnterSide.getId();
		bestRes[startIdx].put(startLong, r0);
		bestCap[startIdx].put(startLong, c0);
		pq.add(new State(startLong, startEnterSide, r0, c0));

		@SuppressWarnings("unchecked")
		Long2ObjectOpenHashMap<Target>[] targets = new Long2ObjectOpenHashMap[6];
		for (int i = 0; i < 6; i++) {
			targets[i] = new Long2ObjectOpenHashMap<>();
		}

		while (!pq.isEmpty()) {
			State cur = pq.poll();
			int curIdx = cur.enterSide.getId();
			if (cur.resistance != bestRes[curIdx].get(cur.pos) || cur.capacity != bestCap[curIdx].get(cur.pos)) continue;

			BlockPos curPos = BlockPos.fromLong(cur.pos);

			for (Direction out : Direction.values()) {
				if (FEApi.CABLE.find(world, curPos, out) == null) continue;

				BlockPos neighborPos = curPos.offset(out);
				Direction neighborEnter = out.getOpposite();

				// If the neighbor is a cable, traverse.
				if (network.cables.contains(neighborPos.asLong())) {
					FECable nextCable = FEApi.CABLE.find(world, neighborPos, neighborEnter);
					if (nextCable != null) {
						int nextR = clampPercent(cur.resistance + nextCable.getResistancePercentClamped());
						long nextC = Math.min(cur.capacity, Math.max(0L, nextCable.getTransferCapacityFE()));
						int nextIdx = neighborEnter.getId();
						long nextPosLong = neighborPos.asLong();

						int prevR = bestRes[nextIdx].get(nextPosLong);
						long prevC = bestCap[nextIdx].get(nextPosLong);
						boolean better = nextR < prevR || (nextR == prevR && nextC > prevC);
						if (better) {
							bestRes[nextIdx].put(nextPosLong, nextR);
							bestCap[nextIdx].put(nextPosLong, nextC);
							pq.add(new State(nextPosLong, neighborEnter, nextR, nextC));
						}
					}
					continue;
				}

				// Otherwise, treat as a potential consumer endpoint.
				FEStorage maybe = FEApi.STORAGE.find(world, neighborPos, neighborEnter);
				if (maybe == null || !maybe.supportsInsertion()) continue;

				int tIdx = neighborEnter.getId();
				long tPosLong = neighborPos.asLong();
				Target existing = targets[tIdx].get(tPosLong);
				Target candidate = new Target(neighborPos, neighborEnter, cur.resistance, cur.capacity);
				if (existing == null || candidate.resistance < existing.resistance
						|| (candidate.resistance == existing.resistance && candidate.capacity > existing.capacity)) {
					targets[tIdx].put(tPosLong, candidate);
				}
			}
		}

		ArrayList<Target> out = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			out.addAll(targets[i].values());
		}
		return out;
	}

	private static long applyResistance(long sendPreLoss, int resistancePercent) {
		int r = clampPercent(resistancePercent);
		if (sendPreLoss <= 0) return 0;
		if (r >= 100) return 0;
		return (sendPreLoss * (100L - r)) / 100L;
	}

	private static long inverseResistanceCeil(long delivered, int resistancePercent) {
		int r = clampPercent(resistancePercent);
		if (delivered <= 0) return 0;
		if (r >= 100) return Long.MAX_VALUE;
		long denom = 100L - r;
		return (delivered * 100L + (denom - 1)) / denom;
	}

	private static int clampPercent(int v) {
		if (v < 0) return 0;
		if (v > 100) return 100;
		return v;
	}

}
