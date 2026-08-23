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

Payment timing: FAILED relative to the requested behavior. The feature currently charged before the teleport completed.

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

### Warp use

Teleport destination: PASS.

The runtime test exposed a message-parity bug: warp use displayed the home success message:

```text
You have been successfully teleported to your team home.
```

The current fix makes the success message depend on the teleport type:

```text
home → You have been successfully teleported to your team home.
warp → You have been successfully teleported to your team warp.
```

Warp payment is now deferred until the actual teleport returns success. Both command and GUI paths use the same `TeamTeleportManager.requestWarp(..., cost)` path.

### Team Ender Chest

`/team enderchest` and `/team ec` correctly open the same feature.

Runtime persistence: FAILED.

Items placed in the team Ender Chest disappeared after closing the GUI.

The verified 2.5.3 reference explicitly serializes the shared Ender Chest inventory and saves it when the chest is released/closed. The Fabric implementation had an NBT serialization path, but it encoded `ItemStack` without the live registry lookup required by the 1.21.11 ItemStack NBT API. The fix now uses `ItemStack.toNbt(RegistryWrapper.WrapperLookup)` and `ItemStack.fromNbt(...)` with the server's registry manager supplied to `TeamStorage`.

## Targeted fixes committed

- `FeatureCostManager` now supports non-mutating affordability checks plus explicit amount charges.
- `TeamTeleportManager` only withdraws teleport currency after `ServerPlayerEntity.teleport(...)` reports success and uses a distinct home/warp success message.
- `/team warp` command and Warp GUI no longer withdraw the warp cost before password/warmup/teleport; both pass the per-warp cost into `TeamTeleportManager`.
- Team Ender Chest persistence now uses the pinned 1.21.11 registry-aware ItemStack NBT API.

## Remaining runtime verification

After pulling the latest `main` changes, rerun the focused tests:

1. `/team home` — verify currency is unchanged during warmup/cancellation and is removed only after successful teleport.
2. `/team warp <name>` and Warp GUI — verify the same post-success payment behavior and the corrected warp success message.
3. `/team enderchest` and `/team ec` — put an item in the chest, close it, reopen it, and verify the item remains.
4. Verify command and GUI paths do not double-charge.

## Important parity decisions

Do not add a `bank-withdraw` feature charge. The verified 2.5.3 withdrawal path does not call the generic feature-cost payment method.

Do not create a rename feature solely from the `rename = 500` configuration entry; no actual 2.5.3 rename charge path or current Fabric rename entry point has been established.

## Build protocol

If runtime testing exposes a failure, fix only the verified failing feature path and rerun:

```powershell
./gradlew clean build --refresh-dependencies
```

Do not begin an unrelated repository-wide audit.
