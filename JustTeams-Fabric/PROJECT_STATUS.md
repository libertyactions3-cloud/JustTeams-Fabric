# CONTINUATION / HANDOFF PROTOCOL — READ THIS FIRST

> This file is the persistent handoff for continuing the JustTeams-Fabric port. Read it before auditing, editing, or claiming progress.

## Canonical project

Repository: `libertyactions3-cloud/JustTeams-Fabric`
Branch: `main`
Project directory: `JustTeams-Fabric/`

Do not use the obsolete `libertyactions3-cloud/test` repository for this work.

Pinned environment:

```text
# Gradle
org.gradle.jvmargs=-Xmx1G
org.gradle.parallel=true
org.gradle.configuration-cache=false

# Fabric / Minecraft
minecraft_version=1.21.11
yarn_mappings=1.21.11+build.4
loader_version=0.18.4
loom_version=1.15-SNAPSHOT

# Mod
mod_version=0.1.0-SNAPSHOT
maven_group=eu.kotori.justteams
archives_base_name=justteams-fabric

# Dependencies
fabric_version=0.141.4+1.21.11

Java 21
Resolved Fabric Loom during the user's local build: 1.15.5
```

**Code-writing rule:** before writing or modifying Java/Fabric code, always verify the relevant syntax, parsing, mappings, and API signatures on the web against this pinned environment. The user's actual local `./gradlew clean build --refresh-dependencies` result is the final authority for compile compatibility.

---

# USER WORKFLOW RULES — CURRENT

## Continue on every message

Treat every user message as permission to continue the project. `Continue` is only a reminder to continue.

## Stay scoped to the current feature

```text
exact verified 2.5.3 behavior
        ↓
identify Fabric pieces actually missing
        ↓
implement only those pieces
```

Do not redesign unrelated architecture. Record unrelated issues only as Later.

## Repository activity / 10-round workflow

A round counts only when something under `src/` changes. Documentation, searching, auditing, builds, and runtime tests do not consume rounds.

The user's canonical verification build is:

```powershell
./gradlew clean build --refresh-dependencies
```

---

# CURRENT RESUME POINT — GUI PRESENTATION / LORE PARITY

The previous core-parity cycle completed 10 source rounds and was followed by a successful local clean build.

The current GUI presentation cycle has now also completed **10 / 10 source-change rounds**. It was based directly on the supplied 2.5.3 `gui.yml` plus the corresponding Java GUI classes.

```text
Round 1  Main /team menu                    ✅
Round 2  Join Requests                      ✅
Round 3  Team Warps                         ✅
Round 4  Team Settings                      ✅
Round 5  Leaderboard Category/View          ✅
Round 6  Member Management                  ✅
Round 7  Blacklist                           ✅
Round 8  Pending Invites                    ✅
Round 9  No-Team menu                       ✅
Round 10 Confirmation GUI                   ✅
```

### GUI presentation source commits

```text
R1  4c46cc5ab2b2fabd2c83861e40d6aa171a8e818d
R2  f882b5dca1aacde6aa88a2d856a6694bb12e9f02
R3  8c94a76f504efa1c15cf9cba23e1a7303a359fa4
R4  5cf0a72e0645a6f9db718f063d22442e6c32b314
R5  449442ea97b0e66786d4ea456ada7f8845debed7
R6  e21197a18b75f591aee3a925bffb7b375acdc74c
R7  dc62876844879aa473ab0f0b933901879785ebdf
R8  0af94413e300e8e67dd165c364e3e8dc3760de96
R9  37fa9ae80b8ce89a245a8a8fc7e37c6188a8eaf5
R10 0b0a1d6151979535101127a7c491215c18954a7f
```

## Presentation decisions

All custom GUI item names and lore explicitly disable italics unless 2.5.3 explicitly requests italic formatting.

The main `/team` menu retains the user's required geometry:

```text
0–8    glass/top border, with Warps at 7 and Join Requests at 8
9–44   member heads, leader first
45     PvP
46     Ender Chest
47     Home
48     blank
49     Sort
50     Bank
51     blank
52     Settings
53     Leave/Disband
```

### Intentional Fabric-specific GUI differences

`TeamBankGui` remains an item-backed team-owned currency inventory. The 2.5.3 BankGUI is a numeric Vault-money menu, so its deposit/balance/withdraw presentation was not transplanted onto the Fabric bank.

`TeamWarpManagementGui` is a Fabric-specific management screen. The supplied 2.5.3 GUI set has no standalone warp-management GUI, so its extra controls were not falsely attributed to the reference.

## Verification status

The 10 GUI rounds are source-complete. The next step is the user's local clean build, followed by focused runtime checks of the affected menus. Do not claim the presentation cycle is runtime-verified until that build and testing occur.

---

# CORE PARITY CYCLE — COMPLETED

The preceding 10-round source cycle completed:

```text
/team info
team creation defaults + validation
protected warp password prompt
disband inventory cleanup
lifecycle success sounds
/teammsg
chat-spy
/team invites
blacklist/unblacklist
/team settings + /guild /clan /party aliases
```

The user then ran the canonical clean build successfully.

---

# ITEM ECONOMY / FEATURE COSTS

The current workstream uses an internal item economy.

Currency denominations:

```text
Emerald               = 1
Emerald Block         = 9
Deepslate Emerald Ore = 81
```

Important separation:

```text
TeamBank
  = team-owned currency-item inventory

ItemEconomyProvider
  = player-owned currency balance / withdraw / deposit abstraction
```

Do not merge these concepts.

Configured feature costs remain:

```text
sethome        100
home            50
enderchest      25
setwarp        200
warp             75
bank-withdraw    10
rename          500
```

The `bank-withdraw` entry is intentionally not charged because the verified 2.5.3 bank withdrawal path does not call its generic feature-cost mechanism.

---

# VERIFIED RUNTIME FEATURES

The user has confirmed successful runtime behavior for the major current feature paths, including:

```text
/team home set
/team home
/team warp
/team warp set <name> [password]
Warp GUI password creation/use
/team ec
/team enderchest
team creation GUI
invalid-tag retry
payment timing after successful teleport
/team info
/team top
/team top kills
/team top balance
/team top members
```

---

# PINNED API / SYNTAX VERIFICATION RULE

Whenever new Java/Fabric code is written:

```text
1. Read the pinned Gradle/Minecraft/Yarn/Fabric settings.
2. Verify the exact API/signature/syntax on current web documentation.
3. Implement the smallest scoped change.
4. Let the user's local Gradle build be the final compile authority.
```

Do not claim compile success before the user's actual build confirms it.
