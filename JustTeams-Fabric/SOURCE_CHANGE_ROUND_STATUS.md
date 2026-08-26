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
Current round: Round 1 — command parity + universal GUI mapping/refresh correction
Next verification build: after Round 10, unless a compile blocker requires an earlier build
```

### Round 1 — command parity + GUI mapping/refresh

The viewer's own member head in the main `/team` menu is now non-interactive.

The persistent member-management view now uses the requested six-row positions:

```text
slot 4  = player-info head
slot 19 = dynamic promote/demote
slot 22 = kick member
slot 25 = transfer ownership
slot 37 = bank withdraw
slot 39 = use team ender chest
slot 41 = set team home
slot 43 = use team home
slot 49 = back
```

Ownership transfer confirmation is handled inside the same persistent 54-slot container rather than opening a separate inventory.

The `/team requests` command now enters the same in-place Join Requests view used by the `/team` menu.

The `/team invite <player>` argument now uses the verified Fabric player argument type so online players can be tab-completed. The inviter receives a success message and the invited player receives the invitation message.

`/team accept <team>` now reports successful joining to the player and notifies existing online team members.

Unset home now uses the verified 2.5.3 message:

```text
[ᴛᴇᴀᴍꜱ] Your team does not have a home set. An Owner or Co-Owner can set one with /team sethome.
```

Post-kick main-menu member heads are rebuilt from current team state rather than restoring a stale submenu snapshot. The persistent team menu title no longer embeds a stale member count.

`/team invites` remains accessible while a player is not in a team; that is intentional because pending invites are specifically for teamless players. The active teamless invite path uses a persistent 54-slot inventory container.

### Slot-mapping rule retained for future GUI work

Do not assume a Bukkit Inventory index equals a Fabric ScreenHandler slot ID.
For every GUI, distinguish:

```text
Bukkit inventory index
Fabric backing Inventory index
Fabric ScreenHandler slot ID
```

Verify the order of `ScreenHandler.addSlot(...)` and the slot x/y coordinates before mapping any reference slot.

The active persistent 54-slot chest handlers add all 54 menu slots first, in row-major order, followed by the player inventory, so menu backing indices 0–53 correspond directly to ScreenHandler slot IDs 0–53 in those handlers. That direct mapping must not be generalized to legacy 27-slot handlers without checking their construction order.

## Verification rule

The user's canonical verification build is:

```powershell
./gradlew clean build --refresh-dependencies
```

Do not claim source compatibility until the user's local build confirms it.
