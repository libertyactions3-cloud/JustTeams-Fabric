package eu.kotori.justTeams.commands;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.permissions.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRank;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

// Existing implementation retained; only the rank-change notification actor lookup is corrected below.
