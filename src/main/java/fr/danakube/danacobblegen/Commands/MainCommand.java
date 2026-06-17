package fr.danakube.danacobblegen.Commands;

import fr.danakube.danacobblegen.CustomCobbleGen;
import fr.danakube.danacobblegen.Files.Lang;
import fr.danakube.danacobblegen.Files.Setting;
import fr.danakube.danacobblegen.GUI.GUIManager;
import fr.danakube.danacobblegen.Managers.BlockManager;
import fr.danakube.danacobblegen.Managers.GenPiston;
import fr.danakube.danacobblegen.Managers.PermissionManager;
import fr.danakube.danacobblegen.Utils.Response;
import fr.danakube.danacobblegen.Utils.pastebin.FileUploader;
import fr.danakube.danacobblegen.databases.MySQLPlayerDatabase;
import fr.danakube.danacobblegen.databases.PlayerDatabase;
import fr.danakube.danacobblegen.databases.YamlPlayerDatabase;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MainCommand implements CommandExecutor{

	private final GUIManager guiManager = GUIManager.getInstance();
	private final PermissionManager pm = new PermissionManager();
	private final CustomCobbleGen plugin = CustomCobbleGen.getInstance();
	
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
		if(args.length < 1){
			if(Setting.GUI_PERMISSIONNEEDED.getBoolean()) {
				if(!pm.hasPermission(sender, "customcobblegen.gui", true)) return false;
			}
			if(!(sender instanceof Player p)){
				sender.sendMessage(Lang.PREFIX.toString() + Lang.PLAYER_ONLY);
				return false;
			}
			if(Setting.ISLANDS_USEPERISLANDUNLOCKEDGENERATORS.getBoolean()
					&& plugin.isConnectedToIslandPlugin()
					&& !plugin.getIslandHook().hasIsland(p.getUniqueId())) {
				p.sendMessage(Lang.PREFIX.toString() + Lang.PLAYER_NO_ISLAND);
				return false;
			}
			guiManager.new MainGUI(p).open();
			return true;
		}else if(args[0].equalsIgnoreCase("help")) {

			sendHelp(sender, label);
			return true;
		}else if(args[0].equalsIgnoreCase("v")){
			sender.sendMessage("CCG Version: " + plugin.getDescription().getVersion());
		}else if(args[0].equalsIgnoreCase("admin")){
			if(!pm.hasPermission(sender, "customcobblegen.admin", true)) return false;
			String adminUsage = Lang.PREFIX + Lang.ADMIN_USAGE.toString().replaceAll("%command%", label);
			if(args.length < 2){
				sender.sendMessage(adminUsage);
				return false;
			}
			if(args[1].equalsIgnoreCase("reload")) {
				if(!pm.hasPermission(sender, "customcobblegen.admin.reload", true)) return false;

				double time = System.currentTimeMillis();
				plugin.reloadPlugin();
				double time2 = System.currentTimeMillis();
				double time3 = (time2-time)/1000;
				if(sender instanceof Player p) {
					plugin.log(p.getName() + " reloaded the plugin");
				}
				sender.sendMessage(Lang.PREFIX + Lang.RELOAD_SUCCESS.toString().replaceAll("%time%", String.valueOf(time3)));
			}else if(args[1].equalsIgnoreCase("support")) {
				sender.sendMessage(Lang.color(Lang.PREFIX + "&7To get support join our discord: https://discord.gg/6UpwEDUm6V"));
			}else if(args[1].equalsIgnoreCase("pastebin")) {
				if(!pm.hasPermission(sender, "customcobblegen.admin.pastebin", true)) return false;
				sender.sendMessage(Lang.color(Lang.PREFIX + "&7Getting contents of files..."));
				
	            final Response<String> postResult = new FileUploader().pastebinUpload("config.yml", "data//players.yml", "data//signs.yml", "lang.yml");

				if (postResult.isError()) {
					sender.sendMessage(Lang.color(Lang.PREFIX + "&cError pasting to pastebin: " + postResult.getResult()));
					return false;
				}
				sender.sendMessage(Lang.color(Lang.PREFIX + "&aSuccess pasting to pastebin! Send this link to the dev:"));
				sender.sendMessage(Lang.color(Lang.PREFIX + "&a" + postResult.getResult()));
				
			}else if(args[1].equalsIgnoreCase("debug")){
				if(sender instanceof Player p) {
					//Giving my own user access so it is easier to help on servers. Pull requests adding own names will not be accepted
					if(p.getName().equals("PhilPlays") && !pm.hasPermission(p, "customcobblegen.debugger", true)) return false;
					UUID uuid = p.getUniqueId();
					//Console or player with permission
					if(args.length < 3) {
						sender.sendMessage(Lang.color("&cCurrently no use for the /ccg admin debug command"));
						return true;
					}else if(args[2].equalsIgnoreCase("island")) {
						if(plugin.getIslandHook() == null) {
							sender.sendMessage(Lang.color("&cNo skyblock plugins available"));
							return true;
						}else if(!plugin.getIslandHook().hasIsland(uuid)) {
							sender.sendMessage(Lang.color("&cYou have no island"));
							return true;
						}
						
						sender.sendMessage(Lang.color("&6&lDebug info on island accessible by plugin"));
						int level = plugin.getIslandHook().getIslandLevel(uuid);
						sender.sendMessage(Lang.color("&6Your uuid: &8" + uuid));
						sender.sendMessage(Lang.color("&6isLeader: &8" + plugin.getIslandHook().isPlayerLeader(uuid)));
						sender.sendMessage(Lang.color("&6Level: &8" + level));
						sender.sendMessage(Lang.color("&6Players online: &8" + Arrays.toString(plugin.getIslandHook().getArrayOfIslandMembers(uuid))));
						
					}else if(args[2].equalsIgnoreCase("selected")) {
						sender.sendMessage(Lang.color("&6&lDebug info on selected tiers accessible by plugin"));
						sender.sendMessage(Lang.color("&8Number of players data loaded: &6" + plugin.getPlayerDatabase().getAllPlayerData().size()));
					}else if(args[2].equalsIgnoreCase("pistons")) {
						sender.sendMessage(Lang.color("&6&lDebug info on pistons accessible by plugin"));
						BlockManager bm = BlockManager.getInstance();
						sender.sendMessage(Lang.color("&8Number of pistons data loaded: &6" + bm.getKnownGenPistons().size()));
						
						
						Map<Location, GenPiston> pistons = bm.getKnownGenPistons();
						if(pistons == null) sender.sendMessage(Lang.color("&cNo pistons loaded"));
						else{
							sender.sendMessage(Lang.color("&6Your uuid: &8" + uuid));
							sender.sendMessage(Lang.color("&7Pistons loaded:"));
							for(GenPiston piston : pistons.values()) {
								sender.sendMessage(Lang.color("&6" + piston.getLoc().toString() + " " + (piston.getUUID().equals(uuid) ? "&a" : "&c") + piston.getUUID().toString()));
							}
						}
						
						
					}
				}else {
					sender.sendMessage(Lang.PLAYER_ONLY.toString());
					return false;
				}
				
			}else if(args[1].equalsIgnoreCase("database")){
				if(!pm.hasPermission(sender, "customcobblegen.admin.database", true)) return false;
				if(args.length < 3) {
					sender.sendMessage(Lang.DATABASE_USAGE.toString().replaceAll("%command%", label));
					return true;
				}
				if(args[2].equalsIgnoreCase("forcesave")) {
					if(!pm.hasPermission(sender, "customcobblegen.admin.database.forcesave", true)) return false;
					plugin.getPlayerDatabase().saveEverythingToDatabase();
					if(sender instanceof Player p) {
						plugin.log(p.getName() + " force saved the player data");
					}
					sender.sendMessage(Lang.PREFIX.toString() + Lang.FORCE_SAVE_SUCCESS);

				}else if(args[2].equalsIgnoreCase("migrate")){
					if(!pm.hasPermission(sender, "customcobblegen.admin.database.migrate", true)) return false;
					// label [0]    [1]      [2]   [3]   [4]
					//  ccg admin database migrate YAML MYSQL
					if(args.length < 5) {
						sender.sendMessage(Lang.DATABASE_MIGRATE_USAGE.toString().replaceAll("%command%", label));
						return true;
					}
					String fromType = args[3].toUpperCase();
					String newType = args[4].toUpperCase();
					PlayerDatabase fromDatabase = this.getDatabaseFromType(fromType);
					if(fromDatabase == null){
						sender.sendMessage(Lang.DATABASE_MIGRATE_INVALID_DATABASE.toString(fromType));
						return true;
					}
					PlayerDatabase newDatabase = this.getDatabaseFromType(newType);
					if(newDatabase == null){
						sender.sendMessage(Lang.DATABASE_MIGRATE_INVALID_DATABASE.toString(newType));
						return true;
					}
					sender.sendMessage(Lang.DATABASE_MIGRATE_STARTING.toString());
					if(fromDatabase.getAllPlayerData().isEmpty()){
						//TODO CHECK IF CONNECTION IS ESTABLISHED
						sender.sendMessage(Lang.DATABASE_MIGRATE_LOADING_START.toString(fromType));
						fromDatabase.loadEverythingFromDatabase();
						sender.sendMessage(Lang.DATABASE_MIGRATE_LOADING_DONE.toString(fromType));
					}
					if(newDatabase.isConnectionClosed()){
						sender.sendMessage(Lang.DATABASE_MIGRATE_ESTABLISHING_CONNECTION.toString(newType));
						Response<String> response = newDatabase.establishConnection();
						if(response.isError()){
							sender.sendMessage(Lang.color("&c" + response.getResult()));
							return true;
						}
					}
					newDatabase.setAllPlayerData(fromDatabase.getAllPlayerData());
					sender.sendMessage(Lang.DATABASE_MIGRATE_SAVING_START.toString(newType));
					newDatabase.saveEverythingToDatabase();
					sender.sendMessage(Lang.DATABASE_MIGRATE_SAVING_DONE.toString(newType));
				}
			}else if(args[1].equalsIgnoreCase("setlevel") || args[1].equalsIgnoreCase("addlevel") || args[1].equalsIgnoreCase("removelevel")) {
				if(!pm.hasPermission(sender, "customcobblegen.admin.level", true)) return false;
				if(args.length < 6) {
					sender.sendMessage(Lang.PREFIX.toString() + Lang.ADMIN_LEVEL_USAGE.toString().replace("%command%", args[1].toLowerCase()));
					return true;
				}
				org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(args[2]);
				if(target == null) {
					sender.sendMessage(Lang.PREFIX.toString() + Lang.ADMIN_LEVEL_PLAYER_NOT_FOUND.toString());
					return true;
				}
				int modeId;
				int amount;
				try {
					modeId = Integer.parseInt(args[3]);
					amount = Integer.parseInt(args[5]);
				} catch(NumberFormatException e) {
					sender.sendMessage(Lang.PREFIX.toString() + Lang.ADMIN_LEVEL_INVALID_NUMBER.toString());
					return true;
				}
				String oreId = args[4].toUpperCase();
				
				UUID targetUuid = target.getUniqueId();
				if (fr.danakube.danacobblegen.Files.Setting.ISLANDS_USEPERISLANDUNLOCKEDGENERATORS.getBoolean() && plugin.isConnectedToIslandPlugin()) {
					targetUuid = plugin.getIslandHook().getIslandLeaderFromPlayer(targetUuid);
				}
				
				fr.danakube.danacobblegen.databases.PlayerData data = plugin.getPlayerDatabase().getPlayerData(targetUuid);
				if (data == null) {
					sender.sendMessage(Lang.PREFIX.toString() + Lang.ADMIN_LEVEL_DATA_NOT_FOUND.toString());
					return true;
				}
				
				int currentLevel = data.getOreLevel(modeId, oreId);
				int newLevel = currentLevel;
				
				if(args[1].equalsIgnoreCase("setlevel")) newLevel = amount;
				else if(args[1].equalsIgnoreCase("addlevel")) newLevel += amount;
				else if(args[1].equalsIgnoreCase("removelevel")) newLevel -= amount;
				
				if(newLevel < -1) newLevel = -1; // -1 = locked
				
				data.setOreLevel(modeId, oreId, newLevel);
				sender.sendMessage(Lang.PREFIX.toString() + Lang.ADMIN_LEVEL_SUCCESS.toString()
						.replace("%player%", target.getName())
						.replace("%oreId%", oreId)
						.replace("%modeId%", String.valueOf(modeId))
						.replace("%level%", String.valueOf(newLevel)));
			}else {
				if(!pm.hasPermission(sender, "customcobblegen.admin", true)) return false;
				sender.sendMessage(adminUsage);
				return false;
			}
			return true;
		}else{
			sendHelp(sender, label);
		}
		return true;
		   
	}
	
	private void sendHelp(CommandSender sender, String label) {

		List<String> helpStrings = Lang.PLAYER_PLUGIN_HELP.toStringList();
		for(String s : helpStrings) {
			s = s.replace("%command%", label);
			sender.sendMessage(s);
		}
	}

	private PlayerDatabase getDatabaseFromType(String type){
		PlayerDatabase database = plugin.getPlayerDatabase();
		switch (type){
			case "MYSQL":
				if(database instanceof MySQLPlayerDatabase) return database;
				else return new MySQLPlayerDatabase();
			case "YAML":
				if(database instanceof YamlPlayerDatabase) return database;
				else return new YamlPlayerDatabase();
			default:
				return null;
		}
	}
}
