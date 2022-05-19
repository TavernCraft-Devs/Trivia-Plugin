/*
 * Trivia by MarCarrot, 2020
 */

package me.marcarrots.trivia.commands;

import me.marcarrots.trivia.Trivia;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class SubCommand {

    protected Trivia trivia;

    public SubCommand(Trivia trivia) {
        this.trivia = trivia;
    }

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getSyntax();

    public abstract String getPermission();

    public abstract boolean perform(CommandSender commandSender, String[] args);

    public abstract List<String> getTabSuggester(CommandSender commandSender, int argsLength);

    public boolean hasPermission(Player player) {
        return player.hasPermission(getPermission());
    }

}
