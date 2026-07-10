package fr.danakube.danacobblegen.databases;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.danakube.danacobblegen.Files.Setting;
import fr.danakube.danacobblegen.Managers.GenPiston;
import fr.danakube.danacobblegen.Utils.Response;
import fr.danakube.danacobblegen.Utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.Material;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MySQLPlayerDatabase extends PlayerDatabase {

	private HikariDataSource ds;
    private String HOST;
    private String TABLE_NAME;
    private String DATABASE_NAME;
    
	public MySQLPlayerDatabase() {
		super();
	}
	
	private Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
	
	@Override
	public Response<String> establishConnection() {
		HikariConfig databaseConfig = new HikariConfig();
		HOST = Setting.DATABASE_HOST.getString();
		DATABASE_NAME = Setting.DATABASE_DATABASE.getString();
		String jdbcUrl = "jdbc:mysql://" + HOST + "/" +  DATABASE_NAME + "?useSSL=false";
		databaseConfig.setJdbcUrl(jdbcUrl);
		databaseConfig.setUsername(Setting.DATABASE_USERNAME.getString());
		databaseConfig.setPassword(Setting.DATABASE_PASSWORD.getString());
		databaseConfig.addDataSourceProperty( "cachePrepStmts" , "true" );
		databaseConfig.addDataSourceProperty( "prepStmtCacheSize" , "250" );
		databaseConfig.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
		TABLE_NAME = Setting.DATABASE_TABLE.getString().toUpperCase();
        ds = new HikariDataSource(databaseConfig);
        Response<String> response;
		plugin.debug("Establishing connection...");
        try {
			Connection connection = this.getConnection();
			PreparedStatement stmt = null;
	        ResultSet rs = null;
	        try {
	        	stmt = connection.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?",
						ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_UPDATABLE);
				stmt.setString(1, DATABASE_NAME);
				stmt.setString(2, TABLE_NAME);
				rs = stmt.executeQuery();

				if(!rs.next()) {
					stmt.close();
					rs.close();
					stmt = connection.prepareStatement("CREATE TABLE `" + TABLE_NAME + "` (uuid VARCHAR(36), dynamic_ores TEXT, pistons TEXT)",
							ResultSet.TYPE_SCROLL_INSENSITIVE,
							ResultSet.CONCUR_UPDATABLE);
					stmt.execute();
				} else {
                    stmt.close();
                    rs.close();
                    stmt = connection.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = 'selected_tiers'");
                    stmt.setString(1, DATABASE_NAME);
                    stmt.setString(2, TABLE_NAME);
                    rs = stmt.executeQuery();
                    if(rs.next()) {
                        stmt.close();
                        stmt = connection.prepareStatement("ALTER TABLE `" + TABLE_NAME + "` DROP COLUMN `selected_tiers`, DROP COLUMN `purchased_tiers`, ADD COLUMN `dynamic_ores` TEXT");
                        stmt.execute();
                        plugin.log("&cAuto updated MySQL table to new dynamic_ores format. Old tiers were reset.");
                    }
					plugin.debug("Found table " +  TABLE_NAME + " in database");
				}
	        } catch (SQLException e) {
				return new Response<>("Failed to connect to " + HOST + "/" + DATABASE_NAME + " - Unsupported database", true);
			} finally {
	        	if(stmt != null) stmt.close();
	        	if(rs != null) rs.close();
	        }
			
			plugin.debug("Connected to " + HOST + "/" + DATABASE_NAME);
			response = new Response<>("Connected to " + HOST + "/" + DATABASE_NAME, false);
		} catch (SQLException e) {
        	plugin.error("Failed to connect to " + HOST + "/" + DATABASE_NAME);
        	plugin.error(e.getMessage());
        	if(ds != null){
        		ds = null;
        	}
			response =  new Response<>("Failed to connect to " + HOST + "/" + DATABASE_NAME +  " - " + e.getMessage(), true);
		}
		if(response.isError()) plugin.error(response.getResult());
		else plugin.debug("Players is now setup&2 \u2713");
        return response;
	}

	@Override
	public void reloadConnection() {
		if(this.isConnectionClosed()) return;
		this.closeConnection();
		this.establishConnection();
	}

	@Override
	public void closeConnection() {
		if(ds != null) ds.close();
	}

	@Override
	public boolean isConnectionClosed() {
		return ds == null || ds.isClosed();
	}

	private String getDynamicOresString(PlayerData data){
		StringJoiner dynamicOres = new StringJoiner(";");
        Map<Integer, Map<String, Integer>> unlocked = data.getUnlockedOres();
        if (unlocked != null) {
            for (Map.Entry<Integer, Map<String, Integer>> modeEntry : unlocked.entrySet()) {
                for (Map.Entry<String, Integer> oreEntry : modeEntry.getValue().entrySet()) {
                    dynamicOres.add(modeEntry.getKey() + ":" + oreEntry.getKey() + ":" + oreEntry.getValue());
                }
            }
        }
		return dynamicOres.toString();
	}

	@Override
	protected void addToDatabase(PlayerData data){
		this.addToDatabase(data, true);
	}
	@Override
	protected void addToDatabase(PlayerData data, boolean async) {
		if(this.isConnectionClosed()) return;
		Runnable r = () -> {
			try {
				Connection connection = getConnection();
				PreparedStatement stmt = null;
				String dynamicOres = getDynamicOresString(data);
				try {
					UUID uuid = data.getUUID();
					stmt = connection.prepareStatement("INSERT INTO `" + TABLE_NAME +"` (`uuid`, `dynamic_ores`, `pistons`) VALUES (?,?,?)");
					stmt.setString(1, uuid.toString());
					stmt.setString(2, dynamicOres);
					stmt.setString(3, getPistonString(uuid));
					if(stmt.executeUpdate() <= 0) {
						plugin.error("Failed to add player data for uuid " + uuid);
					}

				}finally {
					if(stmt != null) stmt.close();
				}

				plugin.debug("Connected to " + HOST + "/" + DATABASE_NAME);
			} catch (SQLException e) {
				plugin.error("Failed to connect to " + HOST + "/" + DATABASE_NAME);
				plugin.error(e.getMessage());
				if(ds != null){
					ds = null;
				}
			}
		};
		if(async) Bukkit.getScheduler().runTaskAsynchronously(plugin, r);
		else r.run();
	}

	@Override
	public void loadEverythingFromDatabase(boolean async) {
		if(this.isConnectionClosed()) return;
		this.playerData = new HashMap<>();
		blockManager.setKnownGenPistons(new HashMap<>());

		Runnable r = () -> {
			plugin.debug("Loading everything from database:", this.getType());
			try {
				Connection connection = getConnection();
				try (PreparedStatement stmt = connection.prepareStatement("SELECT uuid, dynamic_ores, pistons FROM `" + TABLE_NAME + "`"); ResultSet rs = stmt.executeQuery()) {
					while (rs.next()) {
						String result = rs.getString("uuid");
						if (result == null) continue;
						UUID uuid = UUID.fromString(result);
						if (!load(uuid, rs.getString("dynamic_ores"))) {
							plugin.error("Failed loading user data for " + uuid);
						}
						if (!loadPiston(uuid, rs.getString("pistons"))) {
							plugin.error("Failed loading piston data for " + uuid);
						}
					}

				}
				plugin.debug("Connected to " + HOST + "/" + DATABASE_NAME);
			} catch (SQLException e) {
				plugin.error("Failed to connect to " + HOST + "/" + DATABASE_NAME);
				plugin.error(e.getMessage());
				if(ds != null){
					ds = null;
				}
			}
		};
		if(async) Bukkit.getScheduler().runTaskAsynchronously(plugin, r);
		else r.run();
	}

	public void loadFromDatabase(UUID uuid, boolean async) {
		if(this.isConnectionClosed()) return;
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				Connection connection = getConnection();
				PreparedStatement stmt = null;
				ResultSet rs = null;
				try {
					stmt = connection.prepareStatement("SELECT dynamic_ores, pistons FROM `" + TABLE_NAME +"` WHERE uuid = ?");
					stmt.setString(1, uuid.toString());
					rs = stmt.executeQuery();
					if(rs.next()) {
						if(!load(uuid, rs.getString("dynamic_ores"))) {
							plugin.error("Failed loading user data for " + uuid);
						}
						if(!loadPiston(uuid, rs.getString("pistons"))) {
							plugin.error("Failed loading piston data for " + uuid);
						}
					}else {
						plugin.debug(uuid + " is not in the database");
					}
				}finally {
					if(stmt != null) stmt.close();
					if(rs != null) rs.close();
				}

				plugin.debug("Connected to " + HOST + "/" + DATABASE_NAME);
			} catch (SQLException e) {
				plugin.error("Failed to connect to " + HOST + "/" + DATABASE_NAME);
				plugin.error(e.getMessage());
				if(ds != null){
					ds = null;
				}
			}
		});
		

	}

	private boolean load(UUID uuid, String dynamic_ores) {
		if(uuid == null) return false;
		Map<Integer, Map<String, Integer>> unlockedOres = new HashMap<>();
        if (dynamic_ores != null && !dynamic_ores.isEmpty()) {
            String[] split = dynamic_ores.split(";");
            for (String str : split) {
                if (str.isEmpty()) continue;
                String[] parts = str.split(":");
                if (parts.length >= 3) {
                    try {
                        int modeId = Integer.parseInt(parts[0]);
                        String oreId = parts[1];
                        int level = Integer.parseInt(parts[2]);
                        unlockedOres.computeIfAbsent(modeId, k -> new HashMap<>()).put(oreId, level);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

		PlayerData currentData = this.playerData.getOrDefault(uuid, null);
		if(currentData != null) {
			this.playerData.remove(currentData.getUUID());
		}
		this.playerData.put(uuid, new PlayerData(uuid, unlockedOres));
		return true;
	}
	
	private boolean loadPiston(UUID uuid, String pistons) {
		if(uuid == null || pistons == null) return false;
		String[] pistonsArray = pistons.split(",");
		for(String pistonLoc : pistonsArray) {
			Location loc = StringUtils.deserializeLoc(pistonLoc);
			if(loc == null) continue;
			World world = loc.getWorld();
			if(world == null) {
				plugin.error("Unknown world in database under UUID: " + uuid + " -> pistons with the value: " + pistonLoc);
				continue;
			}
			Block block = world.getBlockAt(loc);
			if(block == null) {
				plugin.error("Can't confirm block is piston in players.yml under UUID: " + uuid + ".pistons at " + pistonLoc);
				continue;
			}
			
			else if(loc.getWorld().getBlockAt(loc).getType() != Material.PISTON) continue;
			blockManager.getKnownGenPistons().remove(loc);
			GenPiston piston = new GenPiston(loc, uuid);
			piston.setHasBeenUsed(true);
			blockManager.addKnownGenPiston(piston);
		}
		return true;
	}
	
	@Override
	public void saveToDatabase(UUID uuid, boolean async) {
		PlayerData data = this.getPlayerData(uuid);
		if(data == null) return;
		this.saveToDatabase(data, async);
	}

	@Override
	public void saveToDatabase(PlayerData data, boolean async) {
		if(this.isConnectionClosed()) return;
		Runnable r = () -> {
			try {
				Connection connection = getConnection();
				PreparedStatement stmt = null;

				String dynamicOres = getDynamicOresString(data);
				try {
					UUID uuid = data.getUUID();
					stmt = connection.prepareStatement("UPDATE `" + TABLE_NAME +"` SET dynamic_ores = ?, pistons = ? WHERE uuid = ?");
					stmt.setString(1, dynamicOres);
					stmt.setString(2, getPistonString(uuid));
					stmt.setString(3, uuid.toString());

					if(stmt.executeUpdate() <= 0) {
						addToDatabase(data);
					}

				}finally {
					if(stmt != null) stmt.close();
				}

				plugin.debug("Connected to " + HOST + "/" + DATABASE_NAME);
			} catch (SQLException e) {
				plugin.error("Failed to connect to " + HOST + "/" + DATABASE_NAME);
				plugin.error(e.getMessage());
				if(ds != null){
					ds = null;
				}
			}
		};
		if(async) Bukkit.getScheduler().runTaskAsynchronously(plugin, r);
		else r.run();
	}

	private String getPistonString(UUID uuid) {
        StringJoiner pistonsString = new StringJoiner(",");
        GenPiston[] pistons = blockManager.getGenPistonsByUUID(uuid);
        if(pistons == null || pistons.length == 0) return "";
		for (GenPiston piston : pistons) {
			pistonsString.add(StringUtils.serializeLoc(piston.getLoc()));
		}
        return pistonsString.toString();
	}

	@Override
	public void savePistonsToDatabase(UUID uuid) {
		if(this.isConnectionClosed()) return;

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				Connection connection = getConnection();
				try (PreparedStatement stmt = connection.prepareStatement("UPDATE `" + TABLE_NAME +"` SET pistons = ? WHERE uuid = ?")) {
					stmt.setString(1, getPistonString(uuid));
					stmt.setString(2, uuid.toString());

					if (stmt.executeUpdate() >= 0) {
						plugin.error("Failed to save piston data for uuid " + uuid);
					}

				}

				plugin.debug("Connected to " + HOST + "/" + DATABASE_NAME);
			} catch (SQLException e) {
				plugin.error("Failed to connect to " + HOST + "/" + DATABASE_NAME);
				plugin.error(e.getMessage());
				if(ds != null){
					ds = null;
				}
			}
		});
		

	}

	@Override
	public void loadPistonsFromDatabase(UUID uuid) {
		if(this.isConnectionClosed()) return;

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				Connection connection = getConnection();
				PreparedStatement stmt = null;
				ResultSet rs = null;
				try {
					stmt = connection.prepareStatement("SELECT pistons FROM `" + TABLE_NAME +"` WHERE uuid = ?");
					stmt.setString(1, uuid.toString());
					rs = stmt.executeQuery();
					if(rs.first()) {
						if(!loadPiston(uuid, rs.getString("pistons"))) {
							plugin.error("Failed loading piston data for " + uuid);
						}
					}else {
						plugin.debug(uuid + " is not in the database");
					}
				}finally {
					if(stmt != null) stmt.close();
					if(rs != null) rs.close();
				}

				plugin.debug("Connected to " + HOST + "/" + DATABASE_NAME);
			} catch (SQLException e) {
				plugin.error("Failed to connect to " + HOST + "/" + DATABASE_NAME);
				plugin.error(e.getMessage());
				if(ds != null){
					ds = null;
				}
			}
		});
		
	}
	
	@Override
	public String getType() {
		return "MYSQL";
	}

}
