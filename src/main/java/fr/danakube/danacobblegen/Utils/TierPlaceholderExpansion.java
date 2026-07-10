package fr.danakube.danacobblegen.Utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import fr.danakube.danacobblegen.API.DynamicOre;
import fr.danakube.danacobblegen.API.OreUpgrade;
import fr.danakube.danacobblegen.CustomCobbleGen;
import fr.danakube.danacobblegen.Managers.DynamicGeneratorManager;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TierPlaceholderExpansion extends PlaceholderExpansion {

    private final CustomCobbleGen plugin;
    private final DynamicGeneratorManager dgm = DynamicGeneratorManager.getInstance();

    public TierPlaceholderExpansion(CustomCobbleGen plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public boolean canRegister(){
        return true;
    }

    @Override
    public String getAuthor(){
        return plugin.getPluginMeta().getAuthors().toString();
    }

    @Override
    public String getIdentifier(){
        return "customcobblegen";
    }

    @Override
    public String getVersion(){
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier){

        if(player == null || !player.isOnline()){
            return "";
        }
        UUID uuid = player.getUniqueId();
		
		String[] identifiers = identifier.split("_");
		
		// %customcobblegen_ore_level_<modeId>_<oreId>%
		if(identifier.startsWith("ore_level") && identifiers.length >= 4) {
			try {
				int modeId = Integer.parseInt(identifiers[2]);
				String oreId = identifiers[3].toUpperCase();
				int level = plugin.getPlayerDatabase().getPlayerData(uuid).getOreLevel(modeId, oreId);
				return String.valueOf(level);
			} catch(NumberFormatException e) {
				return "-1";
			}
		}
		
		// %customcobblegen_ore_percentage_<modeId>_<oreId>%
		if(identifier.startsWith("ore_percentage") && identifiers.length >= 4) {
			try {
				int modeId = Integer.parseInt(identifiers[2]);
				String oreId = identifiers[3].toUpperCase();
				int level = plugin.getPlayerDatabase().getPlayerData(uuid).getOreLevel(modeId, oreId);
				if (level < 0) return "0.0";
				DynamicOre ore = dgm.getDynamicOresByMode().get(modeId).get(oreId);
				if (ore != null) {
					if (level == 0) return String.valueOf(ore.getStartPercentage());
					OreUpgrade upgrade = ore.getUpgrade(level);
					if (upgrade != null) return String.valueOf(upgrade.getPercentage());
				}
				return "0.0";
			} catch(NumberFormatException e) {
				return "0.0";
			}
		}
		
        return null;
    }
}
