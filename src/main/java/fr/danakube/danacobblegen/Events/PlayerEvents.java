package fr.danakube.danacobblegen.Events;

import fr.danakube.danacobblegen.CustomCobbleGen;
import fr.danakube.danacobblegen.Managers.BlockManager;
import fr.danakube.danacobblegen.databases.PlayerData;
import fr.danakube.danacobblegen.databases.PlayerDatabase;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerEvents implements Listener {

	private final BlockManager bm = BlockManager.getInstance();
	private final CustomCobbleGen plugin = CustomCobbleGen.getInstance();
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e){
		UUID uuid = e.getPlayer().getUniqueId();
		PlayerDatabase database = plugin.getPlayerDatabase();
		if(!database.containsPlayerData(uuid)) database.loadFromDatabase(uuid);
		PlayerData data = database.getPlayerData(uuid);
		if(data == null) {
			data = new PlayerData(uuid);
			database.setPlayerData(data);
		}
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		// Cleanup
		bm.cleanupExpiredPistons(p.getUniqueId());
		plugin.getPlayerDatabase().saveToDatabase(p.getUniqueId());
		bm.cleanupExpiredLocations();
	}
}
