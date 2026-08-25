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
Source-change rounds completed: 10 / 10
Current round: Round 11 (active)
Round 10 final build gate: PASSED
Next source-change round: Round 12
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
Scope: final compilation compatibility corrections plus GUI presentation fixes required by the pinned Minecraft 1.21.11/Yarn/Fabric API surface.

The final clean build completed successfully:
```text
BUILD SUCCESSFUL
8 actionable tasks: 8 executed
```

### Round 11
Source commit:
```text
b8c346326605e91a452efec364b66aac3c67ad8a
```
Scope: correct the main Team GUI member/head geometry to the supplied 2.5.3 layout:
- top-row border slots `0–8` use the glass-pane fill
- bottom-row border slots `45–53` use the glass-pane fill
- members begin at slot `9` (second row, left)
- member heads occupy slots `9–44`
- leader is first in the member sequence
- warp and join-request buttons continue to override slots `7` and `8`

Round 11 requires another build before it is considered compile-verified.
