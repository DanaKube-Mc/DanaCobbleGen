package fr.danakube.danacobblegen;

import fr.danakube.danacobblegen.Commands.MainCommand;
import fr.danakube.danacobblegen.Commands.MainTabComplete;
import fr.danakube.danacobblegen.Events.BlockEvents;
import fr.danakube.danacobblegen.Events.PlayerEvents;
import fr.danakube.danacobblegen.Files.Files;
import fr.danakube.danacobblegen.Files.Lang;
import fr.danakube.danacobblegen.Files.Setting;
import fr.danakube.danacobblegen.Files.updaters.ConfigUpdater;
import fr.danakube.danacobblegen.Files.updaters.LangFileUpdater;
import fr.danakube.danacobblegen.GUI.InventoryEvents;
import fr.danakube.danacobblegen.Hooks.*;
import fr.danakube.danacobblegen.Managers.BlockManager;
import fr.danakube.danacobblegen.Managers.EconomyManager;
import fr.danakube.danacobblegen.Managers.GeneratorModeManager;
import fr.danakube.danacobblegen.Managers.DynamicGeneratorManager;
import fr.danakube.danacobblegen.Utils.Metrics.Metrics;
import fr.danakube.danacobblegen.Utils.TierPlaceholderExpansion;
import fr.danakube.danacobblegen.databases.MySQLPlayerDatabase;
import fr.danakube.danacobblegen.databases.PlayerDatabase;
import fr.danakube.danacobblegen.databases.YamlPlayerDatabase;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * CustomCobbleGen By @author Philip Flyvholm
 * CustomCobbleGen.java
 */
public class CustomCobbleGen extends JavaPlugin {

	private static CustomCobbleGen plugin;
	public Files lang;
	public Files guiConfig;
	private PlayerDatabase playerDatabase;
	private FileConfiguration signsConfig;
	private File signsConfigFile;
	private DynamicGeneratorManager dynamicGeneratorManager;
	private GeneratorModeManager generatorModeManager;
	public boolean isUsingPlaceholderAPI = false;
	private List<IslandHook> islandHooks;
	public static IslandHook islandPluginHooked = null;
	private static String connectedIslandPlugin = "None";
	private final String CONSOLEPREFIX = "&8[&3&lCustomCobbleGen&8]: ";
	
	@Override
	public void onEnable(){
		double time = System.currentTimeMillis();
		plugin = this;
		dynamicGeneratorManager = DynamicGeneratorManager.getInstance();
		Setting.setFile(plugin.getConfig());
		new ConfigUpdater();
		saveConfig();
		reloadConfig();
		plugin.debug("Enabling CustomCobbleGen plugin");
		plugin.log("&cIF YOU ENCOUNTER ANY BUGS OR ERRORS PLEASE REPORT THEM ON SPIGOT!");
		plugin.log("&8Special thanks to lelesape (Idea), AddstarMC (Contribution on GitHub), Fang_Zhijian (Chinese translation) and Xitrine (testing)"); // If you contribute to the plugin please add yourself here :D (As a thank you from me)
		// Setup config
		generatorModeManager = GeneratorModeManager.getInstance();
		generatorModeManager.loadFromConfig();
		this.debug("The config is now setup&2 \u2713");
		// Setup player database
		setupPlayerDatabase();
		
		// Setup lang file
		lang = new Files(this, "lang.yml");
		new LangFileUpdater(plugin);
		Lang.setFile(lang);
		this.debug("Lang is now setup&2 \u2713");
		
		// Setup GUI config
		guiConfig = new Files(this, "gui.yml");
		this.debug("GUI Config is now setup&2 \u2713");
		// Setup dynamic generator
		dynamicGeneratorManager.load();
		this.debug("Dynamic Generator is now setup&2 \u2713");
		// Setup signs configs
		signsConfig = null;
		signsConfigFile = null;
		plugin.getPlayerDatabase().loadEverythingFromDatabase();
		islandHooks = new ArrayList<>(Arrays.asList(new BentoboxHook(), new SuperiorSkyblock2Hook()));
		this.setupHooks();

		if(Setting.AUTO_SAVE_ENABLED.getBoolean()){
			dynamicGeneratorManager.startAutoSave();
			plugin.debug("Auto saver started&2 \u2713");
		}

		registerEvents();
		plugin.debug("Events loaded&2 \u2713");
		PluginCommand mainCommand = plugin.getCommand("cobblegen");
		if(mainCommand != null){
			mainCommand.setExecutor(new MainCommand());
			mainCommand.setTabCompleter(new MainTabComplete());
			plugin.debug("Commands loaded&2 \u2713");
		}else{
			plugin.error("Failed load of command");
		}
		

        
		// Connect to BStats
		setupBStats();
        
		plugin.log("CustomCobbleGen is now enabled&2 \u2713");
		double time2 = System.currentTimeMillis();
		double time3 = (time2-time)/1000;
		plugin.debug("Took " + time3 + " seconds to setup CustomCobbleGen");
	}
	@Override
	public void onDisable(){
		// Unload everything
		plugin.log("Disabling CustomCobbleGen&2 \u2713");
		
		dynamicGeneratorManager.unload();
		if(dynamicGeneratorManager.isAutoSaveActive()) dynamicGeneratorManager.stopAutoSave();
		if(this.getPlayerDatabase() != null) {
			plugin.log("Saving player data&2 \u2713");
			this.getPlayerDatabase().saveEverythingToDatabase(false);
		}
		dynamicGeneratorManager = null;
		plugin.log("CustomCobbleGen is now disabled&2 \u2713");
		plugin = null;
	}

	private void setupBStats() {
		int pluginId = 5454;
		Metrics metrics = new Metrics(this, pluginId);
		Metrics.SingleLineChart genChart = new Metrics.SingleLineChart("generators", () -> {
			int numOfGenerators = BlockManager.getInstance().getKnownGenLocations().size();
			if(numOfGenerators > 10000) { // Over 10000 generators found - Prob a mistake
				plugin.warning("&c&lOver 10.000 generators in use. If you believe this is a mistake, then contact the dev (phil14052 on SpigotMC.org)");
				plugin.warning("&cQuick link: https://www.spigotmc.org/conversations/add?to=phil14052&title=CCG%20Support:%20" + numOfGenerators + "%20generators%20are%20active%20on%20my%20server");
			}
			return numOfGenerators;
		});
		Metrics.SimplePie pistonChart = new Metrics.SimplePie("servers_using_pistons_for_automation", () -> Setting.AUTOMATION_PISTONS.getBoolean() ? "Enabled" : "Disabled");
		Metrics.SimplePie islandChart = new Metrics.SimplePie("connected_island_plugins", () -> connectedIslandPlugin);

		Metrics.SimplePie selectOptionChart = new Metrics.SimplePie("tier_unlock_system", () -> Setting.ISLANDS_USEPERISLANDUNLOCKEDGENERATORS.getBoolean() ? "Island based" : "Player based");

		Metrics.SimplePie tiersActiveChart = new Metrics.SimplePie("tiers_active", () -> {
			return dynamicGeneratorManager.getDynamicOresByMode().size() + " modes active";
		});

		Metrics.SimplePie modesActiveChart = new Metrics.SimplePie("modes_active", () -> generatorModeManager.getModes().size() + " generation modes active");
		metrics.addCustomChart(genChart);
		metrics.addCustomChart(pistonChart);
		metrics.addCustomChart(islandChart);
		metrics.addCustomChart(selectOptionChart);
		metrics.addCustomChart(tiersActiveChart);
		metrics.addCustomChart(modesActiveChart);
	}

	private void setupPlayerDatabase() {
		switch (Setting.DATABASE_TYPE.getString().toUpperCase()) {
			case "YAML", "YML" -> this.playerDatabase = new YamlPlayerDatabase();
			case "MYSQL" -> this.playerDatabase = new MySQLPlayerDatabase();
			default -> {
				plugin.error("Unknown database type. Will use YAML", true);
				this.playerDatabase = new YamlPlayerDatabase();
			}
		}
		plugin.debug("Setting up a " + this.playerDatabase.getType() + " player database");
		try {
			this.playerDatabase.establishConnection();
		}catch(Exception e) {
			plugin.error("FAILED SETTING UP " + this.playerDatabase.getType() + " PLAYER DATABASE. DISABLING PLUGIN!");
			plugin.error(e.getLocalizedMessage());
			for(StackTraceElement s : e.getCause().getStackTrace()) {
				plugin.error(s.toString());
			}
			Bukkit.getServer().getPluginManager().disablePlugin(this);
		}
	}
	
	private void setupHooks() {
		this.connectToPlaceholderAPI();
		this.connectToVault();
		connectToIslandPlugin();
		
	}

	public List<IslandHook> getIslandHooks() {
		return islandHooks;
	}

	public void connectToIslandPlugin() {
		PluginManager pm = Bukkit.getPluginManager();
		for(IslandHook hook : islandHooks){
			if(pm.getPlugin(hook.pluginHookName()) != null){
				islandPluginHooked = hook;
				islandPluginHooked.init();
				break;
			}
		}
		if(islandPluginHooked != null) {
			connectedIslandPlugin = islandPluginHooked.getHookName();
			plugin.debug("Found " + islandPluginHooked.getHookName() + "&2 \u2713");
			if(!islandPluginHooked.supportsIslandBalance()
				&& Setting.ISLANDS_USEISLANDBALANCE.getBoolean()) {
				plugin.error("Option 'options -> islands -> useIslandBalance' has been selected in the config, but " +  islandPluginHooked.getHookName() + " does not support island balances. Using player balances instead");
			}
		} 
	}
	
	public void connectToPlaceholderAPI() {
		// Connect to PlaceholderAPI
		this.isUsingPlaceholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
		if(this.isUsingPlaceholderAPI) {
			new TierPlaceholderExpansion(this).register();
			plugin.debug("Found PlaceholderAPI and registed placeholders&2 \u2713");
		}
	}
	
	public void connectToVault() {
		// Connect to vault
		EconomyManager econManager = EconomyManager.getInstance();
		if(econManager.setupEconomy()) {
			this.debug("Economy is now setup");	
		}else {
			this.debug("Economy is not setup");
		}
	}
	
	public boolean isConnectedToIslandPlugin() {
		return islandPluginHooked != null;
	}
	
	public IslandHook getIslandHook() {
		return islandPluginHooked;
	}
	
	public void reloadPlugin() {
		if(dynamicGeneratorManager.isAutoSaveActive()) dynamicGeneratorManager.stopAutoSave();
		this.reloadConfig();
		Setting.setFile(this.getConfig());
		this.lang.reload();
		this.guiConfig.reload();
		this.getPlayerDatabase().reloadConnection();
		this.reloadSignsConfig();
		generatorModeManager.loadFromConfig();
		dynamicGeneratorManager.reload();
		dynamicGeneratorManager = DynamicGeneratorManager.getInstance();
		if(Setting.AUTO_SAVE_ENABLED.getBoolean()) dynamicGeneratorManager.startAutoSave();
	}
	
	public PlayerDatabase getPlayerDatabase() {
		return this.playerDatabase;
	}
	
	
    public void reloadSignsConfig(){
		if(this.signsConfigFile == null){
			this.signsConfigFile = new File(new File(plugin.getDataFolder(), "Data"),"signs.yml");
			this.signsConfig = YamlConfiguration.loadConfiguration(this.signsConfigFile);
			
		}
	}
	 //Return the player config
    public FileConfiguration getSignsConfig() {
 
        if(this.signsConfigFile == null) this.reloadSignsConfig();
 
        return this.signsConfig;
 
    }
 
    //Save the player config
    public void saveSignsConfig() {
 
        if(this.signsConfig == null || this.signsConfigFile == null) return;
 
        try {
            this.getSignsConfig().save(this.signsConfigFile);
        } catch (IOException ex) {
            plugin.getServer().getLogger().log(Level.SEVERE, "Could not save signs config to " + this.signsConfigFile +"!", ex);
        }
 
    }
    

    
    
	private void registerEvents(){
	    PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new BlockEvents(), this);
		pm.registerEvents(new InventoryEvents(), this);
		pm.registerEvents(new PlayerEvents(), this);
	}
	
	public void debug(boolean overrideConfigOption, Object... objects) {
		this.debug(createLogString(objects), overrideConfigOption);
	}
	
	public void debug(Object... objects) {
		this.debug(false, objects);
	}

	
	public void debug(Boolean booleanObject){
		this.debug(booleanObject.getClass().getTypeName() + ": "+ booleanObject);
	}
	public void debug(String message){
		this.debug(message, false);
	}
	public void debug(String message, boolean overrideConfigOption){
		if(!overrideConfigOption && !Setting.DEBUG.getBoolean()) return;
		Bukkit.getConsoleSender().sendMessage(Lang.color("&8[&3&lDanaCobbleGen&8]: &c&lDebug &8-&7 " + message));
	}
	
	public void log(Object... objects) {
		this.log(createLogString(objects));
	}

	private String createLogString(Object[] objects) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for(Object s : objects) {
			if(!first) {
				sb.append(", ");
			}else first = false;
			if(s == null) {
				sb.append("NULL");
			}else if(s instanceof String) {
				sb.append((String) s);
			}else {
				sb.append("[").append(s.getClass().getTypeName()).append(": ").append(s).append("]");
			}
		}
		return sb.toString();
	}

	public void log(String message){
		Bukkit.getConsoleSender().sendMessage(Lang.color(CONSOLEPREFIX + "&8&lLog &8-&7 " + message));
	}
	
	public void error(String message) {
		this.error(message, false);
	}
	
	public void error(String message, boolean userError) {
		if(userError) {
			Bukkit.getConsoleSender().sendMessage((CONSOLEPREFIX + "&4&lUser error &8-&c " + message).replace("&", "\u00A7"));
		}else {

			Bukkit.getConsoleSender().sendMessage((CONSOLEPREFIX + "&4&lError &8-&c " + message).replace("&", "\u00A7"));
		}
	}
	
	public void warning(String message) {
		Bukkit.getConsoleSender().sendMessage((CONSOLEPREFIX + "&4&lWarning &8-&7 " + message).replace("&", "\u00A7"));
	}
	
	
	public static CustomCobbleGen getInstance(){
		return plugin;
	}
}
