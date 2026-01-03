package gavinx.fea.api;

import gavinx.fea.FeaMod;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

/**
 * Block API lookup entrypoint for exposing {@link FEStorage} from block entities.
 */
public final class FEApi {
	private FEApi() {}

	public static final BlockApiLookup<FEStorage, Direction> STORAGE = BlockApiLookup.get(
			new Identifier(FeaMod.MOD_ID, "fe_storage"),
			FEStorage.class,
			Direction.class
	);

	/** Cable lookup. Use the {@link Direction} context to optionally encode sided connectivity. */
	public static final BlockApiLookup<FECable, Direction> CABLE = BlockApiLookup.get(
			new Identifier(FeaMod.MOD_ID, "fe_cable"),
			FECable.class,
			Direction.class
	);

	/** Optional block definition: capacity + sided IO + generation rate reporting. */
	public static final BlockApiLookup<FEBlockEnergy, Void> BLOCK_ENERGY = BlockApiLookup.get(
			new Identifier(FeaMod.MOD_ID, "fe_block_energy"),
			FEBlockEnergy.class,
			Void.class
	);

	/**
	 * Optional link point (socket) capability for non-block connections (ropes/wires/wireless).
	 * Use the {@link Direction} context to represent the port side.
	 */
	public static final BlockApiLookup<FELinkPoint, Direction> LINK_POINT = BlockApiLookup.get(
			new Identifier(FeaMod.MOD_ID, "fe_link_point"),
			FELinkPoint.class,
			Direction.class
	);

	/** ItemStack lookup for exposing {@link FEStorage} from items (batteries, tools, etc). */
	public static final ItemApiLookup<FEStorage, Void> ITEM_STORAGE = ItemApiLookup.get(
			new Identifier(FeaMod.MOD_ID, "fe_item_storage"),
			FEStorage.class,
			Void.class
	);

	public static FEStorage find(ItemStack stack) {
		return ITEM_STORAGE.find(stack, null);
	}
}
