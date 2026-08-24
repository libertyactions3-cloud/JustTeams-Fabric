# JustTeams-Fabric — 2.5.3 Parity Audit: Ten-Round Repository Activity

This is the active ten-round repository-audit cycle for comparing the Fabric port against the actual JustTeams 2.5.3 source in `libertyactions3-cloud/two-test/justTeams-2.5.3`.

## Rules

- Each round must produce real repository evidence. Do not invent work just to fill a round.
- Compare verified 2.5.3 method bodies and GUI behavior against the corresponding Fabric implementation.
- Scope implementation to the verified parity gap.
- Do not perform the final clean build before Round 10 unless a meaningful test requires it.
- Final build command: `./gradlew clean build --refresh-dependencies`
- Before writing new Java/Fabric code, verify the exact API/signature/syntax against the pinned 1.21.11/Yarn/Fabric environment.

## Toolchain

```text
minecraft_version=1.21.11
yarn_mappings=1.21.11+build.4
loader_version=0.18.4
loom_version=1.15-SNAPSHOT
fabric_version=0.141.4+1.21.11
Java 21
```

## Round status

### Round 1 — Repository inventory + source mapping
**Status: COMPLETE**

Verified the canonical Fabric repository and the actual 2.5.3 reference repository. Mapped the core command, GUI, team-state, storage, member-permission, teleport, economy, Ender Chest, glow, and chat paths.

Key evidence:
- 2.5.3 reference: `TeamCommand.java`, `TeamManager.java`, `Team.java`, `TeamPlayer.java`, `TeamGUIListener.java`, `TeamSettingsGUI.java`.
- Fabric: `TeamCommand.java`, `TeamManager.java`, `Team.java`, `TeamPlayer.java`, `TeamStorage.java`, `TeamGuiManager.java`, `TeamSettingsGui.java`.

### Round 2 — Command surface parity
**Status: PENDING**

Compare every user-facing `/team` command in 2.5.3 with Fabric and identify missing commands or materially different behavior.

### Round 3 — GUI surface parity
**Status: PENDING**

Compare the 2.5.3 GUI set and action handlers with Fabric GUI classes and identify missing screens, actions, confirmation flows, and management paths.

### Round 4 — Team state + persistence parity
**Status: PENDING**

Verify every persistent 2.5.3 team/member field against Fabric storage and identify fields that are stored but not actually used, or behaviors that are not persisted.

### Round 5 — Membership + permission lifecycle
**Status: PENDING**

Trace invite/accept/deny/join/unjoin/kick/leave/promote/demote/transfer behavior and the corresponding GUI permissions against 2.5.3 method bodies.

### Round 6 — Home / warp / economy behavior
**Status: PENDING**

Verify the already-tested item-economy and teleport paths against exact 2.5.3 behavior, including validation, password handling, warmup/cooldown, success timing, and GUI/command convergence.

### Round 7 — Combat / stats / glow / team chat
**Status: PENDING**

Verify friendly-fire, kills/deaths, glow lifecycle, team-chat toggle/spy behavior, and cleanup during leave/kick/disband.

### Round 8 — Admin + integrations
**Status: PENDING**

Trace 2.5.3 admin commands, leaderboard, blacklist, PlaceholderAPI, webhooks, aliases, platform information, and other externally visible integrations. Only port behavior that is applicable to the Fabric project.

### Round 9 — Focused runtime parity matrix
**Status: PENDING**

Run focused in-game tests for every newly implemented or corrected parity path. Record exact observed behavior and failures.

### Round 10 — Final clean build + parity disposition
**Status: PENDING**

Run:

```powershell
./gradlew clean build --refresh-dependencies
```

Then record the final remaining differences as either intentional Fabric-specific behavior or verified outstanding parity gaps.

## Confirmed gaps discovered during Round 1

These are evidence-based findings, not yet implementation work:

1. Fabric `/team create` hardcodes defaults (`pvp=true`, `public=false`, `glow=false`) while 2.5.3 derives defaults from configuration.
2. Fabric `/team create` validation is substantially narrower than the 2.5.3 validation path.
3. Fabric currently exposes only a subset of the 2.5.3 command surface. Notable absent command paths include member management (kick/promote/demote), team tag/description/color/rename/transfer commands, public toggle, bank, blacklist/unblacklist, settings, leaderboard/top, admin commands, server aliases, platform/help, team-chat spy, and invite-list UI/command behavior.
4. Fabric has a smaller GUI surface than 2.5.3. The reference contains dedicated invite, blacklist, leaderboard, and admin GUI paths that do not currently exist in the Fabric source tree.
5. Fabric stores `kills` and `deaths` in `Team`/`TeamStorage`, but the current gameplay source does not contain the corresponding stat-increment event path. The friendly-fire handler only permits/blocks damage.
6. 2.5.3 disband closes the inventory of every online team member. The current Fabric disband command performs glow/chat/Ender Chest cleanup but does not close every other member's open handled screen.
7. 2.5.3's warp-password command path can prompt for a missing password through chat input; Fabric's command path currently rejects a missing/wrong password rather than reproducing that prompt flow. The GUI has its own input flow.
8. 2.5.3 contains richer team-info output and messaging than the current Fabric `/team info`, which currently reports only team name/tag and member count/friendly-fire state.
9. The 2.5.3 reference has database/cross-server, webhook, PlaceholderAPI, alias, and related integrations that are not represented by the current Fabric architecture. These require separate applicability decisions rather than blind copying.

## Current runtime/build baseline

Previously verified by the user:

```text
/team home set                 PASS
/team home                     PASS
/team warp                     PASS
warp password creation/use    PASS
/team ec /team enderchest      PASS
team creation GUI              PASS
invalid-tag retry              PASS
command/GUI double-charge      PASS
latest local clean build      PASS
```

The two Loom configuration messages about `modifiers` are currently non-fatal and have not been treated as build failures.
