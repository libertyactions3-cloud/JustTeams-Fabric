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
Source-change rounds completed: 2 / 10
Current round: Round 2 — Join Requests presentation (source-complete)
Next source-change round: Round 3
```

### Round 1 — Main `/team` menu item lore/presentation

Source commit:
```text
4c46cc5ab2b2fabd2c83861e40d6aa171a8e818d
```

Scope: translate the verified 2.5.3 `team-gui` presentation into the current Fabric main menu without redesigning its click behavior:
- Join Requests lore + owner/co-owner locked presentation
- Team Warps lore
- Team Bank dynamic balance lore + disabled/permission presentation
- Team Home set/not-set lore
- Team Ender Chest lore + locked presentation
- Sort Members lore with the current sort mode highlighted
- Team Settings lore + owner/co-owner locked presentation
- PvP status lore with dynamic Enabled/Disabled state
- Leave/Disband lore
- preserve the established 54-slot geometry, glass borders, leader/member head ordering, and explicit non-italic item text

The source change does not alter the existing `TeamGuiManager` click/action routing.

### Round 2 — Join Requests submenu presentation

Source commit:
```text
f882b5dca1aacde6aa88a2d856a6694bb12e9f02
```

Scope: translate the verified 2.5.3 `join-requests-gui` presentation into the existing Fabric Join Requests GUI:
- Join Requests header uses the reference title styling
- pending player heads use explicit online/offline status indicators
- player-head lore matches the reference accept/deny instructions
- empty state uses PAPER with the reference four-line presentation
- Back button uses the reference name/lore and non-italic formatting
- existing request storage, approval/denial, and click routing are unchanged

Verification: pending the next local clean build/runtime test for this GUI presentation pass.

## Next GUI presentation targets

Round 3 should audit and implement the **Team Warps submenu** presentation, including warp item names, lore, password/state indicators, management controls, and Back/Close items, using the verified 2.5.3 `gui.yml` and reference Java behavior.

After that, continue through the existing Fabric inventory GUIs in focused, feature-scoped rounds rather than redesigning the GUI framework.

## Deferred outside the GUI presentation cycle

The following remain later work unless a current GUI feature directly depends on them:
- full admin subsystem
- Redis/cross-server synchronization
- database/migration/recovery architecture
- Discord/webhooks
- PlaceholderAPI
- Bedrock/platform integrations
- custom team-data API
- other Paper-specific integrations
