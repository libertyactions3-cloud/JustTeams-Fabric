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
Source-change rounds completed: 9 / 10
Current round: Round 9 complete — member GUI/rank/sort/name persistence + AutoBank/bank economy + logs + disband/sethome routing
Verification: Round 7 was the last user-verified clean build
Next verification build: Round 10 final gate
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
- Main `/team` member heads used the earlier verified layout `19–25`, `28–34`, `37–43`; this was superseded by Round 9's requested `9–44` placement.
- Join Request heads fill the persistent interior `9–44`.
- Accepting a join request refreshes the saved main-team member-head snapshot while remaining inside Join Requests.
- No `Dynamic` lore line is present on request heads.
- Main GUI click dispatch uses the verified slot map.
- Unset Home chat feedback uses the verified 2.5.3 message.

### Round 6 — command argument coverage + warp node enforcement
Completed.
- Ownership transfer player arguments use plain online-name suggestions.
- Blacklist/unblacklist player arguments use plain online-name suggestions.
- Base passwordless `/team warp set` uses `canSetWarps`.
- Passworded warp creation uses the same permission.
- Base `/team warp remove` allows the owner or Co-Leader to remove any warp.
- A dedicated command override preserves both passwordless and passworded warp creation paths.
- Member-management GUI contains independent feature toggles.

### Round 7 — compile/API corrections
Completed and locally verified.
- Renamed the private `TeamBank.count(Item)` helper so it no longer conflicts with the public `Inventory.count(Item)` API inherited from `SimpleInventory`.
- Corrected online-player suggestions to use the Fabric server player manager and `player.getName().getString()`, preserving plain names without `@`.
- Corrected rank-change notification to reuse the already-resolved `ServerPlayerEntity` instead of calling `getPlayerOrThrow()` from a helper that could propagate `CommandSyntaxException`.
- User subsequently ran `./gradlew clean build --refresh-dependencies` successfully with no compilation errors.

### Round 8 — member management / rank / sorting / AutoBank state separation
Completed at source level; not yet build-verified.
- Main `/team` member heads now start at slot 9 and fill 9–44.
- Main member heads show `Online Status` first, `Rank` second, and Joined date after that.
- `Role` presentation was replaced with `Rank` in the affected GUI.
- Hopper sort modes are `Online Status` (default), `Rank`, and `Alphabetical`; clicking the hopper cycles Online Status → Rank → Alphabetical → Online Status.
- Rank sorting is highest-to-lowest using the seven-rank ladder.
- Member-management no longer contains the golden helmet.
- The green dye, red dye, red wool, and beacon action names are aqua/small-caps.
- Gold ingot at slot 37 now combines bank withdrawal and AutoBank permission; the emerald-block permission item was removed.
- `/team autobank` now has a separate permission bit/state model: the gold-item permission gates the command, while AutoBank can be ON/OFF independently.
- AutoBank is disabled when a member is added to or removed from a team.
- Member-head clicks require the viewer to be Underofficer or higher and the selected target to be strictly lower rank; own/same/higher-rank heads do not open member management.
- Persisted `lastKnownName` was added to team-member data so offline GUI entries can resolve to the real last-known in-game name rather than display a UUID.
- A login hook updates the stored last-known username and the server UUID/name cache.
- Team bank GUI balance display now uses lime `<amount> total emeralds` wording rather than decimal formatting.
- `/team sethome` is the setting command; `/team home` is the use/teleport command with `home clear` retained separately.
- `/team home` when unset uses the requested blue `[ᴛᴇᴀᴍꜱ]` tag and red warning text.
- The main `/team` bank-log entry point is a writable-book / “book and quill” item at slot 6, directly left of the compass at slot 7.

### Round 9 — bank economy change + audit logs + disband confirmation
Completed at source level; not yet build-verified.
- Team-bank AutoBank withdrawal can use 81-value deepslate emerald ore and, when lower denominations are absent and bank space permits, convert the minimum ore into emerald blocks + emeralds as change so the requested value is removed exactly.
- Successful AutoBank withdrawals are logged with UUID, last-known/current username, amount, action, and timestamp.
- Manual bank withdrawals are logged too.
- Each team's bank log is stored separately, capped at 10,000 entries, and entries older than seven days are pruned.
- Added a persistent 54-slot bank-log GUI with a weekly calculated AutoBank top-spender view toggled from the same slot.
- The bank-log GUI deliberately does not display UUIDs to players; it displays the stored username.
- Main TNT disband action now enters a two-stage persistent confirmation flow.
- `/team disband` now enters the first confirmation stage instead of immediately deleting the team.
- The confirmed disband path remains a single final operation after the second confirmation.

## Verified-but-not-yet-resolved parity observations

### Member-button cooldowns
The public v2.5.2 history confirms a configurable **PvP toggle cooldown** (default 300 seconds), but I have not yet found authoritative evidence that the member-management promote/demote/permission buttons themselves use individual cooldown timers. Do not invent a cooldown duration. Trace the 2.5.3 source before implementing any such timer.

### Universal inventory-GUI persistence
The project rule remains that inventory-GUI → inventory-GUI transitions must reuse the same 54-slot handler whenever applicable so the mouse/cursor position does not reset. Vanilla anvil text input remains a separate AnvilScreenHandler exception.

### 2.5.3 disband confirmation presentation
Two-stage disband confirmation behavior is verified from the upstream release history, and the Fabric port now provides two persistent stages. The exact 2.5.3 visual arrangement/text of the second disband confirmation has not been conclusively recovered, so do not claim pixel-perfect second-stage presentation parity yet.

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

For the active persistent six-row handler, the 54 menu slots are added first in row-major order, so menu backing indices `0–53` correspond to ScreenHandler IDs `0–53` in that construction.

## Verification rule

The user's canonical verification build is:

```powershell
./gradlew clean build --refresh-dependencies
```

Round 7 is the last compile-verified source state. Round 10 remains the final build gate for this corrective cycle.
