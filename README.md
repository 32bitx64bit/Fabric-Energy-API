# Fabric Energy API (FEA)

Standardized energy API for Fabric. 
Experimental btw, so don't use it for any real projects yet :P

## FE Spec (Compatibility)

- **Unit**: FE is an integer unit stored in `long`.
- **Rate**: All rates are **FE per tick (FE/t)**, where “tick” means server tick.
- **Transactions/simulation**:
	- All `FEStorage` operations are transactional via Fabric Transfer transactions.
	- Simulation MUST have no side effects (no NBT writes, no permanent state changes).
- **Sided IO**:
	- If a block exposes `FEBlockEnergy` via `FEApi.BLOCK_ENERGY`, then automation/cables MUST respect `getSideMode(side)`.
	- If no `FEBlockEnergy` is provided, behavior is permissive (no extra sided restrictions beyond the exposed `FEStorage`).
- **Cable connectivity**:
	- Cables connect on the 6 cardinal directions.
	- A cable-to-cable edge exists only if both sides expose `FECable` via `FEApi.CABLE` for the touching faces.
- **Cable capacity**:
	- A path’s capacity is the minimum `getTransferCapacityFE()` across the traversed cable segments.
# Fabric Energy API (FEA)

A small, Fabric-native, standardized energy API for Minecraft 1.20.1.

This project gives mods a shared way to expose and consume energy (“FE”) across:
- BlockEntities (machines, batteries)
- Items (tools, batteries)
- Cables (pathfinding + loss)

## Quickstart (as a modder)

### 1) Expose a block/entity energy storage

Register a storage provider for your `BlockEntityType`:

```java
import gavinx.fea.api.FEApi;
import gavinx.fea.api.FEStorage;
import net.minecraft.util.math.Direction;

// in your mod init
FEApi.STORAGE.registerForBlockEntities(
	(be, side) -> ((MyMachineBlockEntity) be).getEnergyStorage(side),
	MY_MACHINE_BLOCK_ENTITY_TYPE
);
```

In your block entity, back it with the reference implementation:

```java
import gavinx.fea.api.FEStorage;
import gavinx.fea.impl.BlockEntityFEStorage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Direction;

public final class MyMachineBlockEntity extends BlockEntity {
	private final BlockEntityFEStorage energy = new BlockEntityFEStorage(100_000, this::markDirty);

	public FEStorage getEnergyStorage(Direction side) {
		return energy;
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		energy.readFromNbt(nbt);
	}

	@Override
	protected void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		energy.writeToNbt(nbt);
	}
}
```

### 2) Consume energy (machines that spend FE)

Use **exact-cost** consumption for “only run if we can pay the full cost” logic:

```java
import gavinx.fea.api.FEConsumption;

long cost = 200;
if (FEConsumption.consumeExact(energyStorage, cost, false)) {
	// do the work
}
```

Or from the world lookup directly:

```java
import gavinx.fea.api.FE;
import net.minecraft.util.math.Direction;

if (FE.consumeExact(world, pos, Direction.UP, 200, false)) {
	// do the work
}
```

### 3) Generate energy (FE/t)

Generation is standardized as FE per tick (FE/t). You call this from your normal server tick logic.

```java
import gavinx.fea.api.FEGeneration;

// Example: produce 40 FE/t while active.
FEGeneration.tickGenerateTo(energyStorage, 40, isBurning, false);
```

## Common APIs

**Core**
- `gavinx.fea.api.FEStorage` – transactional insert/extract + amount/capacity
- `gavinx.fea.api.FEApi` – lookups:
	- `FEApi.STORAGE` (blocks / block entities)
	- `FEApi.ITEM_STORAGE` (items)
	- `FEApi.CABLE` (cables)
	- `FEApi.BLOCK_ENERGY` (optional block metadata: sided IO + capacity + FE/t reporting)

**Helpers**
- `gavinx.fea.api.FETransactions` – simple `insert/extract` with `simulate`
- `gavinx.fea.api.FETransfer` – transactional move from one storage to another
- `gavinx.fea.api.FEConsumption` – `consumeExact` / `consumeUpTo` helpers for consumers
- `gavinx.fea.api.FE` – convenience find/insert/extract/consumeExact for world + items
- `gavinx.fea.api.FEGeneration` – generation helpers (FE/t)
- `gavinx.fea.network.FECableTransfer` – pathfind + distribute through cables

## Examples

### Item energy (batteries/tools)

Register an item storage provider:

```java
import gavinx.fea.api.FEApi;
import gavinx.fea.impl.NbtFEItemStorage;

FEApi.ITEM_STORAGE.registerForItems(
	(stack, ctx) -> new NbtFEItemStorage(stack, 10_000),
	MY_BATTERY_ITEM
);
```

Consume from an item:

```java
import gavinx.fea.api.FEConsumption;

boolean paid = FEConsumption.consumeExact(stack, 50, false);
```

### Optional sided IO + capacity metadata (`FEBlockEnergy`)

If your block exposes `FEBlockEnergy`, automation/cable transfer will respect per-side modes.

```java
import gavinx.fea.api.FEApi;
import gavinx.fea.api.FEBlockEnergy;
import gavinx.fea.api.FESideMode;
import net.minecraft.util.math.Direction;

FEBlockEnergy def = new FEBlockEnergy() {
	@Override
	public long getStorageCapacityFE() {
		return 100_000;
	}

	@Override
	public FESideMode getSideMode(Direction side) {
		return side == Direction.UP ? FESideMode.IN : FESideMode.NONE;
	}

	@Override
	public long getGenerationRateFE() {
		return 40; // FE/t, reporting only
	}
};

FEApi.BLOCK_ENERGY.registerForBlocks(
	(world, pos, state, ctx) -> def,
	MY_MACHINE_BLOCK
);
```

### Cables + network transfer

Expose a cable capability (you can use the `Direction` context to make a face non-connectable by returning `null`):

```java
import gavinx.fea.api.FEApi;
import gavinx.fea.api.FECable;

FECable cable = new FECable() {
	@Override
	public long getTransferCapacityFE() {
		return 1_000; // FE per transfer op (typically per-tick)
	}

	@Override
	public int getResistancePercent() {
		return 2; // 2% loss per segment
	}
};

FEApi.CABLE.registerForBlocks(
	(world, pos, state, side) -> cable,
	MY_CABLE_BLOCK
);
```

Push FE through the cable network from a source side:

```java
import gavinx.fea.network.FECableTransfer;

long extractedPreLoss = FECableTransfer.distributeFrom(serverWorld, sourcePos, sourceSide, 1_000, false);
```

## FE Spec (Compatibility)

- **Unit**: FE is an integer unit stored in `long`.
- **Rate**: all rates are **FE per tick (FE/t)** (server tick).
- **Transactions/simulation**:
	- `FEStorage` operations are transactional (Fabric Transfer transactions).
	- Simulation must have no lasting side effects (no NBT writes, no permanent state changes).
- **Sided IO**:
	- If a block exposes `FEBlockEnergy` via `FEApi.BLOCK_ENERGY`, automation/cables should respect `getSideMode(side)`.
	- If no `FEBlockEnergy` is provided, behavior is permissive beyond the exposed `FEStorage`.
- **Cable connectivity**:
	- Cables connect on the 6 cardinal directions.
	- A cable-to-cable edge exists only if both sides expose `FECable` via `FEApi.CABLE` for the touching faces.
- **Cable capacity**: path capacity is the minimum `getTransferCapacityFE()` across segments.
- **Resistance / loss**:
	- Resistance is a percent in `[0..100]`.
	- Resistance is additive across segments, clamped to 100.
	- Delivered FE is `floor(sent * (100 - resistance) / 100)`.
- **Distribution**: `FECableTransfer` prefers lower resistance first, then higher capacity.
- **World/chunks**: transfers only occur within the queried (loaded) world context; missing lookups return `null`.

## Build (Java 17)

Minecraft 1.20.1 toolchains expect Java 17.

```fish
cd "/home/gavin/Desktop/Fabric Energy API"
set -x JAVA_HOME /usr/lib/jvm/java-17-openjdk
set -x PATH $JAVA_HOME/bin $PATH
./gradlew build
```

## License

See `LICENSE`.
