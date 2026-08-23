# Runtime Test Status

## Round 10 build checkpoint

The user ran the canonical clean build locally on 2026-08-23:

```text
./gradlew clean build --refresh-dependencies

BUILD SUCCESSFUL in 1m 41s
8 actionable tasks: 8 executed
```

The two Loom `Cannot remap modifiers...` messages remained during configuration but did not fail the build.

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
check cooldown / affordability
    ↓
start warmup
    ↓
successful teleport
    ↓
withdraw item currency
    ↓
success message
```

Runtime verification of the corrected payment timing is still pending.

### Warp use

Teleport destination: PASS.

The runtime test exposed a message-parity bug: warp use displayed the home success message:

```text
You have been successfully teleported to your team home.
```

The targeted fix now makes the success message depend on teleport type:

```text
home → You have been successfully teleported to your team home.
warp → You have been successfully teleported to your team warp.
```

Warp payment is now intended to occur only after successful teleport. Runtime verification of the corrected payment timing/message is still pending.

### Team Ender Chest

`/team enderchest` and `/team ec` correctly open the same feature.

Persistence: PASS.

The user confirmed that items placed in `/team ec` remain after closing and reopening it.

The final fix was deliberately aligned with the already-working TeamBank persistence pattern: both inventories use the project's compatible `ItemStack.CODEC` + `NbtOps` serialization path, while the Ender Chest remains attached to the team after release so it can be reused on the next open.

## Targeted fixes committed

- `FeatureCostManager` supports non-mutating affordability checks plus explicit amount charges.
- `TeamTeleportManager` defers teleport currency withdrawal until successful teleport and uses distinct home/warp success messages.
- `/team warp` command and Warp GUI no longer withdraw the warp cost before password/warmup/teleport; both pass the per-warp cost into the teleport path.
- Team Ender Chest persistence uses the same compatible ItemStack codec/NBT approach as TeamBank and preserves the team's Ender Chest instance across close/reopen.

## Remaining runtime verification

After pulling the latest `main` changes, run only the focused remaining tests:

1. `/team home` — verify currency is unchanged during warmup/cancellation and is removed only after successful teleport.
2. `/team warp <name>` and Warp GUI — verify the same post-success payment behavior and the corrected warp success message.
3. Verify command and GUI paths do not double-charge.

## Important parity decisions

Do not add a `bank-withdraw` feature charge. The verified 2.5.3 withdrawal path does not call the generic feature-cost payment method.

Do not create a rename feature solely from the `rename = 500` configuration entry; no actual 2.5.3 rename charge path or current Fabric rename entry point has been established.

## Build protocol

If runtime testing exposes a failure, fix only the verified failing feature path and rerun:

```powershell
./gradlew clean build --refresh-dependencies
```

Do not begin an unrelated repository-wide audit.
