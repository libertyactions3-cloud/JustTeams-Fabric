# Item Economy Status

## Current implementation

The canonical `libertyactions3-cloud/JustTeams-Fabric` `main` branch now has a concrete `ItemEconomyProvider` implementing the existing `EconomyProvider` abstraction.

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

The relevant Yarn inventory APIs were checked against the `1.21.11+build.4` documentation. In particular, `Inventory.getStack(int)`, `Inventory.removeStack(int, int)`, `PlayerInventory.offerOrDrop(ItemStack)`, and the existing `PlayerInventory` inventory access pattern are valid for this environment. citeturn904013search0turn904013search7

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

## Warp integration completed

The item economy is now connected to the real Fabric warp command and warp GUI paths.

### `/team warp`

The current Fabric command now performs:

```text
team lookup
  ↓
warp lookup
  ↓
warp enabled check
  ↓
withdraw item-economy cost
  ↓
warp cooldown check
  ↓
password check
  ↓
TeamTeleportManager.requestWarp(...)
  ↓
5-second warmup
  ↓
teleport
```

This preserves the verified 2.5.3 ordering where the cost is withdrawn before `TeamManager.teleportToTeamWarp()`. The reference source establishes that the payment occurs before the cooldown/password/warmup logic inside `teleportToTeamWarp`.

### Warp GUI

The current Fabric `TeamWarpGui` now also withdraws the configured warp cost before the password prompt/teleport request. This mirrors the reference GUI behavior: the 2.5.3 warp-item click calls `canAffordAndPay(player, "warp")` before `teleportToTeamWarp(...)`. fileciteturn341file0

Therefore a failed password after payment is intentionally **not refunded**, matching the verified reference ordering.

### Fractional costs

Because the Fabric provider is an item economy with integer denominations, fractional warp costs cannot be represented exactly. `TeamWarpManagementGui` now only accepts whole-number non-negative warp costs and displays an explicit validation error for fractional values.

This is an intentional Fabric item-economy constraint rather than a silent rounding/truncation behavior.

## Important reference observation

The 2.5.3 TeamGUI's `warps` button itself also calls `canAffordAndPay(player, "warp")` before opening `WarpsGUI`, while the individual `warp_item` click calls it again before teleporting. fileciteturn336file0 fileciteturn341file0

The current Fabric GUI does **not** intentionally reproduce that apparent double-charge quirk on opening the warp list; it charges at the actual warp-use point. This is recorded as a deliberate parity exception pending a broader decision about whether that 2.5.3 behavior is an intended feature cost or an upstream GUI bug.

## Commits

- `390c51bdc50740dc0633d7d07adddde2c97011a3` — add item-backed emerald economy provider
- `fcc3b2006ce24288a277a73a40202962907bb803` — wire the economy provider into `JustTeamsFabric`
- `d78e56a0e0e611b51f829098f19a51ad34aa450d` — integrate item economy into `/team warp`
- `a926173020eca95fa3fc73e2030f61992fb2698f` — integrate item economy into warp GUI
- `528ef0068e4bf066983009db6d6247020219b57f` — restrict warp costs to whole-number item-economy values

## Build status

No Gradle build was run for these economy changes. The project protocol reserves the appropriate clean-build checkpoint for later. The implementation was checked against the project's pinned Gradle/Yarn environment before inserting the Minecraft API calls.

## Next resume point

Audit the rest of the item-economy consequences before moving to another parity feature:

1. Verify whether other 2.5.3 paid features should use the same item economy.
2. Audit whether `TeamWarp.cost` remains intentionally per-warp in the Fabric port or should be mapped to the reference's global feature cost concept.
3. Reconcile the apparent 2.5.3 double-charge when opening the warp GUI versus using a warp.
4. Then move on to the next evidence-based parity target.
