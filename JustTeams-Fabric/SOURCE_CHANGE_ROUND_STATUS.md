# JustTeams-Fabric — Source-Change Round Counter

A round counts only when a change is made somewhere under `src/` in `JustTeams-Fabric`.
Documentation, searching/auditing, planning, Gradle builds, and runtime testing do not consume rounds.

## Mandatory exhaustive-request rule

For every user implementation prompt, identify **every explicit requested item** and implement **all requested items** that are within scope. Do not choose only the most obvious or convenient subset.

Before reporting completion:

```text
all explicit requests
        ↓
audit every request against current source + 2.5.3
        ↓
implement ALL requested items
        ↓
verify every changed path
        ↓
report each item as implemented, blocked, or runtime-unverified
```

Do not mark a request complete merely because a related feature was changed. Keep a checklist of every distinct request.

## Completed cycles

### Core parity cycle — 10 / 10
Completed. The user verified the resulting clean build successfully.

### GUI presentation/lore cycle — 10 / 10
Completed at source level. Covered the main `/team` menu, Join Requests, Warps, Settings, Leaderboards, Member Management, Blacklist, Pending Invites, No-Team menu, and Confirmation GUI presentation.

### GUI persistent-screen cycle — 10 / 10
Completed at source level. Covered in-place submenu navigation, persistent settings/warps/member-management/blacklist/leaderboard views, direct home behavior, and command entry into the persistent team container.

## Current corrective GUI cycle

```text
Source-change rounds completed: 10 / 10
Current round: Round 10 complete — exhaustive corrective GUI/command/economy/logging parity
Verification: Round 7 was the last user-verified clean build
Next step: user downloads current src/ and runs the normal clean build, then runtime-tests the remaining checklist
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
- Added team-bank currency support for 81/9/1 denominations.
- FeatureCostManager can route supported feature costs through the member's team AutoBank toggle.
- `/team autobank` state is persisted in `teams.dat`.

### Round 4 — command permission parity
Completed.
- `/team invite` uses plain online-player suggestions without `@` and enforces the member invite toggle.
- `/team promote` and `/team demote` step through the seven-rank ladder.
- Warp creation checks the independent warp-creation toggle.
- Co-Leaders may remove any team warp.
- Invite/accept notification paths are preserved.

### Round 5 — persistent GUI layout and Join Requests refresh
Completed.
- Main `/team` member-head layout was initially `19–25`, `28–34`, `37–43`.
- That historical layout is superseded by the requested active `9–44` placement.
- Join Request heads fill `9–44`.
- Accepting a join request refreshes the saved main-team member-head snapshot while remaining inside Join Requests.
- No `Dynamic` lore line is present on request heads.

### Round 6 — command argument coverage + warp node enforcement
Completed.
- Ownership transfer and blacklist-related player arguments use plain online-name suggestions where applicable.
- Base passwordless and passworded warp creation use `canSetWarps`.
- Base `/team warp remove` allows owner or Co-Leader removal.

### Round 7 — compile/API corrections
Completed and locally verified.
- Fixed `TeamBank.count(Item)` visibility collision.
- Fixed online-player completion against the current Fabric API.
- Fixed checked `CommandSyntaxException` propagation in rank-change notification.
- User ran `./gradlew clean build --refresh-dependencies` successfully.

### Round 8 — member-management / rank / sorting / AutoBank state separation
Completed at source level.
- Main `/team` member heads start at slot `9` and fill `9–44`.
- Main heads use Online Status indicator + rank symbol + username formatting.
- Main head lore contains Rank and Joined, with no Online Status or Dynamic line.
- Hopper modes cover Online Status, Rank, Alphabetical, and Join Date.
- Rank order is highest-to-lowest.
- Member-management uses aqua/small-caps action names, no golden helmet, no emerald-block permission item, and a combined gold-ingot Bank Withdraw + AutoBank control.
- Same/higher-rank and self head clicks are blocked; Underofficer+ is the management threshold in main-GUI routing.
- Persisted `lastKnownName` resolves offline usernames.
- AutoBank and team-chat state persist through TeamStorage.
- New member additions disable AutoBank and team-home use by default.

### Round 9 — bank economy + audit logs + disband confirmation
Completed at source level.
- AutoBank withdrawal supports 81/9/1 denominations with higher-denomination fallback and player change.
- Bank logs are capped at 10,000 entries per team and prune entries older than seven days.
- Logs use player heads and include timestamp, amount, action/type, and stored username.
- Weekly AutoBank top-spender view is calculated from the seven-day logs.
- Main `/team` logs button is immediately left of team warps.
- Initial disband confirmation was implemented as a two-stage confirmation flow.
- `/team disband` opens the first confirmation stage.

### Round 10 — exhaustive implementation pass
Completed at source level; **not locally build-verified after these changes**.
- Main `/team` title uses the 2.5.3 `ᴛᴇᴀᴍ - <members>/<max_members>` format and reads `settings.max_team_size` with default 10.
- Combined bank permission lore includes an explanatory line plus `Bank Withdraw and Autobank enabled/disabled`.
- AutoBank change is returned to the player using the exact 81/9/1 overpayment/change model requested.
- Team logs title is `ᴛᴇᴀᴍ ʟᴏɢs`.
- Core/extension command argument usage feedback was expanded for create, warp set/remove, invite, kick, promote, demote, blacklist, and unblacklist.
- User-facing no-team errors audited in the touched command/GUI paths use the #4C9DDE `[ᴛᴇᴀᴍꜱ]` prefix and red message.
- New-member addition path explicitly disables team-home permission.

## Post-gate persistent-disband correction

A subsequent user-requested source correction was made after the 10/10 corrective cycle:
- Disband confirmation is now rendered **inside the existing 54-slot `TeamMenuHandler`** instead of opening a separate 27-slot screen.
- Stage 1 and Stage 2 both reuse the same persistent inventory GUI/handler.
- Slot 11 confirms/advances and slot 15 cancels/restores the previous `/team` GUI.
- Stage 2 displays the actual team name.
- `/team disband` entered from outside the existing GUI still opens the main persistent team handler first, then enters the persistent confirmation state.
- The disband handler is routed before ordinary main-menu click dispatch, so confirmation clicks are consumed by the persistent disband view.

## Active layout facts

The current active persistent `/team` member-head layout is:

```text
9–44
```

Do not restore the historical `19–25 / 28–34 / 37–43` layout from earlier audit notes.

The current active persistent team menu is 54 slots with menu slots added first in row-major order, so menu backing indices `0–53` correspond to ScreenHandler IDs `0–53` in this construction.

## Known non-fatal configure notice

When compilation otherwise succeeds, Loom may print:

```text
Cannot remap modifiers because it does not exist in any of the targets [] or their parents.
```

This is not automatically a failure. The actual Gradle task result is authoritative.

## Remaining verification rule

The current source includes the post-gate persistent-disband correction and has **not** been locally build-verified after that change.

Do not make additional `src/` changes until the user has downloaded the current canonical `src/`, run:

```powershell
./gradlew clean build
```

and runtime-tested the affected feature paths.

Any new compiler error must be handled using the mandatory exact-error workflow and starts a new corrective cycle only after the current verification state is understood.
