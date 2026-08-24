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

One scoped source-change implementation round counts as one round, even when multiple files under `src/` are changed together.

## Current cycle

```text
Source-change rounds completed: 7 / 10
Current round: Round 7
Next source-change round: Round 8
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

The final clean build is reserved for Round 10 unless an earlier build is required to resolve a blocking compile issue.
