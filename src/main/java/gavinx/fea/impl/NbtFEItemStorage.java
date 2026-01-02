package gavinx.fea.impl;

import gavinx.fea.api.FEStorage;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * Reference implementation: stores FE in an {@link ItemStack}'s NBT.
 *
 * <p>This is intentionally simple; mods may want more robust wrappers (e.g. inventory contexts).
 */
public final class NbtFEItemStorage extends SnapshotParticipant<Long> implements FEStorage {
	public static final String DEFAULT_NBT_KEY = "fea_fe";

	private final ItemStack stack;
	private final long capacity;
	private final String nbtKey;
	private final boolean allowInsertion;
	private final boolean allowExtraction;
	private long amount;
	private boolean changed;

	public NbtFEItemStorage(ItemStack stack, long capacity) {
		this(stack, capacity, DEFAULT_NBT_KEY, true, true);
	}

	public NbtFEItemStorage(ItemStack stack, long capacity, String nbtKey, boolean allowInsertion, boolean allowExtraction) {
		if (stack == null) throw new IllegalArgumentException("stack");
		if (capacity < 0) throw new IllegalArgumentException("capacity must be >= 0");
		this.stack = stack;
		this.capacity = capacity;
		this.nbtKey = (nbtKey == null || nbtKey.isBlank()) ? DEFAULT_NBT_KEY : nbtKey;
		this.allowInsertion = allowInsertion;
		this.allowExtraction = allowExtraction;
		this.amount = readAmountFromNbt();
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

	@Override
	protected Long createSnapshot() {
		return amount;
	}

	@Override
	protected void readSnapshot(Long snapshot) {
		amount = clamp(snapshot == null ? 0 : snapshot);
		changed = false;
	}

	@Override
	protected void onFinalCommit() {
		if (!changed) return;
		changed = false;
		writeAmountToNbt(amount);
	}

	private long readAmountFromNbt() {
		NbtCompound nbt = stack.getNbt();
		if (nbt == null) return 0;
		return clamp(nbt.getLong(nbtKey));
	}

	private void writeAmountToNbt(long value) {
		NbtCompound nbt = stack.getOrCreateNbt();
		nbt.putLong(nbtKey, clamp(value));
	}

	private long clamp(long v) {
		if (v < 0) return 0;
		if (v > capacity) return capacity;
		return v;
	}
}
