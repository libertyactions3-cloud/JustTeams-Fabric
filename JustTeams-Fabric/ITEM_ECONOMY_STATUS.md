# Item Economy Status

## Current implementation

The canonical `libertyactions3-cloud/JustTeams-Fabric` `main` branch now has a concrete `ItemEconomyProvider` implementing the existing `EconomyProvider` abstraction.

### Currency denominations

The provider follows the supplied server Skript exactly for the default currency:

- Emerald = 1
- Emerald Block = 9
- Deepslate Emerald Ore = 81

The existing configuration already defaults `bank.currency-items` to those three items, so the provider uses the same configured currency-item boundary as `TeamBank`.

### Withdrawal behavior

The withdrawal algorithm was compared against the supplied Skript logic across the denomination combinations and matches its low-to-high payment behavior, including the unusual preservation rule:

- Emeralds are considered first.
- If a higher denomination exists and using the lower denomination would exhaust it under the Skript's condition, one lower denomination is preserved.
- Emerald Blocks are considered next with the analogous preservation rule when ore exists.
- Deepslate Emerald Ore is used last.
- Overpayment is intentional.
- Change is returned as Emerald Blocks plus Emeralds.
- Deepslate Emerald Ore is not returned as change, matching the Skript.
- Failed balance checks do not mutate the player's inventory.

The implementation was also checked as a pure algorithm against the supplied Skript behavior for a broad set of small inventory/cost combinations; the withdrawal selections and resulting change matched.

### Inventory API verification

The implementation uses APIs verified against the project's pinned Minecraft/Yarn environment:

- Minecraft `1.21.11`
- Yarn `1.21.11+build.4`
- Fabric Loader `0.18.4`
- Fabric API `0.141.4+1.21.11`
- Java 21

Verified Yarn APIs used by the provider include:

- `ServerPlayerEntity.getInventory()` through the existing PlayerEntity inheritance
- `PlayerInventory.getStack(int)`
- `PlayerInventory.size()` through `Inventory`
- `PlayerInventory.removeStack(int, int)`
- `PlayerInventory.markDirty()`
- `PlayerInventory.offerOrDrop(ItemStack)`
- `ItemStack.getCount()`
- `ItemStack.isOf(Item)`
- `new ItemStack(Item, int)`
- `Items.EMERALD`
- `Items.EMERALD_BLOCK`
- `Items.DEEPSLATE_EMERALD_ORE`

### Core wiring

`JustTeamsFabric` now creates the item economy provider during initialization and exposes it through:

```java
JustTeamsFabric.economy()
```

The provider is intentionally separate from `TeamBank`:

```text
TeamBank
  = team-owned currency inventory

ItemEconomyProvider
  = player-owned currency balance / withdraw / deposit abstraction
```

## Not yet completed

The provider is currently wired into the core but is **not yet connected to the Team Warp command and GUI payment paths**.

The next implementation step is to integrate warp payment while preserving the already-verified 2.5.3 ordering:

```text
warp-name validation
    ↓
item-economy withdrawal
    ↓
warp cooldown check
    ↓
password check
    ↓
5-second warmup
    ↓
teleport
```

A cancelled warmup must not refund the payment, matching the verified 2.5.3 behavior.

The existing `TeamWarp.cost` field remains the numeric price used by the Fabric warp model. Because the item economy is discrete, fractional costs will need explicit handling rather than silently rounding.

## Commits

- `390c51bdc50740dc0633d7d07adddde2c97011a3` — add item-backed emerald economy provider
- `fcc3b2006ce24288a277a73a40202962907bb803` — wire the economy provider into `JustTeamsFabric`

## Build status

No Gradle build was run for this checkpoint. The project handoff protocol reserves the final clean build for the appropriate checkpoint. API compatibility was verified against the pinned Yarn mappings and existing project usage before implementation.
