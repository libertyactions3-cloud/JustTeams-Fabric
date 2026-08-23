# Runtime Test Status

## Round 10 build checkpoint

The user ran the canonical clean build locally on 2026-08-23:

```text
./gradlew clean build --refresh-dependencies

BUILD SUCCESSFUL in 1m 41s
8 actionable tasks: 8 executed
```

The two Loom `Cannot remap modifiers...` messages remained during configuration but did not fail the build.

## Current state

Compilation/package verification: **PASS**

Full in-game runtime verification: **PENDING**

## Runtime test scope

Test only the features covered by the current item-economy/parity work:

1. `/team home set`
   - confirm configured `sethome` cost is charged;
   - confirm insufficient currency prevents changing the home.

2. `/team home`
   - confirm configured `home` cost is charged once;
   - confirm cooldown/warmup behavior still works.

3. `/team warp set <name>`
   - confirm configured `setwarp` cost is charged before creation;
   - confirm insufficient currency prevents creation.

4. `/team warp <name>`
   - confirm the existing per-warp `TeamWarp.cost` is charged once;
   - confirm insufficient currency prevents the warp;
   - confirm payment ordering matches the documented parity decision.

5. `/team enderchest` and `/team ec`
   - confirm configured `enderchest` cost is charged once;
   - confirm insufficient currency prevents opening.

6. Team GUI equivalents for home, warp creation/use, and Ender Chest
   - confirm command and GUI paths do not double-charge.

7. Team bank withdrawal
   - confirm withdrawal is governed by member/bypass permission only;
   - confirm the configured `bank-withdraw` value does not charge the player.

## Important parity decisions

Do not add a `bank-withdraw` feature charge. The verified 2.5.3 withdrawal path does not call the generic feature-cost payment method.

Do not create a rename feature solely from the `rename = 500` configuration entry; no actual 2.5.3 rename charge path or current Fabric rename entry point has been established.

## Build protocol

If runtime testing exposes a failure, fix only the verified failing feature path and rerun:

```powershell
./gradlew clean build --refresh-dependencies
```

Do not begin an unrelated repository-wide audit.