package net.solar_studios;

import net.solar_studios.CharacterSheetPlugin;
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
        this.getCommand("lockcombat").setExecutor(this);
        this.getCommand("removefromcombat").setExecutor(this);

        if (Bukkit.getPluginManager().getPlugin("SolarsRPCharacterSheetPlugin") != null) {
            getLogger().info("CharacterSheetPlugin is loaded!");
        }
        else {
            getLogger().info("CharacterSheetPlugin is NOT loaded! Defaulting values for all combats.");
        }
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
            case "lockcombat":
                return lockCombat(player);
            case "removefromcombat":
                return removeFromCombat(player, args);
            default:
                return false;
        }
    }

    public int getPlayerHealth(Player player) {

        CharacterSheetPlugin characterSheetPlugin = (CharacterSheetPlugin) Bukkit.getPluginManager().getPlugin("SolarsRPCharacterSheetPlugin");

        if (characterSheetPlugin != null) {
            return characterSheetPlugin.getHealth();
        } else {
            player.sendMessage(ChatColor.RED + "CharacterSheetPlugin not found!");
            return 100; //Default value for player health for combat.
        }
    }

    private boolean removeFromCombat(Player player, String[] args) {
        Battle battle = getBattleByPlayer(player);
        if (battle == null) {
            player.sendMessage(ChatColor.RED + "You are not in any combat!");
            return true;
        }
        if (!battle.getOwner().equals(player)) {
            player.sendMessage(ChatColor.RED + "Only the battle creator can remove players from the combat!");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "The specified player is not online.");
            return true;
        }

        if (!battle.isPlayerInBattle(target)) {
            player.sendMessage(ChatColor.RED + target.getName() + " is not part of this combat!");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /removePlayerFromCombat <player>");
            return true;
        }

        battle.removePlayer(target);

        return true;

    }

    private boolean lockCombat(Player player) {
        Battle battle = getBattleByPlayer(player);
        if (battle == null) {
            player.sendMessage(ChatColor.RED + "You are not in any combat!");
            return true;
        }
        if (!battle.getOwner().equals(player)) {
            player.sendMessage(ChatColor.RED + "Only the battle creator can start the combat!");
            return true;
        }

        battle.setLock();
        player.sendMessage(ChatColor.GREEN + "The battle is lock is now: " + battle.isLocked());
        return true;

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
        if(battle.isLocked())
        {
            player.sendMessage(ChatColor.RED + "Battle is locked. You may not join." + battleId);
        }

        if(battle.isStarted())
        {
            player.sendMessage(ChatColor.RED + "This battle has already started, you will be added at the end of the turn order." + battleId);
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
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /damage <target> <damage>");
            return true;
        }
        Battle battle = getBattleByPlayer(player);
        Player target = Bukkit.getPlayer(args[0]);
        if (battle == null) {
            player.sendMessage(ChatColor.RED + "You are not in a combat!");
            return true;
        }
        if(!battle.isTurn(player))
        {
            player.sendMessage(ChatColor.RED + "It is not your turn!");
            return true;
        }

        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "The specified player is not online.");
            return true;
        }

        int damage;
        try {
            damage = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Damage must be a number!");
            return true;
        }




        if (!battle.isPlayerInBattle(target)) {
            player.sendMessage(ChatColor.RED + target.getName() + " is not part of this combat!");
            return true;
        }

        battle.dealDamage(target, damage);

        if (battle.getPlayers().size() == 1) {
            battles.remove(battle.getBattleId());
            battle.getPlayers().getFirst().remove(); //Jank but should solve the scoreboard lingering issue and clean up residual stuff.
            player.sendMessage(ChatColor.GREEN + "The battle has ended because only one players remain.");
        }

        battle.advanceTurn();
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
