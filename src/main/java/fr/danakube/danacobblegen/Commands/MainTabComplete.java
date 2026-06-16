package fr.danakube.danacobblegen.Commands;

import fr.danakube.danacobblegen.Managers.PermissionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MainTabComplete implements TabCompleter{
	private final PermissionManager pm = new PermissionManager();
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		if(args.length < 1 || !(sender instanceof Player)) return null;
		if(args.length < 2) {
			List<String> subCommands = new ArrayList<>();
			subCommands.add("help");
			if(pm.hasPermission(sender, "customcobblegen.admin", false)) subCommands.add("admin");
			return subCommands;	
		} else if(args.length < 3 && args[0].equalsIgnoreCase("admin")) {
			List<String> subArgs = new ArrayList<>();
			if(pm.hasPermission(sender, "customcobblegen.admin.reload", false)) subArgs.add("reload");
			if(pm.hasPermission(sender, "customcobblegen.admin.pastebin", false)) subArgs.add("pastebin");
			if(pm.hasPermission(sender, "customcobblegen.admin.database", false)) subArgs.add("database");
			if(pm.hasPermission(sender, "customcobblegen.admin.level", false)) {
				subArgs.add("setlevel");
				subArgs.add("addlevel");
				subArgs.add("removelevel");
			}
			subArgs.add("support");
			return subArgs;	
		} else if(args[0].equalsIgnoreCase("admin")) {
			if(args.length == 3 && args[1].equalsIgnoreCase("database")){
				List<String> subArgs = new ArrayList<>();
				if(pm.hasPermission(sender, "customcobblegen.admin.database.forcesave", false)) subArgs.add("forcesave");
				if(pm.hasPermission(sender, "customcobblegen.admin.database.migrate", false)) subArgs.add("migrate");
				return subArgs;
			} else if(args[1].equalsIgnoreCase("setlevel") || args[1].equalsIgnoreCase("addlevel") || args[1].equalsIgnoreCase("removelevel")) {
				if(args.length == 3) {
					return null; // Autocomplete player names automatically
				} else if(args.length == 4) {
					List<String> subArgs = new ArrayList<>();
					subArgs.add("0"); // Suggest mode 0 by default
					return subArgs;
				} else if(args.length == 5) {
					List<String> subArgs = new ArrayList<>();
					try {
						int modeId = Integer.parseInt(args[3]);
						subArgs.addAll(fr.danakube.danacobblegen.Managers.DynamicGeneratorManager.getInstance().getDynamicOresByMode().getOrDefault(modeId, new java.util.HashMap<>()).keySet());
					} catch(Exception ignored) {}
					return subArgs;
				} else if(args.length == 6) {
					List<String> subArgs = new ArrayList<>();
					subArgs.add("1");
					return subArgs;
				}
			}
		}
	
		return new ArrayList<>();
	}
}
