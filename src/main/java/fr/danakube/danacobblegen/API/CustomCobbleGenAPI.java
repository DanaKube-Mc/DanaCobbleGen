package fr.danakube.danacobblegen.API;

import fr.danakube.danacobblegen.CustomCobbleGen;
import fr.danakube.danacobblegen.Hooks.IslandHook;
import fr.danakube.danacobblegen.Managers.BlockManager;
import fr.danakube.danacobblegen.Managers.DynamicGeneratorManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * CustomCobbleGen By @author Philip Flyvholm
 * CustomCobbleGenAPI.java
 */
public class CustomCobbleGenAPI {

	private static CustomCobbleGenAPI instance = null;
	private final CustomCobbleGen plugin;
	private final BlockManager bm = BlockManager.getInstance();
	
	/**
	 * Instantiate the API
	 * Use CustomCobbleGenAPI.getAPI() to get instance!
	 */
	public CustomCobbleGenAPI() {
		plugin = CustomCobbleGen.getInstance();
	}
	
	/**
	 * Get the DynamicGeneratorManager
	 */
	public DynamicGeneratorManager getDynamicGeneratorManager() {
		return DynamicGeneratorManager.getInstance();
	}
	
	
	/**
	 * Get a instance of the API
	 * @return returns an active instance of the API
	 */
	public static CustomCobbleGenAPI getAPI() {
		if(instance == null) instance = new CustomCobbleGenAPI();
		return instance;
	}

	/**
	 * Add an custom hook
	 * @param hook the new hook
	 */
	public void addIslandHook(IslandHook hook){
		plugin.getIslandHooks().add(hook);
		plugin.connectToIslandPlugin();
	}

	public void registerBlockBreak(Player player, Location location){
		if(bm.isGenLocationKnown(location)) {
			bm.setPlayerForLocation(player.getUniqueId(), location, false);
		}
	}
}
