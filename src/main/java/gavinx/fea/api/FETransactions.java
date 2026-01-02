package gavinx.fea.api;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

/** Convenience helpers for callers that want a simple simulate/execute flow. */
public final class FETransactions {
	private FETransactions() {}

	/**
	 * Insert into the storage.
	 *
	 * @param simulate if true, do not commit (no lasting side effects)
	 */
	public static long insert(FEStorage storage, long maxAmount, boolean simulate) {
		try (Transaction transaction = Transaction.openOuter()) {
			long inserted = storage.insert(maxAmount, transaction);
			if (!simulate) {
				transaction.commit();
			}
			return inserted;
		}
	}

	/**
	 * Extract from the storage.
	 *
	 * @param simulate if true, do not commit (no lasting side effects)
	 */
	public static long extract(FEStorage storage, long maxAmount, boolean simulate) {
		try (Transaction transaction = Transaction.openOuter()) {
			long extracted = storage.extract(maxAmount, transaction);
			if (!simulate) {
				transaction.commit();
			}
			return extracted;
		}
	}
}
