package gavinx.fea.impl;

import gavinx.fea.api.FEStorage;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

public final class SimpleFEStorage extends SnapshotParticipant<Long> implements FEStorage {
	private final long capacity;
	private long amount;
	private final boolean allowInsertion;
	private final boolean allowExtraction;

	public SimpleFEStorage(long capacity, long initialAmount, boolean allowInsertion, boolean allowExtraction) {
		if (capacity < 0) throw new IllegalArgumentException("capacity must be >= 0");
		if (initialAmount < 0) throw new IllegalArgumentException("initialAmount must be >= 0");
		this.capacity = capacity;
		this.amount = Math.min(initialAmount, capacity);
		this.allowInsertion = allowInsertion;
		this.allowExtraction = allowExtraction;
	}

	public SimpleFEStorage(long capacity) {
		this(capacity, 0, true, true);
	}

	@Override
	public long insert(long maxAmount, TransactionContext transaction) {
		if (!allowInsertion || maxAmount <= 0) return 0;
		updateSnapshots(transaction);

		long space = capacity - amount;
		long inserted = Math.min(space, maxAmount);
		amount += inserted;
		return inserted;
	}

	@Override
	public long extract(long maxAmount, TransactionContext transaction) {
		if (!allowExtraction || maxAmount <= 0) return 0;
		updateSnapshots(transaction);

		long extracted = Math.min(amount, maxAmount);
		amount -= extracted;
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
		amount = snapshot;
	}
}
