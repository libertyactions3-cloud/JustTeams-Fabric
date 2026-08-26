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

A round counts only when a change is made somewhere under `src/` in `JustTeams-Fabric`.
Documentation, searching/auditing, planning, Gradle builds, and runtime testing do not consume rounds.

The user's canonical verification build is:

```powershell
./gradlew clean build --refresh-dependencies
```

---

# CURRENT RESUME POINT — GUI INTERACTION / PERSISTENT SCREEN PARITY

The previous core-parity cycle completed 10 source rounds and was followed by a successful local clean build.

The GUI presentation/lore cycle also completed 10 source rounds and was followed by a successful local clean build after compile corrections.

The GUI persistent-screen cycle also completed 10 source rounds. Its goal was to make inventory GUI navigation reuse the same 54-slot handler so menu items change in-place without resetting the player's mouse/cursor position.

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

## Current corrective GUI cycle

```text
Source-change rounds completed: 1 / 10
Round 1 — command parity + universal GUI mapping/refresh correction ✅
```

### Corrective Round 1 completed work

The viewer's own member head in `/team` is now non-interactive.

The persistent member-management view now uses the requested six-row positions:

```text
slot 4  = PLAYER_HEAD / player-info
slot 19 = dynamic PROMOTE TO CO-OWNER / DEMOTE TO MEMBER
slot 22 = RED_WOOL / KICK MEMBER
slot 25 = BEACON / TRANSFER OWNERSHIP
slot 37 = GOLD_INGOT / ʙᴀɴᴋ ᴡɪᴛʜᴅʀᴀᴡ
slot 39 = ENDER_CHEST / ᴜsᴇ ᴇɴᴅᴇʀ ᴄʜᴇsᴛ
slot 41 = GRASS_BLOCK / sᴇᴛ ᴛᴇᴀᴍ ʜᴏᴍᴇ
slot 43 = ENDER_PEARL / ᴜsᴇ ᴛᴇᴀᴍ ʜᴏᴍᴇ
slot 49 = ARROW / ʙᴀᴄᴋ
```

Ownership transfer confirmation is now rendered inside the same persistent 54-slot inventory rather than opening the legacy 27-slot confirmation GUI.

`/team requests` now enters the same in-place Join Requests view used by the `/team` menu rather than the legacy separate request screen.

`/team invite <player>` now uses Fabric's player argument type so online players can be tab-completed. The command gives the inviter a success message and the target a direct invitation message.

`/team accept <team>` now reports success to the joining player and notifies existing online team members.

The unset-home message now matches the verified 2.5.3 wording:

```text
[ᴛᴇᴀᴍꜱ] Your team does not have a home set. An Owner or Co-Owner can set one with /team sethome.
```

Post-kick member heads are rebuilt from current team state. The submenu snapshot is cleared on return so stale members are not restored by a later Back operation. The persistent main-menu title no longer embeds a stale member count.

`/team invites` remains accessible while a player is not in a team. This is intentional because pending invitations are specifically for teamless players. Its active path now uses the persistent 54-slot teamless inventory container.

## Slot-mapping rule — mandatory for every future GUI

Do **not** assume a Bukkit Inventory index equals a Fabric ScreenHandler slot ID.

For every GUI, distinguish:

```text
Bukkit Inventory index
Fabric backing Inventory index
Fabric ScreenHandler slot ID
```

Determine all three explicitly, then verify the x/y coordinates and the order of `ScreenHandler.addSlot(...)` calls.

For an active persistent six-row chest handler that adds the 54 menu slots first in row-major order:

```text
0  1  2  3  4  5  6  7  8
9 10 11 12 13 14 15 16 17
18 19 20 21 22 23 24 25 26
27 28 29 30 31 32 33 34 35
36 37 38 39 40 41 42 43 44
45 46 47 48 49 50 51 52 53
```

those menu backing indices do correspond directly to ScreenHandler slot IDs `0–53` because the handler constructor establishes that order. This direct mapping must **not** be generalized to legacy 27-slot handlers, whose player-inventory slots are added immediately after their 27 menu slots.

## Friendly-fire/PvP status

The Fabric friendly-fire disabler is already correctly implemented in `TeamFriendlyFire`: when the attacker and victim are team members, damage is allowed only when `team.isPvpEnabled()` is true. The GUI PvP toggle updates that same team state, so no new friendly-fire architecture is needed.

## Inventory GUI transition rule

Inventory-GUI → inventory-GUI transitions must reuse the same persistent 54-slot handler whenever the feature is participating in the persistent GUI system. A 27-slot reference layout is rendered inside the 54-slot handler rather than switching to a `GENERIC_9X3` handler, because 27- and 54-slot generic container handlers are distinct screen-handler types and replacing the handler resets the client-side screen state.

Vanilla anvil text input remains a separate screen because it is an `AnvilScreenHandler`, not an inventory/chest GUI. It is not treated as an inventory-to-inventory transition.

## Verification status

The corrective cycle is currently source-complete at **1 / 10**.

The next source rounds should continue auditing remaining inventory GUI transitions, especially the legacy Bank, Ender Chest, Home, and Confirmation surfaces, until they all conform to the same persistent-screen rule.

Do not claim compile/runtime verification until the user's actual local build and focused runtime tests succeed.

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
