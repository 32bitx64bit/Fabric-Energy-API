package gavinx.fea.impl;

import gavinx.fea.api.FEStorage;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.nbt.NbtCompound;

/**
 * Reference implementation for block entities.
 *
 * <p>Stores the amount in-memory and supports Fabric transactions.
 * Call {@link #readFromNbt(NbtCompound)} / {@link #writeToNbt(NbtCompound)} from your BE NBT methods.
 */
public final class BlockEntityFEStorage extends SnapshotParticipant<Long> implements FEStorage {
	public static final String DEFAULT_NBT_KEY = "fea_fe";

	private final long capacity;
	private long amount;
	private final boolean allowInsertion;
	private final boolean allowExtraction;
	private final Runnable onFinalCommit;
	private final String nbtKey;

	private boolean changed;

	public BlockEntityFEStorage(long capacity, Runnable onFinalCommit) {
		this(capacity, 0, DEFAULT_NBT_KEY, true, true, onFinalCommit);
	}

	public BlockEntityFEStorage(long capacity, long initialAmount, Runnable onFinalCommit) {
		this(capacity, initialAmount, DEFAULT_NBT_KEY, true, true, onFinalCommit);
	}

	public BlockEntityFEStorage(long capacity, long initialAmount, String nbtKey, boolean allowInsertion, boolean allowExtraction,
			Runnable onFinalCommit) {
		if (capacity < 0) throw new IllegalArgumentException("capacity must be >= 0");
		if (initialAmount < 0) throw new IllegalArgumentException("initialAmount must be >= 0");
		this.capacity = capacity;
		this.amount = Math.min(initialAmount, capacity);
		this.nbtKey = (nbtKey == null || nbtKey.isBlank()) ? DEFAULT_NBT_KEY : nbtKey;
		this.allowInsertion = allowInsertion;
		this.allowExtraction = allowExtraction;
		this.onFinalCommit = onFinalCommit;
	}

	@Override
	public long insert(long maxAmount, TransactionContext transaction) {
		if (!allowInsertion || maxAmount <= 0) return 0;
		updateSnapshots(transaction);

		long space = capacity - amount;
		long inserted = Math.min(space, maxAmount);
		if (inserted > 0) {
			amount += inserted;
			changed = true;
		}
		return inserted;
	}

	@Override
	public long extract(long maxAmount, TransactionContext transaction) {
		if (!allowExtraction || maxAmount <= 0) return 0;
		updateSnapshots(transaction);

		long extracted = Math.min(amount, maxAmount);
		if (extracted > 0) {
			amount -= extracted;
			changed = true;
		}
		return extracted;
	}

	@Override
	public long getAmount() {
		return amount;
	}

	@Override
	public long getCapacity() {
		return capacity;
	}

	@Override
	public boolean supportsInsertion() {
		return allowInsertion;
	}

	@Override
	public boolean supportsExtraction() {
		return allowExtraction;
	}

	public void readFromNbt(NbtCompound nbt) {
		readFromNbt(nbt, nbtKey);
	}

	public void readFromNbt(NbtCompound nbt, String key) {
		if (nbt == null) {
			amount = 0;
			return;
		}
		String k = (key == null || key.isBlank()) ? DEFAULT_NBT_KEY : key;
		long v = nbt.getLong(k);
		amount = clamp(v);
	}

	public void writeToNbt(NbtCompound nbt) {
		writeToNbt(nbt, nbtKey);
	}

	public void writeToNbt(NbtCompound nbt, String key) {
		if (nbt == null) return;
		String k = (key == null || key.isBlank()) ? DEFAULT_NBT_KEY : key;
		nbt.putLong(k, amount);
	}

	@Override
	protected Long createSnapshot() {
		return amount;
	}

	@Override
	protected void readSnapshot(Long snapshot) {
		amount = snapshot == null ? 0 : clamp(snapshot);
		changed = false;
	}

	@Override
	protected void onFinalCommit() {
		if (!changed) return;
		changed = false;
		if (onFinalCommit != null) {
			onFinalCommit.run();
		}
	}

	private long clamp(long v) {
		if (v < 0) return 0;
		if (v > capacity) return capacity;
		return v;
	}
}
