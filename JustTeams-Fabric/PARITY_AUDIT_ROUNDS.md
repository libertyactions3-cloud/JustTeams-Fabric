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

### Round 2 — Command surface parity
**Status: COMPLETE — AUDIT/DESIGN ONLY**

Compared the current Fabric `/team` command tree and implementation against the actual 2.5.3 `TeamCommand.java` / `TeamManager.java` behavior. No Java source was changed during this round.

Verified command paths already present in Fabric include:

```text
/team
/team gui
/team create
/team info
/team leave
/team disband
/team pvp
/team glow
/team enderchest
/team ec
/team home [set|clear]
/team warp [set|remove|list|use]
/team invite
/team accept
/team deny
/team join
/team unjoin
/team requests
/team chat
```

Verified command/method gaps requiring later implementation include:

1. **Member management commands** — 2.5.3 has `kick`, `promote`, and `demote` paths backed by `TeamManager.kickPlayer`, `promotePlayer`, and `demotePlayer`; current Fabric `TeamCommand` has no corresponding command registration or TeamManager methods.
2. **Team metadata commands** — 2.5.3 has team-tag and description setters plus color/name-related operations. Current Fabric has tag/description fields and a settings GUI path, but no command-surface equivalents for the reference command paths.
3. **Ownership transfer** — 2.5.3 has `transferOwnership`; current Fabric `Team` stores an owner UUID but no corresponding command/method was found in the current command surface.
4. **Public/private toggle** — 2.5.3 exposes a `public` command path; current Fabric stores `publicTeam` and the settings GUI toggles it, but the command path is absent.
5. **Team bank command** — 2.5.3 exposes `/team bank`; current Fabric has a bank GUI/screen implementation but the current `TeamCommand` does not register a bank command path.
6. **Blacklist commands** — 2.5.3 exposes blacklist/unblacklist behavior; the current Fabric command tree has no corresponding command path.
7. **Settings command** — 2.5.3 exposes a settings command; Fabric has `TeamSettingsGui` but no matching `/team settings` command registration in `TeamCommand`.
8. **Leaderboard/top** — 2.5.3 exposes leaderboard/top behavior; current Fabric source has no matching command path established in this round.
9. **Admin commands** — 2.5.3 contains admin command paths; no equivalent Fabric admin command surface was established in this round.
10. **Server alias/platform/help paths** — present in the 2.5.3 command surface but not represented in the current Fabric command tree.
11. **Team-chat spy** — 2.5.3 has chat-spy behavior; Fabric currently has team chat toggle but no verified spy command.
12. **Invite-list command behavior** — 2.5.3 has dedicated invite/list behavior beyond the current Fabric command subset.

### Verified behavioral differences on existing commands

#### `/team create`

Current Fabric hardcodes:

```text
pvp = true
public = false
glow = false
```

whereas 2.5.3 derives the defaults from configuration. Fabric validation is also narrower than the reference validation path. This is a confirmed parity gap.

#### `/team info`

2.5.3 reports substantially richer information, including team description, owner/co-owners, kills, deaths, KDR, member count, and member list. Current Fabric reports only name/tag plus a basic member-count/friendly-fire line. This is a confirmed parity gap.

#### `/team warp <name> [password]`

Current Fabric checks the supplied password directly. The 2.5.3 manager path can enter a password-prompt flow for a protected warp when no password was supplied. The GUI already has its own input path. This is a confirmed behavioral difference requiring an explicit implementation decision in a later round.

#### `/team disband`

2.5.3 closes inventories for every online team member during disband. Current Fabric does not close every other member's open handled screen. This is a confirmed parity gap.

### Reference implementation details verified this round

`TeamManager.promotePlayer(...)` changes a member to `CO_OWNER` and enables `canWithdraw`, `canUseEnderChest`, `canSetHome`, and `canUseHome`, along with the corresponding editing permissions. `demotePlayer(...)` reverses those permissions to normal member defaults. Both operations persist the role/permissions and refresh glow for online targets. fileciteturn197file0

`TeamManager.setTeamTag(...)` and `setTeamDescription(...)` perform permission/validation checks, persist the changed field, publish/update team state, send success feedback, and refresh relevant GUI state. fileciteturn194file0

The current Fabric `TeamPlayer` already contains the same member permission fields and role semantics, so the missing work is primarily command/action exposure and correct lifecycle behavior rather than inventing new permission fields. fileciteturn184file0

### Round 2 implementation decision

Do **not** implement the entire missing command surface in one change. Each command group must become its own narrowly traced feature path against the 2.5.3 method body, then be implemented and runtime-tested before moving to the next group.

The next implementation priority is:

```text
kick / promote / demote
    ↓
team tag / description / public
    ↓
bank command
    ↓
transfer ownership
    ↓
blacklist / settings / leaderboard / admin / remaining utility commands
```

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

## Confirmed gaps discovered during Rounds 1–2

1. Fabric `/team create` hardcodes defaults while 2.5.3 derives defaults from configuration.
2. Fabric `/team create` validation is substantially narrower than the 2.5.3 validation path.
3. Fabric currently exposes only a subset of the 2.5.3 command surface, including missing kick/promote/demote, tag/description/color/rename/transfer, public, bank, blacklist/unblacklist, settings, leaderboard/top, admin, alias/platform/help, chat-spy, and richer invite-list behavior.
4. Fabric has a smaller GUI surface than 2.5.3; the reference includes dedicated invite, blacklist, leaderboard, and admin GUI paths that are not currently represented in the Fabric source tree.
5. Fabric stores `kills` and `deaths`, but the current gameplay source does not contain the corresponding stat-increment event path.
6. 2.5.3 disband closes the inventory of every online team member; Fabric does not yet do this for every member.
7. 2.5.3 can prompt for a missing protected-warp password through chat input; Fabric's command path currently checks a supplied password directly.
8. 2.5.3 `/team info` has substantially richer output than Fabric `/team info`.
9. The 2.5.3 reference contains database/cross-server, webhook, PlaceholderAPI, alias, and related integrations that are not represented by the current Fabric architecture; these require applicability decisions rather than blind copying.

## Current runtime/build baseline

Previously verified by the user:

```text
/team home set                 PASS
/team home                     PASS
/team warp                     PASS
warp password creation/use     PASS
/team ec /team enderchest      PASS
team creation GUI              PASS
invalid-tag retry              PASS
command/GUI double-charge      PASS
latest local clean build       PASS
```

The two Loom configuration messages about `modifiers` are currently non-fatal and have not been treated as build failures.
