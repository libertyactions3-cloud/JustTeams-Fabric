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
Source-change rounds completed: 6 / 10
Current round: Round 6 — persistent member management + corrected member-head slot mapping
Final build gate: after Round 10
```

### Round 1 — In-place Join Requests + Team Warps foundation

The main `/team` inventory now replaces its contents in-place for Join Requests and Warps. The in-place views use glass only in the top and bottom rows and return to the original menu snapshot without creating a new chest screen.

### Round 2 — In-place Team Settings + dynamic sort/PvP state

Team Settings now replaces the current menu items in-place. Tag/description chat input returns to the same view. Sorting and PvP item state are updated directly in the existing inventory instead of reopening it.

### Round 3 — In-place Member Management

Member heads now start at slots `9–44` as required, with leader first. Clicking a member opens the management items in-place and promote/demote/permission/kick actions stay within the same 54-slot screen.

### Round 4 — Main Team Home action

The main Team Home item now performs the home teleport directly instead of opening the separate Team Home chest management GUI. Setting/clearing the home remains a separate management surface.

### Round 5 — corrective source integration

The persistent-screen implementation was synchronized across its renderer/manager source so the new in-place views share the same menu state and mouse position.

### Round 6 — current source state

Member-management persistence and the corrected `9–44` member-head click mapping are now committed. Bank and Ender Chest remain the two special item-inventory surfaces whose underlying storage slots must not be replaced by decorative submenu items without preserving their real inventory semantics.

## Remaining rounds in this cycle

```text
Round 7 — Bank handling / storage semantics
Round 8 — Ender Chest handling / storage semantics
Round 9 — remaining in-place GUI parity and navigation cleanup
Round 10 — final source verification + handoff/build gate
```

The final clean build for this cycle will be run by the user at Round 10, as established by the workflow.
