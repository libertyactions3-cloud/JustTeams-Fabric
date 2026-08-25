# CONTINUATION / HANDOFF PROTOCOL — READ THIS FIRST

This file is the persistent handoff for continuing the JustTeams-Fabric port. Read it before auditing, editing, or claiming progress.

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

## Continue on every message

Treat every user message as permission to continue the project. `Continue` is only a reminder to continue.

## Stay scoped to the current feature

```text
exact verified 2.5.3 behavior
        ↓
identify Fabric pieces actually missing
        ↓
implement only those pieces
```

Do not redesign unrelated architecture. Record unrelated issues only as Later.

## Repository activity / 10-round workflow

A round counts only when something under `src/` changes. Documentation, searching, auditing, builds, and runtime tests do not consume rounds.

The user's canonical verification build is:

```powershell
./gradlew clean build --refresh-dependencies
```

---

# CURRENT RESUME POINT — GUI INTERACTION / PERSISTENT SCREEN PARITY

The previous core-parity cycle completed 10 source rounds and was followed by a successful local clean build.

The GUI presentation/lore cycle also completed 10 source rounds and was followed by a successful local clean build after compile corrections.

The GUI interaction / persistent-screen cycle also completed 10 source rounds. Its goal was to make `/team` navigation reuse the same 54-slot handler so menu items change in-place without resetting the player's mouse/cursor position.

```text
Round 1  Join Requests + Warps in-place foundation       ✅
Round 2  Settings + dynamic Sort/PvP                    ✅
Round 3  Member Management in-place                     ✅
Round 4  Main Home direct action                         ✅
Round 5  Persistent /team handler reuse                 ✅
Round 6  Persistent /team settings command entry        ✅
Round 7  Persistent warp management                     ✅
Round 8  Persistent blacklist management                ✅
Round 9  Persistent navigation cleanup                  ✅
Round 10 Persistent leaderboard command/view            ✅
```

### Persistent GUI behavior now implemented

`TeamGuiManager.openMain()` reuses an already-open `TeamMenuHandler` instead of opening a new handled screen when the player is already viewing the team menu.

The persistent in-place renderer covers:

```text
Join Requests
Team Warps
Team Settings
Member Management
Warp Management
Blacklist Management
Leaderboards
Sort state
PvP state
```

Back actions restore the existing `/team` inventory rather than opening another chest screen wherever the persistent view is applicable.

### Command entry behavior

`/team settings` enters the persistent 54-slot team handler for team members.

`/team top` enters the persistent leaderboard category/ranked views for team members. Players who are not in a team retain the standalone leaderboard GUI.

`/team blacklist` enters the persistent team handler for team members.

`/team invites` remains accessible while a player is not in a team. This is intentional: pending invitations are specifically for players who are not already in a team.

### Intentional storage boundaries

`TeamBankGui` remains an item-backed team-owned currency inventory. The 2.5.3 BankGUI is a numeric Vault-money menu, so that presentation was not transplanted onto the Fabric bank.

`Team Ender Chest` remains a real persistent item inventory. Its underlying slots and persistence must be preserved if it is later made persistent in the same screen; it is not treated as a decorative submenu.

`TeamWarpManagementGui` is a Fabric-specific management surface with no standalone 2.5.3 GUI counterpart, but its existing controls are now rendered in-place when entered from the persistent team GUI.

## Current corrective GUI cycle

After the completed 10-round persistent-screen cycle, one additional scoped source round was required for runtime-discovered member-head behavior:

```text
Round 1 / 10 — self-head interaction + member-editor layout correction ✅
```

The viewer's own member head is now non-interactive. The persistent member editor preserves the original 27-slot relative arrangement but centers that three-row arrangement inside the persistent six-row container.

## Verification status

The current corrective GUI cycle is source-complete for Round 1 only.

The next step is the user's local clean build when we reach the appropriate verification gate for this corrective cycle. Do not claim compile/runtime verification until the user's actual build and focused runtime tests succeed.

---

# CORE PARITY CYCLE — COMPLETED

The preceding 10-round source cycle completed:

```text
/team info
team creation defaults + validation
protected warp password prompt
disband inventory cleanup
lifecycle success sounds
/teammsg
chat-spy
team invite-list GUI
blacklist/unblacklist
/team settings + /guild /clan /party aliases
```

The user then ran the canonical clean build successfully.

---

# GUI PRESENTATION / LORE CYCLE — COMPLETED

The preceding GUI presentation cycle completed 10 source rounds covering:

```text
main /team menu
Join Requests
Team Warps
Team Settings
Leaderboard Category/View
Member Management
Blacklist
Pending Invites
No-Team menu
Confirmation GUI
```

Names/lore were translated from the supplied 2.5.3 `gui.yml`, with explicit non-italic formatting for custom item text unless the reference required otherwise.

---

# ITEM ECONOMY / FEATURE COSTS

The current workstream uses an internal item economy.

Currency denominations:

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

Configured feature costs remain:

```text
sethome        100
home            50
enderchest      25
setwarp        200
warp             75
bank-withdraw    10
rename          500
```

The `bank-withdraw` entry is intentionally not charged because the verified 2.5.3 bank withdrawal path does not call its generic feature-cost mechanism.

---

# VERIFIED RUNTIME FEATURES

The user has confirmed successful runtime behavior for the major current feature paths, including:

```text
/team home set
/team home
/team warp
/team warp set <name> [password]
Warp GUI password creation/use
/team ec
/team enderchest
team creation GUI
invalid-tag retry
payment timing after successful teleport
/team info
/team top
/team top kills
/team top balance
/team top members
```

---

# PINNED API / SYNTAX VERIFICATION RULE

Whenever new Java/Fabric code is written:

```text
1. Read the pinned Gradle/Minecraft/Yarn/Fabric settings.
2. Verify the exact API/signature/syntax on current web documentation.
3. Implement the smallest scoped change.
4. Let the user's local Gradle build be the final compile authority.
```

Do not claim compile success before the user's actual build confirms it.
