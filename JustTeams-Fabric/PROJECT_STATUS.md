# JustTeams-Fabric Project Status

## Purpose

Persistent implementation checklist, completed-work ledger, investigation notebook, and resume point for the Fabric recreation of JustTeams 2.5.3.

## Working rules

1. Use the `JustTeams-Fabric` branch for this project; do not use `main` as the working reference.
2. Consult this file before beginning a feature so completed work is not repeated.
3. Compare the Fabric implementation against the actual justTeams 2.5.3 source before declaring parity.
4. Establish reference behavior first; only then translate it to the pinned Fabric/Yarn API.
5. Do not mark a feature replicated merely because it compiles.
6. Record deliberate deviations explicitly.
7. The user has requested **no clean build until Round 10**.

## Version constraints

- Minecraft: `1.21.11`
- Yarn mappings: `1.21.11+build.4`
- Fabric Loader: `0.18.4`
- Fabric API: `0.141.4+1.21.11`
- Fabric Loom observed during builds: `1.15.5`

Verify API calls against these pinned versions before committing implementation code.

## Build cadence

Work proceeds through **10 repository/activity rounds**, followed by the user's clean build:

```powershell
./gradlew clean build --refresh-dependencies
```

**Do not run that clean build before Round 10.**

Latest previously recorded successful user build checkpoint:

- `BUILD SUCCESSFUL`
- Fabric Loom `1.15.5`
- 8 actionable tasks executed
- approximately 2m 52s

## Completed infrastructure requiring audit, not recreation

- Core Fabric project setup and pinned dependency compatibility.
- Team creation, membership, ownership/co-owner state, persistence.
- Invites and join requests.
- Team chat and chat-spy infrastructure.
- Friendly-fire/PvP state and event handling.
- Team home and warps.
- Item-backed team bank with configured currency items.
- LuckPerms-aware permission service with fallback behavior.
- Team GUI/settings GUI infrastructure.
- Persisted team glow state and Round 7 glow implementation/lifecycle work.

These areas still receive the final feature-parity audit before release.

## Reference investigation method

For each reference feature:

```text
command/UI
  -> authorization
  -> state mutation
  -> gameplay/rendering mechanism
  -> affected players
  -> lifecycle/cleanup
  -> persistence
```

Use call-chain and resemblance searches rather than relying only on exact class names or permission names. Verify permission context broadly before inventing a Fabric permission node.

## Round history

### Rounds 1–6

Established the project ledger, verified persisted glow state, traced `/team glow` authorization, and established that the reference does not use a dedicated `justteams.command.glow` check in the command handler.

### Round 7 — Team Glow — COMPLETE

The Fabric branch now contains the glow state, command integration, refresh mechanism, team-role coloring, and membership/lifecycle cleanup established during the Round 7 work.

Runtime verification remains part of the final testing pass, but Round 7 implementation work is complete. Do not restart the glow investigation unless a specific parity defect is found.

### Round 8 — Team Ender Chest — ACTIVE

Reference behavior from justTeams 2.5.3 has been established:

- `/team enderchest` and `/team ec` open a shared team-owned inventory.
- Access is controlled by the Ender Chest feature setting.
- A member needs `canUseEnderChest`, unless `justteams.bypass.enderchest.use` bypasses that restriction.
- The inventory is persistent and belongs to the team, not an individual player.
- Inventory size is configurable by rows.
- Multiple team members can view the same inventory simultaneously.
- Viewers are tracked and the in-memory inventory can be released after the final viewer closes it.
- Inventory changes are persisted.
- Paper 2.5.3 additionally uses distributed database locking and Redis synchronization for cross-server operation.

### Round 8 implementation completed so far

- Added `TeamEnderChest`, a shared `SimpleInventory` owned by `Team`.
- Added viewer tracking and save callbacks.
- Added 1.21.11 `ItemStack.CODEC`/NBT serialization for occupied inventory slots.
- Added Ender Chest state accessors to `Team`.
- Added Ender Chest persistence to `TeamStorage`.
- Bumped team storage data version from 4 to 5.
- Added `enderchest.enabled` configuration with default `true`.
- Added `enderchest.rows` configuration with default `3`, clamped to 1–6 rows.
- Added `TeamEnderChestGui` with membership, `canUseEnderChest`, and bypass authorization.
- Added `TeamEnderChestScreenHandler` using the appropriate vanilla generic chest handler for the configured row count.
- Replaced Team GUI slot 46's Ender Chest placeholder with `TeamEnderChestGui.open(...)`.
- Added `/team enderchest` and `/team ec` using `justteams.command.enderchest`.
- Added forced-removal cleanup so a kicked viewer's shared chest is closed/released before membership removal.
- Added disconnect cleanup through `ServerPlayConnectionEvents.DISCONNECT`; disconnected viewers are removed from the tracked viewer set and the shared chest is released after the final viewer leaves.

### Important current correction

The branch's Ender Chest work was initially ahead of its supporting `Team`, configuration, and storage pieces. Round 8 has now reconciled those pieces on the actual `JustTeams-Fabric` branch:

- `Team` owns optional `TeamEnderChest` state.
- `JustTeamsConfig` exposes the referenced Ender Chest settings.
- `TeamStorage` writes and restores the inventory.
- Command aliases now exist.
- Screen closure and disconnect paths remove stale viewer registrations.

No clean build has been run to validate these changes because the user explicitly requires waiting until Round 10.

## Known deliberate deviation

The Paper 2.5.3 Ender Chest has distributed database locking and Redis cross-server synchronization. The Fabric port currently has no equivalent database/Redis infrastructure.

Therefore this is **not** to be silently classified as replicated. For the final acceptance checklist it must be recorded as a **Deliberate exception** unless a Fabric-side equivalent is intentionally added later.

## Round 8 remaining work

1. Audit simultaneous-viewer behavior and save/release ordering against the reference.
2. Verify leave/disband behavior while one or more members have the shared Ender Chest open.
3. Verify persistence semantics for empty and non-empty inventories and configured row counts.
4. Verify GUI and command authorization against the exact 2.5.3 reference.
5. Update this document after the lifecycle/parity checks.

## Permission parity — ACTIVE

The canonical permission class contains the Ender Chest command and bypass nodes, but every feature must still be audited to verify that handlers actually enforce the same semantics as 2.5.3.

Do not infer parity from a constant's existence alone.

## Remaining roadmap after Round 8

### Round 9 — Compatibility / parity / edge-case pass

Audit every implemented feature against 2.5.3 for commands, permissions, state transitions, messages, GUI actions, lifecycle behavior, persistence, and edge cases. Resolve only verified discrepancies.

### Round 10 — Final integration and acceptance

- Finish all remaining fixes.
- Remove dead code and unjustified placeholders.
- Document deliberate deviations.
- Perform the final clean Gradle build only now.
- Use the build result as compilation verification, not as the sole parity criterion.
- Produce the final acceptance classification for every reference capability:
  - **Replicated**
  - **Deliberate exception**
  - **Not applicable**
  - **Still missing**

## Current resume point

**Continue Round 8 with simultaneous-viewer behavior, leave/disband lifecycle ordering, persistence semantics, and exact authorization parity.**

Do not begin unrelated feature work until this Round 8 audit is complete. Do not clean-build yet.
