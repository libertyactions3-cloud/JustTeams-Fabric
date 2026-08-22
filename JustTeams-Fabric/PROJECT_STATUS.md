# CONTINUATION / HANDOFF PROTOCOL — READ THIS FIRST

> **Purpose:** This section is the operational handoff for any new ChatGPT conversation continuing the JustTeams-Fabric project. Read this entire section **before searching, editing, auditing, or claiming progress**. The project below is a Fabric recreation/port whose goal is behaviorally informed parity with the actual JustTeams 2.5.3 reference—not merely a project that compiles.

## 1. Canonical project and repositories

### Fabric implementation — canonical target

- Repository: `libertyactions3-cloud/JustTeams-Fabric`
- Branch: `main`
- Project directory inside repository: `JustTeams-Fabric/`
- Primary continuation/status file: `JustTeams-Fabric/PROJECT_STATUS.md`

Do **not** accidentally continue work in the old `libertyactions3-cloud/test` repository. That repository/branch was used earlier during development and investigation; it is not the canonical target now.

### Paper reference implementation

The behavioral reference is **justTeams 2.5.3**, previously examined from:

- `libertyactions3-cloud/two-test/tree/main/justTeams-2.5.3`

Use the actual reference source to establish behavior whenever parity is being investigated. Do not infer reference behavior from permission names, assumptions, or similarly named APIs.

---

# 2. User collaboration protocol — IMPORTANT

The user is an active research partner. Preserve this workflow.

## Ask the user when research would materially unblock or improve the work

The user explicitly wants the assistant to say what it is looking for and ask for help when functionality references, source searches, exact API information, or other information would help.

When appropriate, explain in this form:

```text
What I am trying to determine:
...

Why it matters:
...

What exact search/reference would help:
...

What I can continue doing without that information:
...
```

Do **not** pretend to know something that has not been verified. Do not silently substitute a weak guess for an exact reference if the user can help retrieve the missing information.

## Repository-wide search protocol

The user specifically said:

> Ask me whenever you need to search repository-wide.

This is especially important when GitHub connector code search is unavailable or reports:

```text
is_code_search_indexed: false
```

When true repository-wide enumeration is needed, ask the user for the exact search instead of claiming that individual-file inspection proves repository-wide absence.

However, do **not** unnecessarily ask the user to perform work that the available GitHub connector can do directly. First use efficient direct inspection of known files/paths and the available connector capabilities.

### Critical distinction

- **Exact search returning no result:** only proves no result for that exact search/scope.
- **Repository-wide indexed search:** may support a broader absence claim if the index and scope are valid.
- **Individual known-file inspection:** does not prove repository-wide absence.

Never blur these categories.

---

# 3. Core development philosophy

The required comparison chain is:

```text
actual 2.5.3 feature entry point
        ↓
authorization / permission context
        ↓
state mutation
        ↓
gameplay / rendering mechanism
        ↓
affected players
        ↓
lifecycle and cleanup
        ↓
persistence / synchronization
        ↓
edge cases
```

A feature is **not** considered replicated merely because:

- a permission constant exists;
- a similarly named method exists;
- a GUI button exists;
- the project compiles;
- the state can be changed but side effects/lifecycle behavior differ.

Trace the real call chain.

## Use resemblance and call-chain searching

The reference implementation may use terminology different from the expected name. Search by:

- method behavior;
- nearby `hasPermission(...)` or elevated-permission checks;
- callers and callees;
- state mutations;
- persistence calls;
- lifecycle methods;
- related GUI actions;
- command literals;
- data structures.

Do not conclude that a feature is absent merely because searching for one guessed name returns zero results.

---

# 4. Verification hierarchy

Use this order of confidence:

1. **Actual source code and exact call chain**
2. **Pinned Yarn/Fabric API documentation matching the project's exact versions**
3. **Existing project code already using the relevant API**
4. **Trusted official documentation**
5. **Reasoned inference, clearly labeled as inference**

Never present level 5 as level 1.

When working with Minecraft APIs, verify against the pinned versions below rather than using remembered APIs from older versions.

---

# 5. Version constraints

- Minecraft: `1.21.11`
- Yarn mappings: `1.21.11+build.4`
- Fabric Loader: `0.18.4`
- Fabric API: `0.141.4+1.21.11`
- Fabric Loom observed during builds: `1.15.5`

Known packet/API work for the viewer-specific glow implementation was based on exact Yarn 1.21.11+build.4 names, including:

```text
EntityTrackerUpdateS2CPacket
TeamS2CPacket
DataTracker.SerializedEntry<?>
ServerPlayerEntity.networkHandler.sendPacket(...)
```

Do not downgrade these APIs to older mapping names without verification.

---

# 6. Build and testing protocol

The user builds locally in PowerShell using:

```powershell
./gradlew clean build --refresh-dependencies
```

A successful build proves compilation/remapping success. It does **not** by itself prove behavioral parity.

## Most recent known user build

After the GUI kick lifecycle cleanup fix, the user reported:

```text
BUILD SUCCESSFUL in 1m 58s
Fabric Loom: 1.15.5
8 actionable tasks: 8 executed
```

Therefore the current code state at that checkpoint compiled successfully.

Do not ask for another clean build until a meaningful subsequent code change requires verification, unless the current round/testing plan explicitly requires it.

---

# 7. The 10-round workflow

The user and assistant adopted a structured **10-round** workflow.

Each round should generally follow:

```text
1. State the investigation target.
2. Establish the reference behavior.
3. Inspect the current Fabric implementation.
4. Identify only concrete discrepancies.
5. Implement the smallest verified correction.
6. Preserve existing working behavior.
7. Record the result and reasoning in this status file.
8. Build/test when appropriate.
9. Move to the next round only when the current target is adequately resolved or explicitly recorded as an exception/unresolved dependency.
```

Do not restart completed investigations without a concrete new parity defect.

Do not use rounds as permission to invent work merely to fill ten rounds. A round should be evidence-driven.

---

# 8. Current known progress and important completed fixes

## Membership system

The Fabric branch has a central membership mutation system:

```text
TeamManager.addMember(...)
TeamManager.removeMember(...)
```

Membership mutations maintain both team membership and the UUID → team index.

Two verified membership entry paths include:

```text
/team invite
    → Team.addInvite(...)
    → /team accept
    → TeamManager.addMember(...)
```

and:

```text
/team join
    → Team.addJoinRequest(...)
    → /team requests GUI
    → approve(...)
    → TeamManager.addMember(...)
```

## Glow — Round 7 implementation work

Viewer-specific glow is implemented using Fabric/Minecraft packets rather than globally changing entity glow for all viewers.

Architecture conceptually:

```text
team glow state
    ↓
GlowManager refresh
    ↓
receiver-specific TeamS2CPacket + entity metadata glowing state
    ↓
receiver sees target glowing with team/role color
```

Known characteristics:

- viewer-specific cache;
- packet-only/fake glow teams;
- configured team glow color with role-color fallback;
- refresh handling around player lifecycle;
- join/respawn/world-change refresh work;
- membership cleanup integration.

Do not restart the entire glow implementation. Audit only concrete parity defects.

## Join-request approval lifecycle

A prior lifecycle audit identified that join-request approval needed to refresh glow after membership was actually added. That fix was completed and successfully compiled in subsequent builds.

## GUI kick lifecycle cleanup

A concrete inconsistency was found:

- command leave/disband paths performed runtime cleanup;
- the GUI kick path originally removed membership without equivalent cleanup.

The GUI kick path was fixed to:

```text
1. disable target team-chat mode
2. remove/stop target viewer-specific glow relationships
3. remove target from TeamManager
4. persist state
```

Commit recorded during this work:

```text
633e499869b85d2a4c1c6338057cde0dcce92a95
```

The user subsequently ran a successful clean Gradle build after this fix.

## GUI leave/disband lifecycle cleanup

The status history also records a canonical-repository fix for GUI leave/disband runtime cleanup:

```text
5a3c41a4438895ee8929aa9233c43dc0d652e1eb
Round 9: fix GUI leave/disband lifecycle cleanup
```

The intended cleanup parity is:

```text
team chat cleanup
+ viewer-specific glow cleanup
+ Ender Chest viewer/release cleanup where applicable
before membership/team removal
```

Before changing this area again, inspect the **current canonical repository file**, not stale conversation snippets.

## Team Ender Chest

The status history records an implemented shared Ender Chest system with deliberate exceptions concerning:

- cross-server database/Redis locking and synchronization;
- Bukkit serialization compatibility;
- downward configured-row-count restoration behavior;
- full Paper message/effects infrastructure.

Treat those as documented deliberate architectural differences unless new requirements change the scope.

---

# 9. Current feature-parity mindset

Do not treat the following as automatically missing merely because they are not direct `/team` commands:

- kick;
- promote/demote;
- tag/description/public controls;
- other GUI-driven features.

First determine whether 2.5.3 itself exposes a command or implements the feature through GUI.

Likewise, **permission constants are evidence, not proof of command existence**.

The current permission audit established this important correction:

```text
Wrong question:
"Which permission constants aren't registered as Fabric commands?"

Correct question:
"Which actual 2.5.3 user-facing capabilities and call chains are missing or behaviorally different in Fabric?"
```

Known example:

- `COMMAND_KICK` exists as a permission node, but the reference kick implementation is GUI-based rather than proof that `/team kick` must exist.
- `Team.setOwnerUuid(...)` existing does not prove a complete ownership-transfer feature exists.

Do not invent commands solely from permission names.

---

# 10. Current resume point

The latest active work was a lifecycle/parity audit focused on **member removal paths**.

The next useful continuation target is:

```text
Enumerate and audit all actual member-removal/team-removal paths that can be reliably established:

- command leave
- command disband
- GUI leave
- GUI disband
- GUI kick
- any other verified removal path

For each path compare:

team-chat cleanup
viewer-specific glow cleanup
Ender Chest viewer/lock cleanup
membership/index mutation
team unregistering where applicable
persistence ordering
notifications/effects where relevant
```

### Important limitation at the last stop

The GitHub connector previously reported repository code search as unavailable/unindexed:

```text
is_code_search_indexed: false
```

Therefore a new chat must **not claim that every removal path has been audited repository-wide** unless:

1. the index has become usable and a valid repository-wide search is performed, or
2. the user provides repository-wide search results, or
3. all relevant paths can otherwise be enumerated from a verified command/GUI/lifecycle call graph.

If true repository-wide search is necessary, ask the user as requested.

---

# 11. Status-file maintenance protocol

This file is not merely a changelog. It is the continuation memory for future chats.

After meaningful work, update it with:

- what was investigated;
- exact reference behavior established;
- Fabric behavior established;
- discrepancy found;
- fix made;
- commit, if available;
- build/test result;
- deliberate exceptions;
- unresolved blockers;
- exact next resume point.

Also record **useful investigation approaches** when they may prevent future repeated work, especially:

- successful resemblance-search strategies;
- terminology differences between Paper and Fabric;
- why a guessed search failed;
- where the actual state mutation was found.

Do not overwrite older history blindly. Correct stale conclusions explicitly.

---

# 12. Communication requirements

The user values clear, cooperative communication. Preserve these habits:

- Say what you are currently looking for.
- Explain what the thing is intended to do and why it matters.
- Ask for functionality references/search help when genuinely useful.
- Do not hide uncertainty.
- Do not claim repository-wide absence from a narrow search.
- Prefer efficient methods; do not intentionally switch to inferior/manual approaches when a reliable connector method exists.
- When the user helps resolve a difficult research/API problem, incorporate the verified result into the status notes so it is not rediscovered unnecessarily.
- Before asking the user to build, explain exactly what changed and what the build is expected to verify.

A useful communication pattern is:

```text
What I verified:
...

What remains uncertain:
...

What I am checking next:
...

What I need from you, if anything:
...
```

---

# 13. Rules for the next chat — short checklist

Before continuing:

```text
[ ] Read this handoff section.
[ ] Read the remaining PROJECT_STATUS history below it.
[ ] Inspect the current canonical repository state.
[ ] Do not assume old commits/snippets still match current files.
[ ] State the current investigation target.
[ ] Establish reference behavior before changing Fabric behavior.
[ ] Use exact pinned 1.21.11/Yarn build.4 APIs.
[ ] Ask the user when repository-wide search is required.
[ ] Do not infer features from permission constants alone.
[ ] Update this file after meaningful progress.
[ ] Build only when appropriate for the current change/test checkpoint.
```

---

# 14. Handoff instruction

If this project is opened in a new chat, the new assistant should begin from the **Current resume point** above, inspect the current repository state, and then continue the lifecycle/parity audit rather than restarting old rounds or reimplementing already-completed glow, membership, or Ender Chest work.

The next assistant should preserve the collaborative workflow with the user and explicitly ask for repository-wide search or external reference help when those capabilities are genuinely needed.

---

# Historical project status

