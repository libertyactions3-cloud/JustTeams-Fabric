# JustTeams-Fabric — Source-Change Round Counter

This file defines the repository-activity counter used for the current parity implementation cycle.

## Counting rule

A round counts **only when a change is made somewhere under `src/`** in `JustTeams-Fabric`.

The following do **not** consume a round:

- `.md` / documentation-only changes
- repository searching or source auditing
- design/planning only
- Gradle builds
- runtime testing

One scoped source-change implementation round counts as one round, even when multiple files under `src/` or multiple corrective commits are required for the same scoped feature group.

## Previous parity cycle

The previous core-parity cycle completed all 10 source-change rounds. The final local clean build returned:

```text
BUILD SUCCESSFUL
8 actionable tasks: 8 executed
```

That cycle covered `/team info`, team creation defaults/validation, protected warp password prompting, disband inventory cleanup, lifecycle success sounds, `/teammsg`, chat-spy, invite-list GUI, blacklist/unblacklist, `/team settings`, and `/guild`/`/clan`/`/party` aliases.

## Previous GUI presentation cycle

```text
Source-change rounds completed: 10 / 10
Current round: Round 10 — Confirmation submenu presentation (source-complete)
```

That cycle covered the main `/team` menu, Join Requests, Team Warps, Team Settings, Leaderboards, Member Management, Blacklist, Pending Invites, No-Team menu, and Confirmation GUI presentation/lore.

## Current GUI interaction / persistent-screen cycle

This is a **new 10-round source-change cycle** focused specifically on the user's requirement that `/team` submenu navigation reuse the same 54-slot inventory rather than opening another chest GUI and resetting mouse position.

```text
Source-change rounds completed: 10 / 10
Current round: Round 10 — persistent leaderboard command/view integration (source-complete)
Final build gate: pending user clean build
```

### Round 1 — In-place Join Requests + Team Warps foundation

The main `/team` inventory now replaces its contents in-place for Join Requests and Warps. The in-place views use glass only in the top and bottom rows and return to the original menu snapshot without creating a new chest screen.

### Round 2 — In-place Team Settings + dynamic sort/PvP state

Team Settings now replaces the current menu items in-place. Tag/description chat input returns to the same view. Sorting and PvP item state are updated directly in the existing inventory instead of reopening it.

### Round 3 — In-place Member Management

Member heads now start at slots `9–44` as required, with leader first. Clicking a member opens the management items in-place and promote/demote/permission/kick actions stay within the same 54-slot screen.

### Round 4 — Main Team Home action

The main Team Home item now performs the home teleport directly instead of opening the separate Team Home chest management GUI. Setting/clearing the home remains a separate management surface.

### Round 5 — Persistent `/team` handler reuse

`TeamGuiManager.openMain()` now reuses an already-open `TeamMenuHandler` instead of creating a new handled screen. Persistent view state is therefore retained when returning to `/team`.

### Round 6 — Persistent command entry foundation

`/team settings` was moved to the persistent 54-slot handler when a team member invokes it. The same command can reuse an already-open team handler or open the handler once and render the requested view.

### Round 7 — Persistent warp management

Warp right-click management now uses the persistent team handler. Enable/disable, member-use permission, cost, password, location, remove, Back, and Close all update or navigate within the same screen.

### Round 8 — Persistent blacklist management

`/team blacklist` now enters the persistent team handler for team members. Blacklist entries can be removed in-place and Back restores the original `/team` layout.

### Round 9 — Persistent navigation cleanup

Main-menu resets now explicitly close persistent warp/blacklist state before restoring `/team`, keeping the menu-state dispatcher consistent when commands or Back actions return to the main screen.

### Round 10 — Persistent leaderboard command/view integration

`/team top` now reuses the persistent team handler for team members. Category selection, ranked leaderboard views, and Back all replace items within the same 54-slot inventory. Players who are not in a team retain the existing standalone leaderboard screen.

## Intentional storage boundaries

Team Bank and Team Ender Chest remain actual item-backed storage inventories. They are not being converted into decorative submenu items merely to force visual similarity; any future persistent handling must preserve their real insertion, withdrawal, and persistence semantics.

The final clean build for this cycle will be run by the user at Round 10, as established by the workflow.
