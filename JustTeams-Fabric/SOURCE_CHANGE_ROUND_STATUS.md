# JustTeams-Fabric — Source-Change Round Counter

This file defines the repository-activity counter used for the current parity implementation cycle.

## Counting rule

A round counts **only when a change is made somewhere under `src/`** in `JustTeams-Fabric`.

The following do **not** consume a round:

- `.md` / documentation-only changes
- repository searching or source auditing
- design/planning only
- Gradle builds
- runtime testing

One scoped source-change implementation round counts as one round, even when multiple files under `src/` or multiple corrective commits are required for the same scoped feature group.

## Current cycle

```text
Source-change rounds completed: 9 / 10
Current round: Round 10 (active/pending)
Round 10 source corrections: in progress after a failing clean build
Final build gate: not yet passed
```

### Round 1
```text
Source commits:
eda4f942fa2b8b520ec425640919704b3fb6834a
```
Scope: `/team kick`, `/team promote`, `/team demote` command parity.

### Round 2
Scope: member-permission behavior required by the member-management parity group.

### Round 3
Scope: command-extension wiring for the member-management group.

### Round 4
Scope: team metadata command implementation (`settag`, `setdescription`, `public`).

### Round 5
Scope: Team Settings GUI parity with the verified metadata behavior.

### Round 6
Source commit:
```text
5fe5397a217cad639eda2d0d7d09a56e23336ac0
```
Scope: missing `/team bank` command path opening the already-functional item-backed Fabric team bank.

### Round 7
Source commits:
```text
f804b61e74085050b6709251d5d0fbde4a83415b
952342093e22f531559b59b3f25dc5942b0a75d8
0f16d5ec2705f2b0ef0e9a37a8fd8fec8d003402
abb49bbc802683576ad1c1099552c11515b63937
```
Scope: 2.5.3-style ownership transfer with a confirmation GUI, `/team transfer <player>`, owner/member permission transitions, persistence, glow refresh, notifications, and command registration.

### Round 8
Source commits:
```text
57ffbf74c910e3b08a08bcb1a3ed882fac67e61a
346fe87c3d864c6cd2c96a2a5b290a3a51f6c383
abd58a235e370fb72eea0de1d61efd58d705f0e3
```
Scope: 2.5.3-style player kill/death statistics tracking using Fabric's `AFTER_DEATH` and `AFTER_KILLED_OTHER_ENTITY` events, including same-team kill exclusion and persistence of the updated stats. The corrective listener commit and initializer registration are part of the same scoped Round 8 feature group.

### Round 9
Source commits:
```text
842e7045fa4648bd83b2f408bb07458ce0b97160
45d0880506b546469c38b03756c5830c8cbaf007
6e39430998a102fa66b530145399d055a79a4eeb
```
Scope: 2.5.3-style `/team top` two-stage leaderboard UI with kills, balance, and member-count categories, ranked team entries, and back navigation.

### Round 10
Final verification round.

The first clean build of the Round 9 source state failed with 16 compile errors. Round 10 is therefore active and contains the corrective source changes for the pinned Minecraft 1.21.11/Yarn/Fabric API surface, including:

- `ServerPlayerEntity.getEntityWorld().getServer()` in place of unavailable `getServer()` calls.
- `MinecraftServer.getApiServices().nameToIdCache().findByName(...)` in place of unavailable `MinecraftServer.getUserCache()`.
- server-side handled-screen closure through `ServerPlayerEntity`.
- `ScreenHandler` slot construction performed inside the handler subclass constructor because `addSlot` is protected.
- `TeamSettingsGui` text generic typing and Fabric storage reference correction.

The **next clean build is the final build gate**. Round 10 is not marked passed until it returns `BUILD SUCCESSFUL`.
