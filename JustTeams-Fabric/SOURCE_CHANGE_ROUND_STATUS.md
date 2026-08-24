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

One scoped source-change implementation commit counts as one round, even when multiple files under `src/` are changed together.

## Current cycle

```text
Source-change rounds completed: 1 / 10
Current round: Round 1
Round 1 source commit: eda4f942fa2b8b520ec425640919704b3fb6834a
Round 1 scope: /team kick, /team promote, /team demote command parity
Next source-change round: Round 2
```

The final clean build is reserved for Round 10 unless an earlier build is required to resolve a blocking compile issue.
