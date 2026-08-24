# CONTINUATION / HANDOFF PROTOCOL — READ THIS FIRST

> This file is the persistent handoff for continuing the JustTeams-Fabric port. Read it before auditing, editing, or claiming progress.

## Canonical project

Repository: `libertyactions3-cloud/JustTeams-Fabric`
Branch: `main`
Project directory: `JustTeams-Fabric/`

Do not use the obsolete `libertyactions3-cloud/test` repository for this work.

Pinned environment:

```text
# Gradle
org.gradle.jvmargs=-Xmx1G
org.gradle.parallel=true
org.gradle.configuration-cache=false

# Fabric / Minecraft
minecraft_version=1.21.11
yarn_mappings=1.21.11+build.4
loader_version=0.18.4
loom_version=1.15-SNAPSHOT

# Mod
mod_version=0.1.0-SNAPSHOT
maven_group=eu.kotori.justteams
archives_base_name=justteams-fabric

# Dependencies
fabric_version=0.141.4+1.21.11

Java 21
Resolved Fabric Loom during the user's local build: 1.15.5
```

**Code-writing rule:** before writing or modifying Java/Fabric code, always verify the relevant syntax, parsing, mappings, and API signatures on the web against this pinned environment. The user's actual local `./gradlew clean build --refresh-dependencies` result is the final authority for compile compatibility.

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

# CURRENT RESUME POINT — RUNTIME / GUI PARITY FOR CURRENT FEATURE

The current workstream is the **runtime completion of the item-economy/teleport/GUI feature paths just implemented**.

Current verified runtime results:

```text
/team home set                 PASS
/team home destination         PASS
/team home payment timing      PASS
/team warp destination         PASS
/team warp payment timing      PASS
/team warp success message     PASS
/team enderchest /team ec      PASS
warp password creation/use     PASS
Warp GUI password creation/use PASS
team creation GUI              PASS
invalid-tag retry              PASS
command/GUI double-charge      PASS
latest clean Gradle build      PASS
```

The user has confirmed that all remaining focused runtime tests work as intended, and the latest local clean build completed successfully.

Current GUI/command paths covered:

```text
/team warp set <name> [password]
Warp GUI → create warp → optional password
Warp GUI → manage warp → password editing
/team ec /team enderchest
Team main GUI → Ender Chest
/team GUI → create team
```

---

# ITEM ECONOMY / FEATURE COSTS

The current workstream uses an internal item economy instead of requiring an external economy plugin/mod.

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

```text
Emerald               = 1
Emerald Block         = 9
Deepslate Emerald Ore = 81
```

Important separation:

```text
TeamBank
  = team-owned currency-item inventory

ItemEconomyProvider
  = player-owned currency balance / withdraw / deposit abstraction
```

Do not merge these concepts.

## Item-economy semantics

The provider follows the supplied server Skript's established denomination behavior, including its change rules.

Change is returned as:

```text
Emerald Blocks + Emeralds
```

Deepslate Emerald Ore is not returned as change.

Failed withdrawals must not mutate the player's inventory.

The provider is intended for the pinned 1.21.11/Yarn/Fabric environment and must continue to be checked against the actual local Gradle compile.

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

## Home teleport

`TeamTeleportManager.requestHome(...)` owns the `home` feature charge, covering both `/team home` and Home GUI use without double charging.

Required current runtime behavior:

```text
check validity / cooldown
    ↓
warmup
    ↓
successful teleport
    ↓
withdraw item currency
    ↓
home success message
```

The user runtime-tested the corrected payment timing and confirmed it works as intended.

## Set home

Both known entry points charge `sethome` before changing the stored location:

```text
/team home set
Home GUI → Set Home
```

The user's runtime test confirmed a 100-unit charge exactly:

```text
64 Deepslate Emerald Ore
→ 62 Deepslate Emerald Ore
   6 Emerald Blocks
   8 Emeralds
```

The destination was stored correctly.

## Team Ender Chest

`TeamEnderChestGui.open(...)` owns the `enderchest` feature charge, so these callers converge on one charge boundary:

```text
/team enderchest
/team ec
Team main GUI → Ender Chest
```

Do not add another caller-side charge.

The persistent team Ender Chest is retained on the `Team` object after normal release so the next open reuses the saved inventory. The user has runtime-tested `/team ec` and confirmed persistence works.

## Warp creation

Both command and GUI creation charge `setwarp` before creating the warp.

The GUI supports an optional password and password editing through `TeamStringInputGui`.

The command supports:

```text
/team warp set <name> [password]
```

The optional command password is implemented by `TeamWarpCommandExtensions`, which adds a final `greedyString` password argument to the existing `/team warp set <name>` node and stores it on `TeamWarp`.

The user has runtime-tested password-protected warp creation/use successfully.

## Warp use

The command and GUI both pass the per-warp `TeamWarp.cost` into `TeamTeleportManager.requestWarp(...)`.

Required current runtime behavior:

```text
validate warp/password/cooldown
    ↓
warmup
    ↓
successful teleport
    ↓
withdraw TeamWarp.cost
    ↓
warp success message
```

The user has confirmed the command message:

```text
You have successfully teleported to your team warp.
```

The user also confirmed the corrected post-success payment timing works.

---

# GUI TEAM CREATION

The `/team` GUI for players without a team previously used a chat-input session for team name/tag entry.

The user reported that typing the team name in chat did not register.

The current implementation intentionally uses the already-working `TeamStringInputGui` anvil text input for both:

```text
team name
team tag
```

This avoids depending on the fragile chat interception path for this feature while remaining server-side and client-compatible.

### Invalid-tag crash fix

The GUI previously had a re-entrant close/open recursion:

```text
NoTeamGui.open()
    ↓
TeamStringInputGui.onClosed()
    ↓
cancelled.run()
    ↓
NoTeamGui.open()
    ↓
closeHandledScreen()
    ↓
...
```

`TeamStringInputGui.onClosed()` now defers the cancellation callback through the server executor so the current screen-close operation completes before another GUI is opened.

The user runtime-tested the invalid 5-character tag scenario and confirmed it now cancels/retries safely without a server crash.

---

# BANK-WITHDRAW — VERIFIED 2.5.3 PARITY DECISION

The shipped 2.5.3 configuration contains:

```text
feature_costs.economy.bank_withdraw = 10.0
```

However, the actual verified 2.5.3 bank-withdraw path does **not** call:

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

Fabric must not invent a charge for it.

Current Fabric bank withdrawal is authorization/inventory mechanics only:

```text
BYPASS_BANK_WITHDRAW
       OR
member.canWithdraw()
```

Do **not** add `FeatureCostManager.charge(player, "bank-withdraw")` to the bank withdrawal predicate.

---

# TEAMWARP.COST — DELIBERATE FABRIC EXTENSION

2.5.3 uses a global:

```text
feature_costs.economy.warp = 75
```

Fabric already has persistent `TeamWarp.cost` and its GUI allows per-warp configuration.

Current decision: **keep `TeamWarp.cost`**. Do not replace persisted per-warp costs with the global 75-unit value unless the user explicitly chooses strict global-cost parity.

---

# RENAME COST — NOT A CURRENT IMPLEMENTATION TARGET

The reference has:

```text
rename = 500
```

Do not create a rename feature solely because this configuration entry exists.

Only investigate it when rename itself becomes the current feature being traced.

---

# PINNED API / SYNTAX VERIFICATION RULE

Whenever new code is about to be written:

```text
1. Read the current pinned Gradle/Minecraft/Yarn/Fabric settings.
2. Verify the exact API/signature/syntax on current web documentation for those versions.
3. Implement the smallest scoped change.
4. Run the user's canonical Gradle build locally before claiming compile success.
5. Treat the user's actual compiler/runtime result as authoritative over generic API documentation.
```

Do not claim that syntax was verified merely because a public Yarn page appears to contain a similarly named method. The exact project compile path matters.

---

# ROUND 10 / BUILD + RUNTIME PROTOCOL

The latest local clean build succeeded on 2026-08-24:

```text
./gradlew clean build --refresh-dependencies
BUILD SUCCESSFUL in 1m 46s
8 actionable tasks: 8 executed
```

The two Loom messages:

```text
Cannot remap modifiers because it does not exist in any of the targets [] or their parents.
```

appeared during configuration but did not prevent `build` from succeeding. They are not currently treated as build failures.

The current scoped runtime and compile verification are complete.

If a future build or runtime test exposes a failure, fix only the verified failing feature path and rerun the clean build.

Do not begin an unrelated repository-wide audit.

---

# HANDOFF / SEARCH PROTOCOL

When a repository-wide search is required:

1. Use the GitHub repository search when it is actually available and reliable.
2. If the connector cannot perform a reliable repository-wide search, do not claim repository-wide absence from a narrow result.
3. Use the user's supplied repository search results as authoritative evidence when they directly show the relevant call path.
4. For feature work, prefer exact source tracing over broad architectural redesign.

When a feature is being audited, the required round output is:

```text
exact verified 2.5.3 behavior
        ↓
current Fabric behavior
        ↓
missing Fabric pieces
        ↓
implementation decision
        ↓
local compile/runtime verification
```

Nothing outside that feature path should be investigated unless it blocks the current feature.

---

# HISTORICAL NOTE

Earlier completed rounds included the Fabric setup, permissions, team chat, glow, membership lifecycle, Ender Chest, teleport, and item-economy foundation. Their detailed history may exist in older project `.md` handoff/audit files, but this file is the current operational source of truth for continuation behavior, pinned toolchain/API verification, and current status.