# CONTINUATION / HANDOFF PROTOCOL — READ THIS FIRST

> **Purpose:** This section is the operational handoff for any new ChatGPT conversation continuing the JustTeams-Fabric project. Read this entire section **before searching, editing, auditing, or claiming progress**. The project below is a Fabric recreation/port whose goal is behaviorally informed parity with the actual JustTeams 2.5.3 reference—not merely a project that compiles.

## Latest continuation update — economy / current resume point

**Canonical repository:** `libertyactions3-cloud/JustTeams-Fabric`  
**Branch:** `main`  
**Project directory:** `JustTeams-Fabric/`

The old `libertyactions3-cloud/test` repository is obsolete for this work. Do not use it.

### Exact pinned environment

Use and verify Minecraft/Fabric APIs against the current project versions:

- Minecraft `1.21.11`
- Yarn mappings `1.21.11+build.4`
- Fabric Loader `0.18.4`
- Fabric API `0.141.4+1.21.11`
- Fabric Loom observed during builds: `1.15.5`
- Java 21

The user specifically requires that inserted Minecraft/Fabric methods be verified against these actual mappings/project settings instead of being invented from memory or copied from another Minecraft version.

### User continuation rule

Treat every user message as permission to continue the project. The word **“Continue”** is only a reminder, not a special trigger.

When repository-wide search is genuinely required and the GitHub connector cannot perform a reliable repository-wide search, ask the user for the exact search and use their results. Do not claim repository-wide absence from a narrow search.

### 10-round workflow

The project uses a structured 10-round workflow, but rounds are evidence-driven. Do not invent work merely to fill a round.

Do not clean-build before the final Round 10 checkpoint unless a meaningful testing decision explicitly requires it. The user has stated that they will not clean-build until Round 10.

---

# Current completed work

## Core project / Fabric setup

Completed earlier.

## Team system / persistence

Completed earlier.

## Permissions / command framework

Completed earlier.

## Team chat

Completed earlier.

## Currency / bank foundation

Completed earlier.

## Glow — Round 7

Viewer-specific team glow was implemented using receiver-specific packets/metadata rather than globally making entities glow for everyone.

Important existing architecture:

```text
team glow state
    ↓
GlowManager refresh
    ↓
receiver-specific TeamS2CPacket + entity metadata glowing state
    ↓
receiver sees target glowing with team/role color
```

Do not restart the glow implementation without a concrete parity defect.

## Membership lifecycle

`TeamManager.addMember(...)` and `TeamManager.removeMember(...)` maintain both team membership and the UUID → team index.

Verified lifecycle cleanup work includes:

- GUI kick cleanup;
- GUI leave/disband cleanup;
- team-chat cleanup;
- viewer-specific glow cleanup;
- Ender Chest viewer/release cleanup where applicable;
- persistence after membership mutation.

Known commits from earlier work:

```text
633e499869b85d2a4c1c6338057cde0dcce92a95
5a3c41a4438895ee8929aa9233c43dc0d652e1eb
```

The user previously reported a successful clean build after the GUI kick lifecycle fix. No new clean build has been run for the later economy work.

## Team Ender Chest

The Fabric port has a shared persistent team-owned Ender Chest with real-time multi-viewer inventory behavior.

Known deliberate architectural differences from Paper/2.5.3 include:

- no Bukkit serialization;
- no Paper database/Redis locking implementation unless separately reintroduced;
- Fabric-native item/NBT persistence;
- current configured row behavior is Fabric-native where it differs from the old reference;
- Paper-only message/effects infrastructure is not copied literally.

Do not reimplement this subsystem without a concrete new parity defect.

## Teleport parity

A centralized `TeamTeleportManager` is implemented.

Verified/implemented parity work includes:

- home warmup: 5 seconds;
- home cooldown: 300 seconds;
- warp warmup: 5 seconds;
- warp cooldown: 300 seconds;
- warmup movement cancellation;
- squared-distance movement threshold consistent with the investigated reference behavior;
- same-world/start-world handling during warmup;
- immediate first countdown timing rather than an accidental extra second;
- configured warmup/success particles;
- configured success/error/teleport sounds;
- cooldown bypass handling where already established;
- centralized routing of `/team home`, warp usage, Home GUI, and Warp GUI through the teleport controller.

Do not restart this work without a concrete parity defect.

---

# Item economy implementation — NEWEST COMPLETED WORK

The project already had the `EconomyProvider` abstraction, and the user explicitly chose to create an **internal item economy** rather than depend on an external economy plugin/mod.

Existing abstraction:

```text
EconomyProvider
    ├── getCurrencyName()
    ├── isAvailable()
    ├── getBalance(player)
    ├── withdraw(player, amount)
    └── deposit(player, amount)
```

`EconomyTransactionResult` provides structured outcomes:

```text
SUCCESS
INSUFFICIENT_FUNDS
UNAVAILABLE
INVALID_AMOUNT
```

A concrete `ItemEconomyProvider` has been added and wired through:

```java
JustTeamsFabric.economy()
```

### Item denominations

The user supplied an existing server Skript that establishes the desired item-currency behavior. The implementation follows that model:

```text
Emerald                  = 1
Emerald Block            = 9
Deepslate Emerald Ore    = 81
```

The existing Fabric configuration already defaults `bank.currency-items` to those three items.

Important distinction:

```text
TeamBank
  = team-owned currency inventory

ItemEconomyProvider
  = player-owned currency balance / withdraw / deposit abstraction
```

Do not conflate the TeamBank's storage with the player's personal item-economy balance.

### Skript behavior used as the item-economy reference

The user's Skript calculates:

```text
ore amount × 81
+ block amount × 9
+ emerald amount
= total player currency value
```

Withdrawal order is:

```text
Emeralds
    ↓
Emerald Blocks
    ↓
Deepslate Emerald Ore
```

The Skript deliberately preserves a lower denomination in certain cases when a higher denomination exists. The Fabric implementation reproduces that preservation behavior rather than using a simpler greedy algorithm.

The Skript allows overpayment and returns change as:

```text
Emerald Blocks + Emeralds
```

It does **not** return Deepslate Emerald Ore as change.

The provider follows these semantics.

### Important example

A stack of 64 Deepslate Emerald Ore is **64 items worth 81 each**, not 5,184 items:

```text
64 × 81 = 5,184 currency value
```

If a cost is 75, the desired exact payment can be represented as:

```text
8 Emerald Blocks = 72
3 Emeralds       = 3
Total            = 75
```

The physical item counts are unchanged by the arithmetic; 5,184 is only the calculated currency value.

### Inventory API verification

The provider was implemented against the project's pinned Yarn/Fabric environment and the relevant APIs were checked before insertion.

Known verified APIs include the project's current `PlayerInventory` access pattern, `getStack(int)`, `removeStack(int,int)`, `markDirty()`, `offerOrDrop(ItemStack)`, `ItemStack.isOf(...)`, `ItemStack.getCount()`, and the current Minecraft item constants used by the provider.

Do not replace these with older mapping names without verifying against Yarn `1.21.11+build.4` first.

---

# Feature-cost layer

A `FeatureCostManager` was added as the generic bridge from the verified 2.5.3 feature-cost concept into the Fabric item economy.

Current configured defaults:

```text
sethome        100
home            50
enderchest      25
setwarp        200
warp             75
bank-withdraw    10
rename          500
```

These are Fabric item-currency units, not Vault money.

The current configuration keys are:

```text
feature-costs.enabled
feature-costs.sethome
feature-costs.home
feature-costs.enderchest
feature-costs.setwarp
feature-costs.warp
feature-costs.bank-withdraw
feature-costs.rename
```

Because item currency uses discrete whole denominations, fractional feature costs are rejected rather than silently rounded/truncated.

---

# Paid-feature integration currently completed

## `/team warp` usage

Current Fabric warp use charges the item's configured **per-warp `TeamWarp.cost`** before the downstream cooldown/password/warmup process.

Current command ordering:

```text
team lookup
    ↓
warp lookup
    ↓
warp enabled check
    ↓
withdraw per-warp TeamWarp.cost
    ↓
warp cooldown check
    ↓
password check
    ↓
TeamTeleportManager.requestWarp(...)
    ↓
warmup
    ↓
teleport
```

This preserves the verified 2.5.3 ordering where payment occurs before the call to the main warp teleport method, which then performs its cooldown/password/warmup work.

A failed password after payment is intentionally **not refunded**, because that is the observed reference ordering.

Warmup cancellation likewise does not refund the payment.

## Warp GUI usage

The actual Warp GUI item-click path also charges before password/teleport. This matches the verified 2.5.3 GUI item path.

The 2.5.3 TeamGUI also appears to call `canAffordAndPay("warp")` when opening the warp list and then again on the individual warp item. That looks like an upstream double-charge bug. The Fabric port deliberately does **not** reproduce the apparent double charge when merely opening the warp-list GUI.

Record this as a deliberate parity exception, not as an unresolved implementation failure.

## Home teleport

`TeamTeleportManager.requestHome(...)` now owns the `home` feature charge, so both:

```text
/team home
Home GUI → teleport to home
```

converge on one charge path and do not double-charge.

The charge occurs before the home cooldown/warmup process.

## Set home

Both known entry points charge `sethome`:

```text
/team home set
Home GUI → Set Home
```

The direct command was fixed after an audit found it bypassing the GUI/feature-cost layer.

## Team Ender Chest

The actual `TeamEnderChestGui.open(...)` feature entry point owns the `enderchest` charge. This means these callers converge on one charging point:

```text
/team enderchest
/team ec
Team main GUI → Ender Chest
```

Do not add another caller-side `enderchest` charge without checking for double charging.

## Warp creation

Both known entry points charge `setwarp`:

```text
/team warp set <name>
Warp GUI → Create New Warp
```

The direct command was fixed after an audit found it bypassing the GUI/feature-cost layer.

---

# `TeamWarp.cost` vs. 2.5.3 global warp cost — current decision

The 2.5.3 reference feature-cost system uses a global configured cost:

```text
feature_costs.economy.warp = 75
```

The Fabric port already has a persistent per-warp field:

```java
TeamWarp.cost
```

and the Warp Management GUI allows that value to be changed.

**Current decision:** keep `TeamWarp.cost` as a deliberate Fabric-side extension rather than destructively replacing/removing it. Do not delete persisted per-warp cost data.

The global Fabric `feature-costs.warp = 75` remains documented as the 2.5.3 reference value, but current warp-use payment uses the per-warp `TeamWarp.cost` model.

This is an explicit parity/porting exception that should be revisited only if the user decides to enforce strict global-cost parity.

---

# Bank-withdraw — CURRENT UNRESOLVED RESEARCH TARGET

The Fabric TeamBank currently enforces withdrawal permission through the inventory slot layer:

```text
TeamBankScreenHandler
    ↓
BankSlot.canTakeItems(player)
    ↓
member.canWithdraw()
OR
BYPASS_BANK_WITHDRAW
```

This is currently **not** charged through `FeatureCostManager`.

Do **not** simply put `FeatureCostManager.charge(player, "bank-withdraw")` inside `Slot.canTakeItems(...)` without first verifying the reference behavior. That method participates in inventory mechanics and may be called for many kinds of inventory interactions rather than a single explicit withdrawal transaction.

The unresolved question is:

```text
What exactly does 2.5.3 mean by the bank_withdraw feature cost?
```

We need the actual reference call chain to establish whether the cost applies:

- once when opening/using the bank;
- once per item withdrawal transaction;
- once per click/action;
- once per GUI operation;
- or under some other feature-specific condition.

### Repository-wide search handoff for the next chat

The GitHub connector's broad code search has returned no useful results for this investigation, so a new chat should **ask the user to perform repository-wide searches** rather than making an unsupported absence claim.

For the **Fabric repository**, ask the user to search:

```text
bank-withdraw
canWithdraw
canTakeItems
TeamBank
withdraw
```

For the **2.5.3 reference**, ask the user to search:

```text
bank_withdraw
canAffordAndPay
withdrawPlayer
bank
```

The most useful reference result is code showing where the `bank_withdraw` feature cost is charged relative to the actual inventory withdrawal operation.

Once those results are supplied:

```text
reference charge boundary
        ↓
Fabric equivalent charge boundary
        ↓
implement only if behavior is established
```

Do not fabricate a charge boundary.

---

# Rename cost — unresolved only if the Fabric rename feature exists

The reference has a global `rename` feature cost of 500.

Before adding any Fabric rename charge:

1. establish whether the corresponding actual Fabric rename feature exists;
2. identify its real command/GUI entry point;
3. compare its 2.5.3 call chain;
4. only then add the charge.

Do not create a rename command solely because `feature-costs.rename` exists.

---

# Remaining major work after economy

Once `bank-withdraw` is established and any necessary rename behavior is handled:

1. Continue the broader actual-feature parity audit against 2.5.3.
2. For each feature, trace:

```text
2.5.3 entry point
    ↓
authorization / permission context
    ↓
state mutation
    ↓
gameplay / rendering effect
    ↓
affected players
    ↓
lifecycle cleanup
    ↓
persistence / synchronization
    ↓
edge cases
```

3. Do not infer missing functionality solely from permission constants or similarly named classes.
4. Do not restart completed glow, membership, Ender Chest, teleport, or item-economy work unless a concrete defect is found.
5. Record meaningful discrepancies and decisions in the status files.

---

# Build / Round 10 protocol

The user will not perform another clean build until Round 10.

Use:

```powershell
./gradlew clean build --refresh-dependencies
```

for the eventual final checkpoint.

No clean Gradle build has been run for the most recent item-economy changes.

Before requesting Round 10 build/testing, ensure:

```text
item economy complete
bank-withdraw semantics resolved
remaining concrete parity discrepancies addressed or explicitly documented
status files reconciled
```

Then Round 10 should be:

```text
clean build
    ↓
user local runtime testing
    ↓
fix any verified failures
    ↓
final PROJECT_STATUS.md update
    ↓
completion
```

---

# Communication / repository-search protocol

The user wants the assistant to continue working on every message; “Continue” is merely a reminder.

When external or repository-wide research would materially unblock the task:

```text
What I am trying to determine:
...

Why it matters:
...

What exact search/reference would help:
...

What I can continue doing without that information:
...
```

When the GitHub connector can search directly, use it first.

When the connector cannot perform a reliable repository-wide search, ask the user to run the exact search and provide the results.

Never confuse:

- no result from a narrow search;
- an unavailable/unindexed code search;
- repository-wide absence.

Always verify Minecraft APIs against the pinned Yarn/Fabric versions before inserting them into the repository.

---

# Historical project status
