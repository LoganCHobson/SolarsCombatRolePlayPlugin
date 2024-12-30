package net.solar_studios;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

class Battle {
    private final String id;
    private final Player owner;
    private final List<Player> players = new ArrayList<>();
    private final Map<Player, Integer> healthMap = new HashMap<>();
    private final List<Player> turnOrder = new ArrayList<>();
    private int currentTurnIndex = 0;

    private boolean started = false;

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
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
        healthMap.put(player, 100); // Default health
        return true;
    }

    public boolean containsPlayer(Player player) {
        return players.contains(player);
    }

    public void startCombat() {
        players.forEach(player -> turnOrder.add(player));
        Collections.shuffle(turnOrder); // Random initiative
        updateScoreboard();
    }

    public boolean isTurn(Player player) {
        return turnOrder.get(currentTurnIndex).equals(player);
    }

    public void dealDamage(Player attacker, Player target, int damage) {
        int newHealth = healthMap.get(target) - damage;
        healthMap.put(target, Math.max(0, newHealth));
        Bukkit.broadcastMessage(ChatColor.RED + target.getName() + " takes " + damage + " damage! Remaining health: " + newHealth);
        nextTurn();
    }

    private void nextTurn() {
        currentTurnIndex = (currentTurnIndex + 1) % turnOrder.size();
        updateScoreboard();
    }

    private void updateScoreboard() {
        // Scoreboard logic here
    }

    public List<Player> getPlayers() {

   return players;
    }

    public boolean removePlayer(Player player) {
       if(players.contains(player))
       {
           players.remove(player);
           return true;
       }
       else {
           return false;
       }
    }

    public String getBattleId() {
        return id;
    }
}
