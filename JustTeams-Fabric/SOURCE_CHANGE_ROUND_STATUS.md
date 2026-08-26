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
Source-change rounds completed: 7 / 10
Current round: Round 7 complete — compile/API corrections for TeamBank, player-name suggestions, and rank-change notification
Verification: user's local clean build succeeded after Round 7
Next source round: Round 8
```

### Round 1 — self-head interaction + member-editor layout
Completed.
- Viewer own member head is non-interactive.
- Persistent member editor was centered within the six-row container.
- `/team invites` remained valid for teamless players.

### Round 2 — rank/permission foundation + persistence
Completed.
- Added the seven-rank ladder: Leader, Co-Leader, Officer, Underofficer, Associate, Member, Initiate.
- Added persisted per-member toggles for invite, warp creation, and AutoBank.
- Existing Owner/Co-Owner/Member legacy role compatibility remains intact.
- TeamStorage persists the new rank and toggles.

### Round 3 — AutoBank economy foundation
Completed.
- Added exact team-bank currency withdrawal/availability for 81/9/1 denominations.
- FeatureCostManager can route supported feature costs through the member's team AutoBank toggle instead of player inventory.
- `/team autobank` persists the toggle across restarts.

### Round 4 — command permission parity
Completed.
- `/team invite` uses plain online-player suggestions without `@` and enforces the member invite toggle.
- `/team promote` and `/team demote` step through the seven-rank ladder.
- Warp creation checks the independent warp-creation toggle.
- Co-Leaders may remove any team warp.
- Invite success/notification and accept/join notification paths are preserved.

### Round 5 — persistent GUI layout and Join Requests refresh
Completed.
- Main `/team` member heads use the verified 2.5.3 positions: `19–25`, `28–34`, `37–43`.
- Join Request heads fill the entire persistent interior: `9–44`, immediately under the top glass row through the bottom interior row.
- Accepting a join request refreshes the saved main-team member-head snapshot while the player remains inside Join Requests.
- No `Dynamic` lore line is present on the request heads.
- Main GUI click dispatch uses the same verified slot map.
- Unset Home chat feedback uses the verified 2.5.3 message.

### Round 6 — command argument coverage + warp node enforcement
Completed.
- Ownership transfer player arguments use plain online-name suggestions.
- Blacklist/unblacklist player arguments use plain online-name suggestions.
- Base passwordless `/team warp set` now uses `canSetWarps`.
- Passworded warp creation uses the same permission.
- Base `/team warp remove` now allows the owner or Co-Leader to remove any warp.
- A dedicated command override preserves both passwordless and passworded warp creation paths.
- The member-management GUI now contains the requested independent toggles at slots 38, 40, and 42.

### Round 7 — compile/API corrections
Completed and locally verified.
- Renamed the private `TeamBank.count(Item)` helper so it no longer conflicts with the public `Inventory.count(Item)` API inherited from `SimpleInventory`.
- Corrected online-player suggestions to use the Fabric server player manager and `player.getName().getString()`, preserving plain names without `@`.
- Corrected rank-change notification to reuse the already-resolved `ServerPlayerEntity` instead of calling `getPlayerOrThrow()` from a helper that could not propagate `CommandSyntaxException`.
- User subsequently ran `./gradlew clean build --refresh-dependencies` successfully with no compilation errors.

## Verified-but-not-yet-resolved parity observations

### Member-button cooldowns
The public v2.5.2 history confirms a configurable **PvP toggle cooldown** (default 300 seconds), but I have not yet found authoritative evidence that the member-management promote/demote/permission buttons themselves use individual cooldown timers. Do not invent a cooldown duration. Trace the 2.5.3 source before implementing any such timer.

### Home button exact GUI color
The verified 2.5.3 GUI behavior includes the Ender Pearl Home button and the `Home not set.` lore state, and the verified chat message is recorded elsewhere. The exact MiniMessage color tag for the `Home not set.` lore line has not yet been conclusively recovered from the reference source. Do not claim an exact color until verified.

### Universal inventory-GUI persistence
The project rule remains that inventory-GUI → inventory-GUI transitions must reuse the same 54-slot handler whenever applicable so the mouse/cursor position does not reset. Vanilla anvil text input remains a separate AnvilScreenHandler exception.

## Mandatory Paper → Fabric slot mapping rule

Do **not** assume a Bukkit/Paper Inventory slot number equals a Fabric ScreenHandler slot ID.

For every GUI:

1. Determine the 2.5.3 Bukkit Inventory index.
2. Determine the Fabric backing Inventory index.
3. Determine the order of Fabric `ScreenHandler.addSlot(...)` calls.
4. Determine the resulting Fabric ScreenHandler slot ID.
5. Verify the Fabric slot x/y coordinates put it in the same visual row/column.

Always distinguish:

```text
Bukkit Inventory index
Fabric backing Inventory / Slot.index
Fabric ScreenHandler slot ID
```

For the active persistent six-row handler, the 54 menu slots are added first in row-major order:

```text
0  1  2  3  4  5  6  7  8
9 10 11 12 13 14 15 16 17
18 19 20 21 22 23 24 25 26
27 28 29 30 31 32 33 34 35
36 37 38 39 40 41 42 43 44
45 46 47 48 49 50 51 52 53
```

Only in handlers constructed that way do backing indices `0–53` directly correspond to ScreenHandler IDs `0–53`.

## Verification rule

The user's canonical verification build is:

```powershell
./gradlew clean build --refresh-dependencies
```

Round 7 is now compile-verified by the user's local build. Round 10 remains the final build gate for this corrective cycle.
