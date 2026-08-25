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

# CURRENT RESUME POINT — MAIN `/team` GUI PRESENTATION PARITY

The previous core-parity source cycle is complete: all 10 rounds were implemented, followed by the user's local clean build.

Latest verified local build:

```text
./gradlew clean build --refresh-dependencies
BUILD SUCCESSFUL in 2m
8 actionable tasks: 8 executed
```

The two recurring Loom messages:

```text
Cannot remap modifiers because it does not exist in any of the targets [] or their parents.
```

appear during configuration but did not prevent the build from succeeding. They are not currently treated as build failures.

The current workstream is now the **main `/team` inventory GUI presentation/lore parity pass** based directly on the verified 2.5.3 `gui.yml`.

Current GUI presentation source change:

```text
TeamMenuHandler.java
commit: 4c46cc5ab2b2fabd2c83861e40d6aa171a8e818d
```

It adds/aligns:

```text
slot 7  Team Warps lore
slot 8  Join Requests lore + owner/co-owner locked state
slot 45 PvP status lore
slot 46 Team Ender Chest lore + locked state
slot 47 Team Home set/not-set lore
slot 49 Sort Members dynamic status lore
slot 50 Team Bank dynamic balance lore + disabled/permission state
slot 52 Team Settings lore + owner/co-owner locked state
slot 53 Leave/Disband lore
```

Member heads already have role/joined/server lore, and the established layout remains:

```text
0–8    glass/top border, with Warps at 7 and Join Requests at 8
9–44   member heads, leader first
45     PvP
46     Ender Chest
47     Home
48     blank
49     Sort
50     Bank
51     blank
52     Settings
53     Leave/Disband
```

Custom item names/lore explicitly disable italics, matching the intended 2.5.3 presentation.

The current GUI click/action routing in `TeamGuiManager` was intentionally not redesigned during this presentation pass.

## GUI presentation round counter

```text
GUI presentation rounds completed: 1 / 10
Current round: Round 1 — main /team menu lore/presentation (source-complete)
Next round: Round 2 — Join Requests GUI presentation
```

The GUI presentation cycle is separate from the completed core-parity cycle. Its rounds still count only when `src/` changes.

---

# CORE PARITY CYCLE — COMPLETED

The preceding 10-round source cycle completed:

```text
1  /team info parity
2  team creation defaults + validation
3  protected warp password prompt
4  disband lifecycle inventory cleanup
5  lifecycle notification success sounds
6  /teammsg
7  chat-spy
8  /team invites GUI
9  blacklist/unblacklist
10 /team settings + /guild /clan /party aliases
```

The user's local final build passed after the source corrections required for 1.21.11/Yarn compatibility.

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

---

# VERIFIED PAID-FEATURE INTEGRATIONS

The user has runtime-tested the major item-economy/teleport paths and confirmed:

```text
/team home set
/team home
/team warp
/team warp set <name> [password]
Warp GUI password creation/use
/team ec
/team enderchest
```

The user also confirmed that the corrected payment timing only charges after successful teleportation.

---

# GUI TEAM CREATION

The `/team` no-team GUI uses `TeamStringInputGui` for the team name and tag inputs.

The user runtime-tested the previous invalid-tag close/open recursion fix and confirmed that cancellation/retry no longer crashes the server.

---

# BANK-WITHDRAW — VERIFIED 2.5.3 PARITY DECISION

The shipped 2.5.3 configuration contains:

```text
feature_costs.economy.bank_withdraw = 10.0
```

However, the actual bank-withdraw operation does not call `canAffordAndPay(player, "bank_withdraw")`.

Fabric must not invent a feature charge for bank withdrawal.

---

# TEAMWARP.COST — DELIBERATE FABRIC EXTENSION

Fabric intentionally keeps persistent per-warp `TeamWarp.cost` rather than replacing it with the global 2.5.3 numeric warp cost.

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

---

# HANDOFF / SEARCH PROTOCOL

When a repository-wide search is required:

1. Use the GitHub repository search when it is actually available and reliable.
2. If the connector cannot perform a reliable repository-wide search, do not claim repository-wide absence from a narrow result.
3. Use directly supplied repository search results as authoritative evidence when they show the relevant call path.
4. Prefer exact source tracing over broad architectural redesign.

Feature work should always follow:

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

Nothing outside the current feature should be investigated unless it blocks that feature.

---

# HISTORICAL NOTE

Earlier completed work includes the Fabric setup, permissions, team chat, glow, membership lifecycle, Ender Chest, teleport, item economy, stats, leaderboards, ownership transfer, and core command/GUI parity. Older `.md` audit files may contain the detailed history, but this file is the current operational source of truth for continuation behavior, pinned toolchain/API verification, and current status.
