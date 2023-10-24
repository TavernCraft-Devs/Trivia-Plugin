package me.marcarrots.triviatreasure.listeners;

import com.gmail.nossr50.api.ChatAPI;
import me.marcarrots.triviatreasure.TriviaTreasure;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class ChatEvent implements Listener {

    private final TriviaTreasure triviaTreasure;

    public ChatEvent(TriviaTreasure triviaTreasure) {
        this.triviaTreasure = triviaTreasure;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {

        if (triviaTreasure.getGame() == null) {
            return;
        }

        // ignore messages in mcmmo party chat
        if (ChatAPI.isUsingPartyChat(event.getPlayer()) || ChatAPI.isUsingAdminChat(event.getPlayer())) {
            return;
        }

        // run on main thread
        new BukkitRunnable() {
            @Override
            public void run() {
                triviaTreasure.getGame().playerAnswer(event);
            }
        }.runTask(triviaTreasure);

    }

}
