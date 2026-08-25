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
Source-change rounds completed: 1 / 10
Current round: Round 1 completed at source level
Next source-change round: Round 2
```

### Round 1 — `/team info` parity
Source commits:
```text
e310482830b891ed0eae32902a7922c7e8f5b1a2
92d8470e5017c7bb4b611bf0dbd3f38b691def21
99e8af793f6baf3d0437cd447b44b6b15e28f181
```

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

Implementation uses Fabric's 1.21.11 `NameToIdCache` for offline member-name resolution and retains the existing Fabric item-backed economy rather than inventing a Vault-style numeric bank balance. The command registration is wired through `TeamInfoCommandExtensions`.

The source implementation is complete, but it still requires a clean build and in-game verification before the round is considered fully verified.

### Round 2 — Team creation defaults + validation parity
Port the verified 2.5.3 default PVP/public/glow behavior and the user-facing name/tag validation rules into the existing Fabric configuration and creation path.

### Round 3 — Protected warp password prompt parity
When a protected warp is invoked without a password, reproduce the 2.5.3 chat-input prompt using the existing Fabric chat-input infrastructure.

### Round 4 — Disband lifecycle parity
Close the relevant handled screens for every online team member during disband, matching the verified 2.5.3 lifecycle without changing unrelated GUI architecture.

### Round 5 — Notification sound parity
Add the verified 2.5.3 success/error sound side effects to the existing Fabric notification layer.

### Round 6 — `/teammsg` direct team messaging
Implement the one-shot team-message command and its verified validation/cooldown behavior; keep cross-server behavior separate unless required by later parity scope.

### Round 7 — Chat-spy behavior
Implement `/team chatspy` / `/team spy` based on the actual 2.5.3 listener behavior.

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
