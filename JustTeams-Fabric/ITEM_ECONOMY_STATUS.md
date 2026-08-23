# Item Economy Status

## Working / conversation protocol

These instructions are part of the active project handoff and should be followed when continuing this repository work:

1. The goal is **not** to audit the entire Fabric project. For the current feature, establish the exact verified 2.5.3 behavior, identify only the Fabric pieces required for that feature, and then implement only those missing pieces.
2. Stay scoped to the **current feature path being traced or implemented**. Do not investigate or redesign unrelated parts of the repository merely because they are noticed.
3. Record unrelated bugs or architectural observations only as a brief **Later** note when useful. Do not investigate them further unless they directly block or affect the current feature.
4. Do **not** change repository code during audit/design unless the user explicitly says the work is moving into implementation.
5. Prefer the **smallest correct implementation** that reproduces the verified 2.5.3 behavior.
6. When command and GUI paths implement the same feature, make them use the same underlying behavior where necessary for parity, but do not use that as a reason to redesign unrelated systems.
7. Continue the established **repository-activity rounds** before the eventual Gradle clean-build checkpoint. Repository activity should be focused, incremental source inspection rather than broad project-wide auditing.
8. The `.md` handoff files are used as project memory/state. Important decisions, verified behavior, missing pieces, and workflow instructions should be recorded here so later sessions can resume accurately.
9. The standing chat rule is: **always continue the current work on every user message**, including questions, clarifications, corrections, or other messages. The user's word **"Continue"** is only a reminder to continue; it does not mean to pause, restart, or ask for confirmation.

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

`TeamTeleportManager.requestHome()` charges the `home` feature cost before the home cooldown/warmup path. This covers both `/team home` and the Home GUI because both route through the centralized teleport controller.

The Home GUI's **set home** action separately charges `sethome` before changing the stored team location.

The Home GUI does not charge the teleport a second time; the centralized controller owns that charge.

The direct `/team home set` command also charges `sethome` before changing the stored location.

### Ender Chest

`TeamEnderChestGui.open(...)` charges `enderchest` before opening the shared team Ender Chest. This is the central feature entry point, so `/team enderchest`, `/team ec`, and GUI access converge on the same charge path instead of duplicating charges.

### Warp creation

The Fabric Warp GUI's `set new warp` path charges `setwarp` before creating the warp.

The direct `/team warp set <name>` command also charges `setwarp` before creating the warp.

Both match the verified 2.5.3 command/GUI feature-cost ordering: charge before the underlying creation method continues.

## Bank-withdraw parity decision

The configured 2.5.3 value `feature_costs.economy.bank_withdraw = 10.0` is **not charged by the actual 2.5.3 bank-withdraw call path** that was established through repository-wide source search supplied by the user.

The reference bank withdrawal performs:

```text
permission check
  ↓
amount validation
  ↓
team balance check
  ↓
remove amount from team balance
  ↓
deposit amount into player's Vault balance
```

There is no `canAffordAndPay(player, "bank_withdraw")` call in that path.

Therefore Fabric deliberately does **not** add `FeatureCostManager.charge(player, "bank-withdraw")` to `TeamBankScreenHandler.canTakeItems()` or any other bank withdrawal predicate.

`TeamBankScreenHandler.canTakeItems()` remains an authorization/mechanics gate only. This is a verified parity decision, not an unresolved omission.

The `feature-costs.bank-withdraw` configuration entry remains because it mirrors the shipped 2.5.3 configuration, but it is intentionally unused just as the reference call graph leaves it unused.

## Team bank vs player item economy

These remain separate concepts:

```text
TeamBank
  = team-owned configured currency-item storage

ItemEconomyProvider
  = player's configured currency-item balance
```

That mirrors the 2.5.3 conceptual split between team bank balance and the player's Vault balance.

## Remaining economy questions

- `TeamWarp.cost` remains a deliberate Fabric extension. The current Fabric `TeamWarp` model persists a per-warp numeric cost and the warp-management GUI exposes it for configuration. The strict 2.5.3 global reference value remains documented as `feature-costs.warp = 75`; the Fabric implementation does not destructively remove the existing per-warp model.
- The 2.5.3 TeamGUI contains an apparent double-charge path for the `warps` button: it calls `canAffordAndPay(player, "warp")` before opening the warp GUI, and the individual warp item calls it again before teleporting. The current Fabric GUI does **not** intentionally reproduce that apparent double-charge quirk; it charges at the actual warp-use point. This remains a documented parity exception unless new evidence establishes that the upstream behavior was intentional.
- Perform a final audit for any additional actual GUI or command entry point that performs a paid feature without passing through the centralized cost path. Repository-wide search should be requested from the user whenever the connector's repository-wide search becomes unavailable.

## Build status

No Gradle build has been run for these economy changes. The project protocol reserves the clean build for the later final checkpoint. The Minecraft inventory APIs used by the provider were checked against the project's pinned Gradle/Yarn environment before insertion.
