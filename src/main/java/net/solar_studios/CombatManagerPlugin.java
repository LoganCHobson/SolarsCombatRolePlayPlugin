package net.solar_studios;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.util.*;

public class CombatManagerPlugin extends JavaPlugin implements TabExecutor {

    private final Map<String, Battle> battles = new HashMap<>();
    private final Random random = new Random();

    @Override
    public void onEnable() {
        this.getCommand("createcombat").setExecutor(this);
        this.getCommand("joincombat").setExecutor(this);
        this.getCommand("startcombat").setExecutor(this);
        this.getCommand("damage").setExecutor(this);
        this.getCommand("leavecombat").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }
        Player player = (Player) sender;

        switch (command.getName().toLowerCase()) {
            case "createcombat":
                return createCombat(player);
            case "joincombat":
                return joinCombat(player, args);
            case "startcombat":
                return startCombat(player);
            case "damage":
                return dealDamage(player, args);
            case "leavecombat":
                return leaveCombat(player);
            default:
                return false;
        }
    }

    private boolean createCombat(Player player) {
        String battleId = generateBattleId();
        if (battles.containsKey(battleId)) {
            player.sendMessage(ChatColor.RED + "Error: Battle ID already exists!");
            return true;
        }

        for (Battle battle : battles.values()) {
            if (battle.containsPlayer(player)) {
                player.sendMessage(ChatColor.RED + "You are already in a battle! Finish or leave your current battle first.");
                return true;
            }
        }

        Battle battle = new Battle(battleId, player);
        battles.put(battleId, battle);
        player.sendMessage(ChatColor.GREEN + "Combat created! Your battle ID is: " + ChatColor.YELLOW + battleId);
        return true;
    }

    private boolean joinCombat(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /joincombat <battleId>");
            return true;
        }
        String battleId = args[0];
        Battle battle = battles.get(battleId);
        if (battle == null) {
            player.sendMessage(ChatColor.RED + "Battle not found with ID: " + battleId);
            return true;
        }
        if (battle.addPlayer(player)) {
            player.sendMessage(ChatColor.GREEN + "You have joined the combat!");
        } else {
            player.sendMessage(ChatColor.RED + "You are already in this combat!");
        }
        return true;
    }

    private boolean leaveCombat(Player player) {
        Battle battle = getBattleByPlayer(player);
        if (battle == null) {
            player.sendMessage(ChatColor.RED + "You are not in any combat!");
            return true;
        }

        if (battle.removePlayer(player)) {
            player.sendMessage(ChatColor.GREEN + "You have left the combat!");
            if (battle.getPlayers().isEmpty()) {
                battles.remove(battle.getBattleId());
                player.sendMessage(ChatColor.YELLOW + "The battle has ended because no players remain.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "You are not part of this battle!");
        }
        return true;
    }

    private boolean startCombat(Player player) {
        Battle battle = getBattleByPlayer(player);
        if (battle == null) {
            player.sendMessage(ChatColor.RED + "You are not part of any combat!");
            return true;
        }
        if (!battle.getOwner().equals(player)) {
            player.sendMessage(ChatColor.RED + "Only the battle creator can start the combat!");
            return true;
        }
        if (battle.isStarted()) {
            player.sendMessage(ChatColor.RED + "This battle has already started!");
            return true;
        }
        if (battle.getPlayers().size() < 2) {
            player.sendMessage(ChatColor.RED + "You need at least two players to start the battle!");
            return true;
        }
        battle.startCombat();
        player.sendMessage(ChatColor.GREEN + "Combat has started! Turn order has been rolled.");
        return true;
    }

    private boolean dealDamage(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /damage <playername> <target> <damage>");
            return true;
        }

        String targetName = args[1];
        int damage;
        try {
            damage = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Damage must be a number!");
            return true;
        }

        Battle battle = getBattleByPlayer(player);
        if (battle == null) {
            player.sendMessage(ChatColor.RED + "You are not in a combat!");
            return true;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !battle.containsPlayer(target)) {
            player.sendMessage(ChatColor.RED + "Target player is not in this combat!");
            return true;
        }

        if (!battle.isTurn(player)) {
            player.sendMessage(ChatColor.RED + "It's not your turn!");
            return true;
        }

        battle.dealDamage(player, target, damage);
        return true;
    }

    private Battle getBattleByPlayer(Player player) {
        for (Battle battle : battles.values()) {
            if (battle.containsPlayer(player)) {
                return battle;
            }
        }
        return null;
    }

    private String generateBattleId() {
        return Integer.toHexString(random.nextInt(0xFFFF)).toUpperCase();
    }
}
