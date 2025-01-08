package net.solar_studios;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

class Battle {
    private final String id;
    private final Player owner;
    private final List<Player> players = new ArrayList<>();
    private final Map<Player, Integer> healthMap = new HashMap<>();
    private final List<Player> turnOrder = new ArrayList<>();
    private int currentTurnIndex = 0;

    private boolean started = false;
    private boolean locked = false;
    private final Map<Player, Scoreboard> playerScoreboards = new HashMap<>();

    public boolean isStarted() {
        return started;
    }

    public boolean isLocked()
    {
        return locked;
    }
    public void setLock()
    {
        locked = !locked;
    }

    public Battle(String id, Player owner) {
        this.id = id;
        this.owner = owner;
        addPlayer(owner);
    }

    public String getId() {
        return id;
    }

    public Player getOwner() {
        return owner;
    }

    public boolean addPlayer(Player player) {
        if (players.contains(player)) return false;
        players.add(player);
        healthMap.put(player, 100);
        createScoreboardForPlayer(player);
        updateScoreboard();
        return true;
    }

    public boolean containsPlayer(Player player) {
        return players.contains(player);
    }

    public void startCombat() {
        players.forEach(player -> turnOrder.add(player));
        Collections.shuffle(turnOrder);
        updateScoreboard();
    }

    public boolean isTurn(Player player) {
        return turnOrder.get(currentTurnIndex).equals(player);
    }

    public void dealDamage(Player target, int damage) {
        if (!isPlayerInBattle(target)) return;

        int newHealth = healthMap.get(target) - damage;
        healthMap.put(target, Math.max(0, newHealth));

        Bukkit.broadcastMessage(
                ChatColor.RED + target.getName() + " takes " + damage + " damage! Remaining health: " + newHealth
        );

        if (newHealth <= 0) {
            Bukkit.broadcastMessage(ChatColor.GOLD + target.getName() + " has been eliminated!");
            removePlayer(target);
            /*if (turnOrder.size() == 1) {
                Player winner = turnOrder.get(0);

                Bukkit.broadcastMessage(ChatColor.GREEN + winner.getName() + " wins the battle!");
            }*/
        }
    }


    public boolean isPlayerInBattle(Player player) {
        return healthMap.containsKey(player);
    }
    private void createScoreboardForPlayer(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();

        Objective objective = scoreboard.registerNewObjective("Combat", "dummy", ChatColor.GREEN + "Combat Turn Order");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        player.setScoreboard(scoreboard);
        playerScoreboards.put(player, scoreboard);
    }

    private void updateScoreboard() {
        for (Player player : players) {
            Scoreboard scoreboard = playerScoreboards.get(player);
            if (scoreboard == null) continue;

            Objective objective = scoreboard.getObjective("Combat");
            if (objective == null) continue;

            objective.getScoreboard().getEntries().forEach(scoreboard::resetScores);

            for (int i = 0; i < turnOrder.size(); i++) {
                Player currentPlayer = turnOrder.get(i);
                ChatColor color = (i == currentTurnIndex) ? ChatColor.YELLOW : ChatColor.GRAY;
                String entry = color + currentPlayer.getName();
                objective.getScore(entry).setScore(turnOrder.size() - i);
            }
        }
    }

    public List<Player> getPlayers() {
        return players;
    }

    public boolean removePlayer(Player player) {
        if (players.contains(player)) {
            players.remove(player);
            healthMap.remove(player);
            turnOrder.remove(player);
            playerScoreboards.remove(player);

            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            updateScoreboard();
            return true;
        }
        return false;
    }

    public String getBattleId() {
        return id;
    }

    public void advanceTurn()
    {
        if (currentTurnIndex == turnOrder.size() - 1)
        {
            currentTurnIndex = 0;
        }
        else {
            currentTurnIndex++;
        }
        updateScoreboard();
        Bukkit.broadcastMessage(ChatColor.BLUE + "It is " + turnOrder.get(currentTurnIndex).getName() + " turn.");
    }
}
