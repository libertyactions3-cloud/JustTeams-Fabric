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

## Round 9 — Compatibility / parity / edge-case pass — ACTIVE

### Round 9 audit: GUI membership lifecycle cleanup

The actual 2.5.3 source audit identified a cleanup consistency issue around GUI-driven member removal/disbanding: the command leave/disband paths explicitly disable team chat and stop glow, while the GUI leave/disband path did not. The reference call-chain evidence records the command cleanup as `TeamChatManager.disable(...)` + `GlowManager.stopGlowForPlayer(...)` before membership/team removal. The GUI paths were missing those cleanup calls.

The canonical Fabric `TeamGuiManager.leaveOrDisband()` was verified to have the same gap: its non-owner leave path removed the member and saved, but did not disable team chat or stop glow; its owner disband path unregistered and saved without clearing those per-member runtime states.

**Fixed in Round 9:**

- GUI leave now disables the leaving player's team-chat mode and stops that player's glow before membership removal.
- GUI disband now disables team-chat mode and stops glow for every team member before unregistering the team.
- The existing Ender Chest viewer/release cleanup remains before membership/team removal.

Commit: `5a3c41a4438895ee8929aa9233c43dc0d652e1eb` — `Round 9: fix GUI leave/disband lifecycle cleanup`

The implementation uses existing verified project methods (`TeamChatManager.disable`, `GlowManager.stopGlowForPlayer`, `TeamManager.removeMember`, and `TeamManager.unregister`) rather than introducing new lifecycle APIs.

### Round 9 audit: command surface vs permission ledger — CORRECTED / ACTIVE

The earlier audit treated every permission constant absent from the Fabric `TeamCommand.register()` method as a possible missing command. That was too broad. The reference source must be considered separately from the Fabric permission ledger.

The canonical Fabric permission class deliberately mirrors the Paper permission nodes and therefore contains nodes that are **not necessarily commands actually registered by the 2.5.3 command dispatcher**. The permission class currently includes `COMMAND_KICK`, `COMMAND_TRANSFER`, `COMMAND_SETTAG`, `COMMAND_SETDESCRIPTION`, `COMMAND_PROMOTE`, `COMMAND_DEMOTE`, `COMMAND_TOP`, `COMMAND_TEAMMSG`, `COMMAND_PUBLIC`, and `COMMAND_BANK`. fileciteturn292file0

#### Verified reference findings

- **`kick`: not a missing `/team kick` command.** The actual 2.5.3 `eu.kotori.justTeams` command system has no direct `/team kick` registration. The `COMMAND_KICK` permission node exists, but the actual kick implementation is GUI-based in `MemberManagementGui`. Its verified cleanup calls disable team chat, stop glow, and remove the member. fileciteturn302file8
- **`transfer`: not currently evidenced as an implemented reference command.** The reference `Team.setOwnerUuid(UUID)` setter exists, but the source audit found no caller that performs a proper ownership transfer. Therefore the setter exists while an ownership-transfer implementation is absent. Do **not** invent `/team transfer` merely because the Fabric permission constant exists. fileciteturn303file4
- **`settag`, `setdescription`, and `public`: the Fabric side already has team-setting state and GUI behavior for these operations.** `Team` contains verified setters for tag, description, and public state, and `TeamSettingsGui` already exposes those three settings to elevated team members. This establishes existing Fabric state/UI support, but does not by itself establish that 2.5.3 has command registrations for them. fileciteturn297file0 fileciteturn299file0
- **`promote`, `demote`, `top`, `teammsg`, and `bank`: remain unresolved.** The permission constants exist, but the evidence currently retrieved does not establish their actual 2.5.3 command registration/call-chain. They remain audit targets, not implementation targets.

#### Important conclusion

The correct question is **not**:

> “Which permission constants aren't in `TeamCommand`?”

It is:

> “Which actual 2.5.3 user-facing commands/features are missing from the Fabric port, and which permission nodes merely exist in the reference permission ledger without a corresponding command?”

This distinction prevents adding commands that the reference never exposed.

#### Current Fabric command surface

`TeamCommand.register()` currently exposes the verified Fabric command paths for create, GUI, info, leave, disband, pvp, glow, Ender Chest/ec, home, warp, invite, accept, deny, join, unjoin, requests, and chat. fileciteturn295file0

No command should be added solely because a corresponding permission constant exists.

### Remaining Round 9 audit targets

Continue repository-wide comparison of:

- actual 2.5.3 command registration and aliases
- permissions and bypasses
- state transitions
- messages/effects where applicable
- GUI actions
- lifecycle behavior
- persistence
- edge cases

Resolve only verified discrepancies and record deliberate exceptions.

## Round 10 — Final integration and acceptance

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

**Round 9 is active. The GUI leave/disband lifecycle discrepancy has been fixed. The next active audit is the actual 2.5.3 command registration/call-chain comparison, with permission constants treated only as evidence—not as proof that a command exists.**

Do not clean-build yet.
