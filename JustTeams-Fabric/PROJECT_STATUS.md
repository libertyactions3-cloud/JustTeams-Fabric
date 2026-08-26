# CONTINUATION / HANDOFF PROTOCOL — READ THIS FIRST

This file is the persistent handoff and operating manual for continuing the JustTeams-Fabric port. Read this before auditing, editing, building, or claiming progress.

## Canonical project

Repository: `libertyactions3-cloud/JustTeams-Fabric`
Branch: `main`
Project directory: `JustTeams-Fabric/`

The user's local computer is **not connected to GitHub**. The user manually downloads the current `src/` from GitHub and replaces the local project's `src/` before running Gradle.

Do not assume local files are synchronized with GitHub. GitHub `main` is the canonical source.

Behavioral reference:
- supplied JustTeams 2.5.3 source tree
- supplied 2.5.3 `gui.yml`

The 2.5.3 source tree is the behavior reference, but Paper/Bukkit architecture is not to be copied literally when Fabric-native behavior is required.

---

# PINNED BUILD / API ENVIRONMENT

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
Observed local resolved Loom: 1.15.5
```

Before writing/modifying Java/Fabric code:

```text
1. Read these pinned settings.
2. Verify the relevant syntax/parsing/mapping/API signature on the web.
3. Implement the smallest scoped change.
4. Commit the source change to GitHub main.
5. The user's local Gradle result is the final compile authority.
```

Never claim compile success before the user's actual build confirms it.

---

# CONTINUE / SCOPE RULES

Treat every user message as permission to continue the project. `Continue` is only a reminder to continue.

Stay scoped to the current feature:

```text
verified 2.5.3 behavior
        ↓
identify missing Fabric pieces
        ↓
implement only those pieces
```

Do not redesign unrelated architecture merely because an improvement is visible. Put unrelated discoveries under `Later` and do not investigate them unless they block the current feature.

Prefer the smallest implementation that reproduces verified 2.5.3 behavior.

---

# 10-ROUND REPOSITORY-ACTIVITY SYSTEM

A round counts **only when something under `src/` changes** in JustTeams-Fabric.

These do NOT consume rounds:

```text
web/API searches
source auditing without edits
reading the 2.5.3 reference
planning/design discussion
documentation-only changes
PROJECT_STATUS.md changes
SOURCE_CHANGE_ROUND_STATUS.md changes
Gradle builds
runtime testing
user observations/results
```

A round may contain several source-file edits when they are one coherent feature/correction. Do not artificially split one tightly scoped feature into separate rounds because several files were touched.

### Exact cycle procedure

```text
Round 1
  ↓
trace the feature in Fabric + 2.5.3
  ↓
verify APIs/syntax on web
  ↓
make smallest source change
  ↓
commit source to GitHub main
  ↓
record round

Rounds 2–9
  ↓
continue the same scoped feature family

Round 10
  ↓
STOP source changes
  ↓
user downloads current src/
  ↓
user runs clean build
  ↓
runtime-test affected feature
  ↓
start next cycle after verification is understood
```

Canonical build:

```powershell
./gradlew clean build --refresh-dependencies
```

This build is normally the Round 10 verification gate. An earlier build is allowed if a compile blocker makes further source work unsafe or misleading; a build itself never consumes a round.

Known non-fatal configure notice when compilation otherwise succeeds:

```text
Cannot remap modifiers because it does not exist in any of the targets [] or their parents.
```

The actual Gradle task result is authoritative.

---

# LOCAL ↔ GITHUB WORKFLOW

The workflow is:

```text
GitHub canonical main
        ↓
download current src/
        ↓
replace local src/
        ↓
run local Gradle
        ↓
report real build/runtime results
```

Therefore:

- GitHub commits do not automatically reach the user's computer.
- Never claim a GitHub commit changed the user's local files.
- After source commits, tell the user to download/replace `src/` before building.
- Do not tell the user to run `git pull` unless the user explicitly changes their workflow.

---

# PAPER → FABRIC SLOT-MAPPING RULE — MANDATORY

Never assume a Paper/Bukkit inventory slot number equals a Fabric ScreenHandler slot ID.

For every GUI, explicitly determine:

```text
1. Bukkit/Paper Inventory index
2. Fabric backing Inventory index / Slot.index
3. order of ScreenHandler.addSlot(...)
4. resulting Fabric ScreenHandler slot ID
5. Slot x/y coordinates and resulting visual row/column
```

Always distinguish:

```text
Bukkit Inventory index
Fabric backing Inventory / Slot.index
Fabric ScreenHandler slot ID
```

A six-row persistent chest handler that adds all 54 menu slots first in row-major order has:

```text
0  1  2  3  4  5  6  7  8
9 10 11 12 13 14 15 16 17
18 19 20 21 22 23 24 25 26
27 28 29 30 31 32 33 34 35
36 37 38 39 40 41 42 43 44
45 46 47 48 49 50 51 52 53
```

In that specific construction, menu backing indices `0–53` equal ScreenHandler IDs `0–53` because those slots are added first. Do not generalize this to legacy 27-slot handlers.

Always inspect the actual `addSlot(...)` order and x/y.

---

# UNIVERSAL PERSISTENT INVENTORY GUI RULE

Inventory-GUI → inventory-GUI transitions should reuse the same open `ScreenHandler` whenever the feature is participating in the persistent GUI system.

Desired behavior:

```text
same 54-slot handler
        ↓
replace item contents/state
        ↓
mouse/cursor position remains unchanged
```

Do not switch from `GENERIC_9X6` to `GENERIC_9X3` solely to reproduce a 27-slot reference GUI. Render the reference layout inside the persistent 54-slot container when mouse persistence is required.

Exception:

```text
TeamStringInputGui / Anvil input
```

It is an AnvilScreenHandler rather than an inventory/chest GUI and may be a separate input screen.

---

# COMPLETED CYCLES

```text
Core parity cycle                  10 / 10 ✅
GUI presentation/lore cycle        10 / 10 ✅
GUI persistent-screen cycle        10 / 10 ✅
```

The user successfully ran clean builds for the first two completed cycles. The persistent-screen cycle was completed at source level before the current corrective work.

---

# CURRENT CORRECTIVE CYCLE

Current source-change count:

```text
6 / 10
```

### Round 1 — self-head / member editor / command parity
- own member head is non-interactive
- member editor moved into persistent 54-slot layout
- ownership-transfer confirmation kept in persistent handler
- `/team requests` uses persistent Join Requests view
- invite/accept notifications implemented
- unset-home 2.5.3 message implemented
- post-kick member refresh fixed

### Round 2 — persisted rank and permission foundation
Added seven-rank ladder:

```text
Leader
Co-Leader
Officer
Underofficer
Associate
Member
Initiate
```

`TeamRank` supports one-step `promote()` and `demote()`.

Existing legacy `TeamRole` remains for ownership compatibility:

```text
OWNER
CO_OWNER
MEMBER
```

`TeamRank` is the new player-facing ladder. `Leader` maps to legacy Owner, `Co-Leader` maps to legacy Co-Owner, and lower ranks map to legacy Member.

Added persisted member toggles:

```text
canInvite
canSetWarps
canUseAutoBank
```

### Round 3 — AutoBank economy foundation
`TeamBank` now supports exact denomination checks/removal for:

```text
Deepslate Emerald Ore = 81
Emerald Block         = 9
Emerald               = 1
```

`FeatureCostManager` can use the team bank instead of player inventory when the member's `canUseAutoBank` toggle is enabled.

`/team autobank` toggles the member setting and persists through `teams.dat`.

### Round 4 — command permission/rank wiring
- `/team promote <player>` advances the seven-rank ladder.
- `/team demote <player>` moves down the seven-rank ladder.
- `/team invite <player>` uses plain online-name completion and independent invite permission.
- Warp creation uses independent `canSetWarps` permission.
- Owner or Co-Leader can remove any team warp.
- Existing teammates can use team warps by default through the normal enabled/member-use default.

### Round 5 — persistent GUI slot/layout/refresh
Main `/team` member heads are mapped to the verified layout:

```text
19–25
28–34
37–43
```

Join Requests player heads fill all interior rows:

```text
9–44
```

This is directly below the top glass row and directly above the bottom glass row in the persistent 54-slot container.

When a join request is accepted inside Join Requests:

```text
stay in Join Requests
        ↓
update saved main-team member snapshot
        ↓
Back later shows the new teammate head
```

The Join Requests playerhead lore contains no `Dynamic` line.

### Round 6 — player command arguments and base warp enforcement
All currently identified player-name command arguments use the plain online-name suggestion provider rather than `@` selector syntax.

Covered:

```text
/team invite
/team promote
/team demote
/team kick
/team transfer
/team blacklist
/team unblacklist
```

The base `/team warp set` and `/team warp remove` nodes are overridden so the independent warp-creation permission and Co-Leader removal rule apply to both passwordless and passworded warp creation paths.

---

# CURRENT MEMBER-MANAGEMENT GUI LAYOUT

The persistent 54-slot member editor currently uses:

```text
slot 4  = PLAYER_HEAD / player information

slot 19 = LIME_DYE / one rank higher
slot 20 = RED_DYE / one rank lower

slot 22 = RED_WOOL / KICK MEMBER
slot 25 = BEACON / TRANSFER OWNERSHIP

slot 37 = GOLD_INGOT / ʙᴀɴᴋ ᴡɪᴛʜᴅʀᴀᴡ
slot 38 = COMPASS / create team warps toggle
slot 39 = ENDER_CHEST / use team ender chest toggle
slot 40 = EMERALD_BLOCK / team AutoBank toggle
slot 41 = GRASS_BLOCK / set team home toggle
slot 42 = ALLAY_SPAWN_EGG / invite players toggle
slot 43 = ENDER_PEARL / use team home toggle

slot 49 = ARROW / BACK
```

The seven-rank ladder is reflected in the rank display and promote/demote labels.

Independent permission semantics:

```text
Invite permission does not require a particular rank.
Warp-creation permission does not require a particular rank.
AutoBank permission does not require a particular rank.
```

The default invite capability for old/new members is enabled for Leader, Co-Leader, Officer, and Underofficer, but the GUI toggle is authoritative and can explicitly disable or enable it.

All members may use team warps by default when the warp itself is enabled and has default member access.

---

# INVITE / ACCEPT NOTIFICATIONS

Successful `/team invite <player>` sends:

```text
inviter:
<player> has been invited to join <team>.

invitee:
You have been invited to join <team>. Use /team accept <team>.

other online team members:
<inviter> has invited <player> to join the team.
```

Successful `/team accept <team>` sends:

```text
joiner:
You have joined the team <team>.

other online team members:
<player> has joined the team.
```

Runtime behavior is not considered verified until the user tests it.

---

# HOME BEHAVIOR

When the team has no home, the verified 2.5.3 chat message is:

```text
[ᴛᴇᴀᴍꜱ] Your team does not have a home set. An Owner or Co-Owner can set one with /team sethome.
```

That message is used by the command path and active persistent main-GUI path.

The exact MiniMessage color tag for the **Home GUI lore** line `Home not set.` has not yet been conclusively recovered from the supplied 2.5.3 source. Do not invent or claim an exact reference color until it is verified.

---

# FRIENDLY FIRE / PVP

`TeamFriendlyFire` is already correctly wired to the team `pvpEnabled` state:

```text
same team + PvP disabled → team-member damage blocked
same team + PvP enabled  → team-member damage allowed
```

Do not redesign this without a runtime defect.

A public v2.5.2 release note also confirms a **PvP toggle cooldown** exists in the Paper plugin with a configurable default of 300 seconds. This is distinct from the question of whether member-management promote/demote/permission buttons themselves have cooldowns. citeturn585163search0

The separate member-button cooldown behavior has not yet been conclusively verified from 2.5.3 source. Do not invent a timer/duration. Trace it in the reference before implementation.

---

# MAIN `/team` MENU LAYOUT

```text
slot 7  = TEAM WARPS
slot 8  = JOIN REQUESTS

member heads:
19–25
28–34
37–43

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

Top and bottom glass rows are used for persistent inventory presentation; unused content-row slots should remain appropriate open space rather than being filled with decorative panes unless the reference specifies them.

---

# JOIN REQUESTS MENU

Persistent request heads fill:

```text
9–44
```

No `Dynamic` lore line should be present.

Accepting a request while inside this menu must:

```text
add the new teammate
save team data
refresh the saved main `/team` teammate-head list
remain inside Join Requests
```

Do not send the accepting administrator back to `/team` merely because the request was accepted.

---

# WARP RULES

Default team warp behavior:

```text
all teammates can use /team warp
individual warp creation permission is separate
Owner can remove any team warp
Co-Leader can remove any team warp
```

Warp creation checks the member's independent `canSetWarps` toggle.

Passworded and passwordless creation must use the same permission rule.

---

# AUTOBANK RULE

`/team autobank` is a persisted per-member toggle.

When enabled, supported `/team` feature costs use the team's currency-item bank instead of relying on the player's inventory balance.

Current exact denominations:

```text
81 = deepslate emerald ore
9  = emerald block
1  = emerald
```

The AutoBank path currently requires an exact denomination combination; it does not silently take a different amount than the configured feature cost.

If AutoBank is disabled, the existing player-inventory item economy path is used.

---

# ENDER CHEST / BANK BOUNDARY

Do not fake real storage as decorative menu items.

```text
Team Ender Chest
    = actual shared persistent inventory

Team Bank
    = actual team-owned currency-item inventory
```

The GUI persistence rule must not corrupt their real storage semantics.

---

# 2.5.3 PORTING METHOD

Never translate the reference code mechanically. Trace behavior:

```text
2.5.3 command registration
        ↓
command handler
        ↓
storage/model mutation
        ↓
listeners/events
        ↓
messages/sounds/economy
        ↓
GUI Java class + gui.yml
        ↓
Fabric equivalent
```

For GUIs:

```text
gui.yml
  ↓
slot/material/name/lore/placeholders
  ↓
reference GUI Java
  ↓
click listener/action
  ↓
Fabric backing Inventory
  ↓
ScreenHandler.addSlot order
  ↓
x/y coordinates
```

A field, setter, or permission constant is not proof that a feature works. Find the real runtime caller/event path.

---

# ITEM ECONOMY / FEATURE COSTS

```text
Emerald               = 1
Emerald Block         = 9
Deepslate Emerald Ore = 81
```

Keep these concepts separate:

```text
TeamBank
  = team-owned currency-item inventory

ItemEconomyProvider
  = player-owned item-economy abstraction
```

Configured costs currently recorded:

```text
sethome        100
home            50
enderchest      25
setwarp        200
warp             75
bank-withdraw    10
rename          500
```

`bank-withdraw` is intentionally not charged through the generic feature-cost path because that does not match the verified 2.5.3 behavior already established in this project.

---

# VERIFIED RUNTIME FEATURES

The user has previously confirmed successful runtime behavior for major paths including:

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

Whenever source changes touch one of these verified paths, runtime verification must be repeated before calling it verified again.

---

# LATER / NON-BLOCKING

These are not current prerequisites unless explicitly scoped:

```text
Discord/webhook integration
PlaceholderAPI integration
Redis/cross-server architecture
Paper database architecture
Bedrock/platform integrations
custom-data API
other unrelated architecture improvements
```

Record newly noticed unrelated issues here instead of investigating them during the active feature round.

---

# READY-TO-PASTE NEW-CHAT REMINDER

Use this as the first message when starting a new chat about this project:

```text
We are continuing the JustTeams-Fabric project.

Before doing anything else, read PROJECT_STATUS.md and SOURCE_CHANGE_ROUND_STATUS.md and treat them as authoritative.

Canonical repository:
https://github.com/libertyactions3-cloud/JustTeams-Fabric

The user's local project is NOT connected to GitHub. The user manually downloads the current `src/` from GitHub and replaces the local `src/` before building. GitHub `main` is canonical.

The supplied JustTeams 2.5.3 source tree and gui.yml are the behavioral reference. Verify actual 2.5.3 behavior before claiming parity. Translate behavior to Fabric instead of copying Paper architecture literally.

Always continue on every message. “Continue” is only a reminder.

Stay scoped to the current feature. Do not redesign unrelated architecture. Record unrelated discoveries as Later.

10-round rule:
- only changes under `src/` count as a round
- docs/search/audit/planning/build/runtime tests do not count
- one coherent multi-file feature can be one round
- Round 10 is the source-change stop and verification build gate
- canonical build: `./gradlew clean build --refresh-dependencies`
- do not claim compile success until the user's local build succeeds

Before writing Java/Fabric code, verify syntax/parsing/mappings/API signatures online against:
Minecraft 1.21.11
Yarn 1.21.11+build.4
Fabric API 0.141.4+1.21.11
Fabric Loader 0.18.4
Fabric Loom 1.15-SNAPSHOT / local 1.15.5
Java 21

Paper → Fabric GUI rule:
Never assume Bukkit slot == Fabric ScreenHandler slot ID. Trace:
Bukkit Inventory index → Fabric backing Inventory index → addSlot order → ScreenHandler slot ID → x/y coordinates.

Persistent inventory GUI rule:
Inventory GUI → inventory GUI should reuse the same 54-slot ScreenHandler whenever applicable so the mouse/cursor does not reset. A 27-slot reference GUI should be rendered inside the persistent 54-slot handler rather than switching handlers. Anvil/text-input screens are a separate exception.

Current project resume point and exact round count are in SOURCE_CHANGE_ROUND_STATUS.md. Do not invent/reset the round counter.

Do not ask me to repeat information already recorded in these files. Read them and continue from the exact recorded state.
```

---

# FINAL OPERATING PRINCIPLE

The project should always be handled as:

```text
REFERENCE BEHAVIOR
      ↓
VERIFY
      ↓
SMALLEST FABRIC IMPLEMENTATION
      ↓
COMMIT TO GITHUB MAIN
      ↓
UPDATE STATUS
      ↓
NEXT ROUND
      ↓
ROUND 10
      ↓
USER BUILDS LOCALLY
      ↓
RUNTIME VERIFICATION
```

Do not skip the verification steps merely because the source looks correct.
