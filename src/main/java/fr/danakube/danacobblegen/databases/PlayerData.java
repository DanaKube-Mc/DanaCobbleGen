package fr.danakube.danacobblegen.databases;

import fr.danakube.danacobblegen.CustomCobbleGen;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {

	private final CustomCobbleGen plugin = CustomCobbleGen.getInstance();
	
	private UUID uuid;
	// modeId -> (oreId -> level)
	private Map<Integer, Map<String, Integer>> unlockedOres;
	
	public PlayerData(UUID uuid, Map<Integer, Map<String, Integer>> unlockedOres) {
		if(uuid == null) {
			plugin.error("Failed to load player data: uuid is null");
			return;
		}
		if(unlockedOres == null) {
			plugin.error("Failed to load player data: unlockedOres is null for uuid " + uuid);
			return;
		}
		this.uuid = uuid;
		this.unlockedOres = unlockedOres;
	}
	
	public PlayerData(UUID uuid) {
		this(uuid, new HashMap<>());
	}

	public UUID getUUID() {
		return uuid;
	}

	public Map<Integer, Map<String, Integer>> getUnlockedOres() {
		return unlockedOres;
	}

	public void setUnlockedOres(Map<Integer, Map<String, Integer>> unlockedOres) {
		this.unlockedOres = unlockedOres;
	}

	public Map<String, Integer> getOresForMode(int modeId) {
		return unlockedOres.getOrDefault(modeId, new HashMap<>());
	}

	public void setOreLevel(int modeId, String oreId, int level) {
		unlockedOres.computeIfAbsent(modeId, k -> new HashMap<>()).put(oreId, level);
	}

	public int getOreLevel(int modeId, String oreId) {
		return getOresForMode(modeId).getOrDefault(oreId, -1); // -1 means locked
	}

	public boolean isOreUnlocked(int modeId, String oreId) {
		return getOreLevel(modeId, oreId) >= 0;
	}
}
