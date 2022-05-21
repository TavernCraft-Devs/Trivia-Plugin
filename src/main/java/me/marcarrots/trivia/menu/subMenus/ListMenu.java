package me.marcarrots.trivia.menu.subMenus;

import me.marcarrots.trivia.Question;
import me.marcarrots.trivia.Trivia;
import me.marcarrots.trivia.language.Lang;
import me.marcarrots.trivia.language.Placeholder;
import me.marcarrots.trivia.menu.ConversationPrompt;
import me.marcarrots.trivia.menu.MenuType;
import me.marcarrots.trivia.menu.PaginatedMenu;
import me.marcarrots.trivia.menu.PromptType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ListMenu extends PaginatedMenu {
    public ListMenu(Trivia trivia, Player player) {
        super(trivia, player);
    }

    public String getMenuName() {
        return Lang.LIST_MENU_TITLE.format_single(new Placeholder.PlaceholderBuilder()
                .val(String.valueOf(page + 1))
                .build()
        );
    }


    public int getSlots() {
        return 54;
    }

    public void handleMenuClick(InventoryClickEvent event) {
        event.setCancelled(true);

        Material type = Objects.requireNonNull(event.getCurrentItem()).getType();
        Player player = (Player) event.getWhoClicked();

        List<Question> questionList = trivia.getQuestionHolder().getTriviaQuestionList();
        if (type == Material.EMERALD) {
            Conversation conversation = new ConversationFactory(trivia).withFirstPrompt(new ConversationPrompt(PromptType.NEW_ENTRY_QUESTION, trivia)).withLocalEcho(false).withTimeout(60).buildConversation(player);
            conversation.begin();
            player.closeInventory();
        } else if (type == Material.PAPER) {
            trivia.getPlayerMenuUtility(player).setPreviousMenu(MenuType.LIST_MENU);
            trivia.getPlayerMenuUtility(player).setQuestion(trivia.getQuestionHolder().getQuestion(event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(trivia.getNamespacedQuestionKey(), PersistentDataType.INTEGER)));
            (new ViewMenu(trivia, player)).open();
        } else if (event.getCurrentItem().equals(CLOSE)) {
            player.closeInventory();
        } else if (type == Material.DARK_OAK_BUTTON) {
            if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Previous Page")) {
                if (page == 0) {
                    player.sendMessage(ChatColor.GRAY + "You are already on the first page.");
                } else {
                    page--;
                    open();
                }
            } else if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Next Page")) {
                if (index + 1 < questionList.size()) {
                    page++;
                    open();
                } else {
                    player.sendMessage(ChatColor.GRAY + "You are already on the last page.");
                }
            }
        } else if (type == Material.ARROW) {
            trivia.getPlayerMenuUtility(player).setPreviousMenu(MenuType.LIST_MENU);
            (new MainMenu(trivia, player)).open();
        }
    }

    public void handleMenuClose(InventoryCloseEvent event) {
    }

    public void setMenuItems() {
        addMenuBorder();
        List<Question> questionList = trivia.getQuestionHolder().getTriviaQuestionList();
        if (!questionList.isEmpty())
            for (int i = 0; i < getMaxItemsPerPage(); i++) {
                index = getMaxItemsPerPage() * page + i;
                if (index >= questionList.size()) {
                    break;
                }
                if (questionList.get(index) != null) {
                    Question question = questionList.get(index);
                    ItemStack questionItem = new ItemStack(Material.PAPER, question.getId() % 64);

                    List<String> loreWrapped = new ArrayList<>();

                    loreWrapped.add(Lang.LIST_MENU_QUESTION_LORE.format_single(new Placeholder.PlaceholderBuilder()
                            .val(ChatColor.YELLOW + question.getQuestionString())
                            .build()
                    ));
                    loreWrapped.add(Lang.LIST_MENU_ANSWER_LORE.format_single(new Placeholder.PlaceholderBuilder()
                            .val(ChatColor.YELLOW + question.getAnswerList().toString())
                            .build()
                    ));

                    if (question.getAuthor() != null) {
                        loreWrapped.add(Lang.LIST_MENU_AUTHOR_LORE.format_single(new Placeholder.PlaceholderBuilder()
                                .val(question.getAuthor())
                                .build()
                        ));
                    }

                    loreWrapped.add(ChatColor.RED + "Click to modify this question.");

                    ItemMeta questionMeta = questionItem.getItemMeta();
                    questionMeta.getPersistentDataContainer().set(trivia.getNamespacedQuestionKey(), PersistentDataType.INTEGER, question.getId());
                    questionItem.setItemMeta(questionMeta);

                    insertItem(-1, questionItem, Lang.LIST_MENU_QUESTION.format_single(new Placeholder.PlaceholderBuilder()
                                    .val(String.valueOf(question.getId()))
                                    .build()),
                            loreWrapped, false, true);
                }
            }
        insertItem(53, Material.EMERALD, Lang.LIST_MENU_NEW_QUESTION.format_single(), "", false, false);
        if (trivia.getPlayerMenuUtility(player).getPreviousMenu() != null) {
            ItemStack previousMenuItem = BACK;
            ItemMeta previousMenuMeta = previousMenuItem.getItemMeta();
            if (trivia.getPlayerMenuUtility(player).getPreviousMenu() == MenuType.MAIN_MENU)
                previousMenuMeta.setLore(Collections.singletonList("to main menu."));
            previousMenuItem.setItemMeta(previousMenuMeta);
            inventory.setItem(45, previousMenuItem);
        }
    }
}
