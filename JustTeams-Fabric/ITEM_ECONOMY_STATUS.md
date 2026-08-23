# Item Economy Status

## Current implementation

The canonical `libertyactions3-cloud/JustTeams-Fabric` `main` branch has a concrete `ItemEconomyProvider` implementing the existing `EconomyProvider` abstraction.

### Currency denominations

The provider follows the supplied server Skript for the default currency:

- Emerald = 1
- Emerald Block = 9
- Deepslate Emerald Ore = 81

The existing configuration already defaults `bank.currency-items` to those three items, so the provider uses the same configured currency-item boundary as `TeamBank`.

### Withdrawal behavior

The withdrawal algorithm follows the supplied Skript's low-to-high denomination preference and its preservation rule:

- Emeralds are considered first.
- When a higher denomination exists and the Skript would preserve one lower denomination, the provider does the same.
- Emerald Blocks are considered next with the analogous preservation rule when ore exists.
- Deepslate Emerald Ore is used last.
- Overpayment is intentional.
- Change is returned as Emerald Blocks plus Emeralds.
- Deepslate Emerald Ore is not returned as change, matching the supplied Skript.
- Failed balance checks do not mutate the player's inventory.

### Inventory API verification

The implementation is targeted to and checked against the project's pinned environment:

- Minecraft `1.21.11`
- Yarn `1.21.11+build.4`
- Fabric Loader `0.18.4`
- Fabric API `0.141.4+1.21.11`
- Java 21

The relevant Yarn inventory APIs were checked against the `1.21.11+build.4` documentation, including the project's existing `PlayerInventory` access pattern, `getStack(int)`, `removeStack(int, int)`, and `offerOrDrop(ItemStack)` APIs.

### Core wiring

`JustTeamsFabric` creates the item economy provider during initialization and exposes it through:

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

## Generic feature costs

Added `FeatureCostManager`, backed by the same item economy, with the verified 2.5.3 numeric feature-cost defaults:

```text
sethome       100
home           50
enderchest     25
setwarp       200
warp           75
bank-withdraw  10
rename        500
```

These values are stored in the Fabric `justteams.properties` configuration as `feature-costs.*` settings. Because this port is using an item economy, the values represent whole units of the configured emerald currency rather than Vault money.

## Current integrations

### Warp

The item economy is connected to the actual Fabric warp command and GUI paths.

The verified 2.5.3 order is preserved for warp use:

```text
warp lookup
  ↓
warp enabled check
  ↓
withdraw warp cost
  ↓
warp cooldown check
  ↓
password check
  ↓
5-second warmup
  ↓
teleport
```

A failed password after payment is intentionally not refunded, matching the reference ordering.

### Home teleport

`TeamTeleportManager.requestHome()` now charges the `home` feature cost before the home cooldown/warmup path. This covers both `/team home` and the Home GUI because both route through the centralized teleport controller.

The Home GUI's **set home** action separately charges `sethome` before changing the stored team location.

The Home GUI does not charge the teleport a second time; the centralized controller owns that charge.

The direct `/team home set` command now also charges `sethome` before changing the stored location.

### Ender Chest

`TeamEnderChestGui.open(...)` now charges `enderchest` before opening the shared team Ender Chest. This is the central feature entry point, so `/team enderchest`, `/team ec`, and GUI access converge on the same charge path instead of duplicating charges.

### Warp creation

The Fabric Warp GUI's `set new warp` path charges `setwarp` before creating the warp.

The direct `/team warp set <name>` command now also charges `setwarp` before creating the warp.

Both match the verified 2.5.3 command/GUI feature-cost ordering: charge before the underlying creation method continues.

## Reference observations

The verified 2.5.3 `FeatureRestrictionManager` performs feature economy withdrawal before the feature method continues. It is generic across feature names rather than being a warp-only mechanism.

The verified 2.5.3 command paths explicitly charge `sethome`, `home`, `enderchest`, `setwarp`, and `warp` before invoking their feature methods.

The 2.5.3 TeamGUI also contains an apparent double-charge path for the `warps` button: it calls `canAffordAndPay(player, "warp")` before opening the warp GUI, and the individual warp item calls it again before teleporting. The current Fabric GUI does **not** intentionally reproduce that apparent double-charge quirk; it charges at the actual warp-use point. This remains a documented parity exception pending a deliberate decision that the upstream behavior is intentional rather than an apparent GUI bug.

## Known remaining economy work

- Decide whether Fabric's per-warp `TeamWarp.cost` should remain as a deliberate extension or be replaced by the 2.5.3 global `feature-costs.warp` concept. No destructive change has been made yet.
- Review `bank-withdraw` and `rename` only if/when those corresponding Fabric feature paths are actually implemented; do not create dead commands merely to populate the cost table.
- Perform a final audit for any additional actual GUI or command entry point that performs a paid feature without passing through the centralized cost path. Avoid claiming repository-wide absence unless the search/index supports it.

## Build status

No Gradle build has been run for these economy changes. The project protocol reserves the clean build for the later final checkpoint. The Minecraft inventory APIs used by the provider were checked against the project's pinned Gradle/Yarn environment before insertion.
