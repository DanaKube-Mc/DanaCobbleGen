/**
 * CustomCobbleGen By @author Philip Flyvholm
 * ChatReturn.java
 */
package fr.danakube.danacobblegen.Chat;

import fr.danakube.danacobblegen.API.Tier;
import org.bukkit.entity.Player;

/**
 * @author Philip
 *
 */
public interface ChatReturn {

	Player getPlayer();
	void setPlayer(Player p);
	
	Tier getTier();
	void setTier(Tier tier);
	
	ChatReturnType getType();
	void setType(ChatReturnType type);
	
	String validInput(String input);
	
}
