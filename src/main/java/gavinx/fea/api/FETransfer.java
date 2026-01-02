package gavinx.fea.api;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

/**
 * Utilities for moving FE between storages.
 *
 * <p>All operations are transactional: if the transfer can't be completed, it is rolled back.
 */
public final class FETransfer {
	private FETransfer() {}

	/**
	 * Transfer up to {@code maxAmount} FE from {@code from} into {@code to}.
	 *
	 * @return the amount transferred
	 */
	public static long transfer(FEStorage from, FEStorage to, long maxAmount, boolean simulate) {
		if (from == null || to == null) return 0;
		if (from == to) return 0;
		if (maxAmount <= 0) return 0;
		if (!from.supportsExtraction() || !to.supportsInsertion()) return 0;

		try (Transaction transaction = Transaction.openOuter()) {
			long extracted = from.extract(maxAmount, transaction);
			if (extracted <= 0) {
				return 0;
			}

			long inserted = to.insert(extracted, transaction);
			if (inserted < extracted) {
				long remainder = extracted - inserted;
				// Try to refund the remainder back into the source.
				long refunded = from.insert(remainder, transaction);
				if (refunded != remainder) {
					// Can't refund: abort transfer.
					return 0;
				}
			}

			if (!simulate) {
				transaction.commit();
			}
			return inserted;
		}
	}
}
