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

A round counts **only** when a change is made somewhere under `src/` in `JustTeams-Fabric`.

The following do **not** consume rounds:

```text
web/API verification
repository searches
source auditing without edits
reading the 2.5.3 reference
planning/design
editing .md/status files
Gradle builds
runtime testing
user-reported observations
```

A round may include multiple source-file edits when those edits are one tightly scoped feature/correction. Do not artificially split one coherent fix into multiple rounds merely because it touches several source files.

### Exact 10-round procedure

For each cycle:

```text
Round 1
  ↓
trace the current feature path in Fabric + 2.5.3
  ↓
verify APIs/syntax on the web
  ↓
make the smallest source change
  ↓
commit source to GitHub main
  ↓
record the round

Round 2–9
  ↓
continue the same scoped feature family

Round 10
  ↓
STOP source changes
  ↓
run the canonical clean build
  ↓
runtime-test the affected feature paths
  ↓
start the next 10-round cycle only after verification/follow-up is understood
```

The final build is normally **Round 10's verification gate**, not an automatic build after every source edit. An earlier build is permitted if a compile blocker makes continued source work unsafe or misleading; a build itself never consumes a round.

### Canonical verification build

```powershell
./gradlew clean build --refresh-dependencies
```

These messages are known/non-fatal configuration notices when compilation otherwise succeeds:

```text
Cannot remap modifiers because it does not exist in any of the targets [] or their parents.
```

The actual Gradle task result is authoritative.

---

# LOCAL / GITHUB WORKFLOW

The user's computer is **not connected to GitHub** for this project.

The actual workflow is:

```text
GitHub canonical repository
        ↓
download current src/
        ↓
replace local project's src/
        ↓
run Gradle locally
        ↓
report the real build/runtime result back here
```

Therefore:

- GitHub commits do not automatically appear on the user's computer.
- Never assume the local source has the newest GitHub source until the user downloads/replaces `src/`.
- Never tell the user that a GitHub commit changed their local files.
- When source is committed, tell the user to download the current `src/` before building.
- GitHub `main` is the canonical source repository.

---

# PAPER → FABRIC GUI SLOT-MAPPING RULE — MANDATORY

**Never assume that a Paper/Bukkit inventory slot number automatically equals a Fabric ScreenHandler slot ID.**

For every GUI, explicitly determine:

```text
1. Bukkit/Paper Inventory index
2. Fabric backing Inventory index / Slot.index
3. Fabric ScreenHandler slot ID
```

Then verify:

```text
Bukkit 2.5.3 index
        ↓
Fabric backing Inventory index
        ↓
order of ScreenHandler.addSlot(...)
        ↓
resulting ScreenHandler slot ID
        ↓
Slot x/y coordinates
        ↓
actual visual row/column
```

Only map numbers directly when the Fabric implementation demonstrably preserves the same ordering.

### Persistent six-row chest mapping

For the active persistent 54-slot team handler, the menu inventory is constructed row-major as:

```text
0  1  2  3  4  5  6  7  8
9 10 11 12 13 14 15 16 17
18 19 20 21 22 23 24 25 26
27 28 29 30 31 32 33 34 35
36 37 38 39 40 41 42 43 44
45 46 47 48 49 50 51 52 53
```

The 54 menu slots are added before the player's inventory slots, so those menu indices correspond directly to ScreenHandler IDs `0–53` **in that specific handler**.

Do not generalize that result to legacy 27-slot handlers. Legacy handlers can add 27 menu slots and then player inventory/hotbar slots, producing different ScreenHandler IDs.

Also inspect the x/y values used when constructing each `Slot`; the same numeric index is not enough to prove the same visual position.

Web/API documentation used for this rule includes Yarn's `ScreenHandler.addSlot(...)`, `getSlotIndex(...)`, and `Slot` documentation. `Slot` is backed by an `Inventory`, while `ScreenHandler.addSlot(...)` determines the handler's slot ordering. citeturn251307search0turn251307search3

---

# UNIVERSAL INVENTORY-GUI PERSISTENCE RULE

For this project, **inventory GUI → inventory GUI transitions must reuse the same open ScreenHandler whenever the feature is part of the persistent GUI system**.

Desired behavior:

```text
same open 54-slot handler
        ↓
replace item contents/state
        ↓
mouse/cursor position remains unchanged
```

Do not switch between `GENERIC_9X3` and `GENERIC_9X6` solely to reproduce a reference row count. A 27-slot reference layout should be rendered inside the persistent 54-slot container when mouse persistence is required.

Exception:

```text
TeamStringInputGui / Anvil input
```

This is a distinct text-input screen, not an inventory/chest GUI. Do not force it into the persistent chest handler merely to avoid a legitimate input-screen transition.

---

# CURRENT RESUME POINT — GUI INTERACTION / PERSISTENT SCREEN PARITY

Previous completed cycles:

```text
Core parity cycle               10 / 10 ✅
GUI presentation/lore cycle     10 / 10 ✅
GUI persistent-screen cycle    10 / 10 ✅
```

The persistent-screen cycle goal was to make `/team` navigation reuse one 54-slot `TeamMenuHandler` so changing menus does not reset the mouse/cursor.

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

---

# CURRENT CORRECTIVE GUI CYCLE

```text
Source-change rounds completed: 1 / 10
Round 1 — command parity + universal GUI mapping/refresh correction ✅
```

Round 1 includes:

```text
viewer own member head is non-interactive
member editor uses verified persistent 54-slot positions
member editor uses requested control arrangement
ownership-transfer confirmation stays in the persistent handler
post-kick member state refreshes correctly
unset home message matches 2.5.3 wording
/team requests uses the same persistent Join Requests view as /team
/team invite uses online-player autocomplete
/team invite notifies inviter + invitee + existing online members
/team accept notifies joiner + existing online members
```

Current source is committed to GitHub `main` but **not yet verified by the user's local build/runtime**.

Next source work continues toward Round 10 of this corrective cycle, focusing only on remaining inventory-GUI persistence/mapping issues and the specific runtime discrepancies the user reports.

---

# CURRENT INVITE / ACCEPT BEHAVIOR

`/team invite <player>` uses `EntityArgumentType.player()` so online players can be tab-completed. Yarn exposes `EntityArgumentType.player()` and `getPlayer(...)` for the online-player argument path, while the command source also exposes player-name suggestions. citeturn251307search2turn251307search4

On successful invite:

```text
inviter:
    <player> has been invited to join <team>.

invitee:
    You have been invited to join <team>. Use /team accept <team>.

other online team members:
    <inviter> has invited <player> to join the team.
```

On successful accept:

```text
joiner:
    You have joined the team <team>.

other online team members:
    <player> has joined the team.
```

Do not call these runtime-verified until the user tests them.

---

# FRIENDLY FIRE / PVP RULE

The current `TeamFriendlyFire` implementation is already the intended mechanism:

```text
same team + PVP disabled → same-team damage blocked
same team + PVP enabled  → same-team damage allowed
```

The GUI PVP toggle updates the same `team.isPvpEnabled()` state. Do not redesign this unless a runtime test demonstrates a real defect.

---

# MAIN `/team` MEMBER-HEAD RULES

Main menu member heads occupy:

```text
9–44
```

The team leader/owner comes first, followed by the other members according to the established team sorting behavior.

The viewer's own member head is **not clickable**.

When another player's head is clicked, the persistent member editor must use:

```text
slot 4:
    PLAYER_HEAD
    player-info-head
    name = <player_name>
    lore =
      Role: <role>
      Joined: <joindate>

slot 19:
    dynamic promote/demote
    PROMOTE:
      LIME_DYE
      PROMOTE TO CO-OWNER
    DEMOTE:
      GRAY_DYE
      DEMOTE TO MEMBER

slot 22:
    RED_WOOL
    KICK MEMBER

slot 25:
    BEACON
    TRANSFER OWNERSHIP

slot 37:
    GOLD_INGOT
    ʙᴀɴᴋ ᴡɪᴛʜᴅʀᴀᴡ

slot 39:
    ENDER_CHEST
    ᴜsᴇ ᴇɴᴅᴇʀ ᴄʜᴇsᴛ

slot 41:
    GRASS_BLOCK
    sᴇᴛ ᴛᴇᴀᴍ ʜᴏᴍᴇ

slot 43:
    ENDER_PEARL
    ᴜsᴇ ᴛᴇᴀᴍ ʜᴏᴍᴇ

slot 49:
    ARROW
    ʙᴀᴄᴋ
```

The visual names/lore should continue to be derived from the verified 2.5.3 reference rather than invented.

---

# MAIN `/team` MENU LAYOUT

```text
slot 7  = TEAM WARPS
slot 8  = JOIN REQUESTS

member heads = slots 9–44, leader first

slot 45 = PVP
slot 46 = ENDER CHEST
slot 47 = HOME
slot 48 = blank
slot 49 = SORT
slot 50 = BANK
slot 51 = blank
slot 52 = SETTINGS
slot 53 = LEAVE / DISBAND
```

Top and bottom border glass should appear only where the reference/persistent layout requires it. Do not fill the content rows with decorative glass merely because a slot is unused.

---

# HOME / WARP CURRENCY TIMING

Teleportation costs are taken **only after successful teleport completion**, with the success behavior occurring before the currency is removed.

Do not charge when a teleport fails because of:

```text
permission
password
cooldown
missing destination
other failed validation
```

---

# TEAM ENDER CHEST

`/team enderchest` and `/team ec` are the same feature path.

The team Ender Chest storage is persistent and must not lose contents when the GUI closes and reopens.

Do not replace real Ender Chest storage with decorative menu items merely to satisfy the persistent-screen rule.

---

# TEAM BANK / ITEM ECONOMY

The Fabric port intentionally uses the established item economy instead of Paper/Vault numeric economy architecture.

```text
Emerald               = 1
Emerald Block         = 9
Deepslate Emerald Ore = 81
```

Keep these separate:

```text
TeamBank
  = team-owned currency-item inventory

ItemEconomyProvider
  = player-owned currency balance / withdraw / deposit abstraction
```

Configured feature costs:

```text
sethome        100
home            50
enderchest      25
setwarp        200
warp             75
bank-withdraw    10
rename          500
```

`bank-withdraw` is intentionally not charged through the generic feature-cost path because that is not the verified 2.5.3 behavior.

---

# VERIFIED RUNTIME FEATURES

The user has already confirmed successful runtime behavior for major paths including:

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

A future source edit that touches a verified path requires focused retesting before calling it verified again.

---

# HOW TO AUDIT / PORT 2.5.3 BEHAVIOR

Never translate the reference Java code mechanically. Trace the actual in-game behavior:

```text
2.5.3 command registration
        ↓
command handler
        ↓
storage/model mutation
        ↓
listeners/events
        ↓
messages/sounds/economy effects
        ↓
GUI class + gui.yml
        ↓
Fabric equivalent
```

For GUIs:

```text
gui.yml
  ↓
material / slot / name / lore / placeholders
  ↓
reference GUI Java class
  ↓
click listener/action
  ↓
Fabric backing Inventory
  ↓
Fabric ScreenHandler slot order
  ↓
x/y coordinates
```

A permission, setter, field, or model property is not proof that the feature actually works. Find its runtime caller/event path.

---

# WHAT NOT TO DO

Do not:

```text
rewrite unrelated architecture
replace the item economy with Vault
replace local NBT storage with Paper database architecture
redesign working bank/enderchest storage without a current feature need
assume Bukkit slot == Fabric ScreenHandler slot
assume compile success == behavioral parity
perform a whole-project audit when the requested feature is narrower
count docs/search/build/runtime activity as rounds
```

Record unrelated discoveries as a brief `Later` note and return to the current feature.

---

# LATER — NON-BLOCKING DIFFERENCES

Only investigate these if explicitly scoped or if they directly block the current feature:

```text
Discord/webhook integration
PlaceholderAPI
Redis/cross-server synchronization
database/migration/recovery architecture
Bedrock/platform integration hooks
custom team-data API
Paper/Vault architecture differences
```

These are not prerequisites for the core single-server Fabric feature set unless explicitly required.

---

# PINNED API / SYNTAX VERIFICATION RULE

Whenever new Java/Fabric code is written:

```text
1. Read the pinned Gradle/Minecraft/Yarn/Fabric settings.
2. Verify the exact API/signature/syntax on current web documentation.
3. For every GUI, verify Bukkit index → backing Inventory index → ScreenHandler ID → x/y coordinates.
4. Implement the smallest scoped change.
5. Commit actual source changes to GitHub main.
6. Update SOURCE_CHANGE_ROUND_STATUS.md when a round changes.
7. Update PROJECT_STATUS.md when the resume point/rules/verification status changes.
8. Do not claim compile success before the user's local build confirms it.
9. Do not claim behavioral verification before the user's runtime test confirms it.
```

---

# FUTURE-CHAT REMINDER PROMPT

Paste this into a new chat when continuing the project:

```text
Continue the JustTeams-Fabric port from PROJECT_STATUS.md and SOURCE_CHANGE_ROUND_STATUS.md.

Canonical repository: libertyactions3-cloud/JustTeams-Fabric, main.
Do not use the obsolete test repository.
My local project is NOT connected to GitHub. I manually download/replace src/ from GitHub before running Gradle.

Treat PROJECT_STATUS.md as the project operating manual.

Always:
- Continue on every message; “Continue” is only a reminder.
- Stay scoped to the current feature. Do not redesign unrelated architecture.
- Compare Fabric against verified JustTeams 2.5.3 behavior before changing code.
- Search/index both repositories deeply enough to trace the actual feature path.
- Verify every new Java/Fabric syntax/API on the web against:
  Minecraft 1.21.11
  Yarn 1.21.11+build.4
  Loader 0.18.4
  Fabric API 0.141.4+1.21.11
  Loom 1.15-SNAPSHOT
  Java 21
- Never assume a Bukkit/Paper inventory slot number equals a Fabric ScreenHandler slot ID.
- For every GUI explicitly verify Bukkit index, Fabric backing Inventory index, ScreenHandler slot ID, addSlot order, and x/y coordinates.
- Inventory GUI → inventory GUI transitions should reuse the same persistent 54-slot handler so the mouse/cursor position never resets.
- A 27-slot reference GUI should be rendered inside the persistent 54-slot handler when it is an inventory-to-inventory transition.
- Count a repository round only when src/ changes.
- Searches, audits, docs, planning, builds, and runtime tests do not consume rounds.
- Use 10 source rounds per cycle.
- Normally wait until Round 10 for the canonical build gate.
- Canonical build: ./gradlew clean build --refresh-dependencies
- The user’s actual Gradle output is the final compile authority.
- The user’s actual runtime observations are the final behavioral authority.
- Commit source changes to GitHub main; the user then downloads the current src/ locally.
- Keep PROJECT_STATUS.md and SOURCE_CHANGE_ROUND_STATUS.md synchronized with the current round/resume point.
- Do not claim something is verified just because code looks correct or compiles.

Current formula:
exact 2.5.3 behavior → smallest Fabric implementation → local build → focused runtime test → continue.
```
