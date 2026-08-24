# Runtime Test Status

## Round 10 build checkpoint

The user ran the canonical clean build locally on 2026-08-23:

```text
./gradlew clean build --refresh-dependencies

BUILD SUCCESSFUL in 1m 41s
8 actionable tasks: 8 executed
```

The two Loom `Cannot remap modifiers...` messages remained during configuration but did not fail the build.

The source has changed since that successful build, so the current `main` state requires another clean build before current compile verification is claimed.

## Runtime results

### `/team home set`

PASS.

The user started with 64 Deepslate Emerald Ore and ended with:

```text
62 Deepslate Emerald Ore
6 Emerald Blocks
8 Emeralds
```

That is exactly 100 item-currency units spent, matching the configured `sethome = 100` cost.

The team home was stored correctly.

### `/team home`

Teleport destination: PASS.

Payment timing: PASS after the targeted fix. The user confirmed the post-success payment behavior works as intended.

Required behavior:

```text
check validity / cooldown
    ↓
start warmup
    ↓
successful teleport
    ↓
withdraw item currency
    ↓
home success message
```

### Warp use

Teleport destination: PASS.

The runtime test originally exposed the wrong home success message. The targeted fix now uses:

```text
home → You have been successfully teleported to your team home.
warp → You have been successfully teleported to your team warp.
```

The user confirmed `/team warp <name> [password]` works and displays the intended warp success message.

Warp payment timing: PASS after the targeted fix. The user confirmed the post-success payment behavior works as intended.

### Team Ender Chest

`/team enderchest` and `/team ec` correctly open the same feature.

Persistence: PASS.

The user confirmed that items placed in `/team ec` remain after closing and reopening it.

The final fix was aligned with the already-working TeamBank persistence pattern: both inventories use the project's compatible `ItemStack.CODEC` + `NbtOps` serialization path, while the Ender Chest remains attached to the team after release so it can be reused on the next open.

### Warp password creation / management

GUI creation: PASS.

The Warp GUI prompts for an optional password during warp creation using the existing `TeamStringInputGui`.

Warp management GUI: PASS.

The warp management GUI can set/remove the password through its password input control.

Command creation: PASS.

The command supports:

```text
/team warp set <name>
/team warp set <name> <password>
```

The optional password is attached to the existing Brigadier command tree with a final greedy-string argument and persisted through `TeamWarp`.

The user confirmed password-protected warp creation and use work.

### `/team` team-creation GUI

The current GUI creation flow uses the existing server-side `TeamStringInputGui` for both:

```text
team name
team tag
```

The user confirmed the invalid 5-character tag retry no longer crashes the server and that the team-creation GUI works as intended.

#### Crash discovered and fixed during runtime testing

The user reported a server `StackOverflowError` while cancelling/restarting the team-creation anvil input after an invalid 5-character tag.

The recursion was:

```text
NoTeamGui.open()
    ↓
TeamStringInputGui.onClosed()
    ↓
cancelled.run()
    ↓
NoTeamGui.open()
    ↓
closeHandledScreen()
    ↓
TeamStringInputGui.onClosed()
    ↓
...
```

Cause: `ScreenHandler.onClosed()` was reopening another handled screen synchronously from its cancellation callback.

Targeted fix:

```text
TeamStringInputGui.onClosed()
    ↓
server.execute(cancelled)
```

The user retested the invalid-tag scenario and confirmed the crash is resolved.

### Double-charge verification

PASS.

The user confirmed the remaining command/GUI paths do not double-charge for the tested features.

## Focused runtime verification status

All currently requested runtime checks are PASS:

```text
/team home payment timing             ✅
/team warp payment timing             ✅
warp success message                  ✅
/team warp set <name> <password>     ✅
Warp GUI password creation            ✅
Warp password use                     ✅
/team ec persistence                  ✅
/team GUI team creation               ✅
invalid-tag retry without crash       ✅
command/GUI double-charge checks      ✅
```

## Exact current source-verification state

The latest source still requires a clean build after the latest GUI/password/TeamStringInputGui changes. Do not claim the current revision is compile-verified until the user reruns:

```powershell
./gradlew clean build --refresh-dependencies
```

## Targeted fixes committed

- `FeatureCostManager` supports non-mutating affordability checks plus explicit amount charges.
- `TeamTeleportManager` defers teleport currency withdrawal until successful teleport and uses distinct home/warp success messages.
- `/team warp` command and Warp GUI pass the per-warp cost into the teleport path rather than withdrawing before warmup/teleport.
- Team Ender Chest persistence uses the same compatible ItemStack codec/NBT approach as TeamBank and preserves the team's Ender Chest instance across close/reopen.
- Team main GUI no longer double-charges the Ender Chest feature.
- `/team warp set <name> [password]` now supports an optional password.
- Warp GUI creation and management support optional password entry.
- `/team` team-creation GUI uses the existing anvil-style text input for name/tag entry.
- `TeamStringInputGui` defers cancellation callbacks through the server executor to prevent recursive handled-screen reopening.

## Remaining action

Run one fresh clean build on the latest `main` revision:

```powershell
./gradlew clean build --refresh-dependencies
```

If that succeeds, the current focused runtime/compile verification checkpoint is complete unless a new runtime defect is discovered.

## Important parity decisions

Do not add a `bank-withdraw` feature charge. The verified 2.5.3 withdrawal path does not call the generic feature-cost payment method.

Do not create a rename feature solely from the `rename = 500` configuration entry; no actual 2.5.3 rename charge path or current Fabric rename entry point has been established.

## Build protocol

If a new build or runtime test exposes a failure, fix only the verified failing feature path and rerun:

```powershell
./gradlew clean build --refresh-dependencies
```

Do not begin an unrelated repository-wide audit.
