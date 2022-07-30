package me.marcarrots.trivia.menu.subMenus;

import me.marcarrots.trivia.Trivia;
import me.marcarrots.trivia.language.Lang;
import me.marcarrots.trivia.menu.Menu;
import me.marcarrots.trivia.menu.MenuType;
import me.marcarrots.trivia.utils.Elapsed;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Arrays;
import java.util.Objects;

public class MainMenu extends Menu {

    private int taskID = 0;

    public MainMenu(Trivia trivia, Player player) {
        super(trivia, player);
    }

    @Override
    public String getMenuName() {
        return Lang.MAIN_MENU_TITLE.format_single();
    }

    @Override
    public int getSlots() {
        return 36;
    }

    @Override
    public void handleMenuClick(InventoryClickEvent event) {
        event.setCancelled(true);

        Material type = Objects.requireNonNull(event.getCurrentItem()).getType();
        Player player = (Player) event.getWhoClicked();

        trivia.getPlayerMenuUtility(player).setPreviousMenu(MenuType.MAIN_MENU);
        if (type == Material.GREEN_TERRACOTTA) {
            new ParameterMenu(trivia, player).open();
        } else if (type == Material.RED_TERRACOTTA) {
            player.performCommand("trivia stop");
            player.closeInventory();
        } else if (type == Material.PAPER) {
            new ListMenu(trivia, player).open();
        } else if (type == Material.EMERALD) {
            new RewardsMainMenu(trivia, player).open();
        } else if (event.getCurrentItem().equals(CLOSE)) {
            player.closeInventory();
        }
    }

    @Override
    public void handleMenuClose(InventoryCloseEvent event) {
        Bukkit.getScheduler().cancelTask(taskID);
    }

    @Override
    public void setMenuItems() {

        BukkitScheduler scheduler = trivia.getServer().getScheduler();

        insertItem(13, Material.EMERALD, Lang.MAIN_MENU_REWARDS.format_single(),
                Lang.MAIN_MENU_REWARDS_DESCRIPTION.format_single(), false, true);

        insertItem(15, Material.PAPER, Lang.MAIN_MENU_LIST.format_single(),
                Arrays.asList(Lang.MAIN_MENU_LIST_DESCRIPTION.format_single(),
                        ChatColor.GREEN.toString() + trivia.getQuestionHolder().getSize() + " questions loaded."),
                false, true);

        new BukkitRunnable() {

            @Override
            public void run() {
                if (trivia.getGame() != null) {
                    insertItem(11, Material.RED_TERRACOTTA, ChatColor.DARK_RED + "Stop Trivia", ChatColor.RED +
                            "There's currently a game in progress! Click here to stop this current game.", false, true);
                } else {
                    insertItem(11, Material.GREEN_TERRACOTTA, Lang.MAIN_MENU_START.format_single(),
                            ChatColor.DARK_PURPLE + Lang.MAIN_MENU_START_DESCRIPTION.format_single(), false, true);
                }
                taskID = this.getTaskId();
            }

        }.runTaskTimer(trivia, 0, 100);

        if (trivia.getAutomatedGameManager().isSchedulingEnabled()) {
            scheduler.scheduleSyncRepeatingTask(trivia,
                    () -> insertItem(35,
                            Material.CLOCK,
                            ChatColor.GREEN + "Time Until Next Scheduled Game",
                            Arrays.asList(ChatColor.YELLOW + Elapsed.millisToElapsedTime(trivia.getAutomatedGameManager().getNextAutomatedTimeFromNow()).getElapsedFormattedString(), ChatColor.LIGHT_PURPLE + "Minimum players needed: " + trivia.getAutomatedGameManager().getAutomatedPlayerReq()),
                            false,
                            false),
                    0,
                    20);
        } else {
            insertItem(35,
                    Material.CLOCK,
                    ChatColor.RED + "Scheduled Games Not Enabled",
                    Arrays.asList(ChatColor.YELLOW + "Enable this feature through the " + ChatColor.UNDERLINE + "config.yml", ChatColor.YELLOW + "in order to automatically host games in intervals!"),
                    false,
                    false);
        }

        fillRest();

        inventory.setItem(31, CLOSE);


    }


}
