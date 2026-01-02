package gavinx.fea.network;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.server.world.ServerWorld;

/** Static access to per-world cable network managers. */
public final class FECableNetworks {
	private FECableNetworks() {}

	private static final Map<ServerWorld, FECableNetworkManager> MANAGERS = new WeakHashMap<>();

	public static synchronized FECableNetworkManager get(ServerWorld world) {
		return MANAGERS.computeIfAbsent(world, FECableNetworkManager::new);
	}
}
