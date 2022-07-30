/*
 * Trivia by MarCarrot, 2020
 */

package me.marcarrots.trivia.commands.subCommands;

import me.marcarrots.trivia.Game;
import me.marcarrots.trivia.Trivia;
import me.marcarrots.trivia.commands.SubCommand;
import me.marcarrots.trivia.language.Lang;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Start extends SubCommand {

    public Start(Trivia plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public String getDescription() {
        return Lang.COMMANDS_HELP_START.format_single();
    }

    @Override
    public String getSyntax() {
        return "start <# of rounds>";
    }

    @Override
    public String getPermission() {
        return "trivia.admin";
    }

    @Override
    public boolean perform(CommandSender commandSender, String[] args) {
        if (trivia.getGame() != null) {
            commandSender.sendMessage(Lang.COMMANDS_ERROR_GAME_IN_PROGRESS.format_single());
            return false;
        }

        String param1 = null;
        if (args.length == 2) {
            param1 = args[1];
        }

        int setRounds = trivia.getConfig().getInt("Default rounds", 10);
        if (param1 != null && param1.trim().matches("-?\\d+")) {
            setRounds = Integer.parseInt(param1);
        }
        try {
            long timePerQuestion = trivia.getConfig().getLong("Default time per round", 10L);
            boolean doRepetition = false;
            Game game = new Game(trivia, commandSender, timePerQuestion, setRounds, doRepetition);
            trivia.setGame(game);
            trivia.getGame().start();
        } catch (IllegalAccessException e) {
            commandSender.sendMessage(ChatColor.RED + e.getMessage());
        }
        return false;
    }

}
