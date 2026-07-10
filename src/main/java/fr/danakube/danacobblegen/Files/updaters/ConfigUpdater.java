package fr.danakube.danacobblegen.Files.updaters;

import fr.danakube.danacobblegen.CustomCobbleGen;
import fr.danakube.danacobblegen.Files.Setting;
import io.papermc.paper.plugin.configuration.PluginMeta;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

public class ConfigUpdater extends YamlConfiguration {

	public ConfigUpdater() {
		CustomCobbleGen plugin = CustomCobbleGen.getInstance();
		PluginMeta pluginMeta = plugin.getPluginMeta();

		FileConfiguration config = plugin.getConfig();
		config.options().setHeader(List.of(pluginMeta.getName() + "! Version: " + pluginMeta.getVersion() + " By " + (pluginMeta.getAuthors().isEmpty() ? "Phil14052" : pluginMeta.getAuthors().get(0))));
		if(!Setting.isConfigSet()) Setting.setFile(config);
		for(Setting setting : Setting.values()) {
			if(setting.isSection()) continue; //SECTIONS ARE FOR REFERENCE ONLY
			config.addDefault(setting.getPath(), setting.getDefaultValue());
		}
		String generationModePath = Setting.SECTION_GENERATIONMODES.getPath();
		if(!config.contains(generationModePath) || !config.contains(generationModePath + ".0")) {
			config.addDefault(generationModePath + ".0.blocks", new String[] {"WATER", "LAVA"});
			config.addDefault(generationModePath + ".0.displayName", "Cobblestone generator");
			config.addDefault(generationModePath + ".0.fallback", "COBBLESTONE");
			config.addDefault(generationModePath + ".0.particleEffect", "SMOKE_LARGE");
			config.addDefault(generationModePath + ".0.generationSound", "ENTITY_EXPERIENCE_ORB_PICKUP");
		}

		String dynPath = "dynamic-generator.0";
		if(!config.contains("dynamic-generator")) {
			config.addDefault(dynPath + ".default-rates.COBBLESTONE", 95.0);
			config.addDefault(dynPath + ".default-rates.COAL_ORE", 5.0);
			config.addDefault(dynPath + ".buffer-material", "COBBLESTONE");

			String orePath = dynPath + ".ores.DIAMOND_ORE";
			config.addDefault(orePath + ".displayName", "&bDiamond Ore");
			config.addDefault(orePath + ".icon", "DIAMOND_ORE");
			config.addDefault(orePath + ".unlock.start-percentage", 1.0);
			config.addDefault(orePath + ".unlock.money", 35000);

			config.addDefault(orePath + ".upgrades.1.percentage", 2.0);
			config.addDefault(orePath + ".upgrades.1.money", 50000);

			config.addDefault(orePath + ".upgrades.2.percentage", 3.0);
			config.addDefault(orePath + ".upgrades.2.money", 75000);
		}
		config.options().copyDefaults(true);
		plugin.saveDefaultConfig();
	}

}
