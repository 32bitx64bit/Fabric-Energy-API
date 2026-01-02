package gavinx.fea.api;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/**
 * Minimal, standardized energy storage API.
 *
 * <p>Units are intentionally generic ("FE"). All quantities are {@code long}.
 *
 * <h2>Transactions / Simulation</h2>
 * Implementations must be safe to use with Fabric Transfer transactions:
 * changes made during {@link #insert(long, TransactionContext)} and {@link #extract(long, TransactionContext)}
 * must only be persisted if the given {@link TransactionContext} is committed. In particular, simulation
 * (nested transactions that are not committed) must not produce lasting side effects (NBT writes,
 * {@code markDirty}, block updates, etc).
 */
public interface FEStorage {
	/**
	 * Attempt to insert up to {@code maxAmount}.
	 *
	 * <p>Callers should pass a non-negative amount; implementations should treat {@code maxAmount <= 0}
	 * as inserting nothing.
	 *
	 * @return the amount actually inserted
	 */
	long insert(long maxAmount, TransactionContext transaction);

	/**
	 * Attempt to extract up to {@code maxAmount}.
	 *
	 * <p>Callers should pass a non-negative amount; implementations should treat {@code maxAmount <= 0}
	 * as extracting nothing.
	 *
	 * @return the amount actually extracted
	 */
	long extract(long maxAmount, TransactionContext transaction);

	long getAmount();

	long getCapacity();

	default boolean supportsInsertion() {
		return true;
	}

	default boolean supportsExtraction() {
		return true;
	}
}
