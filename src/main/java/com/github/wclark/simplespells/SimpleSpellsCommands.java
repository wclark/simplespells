package com.github.wclark.simplespells;

import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.arguments.BoolArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class SimpleSpellsCommands {
    private static final String TARGETS_ARGUMENT = "targets";
    private static final String VISIBLE_ARGUMENT = "visible";
    private static final String HIDDEN_TEAM_NAME = "ss_hide_names";
    private static final String PREVIOUS_TEAM_KEY = SimpleSpells.MODID + ":previousNametagTeam";

    private SimpleSpellsCommands() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.register(new SimpleSpellsCommands());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("simple_spells")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("show_nametags")
                        .then(Commands.argument(VISIBLE_ARGUMENT, BoolArgumentType.bool())
                                .executes(context -> setNametagVisibility(
                                        context.getSource(),
                                        context.getSource().getEntityOrException(),
                                        BoolArgumentType.getBool(context, VISIBLE_ARGUMENT))))
                        .then(Commands.argument(TARGETS_ARGUMENT, EntityArgument.entities())
                                .then(Commands.argument(VISIBLE_ARGUMENT, BoolArgumentType.bool())
                                        .executes(context -> setNametagVisibility(
                                                context.getSource(),
                                                EntityArgument.getEntities(context, TARGETS_ARGUMENT),
                                                BoolArgumentType.getBool(context, VISIBLE_ARGUMENT)))))));
    }

    private static int setNametagVisibility(CommandSourceStack source, Entity target, boolean visible) {
        return setNametagVisibility(source, List.of(target), visible);
    }

    private static int setNametagVisibility(CommandSourceStack source, Collection<? extends Entity> targets, boolean visible) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        if (!visible) {
            ensureHiddenTeam(scoreboard);
        }

        for (Entity target : targets) {
            if (visible) {
                showNametag(scoreboard, target);
            } else {
                hideNametag(scoreboard, target);
            }
        }

        source.sendSuccess(
                () -> Component.literal((visible ? "Showing" : "Hiding") + " nametags for " + targets.size() + " " + describeEntityCount(targets.size()) + "."),
                true);
        return targets.size();
    }

    private static void hideNametag(Scoreboard scoreboard, Entity target) {
        String scoreboardName = target.getScoreboardName();
        PlayerTeam currentTeam = scoreboard.getPlayersTeam(scoreboardName);
        if (currentTeam == null) {
            target.getPersistentData().putString(PREVIOUS_TEAM_KEY, "");
        } else if (!HIDDEN_TEAM_NAME.equals(currentTeam.getName()) && !target.getPersistentData().contains(PREVIOUS_TEAM_KEY)) {
            target.getPersistentData().putString(PREVIOUS_TEAM_KEY, currentTeam.getName());
        }

        target.setCustomNameVisible(false);
        scoreboard.addPlayerToTeam(scoreboardName, scoreboard.getPlayerTeam(HIDDEN_TEAM_NAME));
    }

    private static void showNametag(Scoreboard scoreboard, Entity target) {
        if (!(target instanceof Player) && target.getCustomName() == null) {
            target.setCustomName(target.getType().getDescription());
        }
        target.setCustomNameVisible(true);
        restorePreviousTeam(scoreboard, target);
    }

    private static void restorePreviousTeam(Scoreboard scoreboard, Entity target) {
        String scoreboardName = target.getScoreboardName();
        PlayerTeam currentTeam = scoreboard.getPlayersTeam(scoreboardName);
        CompoundTag persistentData = target.getPersistentData();

        if (persistentData.contains(PREVIOUS_TEAM_KEY)) {
            String previousTeamName = persistentData.getString(PREVIOUS_TEAM_KEY);
            persistentData.remove(PREVIOUS_TEAM_KEY);
            if (currentTeam != null && HIDDEN_TEAM_NAME.equals(currentTeam.getName())) {
                if (previousTeamName.isEmpty()) {
                    scoreboard.removePlayerFromTeam(scoreboardName);
                } else {
                    PlayerTeam previousTeam = scoreboard.getPlayerTeam(previousTeamName);
                    if (previousTeam == null) {
                        scoreboard.removePlayerFromTeam(scoreboardName);
                    } else {
                        scoreboard.addPlayerToTeam(scoreboardName, previousTeam);
                    }
                }
            }
        } else if (currentTeam != null && HIDDEN_TEAM_NAME.equals(currentTeam.getName())) {
            scoreboard.removePlayerFromTeam(scoreboardName);
        }
    }

    private static PlayerTeam ensureHiddenTeam(Scoreboard scoreboard) {
        PlayerTeam hiddenTeam = scoreboard.getPlayerTeam(HIDDEN_TEAM_NAME);
        if (hiddenTeam == null) {
            hiddenTeam = scoreboard.addPlayerTeam(HIDDEN_TEAM_NAME);
            hiddenTeam.setNameTagVisibility(Team.Visibility.NEVER);
        } else if (hiddenTeam.getNameTagVisibility() != Team.Visibility.NEVER) {
            hiddenTeam.setNameTagVisibility(Team.Visibility.NEVER);
        }
        return hiddenTeam;
    }

    private static String describeEntityCount(int count) {
        return count == 1 ? "entity" : "entities";
    }
}
