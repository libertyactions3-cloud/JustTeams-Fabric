# JustTeams-Fabric — Source-Change Round Counter

A round counts only when a change is made somewhere under `src/` in `JustTeams-Fabric`.
Documentation, searching/auditing, planning, Gradle builds, and runtime testing do not consume rounds.

## Completed cycles

### Core parity cycle — 10 / 10
Completed. The user verified the resulting clean build successfully.

### GUI presentation/lore cycle — 10 / 10
Completed at source level. Covered the main `/team` menu, Join Requests, Warps, Settings, Leaderboards, Member Management, Blacklist, Pending Invites, No-Team menu, and Confirmation GUI presentation.

### GUI persistent-screen cycle — 10 / 10
Completed at source level. Covered in-place submenu navigation, persistent settings/warps/member-management/blacklist/leaderboard views, direct home behavior, and command entry into the persistent team container.

## Current corrective GUI cycle

```text
Source-change rounds completed: 1 / 10
Current round: Round 1 — self-head interaction + persistent member-editor layout correction
Next verification build: after Round 10, unless a compile blocker requires an earlier build
```

### Round 1 — self-head interaction + member-editor layout

The viewer's own member head in the main `/team` menu is now non-interactive, matching the intended player-facing behavior.

The persistent member-management view now keeps the same relative arrangement as the original 27-slot editor but renders it in the middle three rows of the persistent 54-slot container, rather than clustering it in the upper rows.

`/team invites` remains accessible while a player is not in a team; that is intentional because pending invites are specifically for teamless players.

## Verification rule

The user's canonical verification build is:

```powershell
./gradlew clean build --refresh-dependencies
```

Do not claim source compatibility until the user's local build confirms it.
