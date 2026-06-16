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
			subArgs.add("support");
			return subArgs;	
		} else if(args.length < 4 && args[0].equalsIgnoreCase("admin")) {
			if(args[1].equalsIgnoreCase("database")){
				List<String> subArgs = new ArrayList<>();
				if(pm.hasPermission(sender, "customcobblegen.admin.database.forcesave", false)) subArgs.add("forcesave");
				if(pm.hasPermission(sender, "customcobblegen.admin.database.migrate", false)) subArgs.add("migrate");

				return subArgs;
			}
		}
	
		return new ArrayList<>();
	}
}
