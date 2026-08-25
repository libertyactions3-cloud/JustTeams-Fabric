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

## Current GUI presentation cycle

```text
Source-change rounds completed: 10 / 10
Current round: Round 10 — Confirmation submenu presentation (source-complete)
Next cycle: runtime/compile verification, then further GUI parity as needed
```

### Round 1 — Main `/team` menu item lore/presentation
Source commit: `4c46cc5ab2b2fabd2c83861e40d6aa171a8e818d`

### Round 2 — Join Requests submenu presentation
Source commit: `f882b5dca1aacde6aa88a2d856a6694bb12e9f02`

### Round 3 — Team Warps submenu presentation
Source commit: `8c94a76f504efa1c15cf9cba23e1a7303a359fa4`

### Round 4 — Team Settings submenu presentation
Source commit: `5cf0a72e0645a6f9db718f063d22442e6c32b314`

### Round 5 — Leaderboard Category/View presentation
Source commit: `449442ea97b0e66786d4ea456ada7f8845debed7`

### Round 6 — Member Management presentation
Source commit: `e21197a18b75f591aee3a925bffb7b375acdc74c`

### Round 7 — Blacklist presentation
Source commit: `dc62876844879aa473ab0f0b933901879785ebdf`

### Round 8 — Pending Invites presentation
Source commit: `0af94413e300e8e67dd165c364e3e8dc3760de96`

### Round 9 — No-Team menu presentation
Source commit: `37fa9ae80b8ce89a245a8a8fc7e37c6188a8eaf5`

### Round 10 — Confirmation submenu presentation
Source commit: `0b0a1d6151979535101127a7c491215c18954a7f`

The GUI presentation cycle intentionally did not redesign Fabric-specific bank or warp-management architecture. The item-backed bank remains a Fabric-specific presentation, and the warp-management screen has no direct 2.5.3 counterpart.

## Next work

Run the requested clean build to verify all ten GUI presentation rounds together, then perform focused runtime checks of the affected submenus.
