package fr.danakube.danacobblegen.Requirements;

import fr.danakube.danacobblegen.CustomCobbleGen;
import fr.danakube.danacobblegen.Files.Lang;
import fr.danakube.danacobblegen.Hooks.IslandHook;
import org.bukkit.entity.Player;

import java.util.List;

public class LevelRequirement implements Requirement {

	private final int levelNeeded;
	private final CustomCobbleGen plugin;
	
	public LevelRequirement(int levelNeeded) {
		this.levelNeeded = levelNeeded;
		plugin = CustomCobbleGen.getInstance();
	}
	
	@Override
	public boolean furfillsRequirement(Player p) {
		if(!plugin.isConnectedToIslandPlugin()) {
			plugin.log("&cThere is a island requirement in use, but there are no island/skyblock plugins installed. Returning true.");
			return true;
		}
		IslandHook hook = CustomCobbleGen.islandPluginHooked;
		int currentLevel = hook.getIslandLevel(p.getUniqueId());
		plugin.debug(currentLevel);
		return currentLevel >= this.getRequirementValue();
	}

	@Override
	public RequirementType getRequirementType() {
		return RequirementType.LEVEL;
	}

	@Override
	public int getRequirementValue() {
		return this.levelNeeded;
	}

	@Override
	public List<String> addAvailableString(List<String> lore) {
		lore.add(Lang.GUI_PRICE_LEVEL_ACHIEVED.toString());
		return lore;
	}

	@Override
	public List<String> addUnavailableString(List<String> lore) {
		lore.add(Lang.GUI_PRICE_LEVEL_NOT_ACHIEVED.toString());
		return lore;
	}

	@Override
	public void onPurchase(Player p) {
		//Does not do anything on purchase
	}
	
}
