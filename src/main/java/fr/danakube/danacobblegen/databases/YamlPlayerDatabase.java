package fr.danakube.danacobblegen.databases;

import fr.danakube.danacobblegen.Utils.StringUtils;
import io.papermc.paper.plugin.configuration.PluginMeta;
import fr.danakube.danacobblegen.Utils.Response;
import fr.danakube.danacobblegen.Files.Setting;
import fr.danakube.danacobblegen.Managers.GenPiston;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Material;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * CustomCobbleGen By @author Philip Flyvholm
 * YamlPlayerDatabase.java
 */
public class YamlPlayerDatabase extends PlayerDatabase {

    private FileConfiguration playerConfig;
    private File playerConfigFile;

    public YamlPlayerDatabase() {
        super();
    }

    @Override
    public Response<String> establishConnection() {

        // Setup player configs
        playerConfig = null;
        playerConfigFile = null;

        PluginMeta pluginMeta = plugin.getPluginMeta();
        List<String> header = Arrays.asList(
                String.format("%s! Version: %s - By Phil14052", pluginMeta.getName(), pluginMeta.getVersion()),
                "IMPORTANT: ONLY EDIT THIS IF YOU KNOW WHAT YOU ARE DOING!!"
        );
        try{
            this.getPlayerConfig().options().setHeader(header);
        }catch(NoSuchMethodError ignored){
            plugin.debug("Server does not support FileConfigurationOptions#setHeader(List<String>) for some unknown reason");
        }

        if (this.getPlayerConfig().getConfigurationSection("players") == null) {
            this.getPlayerConfig().createSection("players");
        }
        this.getPlayerConfig().options().copyDefaults(true);
        this.savePlayerConfig();

        plugin.debug("Players is now setup&2 \u2713");
        return new Response<>("Success establishing connection to YAML file", false);
    }

    @Override
    public void reloadConnection() {
        if (this.isConnectionClosed()) return;
        this.reloadPlayerConfig();
    }

    @Override
    public void closeConnection() {
        if (this.isConnectionClosed()) return;
        this.saveEverythingToDatabase(false);
        this.playerConfig = null;
        this.playerConfigFile = null;
    }


    @Override
    protected void addToDatabase(PlayerData data, boolean async) {
        if (this.isConnectionClosed()) return;
        this.playerData.put(data.getUUID(), data);
    }

    @Override
    public void loadEverythingFromDatabase(boolean async) {
        if (this.isConnectionClosed()) return;
        this.playerData = new HashMap<>();
        blockManager.setKnownGenPistons(new HashMap<>());
        ConfigurationSection playerSection = this.getPlayerConfig().getConfigurationSection("players");

        if (playerSection == null) {
            plugin.error("Could not find player section in player config");
            return;
        }

        if (Setting.ONLY_LOAD_ONLINE_PLAYERS.getBoolean()) {
            for (Player p : Bukkit.getServer().getOnlinePlayers()){
                if (p != null) this.loadFromDatabase(p.getUniqueId(),async);
            }
        } else {
            for (String uuidString : playerSection.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                this.loadFromDatabase(uuid, async);
            }
        }

    }

    @Override
    public void loadFromDatabase(UUID uuid, boolean async) {
        if (this.isConnectionClosed()){
            plugin.debug("Connection not established to players.yml");
            return;
        }

        if (uuid == null) {
            plugin.error("UUID in player.yml is null");
            return;
        }
        String path = this.getPlayerPath(uuid);
        if (!this.getPlayerConfig().contains(path)) return;
        if (this.containsPlayerData(uuid, false)){
            this.playerData.remove(uuid);
        }
        Map<Integer, Map<String, Integer>> unlockedOres = new HashMap<>();
        ConfigurationSection playerSection = this.getPlayerConfig().getConfigurationSection(path);
        if (playerSection != null && playerSection.contains("dynamic-ores")) {
            ConfigurationSection dynSec = playerSection.getConfigurationSection("dynamic-ores");
            if (dynSec != null) {
                for (String modeKey : dynSec.getKeys(false)) {
                    try {
                        int modeId = Integer.parseInt(modeKey);
                        Map<String, Integer> ores = new HashMap<>();
                        ConfigurationSection modeSec = dynSec.getConfigurationSection(modeKey);
                        if (modeSec != null) {
                            for (String oreId : modeSec.getKeys(false)) {
                                ores.put(oreId, modeSec.getInt(oreId));
                            }
                        }
                        unlockedOres.put(modeId, ores);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        PlayerData data = new PlayerData(uuid, unlockedOres);
        this.playerData.put(data.getUUID(), data);
        this.loadPistonsFromDatabase(uuid);
    }

    @Override
    public void saveToDatabase(UUID uuid, boolean async) {
        PlayerData data = this.getPlayerData(uuid);
        if (data == null) return;
        this.saveToDatabase(data, async);
    }

    public void saveToDatabase(PlayerData data, boolean async) {
        if (this.isConnectionClosed()) return;
        UUID uuid = data.getUUID();
        String path = this.getPlayerPath(uuid);

        Map<Integer, Map<String, Integer>> unlockedOres = data.getUnlockedOres();
        this.getPlayerConfig().set(path + ".dynamic-ores", null); // Clear old
        if (unlockedOres != null && !unlockedOres.isEmpty()) {
            for (Map.Entry<Integer, Map<String, Integer>> modeEntry : unlockedOres.entrySet()) {
                for (Map.Entry<String, Integer> oreEntry : modeEntry.getValue().entrySet()) {
                    this.getPlayerConfig().set(path + ".dynamic-ores." + modeEntry.getKey() + "." + oreEntry.getKey(), oreEntry.getValue());
                }
            }
        }

        /* SAVING THE GENERATING PISTONS */
        savePistonsToDatabase(data.getUUID());

        this.savePlayerConfig();
    }

    private String getPlayerPath(UUID uuid) {
        return "players." + uuid.toString();
    }

    public void reloadPlayerConfig() {
        if (this.playerConfigFile == null) {
            this.playerConfigFile = new File(new File(plugin.getDataFolder(), "Data"), "players.yml");
            this.playerConfig = YamlConfiguration.loadConfiguration(this.playerConfigFile);

        }
    }

    //Return the player config
    public FileConfiguration getPlayerConfig() {

        if (this.playerConfigFile == null) this.reloadPlayerConfig();

        return this.playerConfig;

    }

    //Save the player config
    public void savePlayerConfig() {

        if (this.playerConfig == null || this.playerConfigFile == null) return;

        try {
            this.getPlayerConfig().save(this.playerConfigFile);
        } catch (IOException ex) {
            plugin.getServer().getLogger().log(Level.SEVERE, "Could not save Player config to " + this.playerConfigFile + "!", ex);
        }

    }

    @Override
    public boolean isConnectionClosed() {
        return playerConfig == null || playerConfigFile == null;
    }

    @Override
    public void savePistonsToDatabase(UUID uuid) {
        GenPiston[] generatedPistons = blockManager.getGenPistonsByUUID(uuid);

        if (generatedPistons != null
                && generatedPistons.length > 0) {
            List<String> locations = new ArrayList<>();
            for (GenPiston piston : generatedPistons) {
                if (piston == null || piston.getLoc() == null || !piston.hasBeenUsed() || !piston.getLoc().getBlock().getType().equals(Material.PISTON))
                    continue;

                String serializedLoc = StringUtils.serializeLoc(piston.getLoc());
                if (!locations.contains(serializedLoc)) locations.add(serializedLoc);
            }
            if (!locations.isEmpty()) this.getPlayerConfig().set(this.getPlayerPath(uuid) + ".pistons", locations);
        }
    }

    @Override
    public void loadPistonsFromDatabase(UUID uuid) {
        String path = this.getPlayerPath(uuid);
        if (!this.getPlayerConfig().contains(path + ".pistons")) return;
        List<String> locations = this.getPlayerConfig().getStringList(path + ".pistons");

        for (String stringLoc : locations) {
            Location loc = StringUtils.deserializeLoc(stringLoc);
            if (loc == null) {
                plugin.error("Unknown location in players.yml under UUID: " + uuid + ".pistons" + stringLoc);
                continue;
            }
            World world = loc.getWorld();
            if (world == null) {
                plugin.error("Unknown world in players.yml under UUID: " + uuid + ".pistons: " + stringLoc);
                continue;
            }
            if (loc.getWorld().getBlockAt(loc).getType() != Material.PISTON) continue;
            blockManager.getKnownGenPistons().remove(loc);
            GenPiston piston = new GenPiston(loc, uuid);
            piston.setHasBeenUsed(true);
            blockManager.addKnownGenPiston(piston);
        }

    }

    @Override
    public String getType() {
        return "YAML";
    }


}
