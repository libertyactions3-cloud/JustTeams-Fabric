# CONTINUATION / HANDOFF PROTOCOL — READ THIS FIRST

> This file is the persistent handoff for continuing the JustTeams-Fabric port. Read it before auditing, editing, or claiming progress.

## Canonical project

Repository: `libertyactions3-cloud/JustTeams-Fabric`
Branch: `main`
Project directory: `JustTeams-Fabric/`

Do not use the obsolete `libertyactions3-cloud/test` repository for this work.

Pinned environment:

```text
Minecraft 1.21.11
Yarn 1.21.11+build.4
Fabric Loader 0.18.4
Fabric API 0.141.4+1.21.11
Fabric Loom 1.15.5
Java 21
```

Minecraft/Fabric APIs must be verified against these actual mappings/settings before inserting code.

---

# USER WORKFLOW RULES — CURRENT

These rules override older/general workflow assumptions in this document.

## Continue on every message

Treat **every user message** as permission to continue the project.

The word **`Continue`** is only a reminder to continue. It is not a request to pause, wait, ask for confirmation, or stop repository activity.

## Stay scoped to the current feature

The goal is **not** to audit the entire Fabric project at once.

For the current feature:

```text
exact verified 2.5.3 behavior
        ↓
identify Fabric pieces actually missing
        ↓
implement only those pieces
```

Do not investigate unrelated subsystems merely because a bug or architectural improvement is noticed.

Unrelated issues may be recorded briefly as `Later`, but must not be investigated further unless they directly block or affect the current feature.

Do not redesign broader architecture merely because a cleaner design is possible.

When command and GUI paths implement the same feature, make them converge on the same underlying behavior where necessary for parity, but do not use that as a reason to redesign unrelated systems.

## Audit/design before implementation

Do **not** change repository Java/source code while we are still establishing behavior or designing the missing pieces unless the user explicitly says we are moving into implementation.

Small documentation/status updates are allowed when they record verified evidence or workflow decisions.

Prefer the **smallest correct implementation** that reproduces verified 2.5.3 behavior.

Never create behavior solely because a configuration key, permission, setter, or similarly named class exists.

## Repository activity / 10-round workflow

Continue doing repository activity in the established rounds before the final build checkpoint.

Rounds are evidence-driven. Do not invent work merely to fill a round.

Do not run the final clean build before Round 10 unless a meaningful testing decision explicitly requires it.

Final build command:

```powershell
./gradlew clean build --refresh-dependencies
```

---

# CURRENT RESUME POINT — ITEM ECONOMY / FEATURE COSTS

The current workstream is the **internal item economy and 2.5.3 feature-cost parity**.

The user explicitly chose an internal item economy instead of requiring an external economy plugin/mod.

Existing abstraction:

```text
EconomyProvider
    ├── getCurrencyName()
    ├── isAvailable()
    ├── getBalance(player)
    ├── withdraw(player, amount)
    └── deposit(player, amount)
```

`EconomyTransactionResult` supports:

```text
SUCCESS
INSUFFICIENT_FUNDS
UNAVAILABLE
INVALID_AMOUNT
```

A concrete `ItemEconomyProvider` is wired through:

```java
JustTeamsFabric.economy()
```

## Currency denominations

Reference item economy established from the user's server Skript:

```text
Emerald               = 1
Emerald Block         = 9
Deepslate Emerald Ore = 81
```

The configured currency boundary is `bank.currency-items`.

Important separation:

```text
TeamBank
  = team-owned currency-item inventory

ItemEconomyProvider
  = player-owned currency balance / withdraw / deposit abstraction
```

Do not merge these two concepts.

## Item-economy semantics

The provider follows the supplied Skript's behavior, including its denomination ordering/preservation rules, intentional overpayment, and change behavior.

Change is returned as:

```text
Emerald Blocks + Emeralds
```

Deepslate Emerald Ore is not returned as change.

Failed withdrawals must not mutate the player's inventory.

The implementation was checked against the pinned 1.21.11/Yarn/Fabric environment, including the current PlayerInventory and ItemStack APIs used by the provider.

---

# FEATURE-COST LAYER

`FeatureCostManager` is the generic feature-level charge boundary backed by the item economy.

Configured defaults:

```text
sethome        100
home            50
enderchest      25
setwarp        200
warp             75
bank-withdraw    10
rename          500
```

These are item-currency units, not Vault money.

Configuration keys:

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

Fractional feature costs are rejected because the configured item currency is discrete.

---

# VERIFIED PAID-FEATURE INTEGRATIONS

## Warp use

Current Fabric warp usage charges the existing per-warp `TeamWarp.cost` before cooldown/password/warmup.

```text
team lookup
    ↓
warp lookup
    ↓
warp enabled check
    ↓
withdraw TeamWarp.cost
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

Payment is intentionally not refunded after a failed password or warmup cancellation because that ordering matches the observed 2.5.3 behavior.

The 2.5.3 TeamGUI also appears to charge when opening the warp list and again on the individual warp item. Fabric deliberately does not reproduce that apparent double-charge quirk merely for opening the list. Treat this as a documented parity exception unless later evidence establishes that it was intentional.

## Home teleport

`TeamTeleportManager.requestHome(...)` owns the `home` feature charge, covering both `/team home` and Home GUI teleport use without double charging.

The charge occurs before the cooldown/warmup path.

## Set home

Both known entry points charge `sethome` before changing the stored location:

```text
/team home set
Home GUI → Set Home
```

## Team Ender Chest

`TeamEnderChestGui.open(...)` owns the `enderchest` feature charge, so these callers converge on one charge boundary:

```text
/team enderchest
/team ec
Team main GUI → Ender Chest
```

Do not add another caller-side charge without checking for double charging.

## Warp creation

Both known entry points charge `setwarp` before creating the warp:

```text
/team warp set <name>
Warp GUI → Create New Warp
```

---

# BANK-WITHDRAW — VERIFIED 2.5.3 PARITY DECISION

This is **resolved**, not an outstanding omission.

The shipped 2.5.3 configuration contains:

```text
feature_costs.economy.bank_withdraw = 10.0
```

However, the actual 2.5.3 bank-withdraw call path that was established from the reference source does **not** call:

```text
canAffordAndPay(player, "bank_withdraw")
```

The actual reference withdrawal sequence is:

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

Therefore the configured `bank_withdraw` cost is an unused/dead configuration entry in the verified 2.5.3 path.

Fabric must **not** invent a charge for it.

Current Fabric behavior is correct:

```text
TeamBankScreenHandler
    ↓
BankSlot.canTakeItems(player)
    ↓
BYPASS_BANK_WITHDRAW
       OR
member.canWithdraw()
```

`canTakeItems()` is authorization/inventory mechanics only. It is not a feature-payment transaction boundary.

Do **not** add:

```java
FeatureCostManager.charge(player, "bank-withdraw")
```

to `canTakeItems()` or another bank withdrawal predicate.

The `feature-costs.bank-withdraw` configuration entry remains solely for configuration parity with shipped 2.5.3.

The current repository status for this feature is:

```text
2.5.3 configured cost       10   (present)
2.5.3 actual charge call     none
Fabric charge missing       no
Fabric authorization         present
Fabric bypass permission     present
Fabric TeamBank              present
```

---

# TEAMWARP.COST — DELIBERATE FABRIC EXTENSION

2.5.3 uses a global:

```text
feature_costs.economy.warp = 75
```

Fabric already has persistent:

```java
TeamWarp.cost
```

and its GUI allows per-warp configuration.

Current decision: **keep `TeamWarp.cost`**. Do not delete persisted per-warp cost data or replace it with the global 75-unit value unless the user explicitly chooses strict global-cost parity.

---

# RENAME COST — NOT YET A FEATURE IMPLEMENTATION TARGET

The reference has:

```text
rename = 500
```

Do not create a rename command or charge solely because this configuration entry exists.

Only investigate it when the corresponding actual Fabric rename feature path is the current feature being traced.

Required process:

```text
establish actual Fabric rename entry point
        ↓
establish exact 2.5.3 rename charge call path
        ↓
identify only missing Fabric pieces
        ↓
implement only those pieces
```

---

# STATUS RULES FOR OTHER SUBSYSTEMS

Completed work should not be restarted without a concrete parity defect.

Major completed areas include:

- core Fabric setup;
- team system/persistence;
- permissions/command framework;
- team chat;
- viewer-specific glow;
- team membership lifecycle;
- persistent team Ender Chest;
- centralized home/warp teleport behavior;
- internal item economy.

Record unrelated bugs as a brief `Later` note only. Do not investigate them during the current feature round unless they directly block the feature.

---

# ROUND 10 / BUILD PROTOCOL

The final checkpoint remains:

```text
Rounds 1–9
    ↓
evidence / parity / targeted implementation
    ↓
Round 10
    ↓
./gradlew clean build --refresh-dependencies
    ↓
local runtime testing
    ↓
fix verified failures only
    ↓
final status reconciliation
```

No clean Gradle build has been run for the current/latest item-economy changes.

---

# HANDOFF / SEARCH PROTOCOL

When a repository-wide search is required:

1. Use the GitHub repository search when it is actually available and reliable.
2. If the connector cannot perform a reliable repository-wide search, tell the user exactly what search terms/results are needed.
3. Never convert a narrow/no-result search into a claim of repository-wide absence.
4. Use the user's supplied repository search results as authoritative evidence for the current audit when they directly show the relevant call path.

When a feature is being audited, the required output of the round is:

```text
exact verified 2.5.3 behavior
        ↓
current Fabric behavior
        ↓
missing Fabric pieces
        ↓
implementation decision
```

Nothing outside that feature path should be investigated unless it blocks the current feature.

---

# HISTORICAL NOTE

Earlier completed rounds included the Fabric setup, permissions, team chat, glow, membership lifecycle, Ender Chest, teleport, and item-economy foundation. Their detailed implementation history may exist in older project `.md` handoff/audit files, but this file should be treated as the current operational source of truth for continuation behavior and current status.