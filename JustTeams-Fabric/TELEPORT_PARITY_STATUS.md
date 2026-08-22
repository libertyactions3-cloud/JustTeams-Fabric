# Teleport parity work — 2.5.3 → Fabric

## Reference behavior established

The actual justTeams 2.5.3 reference uses:

- `team_home.warmup_seconds` — shipped value 5
- `team_home.cooldown_seconds` — shipped value 300
- `team_warps.warmup_seconds` — shipped value 5; the 2.5.3 implementation hard-codes 5, but the configuration key establishes the intended configurable behavior
- `team_warps.cooldown_seconds` — shipped value 300
- `effects.sounds.enabled` — shipped `true`
- `effects.sounds.success` — `BLOCK_NOTE_BLOCK_PLING`
- `effects.sounds.error` — `BLOCK_NOTE_BLOCK_BASS`
- `effects.sounds.teleport` — `BLOCK_BEACON_ACTIVATE`
- `effects.particles.enabled` — shipped `true`
- `effects.particles.teleport_warmup` — `PORTAL`
- `effects.particles.teleport_success` — `END_ROD`

Warmup checks the player every server tick and cancels if the player changes world or moves farther than squared distance 1 from the starting position. The countdown/effects occur once per second. Successful teleportation sends `teleport_success`, plays the teleport sound, and spawns 30 success particles. Movement cancellation sends `teleport_moved` and plays the error sound. Home cooldown uses `teleport_cooldown`; warp cooldown uses `warp_cooldown`.

## Fabric implementation completed in this round

Added `team/TeamTeleportManager.java` using Fabric `ServerTickEvents.END_SERVER_TICK` rather than an executor scheduler.

The controller now owns:

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
- exact reference cooldown message wording.

`JustTeamsConfig` now generates/defaults the teleport properties in the existing runtime `config/justteams.properties` file and exposes typed getters.

`TeamCommand`, `TeamHomeGui`, and `TeamWarpGui` now hand approved teleport requests to the centralized controller instead of directly calling `ServerPlayerEntity.teleport(...)`.

The `/team warp` command additionally checks warp cooldown before looking up the warp, matching the verified 2.5.3 command ordering.

## Verification state

- Pinned API references were checked against Yarn 1.21.11 documentation for `ServerPlayerEntity.playSoundToPlayer`, `ServerWorld.spawnParticles`, `ParticleTypes`, and `SoundEvents`.
- No clean Gradle build has been run yet; the project's handoff protocol reserves the clean build for the final Round 10 checkpoint.
- Runtime behavior still needs in-game verification, especially the exact warmup timing, movement cancellation, cross-dimension teleport, cooldown persistence while a player remains online/offline, and configurable effects.

## Known deliberate/remaining details

- The Fabric implementation currently supports the exact 2.5.3 shipped teleport sound/particle names through typed mappings rather than a full generic dynamic effect registry.
- The 2.5.3 `effects.sounds.success` property is preserved in Fabric configuration, but the verified teleport path uses the `TELEPORT` sound (`effects.sounds.teleport`), so the success sound setting is not part of this teleport call chain.
- Full Paper `MessageManager`/`EffectsUtil` infrastructure is not being recreated wholesale; only the verified teleport behavior is being ported into the existing Fabric architecture.

## Next resume point

Do not clean-build yet. First audit the current teleport controller against the remaining 2.5.3 edge cases and inspect any other verified teleport entry points that can bypass `TeamCommand`, `TeamHomeGui`, or `TeamWarpGui`. Then update `PROJECT_STATUS.md` during the next status-file maintenance pass before Round 10.
