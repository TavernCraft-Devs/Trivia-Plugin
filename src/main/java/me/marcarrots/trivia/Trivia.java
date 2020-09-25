/*
 * Trivia by MarCarrot, 2020
 */

package me.marcarrots.trivia;

import me.marcarrots.trivia.api.MetricsLite;
import me.marcarrots.trivia.api.UpdateChecker;
import me.marcarrots.trivia.data.FileManager;
import me.marcarrots.trivia.listeners.ChatEvent;
import me.marcarrots.trivia.listeners.InventoryClick;
import me.marcarrots.trivia.listeners.PlayerJoin;
import me.marcarrots.trivia.menu.PlayerMenuUtility;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;
import java.util.logging.Level;

public final class Trivia extends JavaPlugin {

    private static Economy econ = null;
    private final QuestionHolder questionHolder = new QuestionHolder();
    private final ChatEvent chatEvent = new ChatEvent(this);
    private final PlayerJoin playerJoin = new PlayerJoin(this);
    private final HashMap<Player, PlayerMenuUtility> playerMenuUtilityMap = new HashMap<>();
    private boolean schedulingEnabled;
    private int automatedTime;
    private int automatedPlayerReq;
    private long nextAutomatedTime;
    private int largestQuestionNum = 0;
    private int schedulerTask;
    private Rewards[] rewards;
    private Game game;
    private FileManager questionsFile;
    private FileManager messagesFile;
    private FileManager rewardsFile;

    private String updateNotice = null;

    public static Economy getEcon() {
        return econ;
    }

    public FileManager getQuestionsFile() {
        return questionsFile;
    }

    public FileManager getRewardsFile() {
        return rewardsFile;
    }

    public boolean isSchedulingEnabled() {
        return schedulingEnabled;
    }

    public int getAutomatedPlayerReq() {
        return automatedPlayerReq;
    }

    public Rewards[] getRewards() {
        return rewards;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void clearGame() {
        game = null;
    }

    public long getNextAutomatedTime() {
        return nextAutomatedTime;
    }

    public String getUpdateNotice() {
        return updateNotice;
    }

    public PlayerMenuUtility getPlayerMenuUtility(Player player) {
        PlayerMenuUtility playerMenuUtility;
        if (playerMenuUtilityMap.containsKey(player)) {
            return playerMenuUtilityMap.get(player);
        } else {
            playerMenuUtility = new PlayerMenuUtility(getConfig(), player);
            playerMenuUtilityMap.put(player, playerMenuUtility);
            return playerMenuUtility;
        }
    }

    @Override
    public void onEnable() {
        loadConfig();
        this.questionsFile = new FileManager(this, "questions.yml");
        this.messagesFile = new FileManager(this, "messages.yml");
        this.rewardsFile = new FileManager(this, "rewards.yml");
        loadMessages();
        generateRewards();
        for (String questionNum : questionsFile.getData().getKeys(false)) {
            try {
                if (Integer.parseInt(questionNum) > largestQuestionNum)
                    largestQuestionNum = Integer.parseInt(questionNum);
            } catch (NumberFormatException e) {
                Bukkit.getLogger().log(Level.WARNING, String.format("The key '%s' is invalid and cannot be interpreted.", questionNum));
            }
        }

        readQuestions();
        game = null;
        new MetricsLite(this, 7912);
        getServer().getPluginManager().registerEvents(new InventoryClick(), this);
        getServer().getPluginManager().registerEvents(chatEvent, this);
        getServer().getPluginManager().registerEvents(playerJoin, this);
        Objects.requireNonNull(getCommand("trivia")).setExecutor(new TriviaCommand(this, questionHolder));
        if (!setupEconomy()) {
            Bukkit.getLogger().info("No vault has been detected, disabling vault features...");
        }

        new UpdateChecker(this, 80401).getVersion(version -> {
//            Bukkit.getLogger().info("Version available: " + version + ", Current version: " + getDescription().getVersion());
            if (!getDescription().getVersion().equalsIgnoreCase(version)) {
                updateNotice = String.format("%s - There is a new version available for Trivia (new version: %s, current version: %s)! Get it at: %s.",
                        ChatColor.AQUA + "[Trivia!]" + ChatColor.YELLOW,
                        ChatColor.GREEN + version + ChatColor.YELLOW,
                        ChatColor.GREEN + getDescription().getVersion() + ChatColor.YELLOW,
                        ChatColor.WHITE + "https://www.spigotmc.org/resources/trivia-easy-to-setup-game-%C2%BB-custom-rewards-%C2%BB-in-game-gui-menus-more.80401/" + ChatColor.YELLOW);
                Bukkit.getLogger().info(updateNotice);
            }
        });

        configUpdater();

    }

    @Override
    public void onDisable() {

    }

    public void loadMessages() {
        if (getConfig().contains("Messages")) {
            Bukkit.getLogger().log(Level.INFO, "[Trivia] Migrating old message data to new data...");
            List<String> messageKeys = Arrays.asList(
                    "Trivia Start",
                    "Trivia Over",
                    "Winner Line",
                    "Winner List",
                    "No Winners",
                    "Solved Message",
                    "Question Time Up",
                    "Question Display",
                    "Question Skipped"
            );

            for (String key : messageKeys) {
                messagesFile.getData().set(key, getConfig().getString("Messages." + key, ""));
                messagesFile.saveData();
            }
            getConfig().set("Messages", null);
            saveConfig();
        }
        messagesFile.reloadFiles();
        Lang.setFile(messagesFile.getData());
    }

    public void loadConfig() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        schedulingEnabled = getConfig().getBoolean("Scheduled games", false);
        automatedTime = getConfig().getInt("Scheduled games interval", 60);
        automatedPlayerReq = getConfig().getInt("Scheduled games minimum players", 6);
        automatedSchedule();
    }

    public void writeQuestions(String question, List<String> answer, String author) {
        HashMap<String, Object> questionMap = new HashMap<>();
        questionMap.put("question", question);
        questionMap.put("answer", answer);
        if (author != null) {
            questionMap.put("author", author);
        }
        questionsFile.getData().createSection(String.valueOf(++largestQuestionNum), questionMap);
        questionsFile.saveData();
    }

    public void readQuestions() {
        questionHolder.clear();

        if (getConfig().contains("Questions and Answers")) {
            Bukkit.getLogger().log(Level.INFO, "[Trivia] Migrating old question data to new data...");
            List<String> unparsedQuestions = getConfig().getStringList("Questions and Answers");
            if (unparsedQuestions.size() != 0)
                for (String item : unparsedQuestions) {
                    int posBefore = item.indexOf("/$/");
                    if (posBefore == -1)
                        continue;
                    int posAfter = posBefore + 3;
                    writeQuestions(item.substring(0, posBefore).trim(), Collections.singletonList(item.substring(posAfter).trim()), null);
                }
            getConfig().set("Questions and Answers", null);
            saveConfig();
        }

        questionsFile.reloadFiles();
        questionsFile.getData().getKeys(false).forEach(key -> {
            try {
                Question triviaQuestion = new Question();
                triviaQuestion.setId(Integer.parseInt(key));
                triviaQuestion.setQuestion(questionsFile.getData().getString(key + ".question"));
                triviaQuestion.setAnswer(questionsFile.getData().getStringList(key + ".answer"));
                triviaQuestion.setAuthor(questionsFile.getData().getString(key + ".author"));
                this.questionHolder.add(triviaQuestion);
            } catch (NumberFormatException | NullPointerException e) {
                Bukkit.getLogger().log(Level.SEVERE, String.format("Error with interpreting '%s': Invalid ID. (%s)", key, e.getMessage()));
            }
        });
    }

    private void generateRewards() {
        loadRewards();
        int rewardAmt = 3;
        rewards = new Rewards[rewardAmt];
        for (int i = 0; i < rewardAmt; i++) {
            rewards[i] = new Rewards(this, i);
        }
    }

    private void loadRewards() {
        if (getConfig().contains("Rewards")) {
            for (int i = 0; i < 3; i++) {
                if (getConfig().contains("Rewards." + i)) {
                    Bukkit.getLogger().log(Level.INFO, "[Trivia] Migrating old rewards data to new data...");
                    rewardsFile.getData().set(i + ".Money", getConfig().getDouble("Rewards." + i + ".Money"));
                    rewardsFile.getData().set(i + ".Experience", getConfig().getDouble("Rewards." + i + ".Experience"));
                    rewardsFile.getData().set(i + ".Message", getConfig().getString("Rewards." + i + ".Message"));
                    rewardsFile.getData().set(i + ".Items", getConfig().getList("Rewards." + i + ".Items"));
                    rewardsFile.saveData();
                }
            }
            getConfig().set("Rewards", null);
            saveConfig();
        }
        messagesFile.reloadFiles();
    }

    private boolean setupEconomy() {
        if (!vaultEnabled()) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return true;
    }

    public boolean vaultEnabled() {
        return getServer().getPluginManager().getPlugin("Vault") != null;
    }

    private void automatedSchedule() {
        if (!schedulingEnabled) {
            return;
        }
        setNextAutomatedTime();
        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.cancelTask(schedulerTask);
        schedulerTask = scheduler.scheduleSyncRepeatingTask(this, () -> {
            if (Bukkit.getOnlinePlayers().size() < automatedPlayerReq) {
                return;
            }
            Bukkit.getLogger().info("Automated Trivia Beginning...");
            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "trivia start");
            setNextAutomatedTime();
        }, automatedTime * 20 * 60, automatedTime * 20 * 60);

    }

    private void setNextAutomatedTime() {
        nextAutomatedTime = System.currentTimeMillis() + (automatedTime * 60 * 1000);
    }

    private void configUpdater() {
        HashMap<String, Object> newKeys = new HashMap<>();
        int currentConfigVersion = getConfig().getInt("Config Version");
        // if they have version 1 of the config...
        Bukkit.getLogger().info("Current config version: " + currentConfigVersion);
        if (currentConfigVersion <= 1) {
            newKeys.put("Scheduled games", schedulingEnabled);
            newKeys.put("Scheduled games interval", automatedTime);
            newKeys.put("Scheduled games minimum players", automatedPlayerReq);
            newKeys.put("Time between rounds", 2);
            currentConfigVersion = 2;
        }

        if (currentConfigVersion == 2) {
            newKeys.put(Lang.SKIP.getPath(), Lang.SKIP.getDefault());
        }

        if (newKeys.isEmpty()) {
            return;
        }

        // iterate through all the new keys
        for (Map.Entry<String, Object> entry : newKeys.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            getConfig().set(key, value);
            saveConfig();
        }

        getConfig().set("Config Version", 4);
        saveConfig();
    }

    public static String getElapsedTime(long time) {

        long durationInMillis = time - System.currentTimeMillis();

        if (durationInMillis < 0) {
            durationInMillis *= -1;
        }

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = durationInMillis / daysInMilli;
        durationInMillis = durationInMillis % daysInMilli;

        long elapsedHours = durationInMillis / hoursInMilli;
        durationInMillis = durationInMillis % hoursInMilli;

        long elapsedMinutes = durationInMillis / minutesInMilli;
        durationInMillis = durationInMillis % minutesInMilli;

        long elapsedSeconds = durationInMillis / secondsInMilli;

        StringBuilder stringBuilder = new StringBuilder();

        if (elapsedDays > 0) {
            stringBuilder.append(String.format("%02d days, ", elapsedDays));
        }
        if (elapsedHours > 0) {
            stringBuilder.append(String.format("%02d hours, ", elapsedHours));
        }
        if (elapsedMinutes > 0) {
            stringBuilder.append(String.format("%02d minutes, ", elapsedMinutes));
        }
        stringBuilder.append(String.format("%d seconds", elapsedSeconds));
        return stringBuilder.toString();

    }
}
