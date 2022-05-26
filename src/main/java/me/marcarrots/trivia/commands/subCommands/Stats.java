/*
 * Trivia by MarCarrot, 2020
 */

package me.marcarrots.trivia.commands.subCommands;

import me.marcarrots.trivia.PlayerDataContainer;
import me.marcarrots.trivia.Trivia;
import me.marcarrots.trivia.commands.SubCommand;
import me.marcarrots.trivia.language.Lang;
import me.marcarrots.trivia.language.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.NumberFormat;

public class Stats extends SubCommand {
    public Stats(Trivia plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return Lang.COMMANDS_HELP_STATS.format_single();
    }

    @Override
    public String getSyntax() {
        return "stats";
    }

    @Override
    public String getPermission() {
        return "trivia.player";
    }

    @Override
    public boolean perform(CommandSender commandSender, String[] args) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            PlayerDataContainer stats = trivia.getStats();
            int[] gamesWon = trivia.getStats().getGamesWon(player);
            String border = Lang.BORDER.format_single();
            player.sendMessage(border);

            player.sendMessage(Lang.COMMANDS_STATS_NAME.format_single(new Placeholder.PlaceholderBuilder().player(player).build()));
            player.sendMessage(Lang.COMMANDS_STATS_PARTICIPATED.format_single().replace("%val%", String.valueOf(stats.getGamesParticipated(player))));
            player.sendMessage(Lang.COMMANDS_STATS_ROUNDS_WON.format_single().replace("%val%", String.valueOf(stats.getRoundsWon(player))));
            player.sendMessage(Lang.COMMANDS_STATS_VICTORIES.format_single());
            player.sendMessage(Lang.COMMANDS_STATS_FIRST.format_single().replace("%val%", String.valueOf(gamesWon[0])));
            player.sendMessage(Lang.COMMANDS_STATS_SECOND.format_single().replace("%val%", String.valueOf(gamesWon[1])));
            player.sendMessage(Lang.COMMANDS_STATS_THIRD.format_single().replace("%val%", String.valueOf(gamesWon[2])));
            player.sendMessage(Lang.COMMANDS_STATS_ENCHANTING.format_single().replace("%val%", NumberFormat.getIntegerInstance().format(stats.getExperienceWon(player))));
            if (trivia.isVaultEnabled()) {
                player.sendMessage(Lang.COMMANDS_STATS_MONEY.format_single().replace("%val%", NumberFormat.getCurrencyInstance().format(stats.getMoneyWon(player))));
            }
            player.sendMessage(border);
        } else {
            commandSender.sendMessage("This command is for players only.");
        }
        return false;
    }

}
