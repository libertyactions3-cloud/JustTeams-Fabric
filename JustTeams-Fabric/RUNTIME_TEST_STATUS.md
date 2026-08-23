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

Payment timing: previously FAILED relative to the requested behavior. The targeted fix now defers the withdrawal until the teleport succeeds.

Required behavior now:

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

Runtime verification of the corrected payment timing is still pending.

### Warp use

Teleport destination: PASS.

The runtime test originally exposed the wrong home success message. The targeted fix now uses:

```text
home → You have been successfully teleported to your team home.
warp → You have been successfully teleported to your team warp.
```

The user has now confirmed `/team warp <name> [password]` displays:

```text
You have successfully teleported to your team warp.
```

Warp payment is intended to occur only after successful teleport. Runtime verification of the corrected payment timing remains pending.

### Team Ender Chest

`/team enderchest` and `/team ec` correctly open the same feature.

Persistence: PASS.

The user confirmed that items placed in `/team ec` remain after closing and reopening it.

The final fix was aligned with the already-working TeamBank persistence pattern: both inventories use the project's compatible `ItemStack.CODEC` + `NbtOps` serialization path, while the Ender Chest remains attached to the team after release so it can be reused on the next open.

### Warp password creation / management

GUI creation: IMPLEMENTED.

The Warp GUI already prompts for an optional password during warp creation using the existing `TeamStringInputGui`.

Warp management GUI: IMPLEMENTED.

The warp management GUI can set/remove the password through its password input control.

Command creation: IMPLEMENTED in the latest source.

The command now supports:

```text
/team warp set <name>
/team warp set <name> <password>
```

The optional password is attached to the existing Brigadier command tree with a final greedy-string argument and persisted through `TeamWarp`.

### `/team` team-creation GUI

The previous GUI creation flow used chat input and the user reported that typing the team name in chat did not register.

The current GUI creation flow now uses the existing server-side `TeamStringInputGui` for both:

```text
team name
team tag
```

This keeps team creation inside the already-working GUI text-input path and avoids changing unrelated chat behavior.

Runtime verification of the new creation GUI flow is still pending.

## Targeted fixes committed

- `FeatureCostManager` supports non-mutating affordability checks plus explicit amount charges.
- `TeamTeleportManager` defers teleport currency withdrawal until successful teleport and uses distinct home/warp success messages.
- `/team warp` command and Warp GUI pass the per-warp cost into the teleport path rather than withdrawing before warmup/teleport.
- Team Ender Chest persistence uses the same compatible ItemStack codec/NBT approach as TeamBank and preserves the team's Ender Chest instance across close/reopen.
- Team main GUI no longer double-charges the Ender Chest feature.
- `/team warp set <name> [password]` now supports an optional password.
- Warp GUI creation and management support optional password entry.
- `/team` team-creation GUI now uses the existing anvil-style text input for name/tag entry.

## Remaining focused runtime verification

After pulling the latest `main` changes and successfully building, run only:

1. `/team home` — verify currency is unchanged during warmup/cancellation and is removed only after successful teleport.
2. `/team warp <name>` and Warp GUI — verify the same post-success payment behavior and corrected warp success message.
3. `/team warp set <name> <password>` — verify the password persists and `/team warp <name> <password>` works.
4. Warp GUI → create a password-protected warp — verify the password persists and can be used.
5. `/team` → Create Team GUI — verify entering the name and tag through the GUI creates the team successfully.
6. Verify command and GUI paths do not double-charge.

## Important parity decisions

Do not add a `bank-withdraw` feature charge. The verified 2.5.3 withdrawal path does not call the generic feature-cost payment method.

Do not create a rename feature solely from the `rename = 500` configuration entry; no actual 2.5.3 rename charge path or current Fabric rename entry point has been established.

## Build protocol

If runtime testing exposes a failure, fix only the verified failing feature path and rerun:

```powershell
./gradlew clean build --refresh-dependencies
```

Do not begin an unrelated repository-wide audit.
