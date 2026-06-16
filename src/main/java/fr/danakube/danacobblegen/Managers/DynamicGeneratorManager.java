package fr.danakube.danacobblegen.Managers;

import fr.danakube.danacobblegen.API.DynamicOre;
import fr.danakube.danacobblegen.API.OreUpgrade;
import fr.danakube.danacobblegen.CustomCobbleGen;
import fr.danakube.danacobblegen.Files.Setting;
import fr.danakube.danacobblegen.Requirements.*;
import fr.danakube.danacobblegen.databases.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitScheduler;
import java.util.*;

public class DynamicGeneratorManager {
	private static DynamicGeneratorManager instance = null;
	private final CustomCobbleGen plugin;
	private final BukkitScheduler scheduler;
	private int task = -1;

	private Map<Integer, Map<String, DynamicOre>> dynamicOresByMode; // modeId -> (oreId -> DynamicOre)
	private Map<Integer, Map<Material, Double>> defaultRatesByMode; // modeId -> (material -> percentage)
	private Map<Integer, Material> bufferMaterialByMode;

	public DynamicGeneratorManager() {
		plugin = CustomCobbleGen.getInstance();
		scheduler = Bukkit.getServer().getScheduler();
		dynamicOresByMode = new HashMap<>();
		defaultRatesByMode = new HashMap<>();
		bufferMaterialByMode = new HashMap<>();
	}

	public static DynamicGeneratorManager getInstance() {
		if (instance == null) instance = new DynamicGeneratorManager();
		return instance;
	}

	public void reload() {
		unload();
		load();
	}

	public void unload() {
		dynamicOresByMode.clear();
		defaultRatesByMode.clear();
		bufferMaterialByMode.clear();
	}

    public void load() {
        dynamicOresByMode.clear();
        defaultRatesByMode.clear();
        bufferMaterialByMode.clear();
        ConfigurationSection dynSec = plugin.getConfig().getConfigurationSection("dynamic-generator");
        if (dynSec == null) return;

        for (String modeKey : dynSec.getKeys(false)) {
            ConfigurationSection modeSec = dynSec.getConfigurationSection(modeKey);
            if (modeSec == null) continue;
            
            int modeId;
            try {
                modeId = Integer.parseInt(modeKey);
            } catch(NumberFormatException e) {
                plugin.error("Mode key " + modeKey + " in dynamic-generator is not a valid integer mode ID.");
                continue;
            }

            Map<Material, Double> defaultRates = new HashMap<>();
            ConfigurationSection defaultSec = modeSec.getConfigurationSection("default-rates");
            if (defaultSec != null) {
                for (String matKey : defaultSec.getKeys(false)) {
                    Material mat = Material.matchMaterial(matKey.toUpperCase());
                    if (mat != null) {
                        defaultRates.put(mat, defaultSec.getDouble(matKey));
                    }
                }
            }
            defaultRatesByMode.put(modeId, defaultRates);

            String bufferString = modeSec.getString("buffer-material");
            if (bufferString != null) {
                Material bufferMat = Material.matchMaterial(bufferString.toUpperCase());
                if (bufferMat != null) {
                    bufferMaterialByMode.put(modeId, bufferMat);
                }
            }

            Map<String, DynamicOre> oresMap = new HashMap<>();
            ConfigurationSection oresSec = modeSec.getConfigurationSection("ores");
            if (oresSec != null) {
                for (String oreId : oresSec.getKeys(false)) {
                    ConfigurationSection oreSec = oresSec.getConfigurationSection(oreId);
                    if (oreSec == null) continue;

                    String displayName = oreSec.getString("displayName", oreId);
                    Material icon = Material.matchMaterial(oreSec.getString("icon", "STONE").toUpperCase());
                    if (icon == null) icon = Material.STONE;

                    double startPercentage = oreSec.getDouble("unlock.start-percentage", 1.0);
                    List<Requirement> unlockReqs = parseRequirements(oreSec.getConfigurationSection("unlock"));

                    Map<Integer, OreUpgrade> upgrades = new HashMap<>();
                    ConfigurationSection upgSec = oreSec.getConfigurationSection("upgrades");
                    if (upgSec != null) {
                        for (String levelStr : upgSec.getKeys(false)) {
                            try {
                                int level = Integer.parseInt(levelStr);
                                ConfigurationSection lSec = upgSec.getConfigurationSection(levelStr);
                                double percentage = lSec.getDouble("percentage", 0.0);
                                List<Requirement> reqs = parseRequirements(lSec);
                                upgrades.put(level, new OreUpgrade(level, reqs, percentage));
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    DynamicOre ore = new DynamicOre(oreId, displayName, icon, modeId, unlockReqs, startPercentage, upgrades);
                    oresMap.put(oreId, ore);
                }
            }
            dynamicOresByMode.put(modeId, oresMap);
        }
    }

	private List<Requirement> parseRequirements(ConfigurationSection sec) {
		List<Requirement> requirements = new ArrayList<>();
		if (sec == null) return requirements;

		if (sec.contains("price")) {
            int priceMoney = sec.getInt("price");
            if (priceMoney > 0) requirements.add(new MoneyRequirement(priceMoney));
		}
		if (sec.contains("money")) {
			int priceMoney = sec.getInt("money");
			if (priceMoney > 0) requirements.add(new MoneyRequirement(priceMoney));
		}
        if (sec.contains("xp")) {
            int xp = sec.getInt("xp");
            if (xp > 0) requirements.add(new XpRequirement(xp));
        }
		return requirements;
	}

	public Map<Material, Double> getRatesForPlayer(UUID uuid, int modeId) {
		PlayerData data = plugin.getPlayerDatabase().getPlayerData(uuid);
        if (data == null) {
            return new HashMap<>(defaultRatesByMode.getOrDefault(modeId, new HashMap<>()));
        }

        Map<Material, Double> rates = new HashMap<>(defaultRatesByMode.getOrDefault(modeId, new HashMap<>()));
        Map<String, Integer> playerOres = data.getOresForMode(modeId);
        Map<String, DynamicOre> modeOres = dynamicOresByMode.getOrDefault(modeId, new HashMap<>());
        
        Material bufferMaterial = bufferMaterialByMode.get(modeId);
        double totalAdded = 0;

        for (Map.Entry<String, Integer> entry : playerOres.entrySet()) {
            String oreId = entry.getKey();
            int level = entry.getValue();
            if (level >= 0) {
                DynamicOre dOre = modeOres.get(oreId);
                if (dOre != null) {
                    double percentage = dOre.getStartPercentage();
                    if (level > 0) {
                        OreUpgrade upgrade = dOre.getUpgrade(level);
                        if (upgrade != null) {
                            percentage = upgrade.getPercentage();
                        }
                    }
                    Material m = Material.matchMaterial(oreId.toUpperCase());
                    if (m != null) {
                        rates.put(m, percentage);
                        totalAdded += percentage;
                    }
                }
            }
        }

        if (bufferMaterial != null && rates.containsKey(bufferMaterial)) {
            double currentBuffer = rates.get(bufferMaterial);
            double newBuffer = currentBuffer - totalAdded;
            if (newBuffer < Setting.DYNAMIC_GENERATOR_MIN_BUFFER.getDouble()) {
                newBuffer = Setting.DYNAMIC_GENERATOR_MIN_BUFFER.getDouble();
            }
            rates.put(bufferMaterial, newBuffer);
        }
		return rates;
	}

    public Material getRandomResult(UUID uuid, int modeId) {
		Map<Material, Double> rates = getRatesForPlayer(uuid, modeId);
        return getRandomFromRates(rates);
    }

	private Material getRandomFromRates(Map<Material, Double> rates) {
        if (rates.isEmpty()) return Material.COBBLESTONE;
		double r = Math.random() * 100;
		double prev = 0;
		for (Material m : rates.keySet()) {
			double chance = rates.get(m) + prev;
			if (r > prev && r <= chance) return m;
			prev = chance;
		}
		// If total rates < 100, might not return a material, so fallback
		return rates.keySet().iterator().next();
	}

    public Map<Integer, Map<String, DynamicOre>> getDynamicOresByMode() {
        return dynamicOresByMode;
    }

    public DynamicOre getOre(int modeId, String oreId) {
        return dynamicOresByMode.getOrDefault(modeId, new HashMap<>()).get(oreId);
    }

	public boolean isAutoSaveActive() {
		if(task < 0) return false;
		return scheduler.isCurrentlyRunning(task);
	}

	public void startAutoSave() {
		if(this.isAutoSaveActive()) this.stopAutoSave();
		task = scheduler.scheduleSyncRepeatingTask(plugin, () -> {
			plugin.log("Auto saving player data...");
			long time1 = System.currentTimeMillis();
			plugin.getPlayerDatabase().saveEverythingToDatabase(false);
			long time2 = System.currentTimeMillis();
			double time3 = Double.longBitsToDouble(time2-time1)/1000;
			plugin.log("Player data has been saved. It took: " + time3 + " seconds");
		}, 0L, Setting.AUTO_SAVE_DELAY.getInt()*20L);
	}

	public void stopAutoSave() {
		if(task < 0) return;
		scheduler.cancelTask(task);
	}
}
