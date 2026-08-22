# JustTeams-Fabric Project Status

## Purpose

Persistent implementation checklist, completed-work ledger, investigation notebook, and resume point for the Fabric recreation of JustTeams 2.5.3.

## Working rules

1. The canonical repository is `libertyactions3-cloud/JustTeams-Fabric`; work is being performed on its `main` branch. Do not use the old `libertyactions3-cloud/test` repository/branch.
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

### Round 8 — Team Ender Chest — COMPLETE (with recorded deliberate exceptions)

#### Reference behavior verified against actual justTeams 2.5.3 source

- `/team enderchest` and `/team ec` open a shared team-owned inventory.
- Access checks the Ender Chest feature, team membership, `canUseEnderChest`, and the `justteams.bypass.enderchest.use` bypass.
- The inventory is persistent and belongs to the team, not an individual player.
- The inventory is created at `configManager.getEnderChestRows() * 9` slots; the reference default is `team_enderchest.rows = 3`.
- Multiple members can open the same inventory simultaneously.
- Viewers are tracked and the inventory is saved/released after the final viewer closes.
- Inventory mutations are saved through the shared chest lifecycle.
- Single-server and cross-server modes use locking; cross-server mode additionally uses database locks and synchronization.

#### Fabric implementation established

- `TeamEnderChest` is one shared `SimpleInventory` owned by a `Team` while loaded.
- Every Ender Chest screen handler uses that same inventory instance, preserving simultaneous-viewer behavior.
- The implementation uses the pinned 1.21.11 `ItemStack.CODEC` with NBT operations for occupied-slot persistence instead of Bukkit object streams.
- Ender Chest state is persisted by `TeamStorage` and restored into the current configured size.
- `enderchest.enabled` defaults to `true`; `enderchest.rows` defaults to `3` and is constrained to vanilla generic-container sizes of 1–6 rows.
- Opening checks membership, `canUseEnderChest`, and `justteams.bypass.enderchest.use`.
- `/team enderchest` and `/team ec` are integrated with `justteams.command.enderchest`.
- Normal close, disconnect, kick, leave, and disband paths clean up viewer registrations while preserving save-before-team-removal ordering.

#### Configured-row-count edge case — reference check complete

The actual 2.5.3 `InventoryUtil.deserializeInventory()` reads the saved inventory size and then calls `inventory.setItem(i, item)` for every saved slot without checking whether that slot exists in the newly created inventory. The inventory itself is created first from the *current* configured row count.

Therefore the reference does **not** perform a compatibility resize/migration when the configured row count is reduced. A saved slot beyond the newly created inventory's bounds is not explicitly handled by the reference deserializer.

The Fabric implementation explicitly bounds restored slots to the current inventory size. This prevents an out-of-range restore failure but can omit items stored in slots that no longer exist after a downward row-count change. Record this as a **deliberate compatibility hardening/deviation**, not as unverified parity.

#### Remaining deliberate exceptions

1. **Cross-server locking and Redis synchronization:** Paper 2.5.3 supports database-backed Ender Chest locks and cross-server synchronization. The Fabric port currently has no corresponding database/Redis infrastructure.
2. **Bukkit serialization format:** Fabric uses Minecraft-native `ItemStack.CODEC` persistence and cannot read/write the Bukkit `BukkitObjectOutputStream`/Base64 format.
3. **Downward row-count change handling:** Fabric safely ignores saved slots outside the newly configured inventory instead of relying on the reference's unchecked `Inventory#setItem` path.
4. **Reference messages/effects:** The Paper implementation sends message-manager messages and success/error sounds at several Ender Chest lifecycle points. Fabric currently uses its existing direct-text infrastructure and does not yet have the full 2.5.3 MessageManager/EffectsUtil system; this broader infrastructure gap remains for the later parity pass rather than being silently counted as Ender Chest message parity.

No clean build has been run; compilation verification remains reserved for Round 10 as requested.

## Permission parity — ACTIVE

The canonical permission class contains the Ender Chest command and bypass nodes, but every feature must still be audited to verify that handlers actually enforce the same semantics as 2.5.3.

Do not infer parity from a constant's existence alone.

## Remaining roadmap

### Round 9 — Compatibility / parity / edge-case pass — ACTIVE

Audit every implemented feature against 2.5.3 for:

- commands and aliases
- permissions and bypasses
- state transitions
- messages/effects where applicable
- GUI actions
- lifecycle behavior
- persistence
- edge cases

Resolve only verified discrepancies and record deliberate exceptions.

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

**Round 8 is complete. Continue with Round 9: repository-wide compatibility, parity, and edge-case auditing. Start from the existing command/permission/lifecycle surfaces and compare them against actual justTeams 2.5.3 behavior.**

Do not clean-build yet.
