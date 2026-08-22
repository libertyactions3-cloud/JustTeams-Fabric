# Teleport parity work — 2.5.3 → Fabric

## Reference behavior established

The actual justTeams 2.5.3 reference uses:

- `team_home.warmup_seconds` — shipped value 5
- `team_home.cooldown_seconds` — shipped value 300
- `team_warps.warmup_seconds` — shipped value 5; the 2.5.3 implementation hard-codes 5, but the configuration key establishes the intended configurable behavior
- `team_warps.cooldown_seconds` — shipped value 300
- `effects.sounds.enabled` — shipped `true`
- `effects.sounds.success` — shipped `BLOCK_NOTE_BLOCK_PLING`
- `effects.sounds.error` — shipped `BLOCK_NOTE_BLOCK_BASS`
- `effects.sounds.teleport` — shipped `BLOCK_BEACON_ACTIVATE`
- `effects.particles.enabled` — shipped `true`
- `effects.particles.teleport_warmup` — shipped `PORTAL`
- `effects.particles.teleport_success` — shipped `END_ROD`

Warmup checks the player every server tick and cancels if the player changes world or moves farther than squared distance 1 from the starting position. The countdown/effects occur once per second. Successful teleportation sends the reference `teleport_success` message, plays the teleport sound, and spawns 30 success particles. Movement cancellation sends `teleport_moved` and plays the error sound. Home cooldown uses `teleport_cooldown`; warp cooldown uses `warp_cooldown`.

## Fabric implementation completed in this work

Added `team/TeamTeleportManager.java` using Fabric `ServerTickEvents.END_SERVER_TICK` rather than an executor scheduler.

The controller owns:

- home and warp cooldown maps;
- bypass-permission checks;
- home/warp warmup durations from `JustTeamsConfig`;
- 20-tick countdown timing with the first countdown occurring on the first server tick after request;
- same-world + squared-distance <= 1 movement cancellation;
- cross-dimension teleportation after the warmup;
- warmup particles and success particles;
- teleport/error sounds;
- cooldown application after successful warmup/teleport;
- cooldown cleanup;
- verified reference cooldown message wording.

`JustTeamsConfig` generates/defaults the teleport properties in the existing runtime `config/justteams.properties` file and exposes typed getters.

`TeamCommand`, `TeamHomeGui`, and `TeamWarpGui` hand approved teleport requests to the centralized controller instead of directly calling `ServerPlayerEntity.teleport(...)`.

The `/team warp` command checks warp cooldown before looking up the warp, matching the verified 2.5.3 command ordering.

## Entry-point audit

The current canonical Fabric repository was checked for direct teleport calls and the known teleport entry points were inspected:

```text
/team home
    → TeamCommand.useHome()
    → TeamTeleportManager.requestHome()

TeamHomeGui
    → TeamTeleportManager.requestHome()

/team warp <name> [password]
    → TeamCommand.useWarp()
    → TeamTeleportManager.requestWarp()

TeamWarpGui
    → TeamTeleportManager.requestWarp()
```

The current code search found the actual `.teleport(...)` implementation only in `TeamTeleportManager`, and the three known command/GUI paths route into it. This establishes that the inspected teleport paths no longer bypass the centralized controller. This is not claimed as an absolute repository-wide absence proof because the connector's repository code index has historically been unavailable/unindexed.

## Verification state

- Pinned API references were checked against the project's Yarn `1.21.11+build.4` / Fabric `0.141.4+1.21.11` environment for the relevant player sound, particle, registry, and teleport APIs.
- No clean Gradle build has been run for the current continuation work; the project protocol reserves the clean build checkpoint for Round 10.
- Runtime behavior still needs in-game verification, especially exact warmup timing, movement cancellation, cross-dimension teleport, cooldown behavior, and configurable effects.

## Known remaining detail

The current Fabric controller sends a hardcoded home-specific success sentence from `finishWarmup()`. The 2.5.3 source establishes that the reference sends the configurable `teleport_success` message, but the connector's retrieval of the very large `messages.yml` does not currently expose the exact value cleanly enough to justify inventing or approximating the string. This is therefore recorded as an unresolved message-parity detail rather than silently guessing it.

The shipped 2.5.3 effect settings are preserved conceptually in Fabric. Full Paper `MessageManager`/`EffectsUtil` infrastructure is not being recreated wholesale; only verified teleport behavior is being ported into the existing Fabric architecture.

## Next resume point

Teleport entry-point routing is adequately resolved for the inspected paths. Do not clean-build yet.

Next investigate the next concrete 2.5.3 parity feature from the current project status, beginning with ownership-transfer behavior if its complete reference call chain can be established. Compare the actual 2.5.3 entry point, authorization, state mutation, affected members/roles, notifications, persistence, and lifecycle effects before making any Fabric change.
