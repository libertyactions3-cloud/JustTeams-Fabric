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

## Previous cycle

Rounds 1–11 completed the previous source-change cycle, including core membership commands, metadata, bank command surface, ownership transfer, kill/death tracking, `/team top`, final compilation compatibility corrections, and the main Team GUI geometry correction.

The final build for Round 10 returned:
```text
BUILD SUCCESSFUL
8 actionable tasks: 8 executed
```

## Current cycle

```text
Source-change rounds completed: 7 / 10
Current round: Round 7 (active/pending build and runtime verification)
Next source-change round: Round 8
```

### Round 1 — `/team info` parity
Scope: replace the minimal Fabric `/team info` output with the verified 2.5.3 information surface:
- team name
- tag
- description
- owner
- co-owners
- kills
- deaths
- KDR
- member count
- member names
- footer

Implementation uses Fabric's 1.21.11 `NameToIdCache` for offline member-name resolution and retains the existing Fabric item-backed economy rather than inventing a Vault-style numeric bank balance.

Verification:
- `./gradlew clean build --refresh-dependencies` → `BUILD SUCCESSFUL`
- `/team info` works as a player and displays the expanded information.
- console execution is rejected by the player-only command source requirement.

### Round 2 — Team creation defaults + validation parity
Source commits:
```text
b01f79afc02bc9074cf83b24321c75ebd8cc1556
32fe6301f004d2ca8a86e9ab655a4bab9c34dba3
e6a4359126cc823168a2b7131c7247e5571a7aa5
```
Scope: verified 2.5.3 creation behavior:
- configurable minimum/maximum team-name length (`3–16` by default)
- configurable maximum tag length (`6` by default) with a hard minimum of `2`
- ASCII letters/numbers/underscore validation
- reject names/tags made only of digits/underscores
- reject the reference's blocked administrative/system terms
- creation defaults read from Fabric config (`default-pvp=true`, `default-public=false`)
- glow remains disabled by default, matching the existing Fabric/reference creation semantics

Build verification:
- `./gradlew clean build --refresh-dependencies` → `BUILD SUCCESSFUL`

Focused `/team create` runtime verification remains pending.

### Round 3 — Protected warp password prompt parity
Source commits:
```text
223dfa1ff02812dacdea6e427a227ea3cd395cbd
89b67f8fb2cd9a52924aa4c5f94846259667086
38befe04763e63c20228dcc26b7ef369c3a8f9d7
```
Scope: when a protected warp is invoked without a password, use the existing Fabric chat-input session to prompt for the password; explicit `/team warp <name> <password>` behavior remains supported; the prompted password is revalidated before requesting the teleport.

Round 3 source implementation is complete. Build/runtime verification remains pending.

### Round 4 — Disband lifecycle parity
Source commit:
```text
d4e1996e12869d21343e5bb80b5771411917390c
```
Scope: match verified 2.5.3 disband behavior by closing the currently-open handled screen for every online team member during disband. The existing Ender Chest viewer cleanup remains separate and unchanged.

Round 4 source implementation is complete. Build/runtime verification remains pending.

### Round 5 — Notification sound parity
Source commit:
```text
16daa32a7d1f209649650e4f58db44525dcdd3b9
```
Scope: add the verified 2.5.3 success sound side effect to the existing Fabric lifecycle notification manager for successful leave, kick, and disband actions. Sound playback remains controlled by the existing Fabric sound configuration. Error sounds remain in the existing failure/teleport paths where they are already implemented.

Round 5 source implementation is complete. Build/runtime verification remains pending.

### Round 6 — `/teammsg` direct team messaging
Source commits:
```text
7bb60b2ab092b665a631e301b7f3e26ddfea338d
434b1aca4e6ecf7615c4d22e8a426235d22e840a
```
Scope: implement the local one-shot `/teammsg <message>` command using the verified 2.5.3 behavior:
- player-only command
- team membership required
- 2-second per-player message cooldown
- maximum 20 messages per 60-second window
- maximum 200 characters
- verified blocked-term filter
- delivery to all online members of the player's team

Cross-server Redis/MySQL message transport is intentionally deferred because this round targets the single-server Fabric feature path.

Round 6 source implementation is complete. Build/runtime verification remains pending.

### Round 7 — Chat-spy behavior
Source commits:
```text
0ee3b2d9904e1f061a221ffc9f9ed1e2b278feb2
2ebfb44c5e1dfccdacfa445c15519892a6e0a51a
d863bfd6b0f06013ab6d9b86d26b1f5c179ad3f6
714de9bf2cb1fda576e76002499fb7d6113d1611
```
Scope: reproduce the verified 2.5.3 chat-spy behavior locally:
- `/team chatspy` and `/team spy`
- permission-gated by `CHAT_SPY`
- explicit per-player enable/disable state
- enabled spies receive team-chat messages from teams they are not members of
- spy messages use a `[SPY] [team] player: message` format
- team members do not receive the separate spy-format copy

Round 7 source implementation is complete. Build/runtime verification remains pending.

### Round 8 — Invite-list surface
Implement `/team invites` and the corresponding invitation-list GUI/actions using the existing invitation state.

### Round 9 — Team blacklist
Port blacklist/unblacklist state, persistence, command behavior, and the player-facing blacklist GUI.

### Round 10 — Remaining core command surfaces
Prioritize the missing `/team settings` command and command aliases (`/guild`, `/clan`, `/party`) if they remain the only core command-surface gaps after Rounds 1–9.

## Deferred to a later cycle

The following remain outside this cycle unless they directly block a current feature:
- full admin subsystem
- Redis/cross-server synchronization
- database/migration/recovery architecture
- Discord/webhooks
- PlaceholderAPI
- Bedrock/platform integrations
- custom team-data API
- other Paper-specific integrations

These are intentionally not being treated as prerequisites for the core single-server Fabric feature set.
