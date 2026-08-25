package eu.kotori.justTeams;

import eu.kotori.justTeams.chat.TeamChatEvents;
import eu.kotori.justTeams.commands.TeamCommand;
import eu.kotori.justTeams.commands.TeamCreationCommandExtensions;
import eu.kotori.justTeams.commands.TeamInfoCommandExtensions;
import eu.kotori.justTeams.commands.TeamLeaderboardCommandExtensions;
import eu.kotori.justTeams.commands.TeamOwnershipCommandExtensions;
import eu.kotori.justTeams.commands.TeamWarpCommandExtensions;
import eu.kotori.justTeams.commands.TeamWarpPasswordPromptCommandExtension;
import eu.kotori.justTeams.config.JustTeamsConfig;
import eu.kotori.justTeams.economy.EconomyProvider;
import eu.kotori.justTeams.economy.ItemEconomyProvider;
import eu.kotori.justTeams.gameplay.TeamFriendlyFire;
import eu.kotori.justTeams.gameplay.TeamStatsEvents;
import eu.kotori.justTeams.gui.TeamEnderChestGui;
import eu.kotori.justTeams.permission.LuckPermsPermissionService;
import eu.kotori.justTeams.permission.PermissionService;
import eu.kotori.justTeams.storage.TeamStorage;
import eu.kotori.justTeams.team.GlowManager;
import eu.kotori.justTeams.team.TeamManager;
import eu.kotori.justTeams.team.TeamTeleportManager;
import eu.kotori.justTeams.util.ChatInputEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class JustTeamsFabric implements ModInitializer {
    public static final String MOD_ID = "justteams";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static TeamManager teamManager;
    private static TeamStorage teamStorage;
    private static JustTeamsConfig config;
    private static PermissionService permissionService;
    private static GlowManager glowManager;
    private static TeamTeleportManager teleportManager;
    private static EconomyProvider economyProvider;

    @Override
    public void onInitialize() {
        try {
            config = new JustTeamsConfig(FabricLoader.getInstance().getConfigDir());
        } catch (IOException exception) {
            throw new RuntimeException("Unable to load JustTeams configuration", exception);
        }

        teamManager = new TeamManager();
        teamStorage = new TeamStorage();
        permissionService = createPermissionService();
        glowManager = new GlowManager();
        teleportManager = new TeamTeleportManager();
        economyProvider = new ItemEconomyProvider();

        ServerLifecycleEvents.SERVER_STARTING.register(this::loadTeamData);
        ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> saveTeamData(server, false));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> saveTeamData(server, true));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> TeamEnderChestGui.handleDisconnect(handler.getPlayer()));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TeamCommand.register(dispatcher);
            TeamCreationCommandExtensions.register(dispatcher);
            TeamInfoCommandExtensions.register(dispatcher);
            TeamWarpCommandExtensions.register(dispatcher);
            TeamWarpPasswordPromptCommandExtension.register(dispatcher);
            TeamOwnershipCommandExtensions.register(dispatcher);
            TeamLeaderboardCommandExtensions.register(dispatcher);
        });
        ChatInputEvents.register();
        TeamChatEvents.register();
        TeamFriendlyFire.register();
        TeamStatsEvents.register();
        LOGGER.info("JustTeams Fabric core initialized");
    }

    private static PermissionService createPermissionService() {
        if (FabricLoader.getInstance().isModLoaded("luckperms")) {
            try {
                LOGGER.info("LuckPerms detected; enabling JustTeams LuckPerms permissions");
                return new LuckPermsPermissionService();
            } catch (RuntimeException exception) {
                LOGGER.warn("LuckPerms was detected but its API could not be initialized; using default JustTeams permissions", exception);
            }
        }
        return PermissionService.defaults();
    }

    private void loadTeamData(MinecraftServer server) {
        try {
            teamStorage.load(teamManager);
            LOGGER.info("Loaded {} team(s) including persistent bank inventories", teamManager.size());
        } catch (IOException exception) {
            throw new RuntimeException("Unable to load JustTeams data", exception);
        }
    }

    private void saveTeamData(MinecraftServer server, boolean logSuccess) {
        try {
            teamStorage.save(teamManager);
            if (logSuccess) LOGGER.info("Saved {} team(s) including persistent bank inventories", teamManager.size());
        } catch (IOException exception) {
            LOGGER.error("Unable to save JustTeams data", exception);
        }
    }

    public static TeamManager teams() {
        if (teamManager == null) throw new IllegalStateException("JustTeams has not initialized");
        return teamManager;
    }

    public static TeamStorage storage() {
        if (teamStorage == null) throw new IllegalStateException("JustTeams has not initialized");
        return teamStorage;
    }

    public static JustTeamsConfig config() {
        if (config == null) throw new IllegalStateException("JustTeams has not initialized");
        return config;
    }

    public static PermissionService permissions() {
        if (permissionService == null) throw new IllegalStateException("JustTeams has not initialized");
        return permissionService;
    }

    public static GlowManager glow() {
        if (glowManager == null) throw new IllegalStateException("JustTeams has not initialized");
        return glowManager;
    }

    public static TeamTeleportManager teleports() {
        if (teleportManager == null) throw new IllegalStateException("JustTeams has not initialized");
        return teleportManager;
    }

    public static EconomyProvider economy() {
        if (economyProvider == null) throw new IllegalStateException("JustTeams has not initialized");
        return economyProvider;
    }
}
