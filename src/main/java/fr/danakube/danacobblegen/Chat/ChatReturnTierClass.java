/**
 * CustomCobbleGen By @author Philip Flyvholm
 * ChatReturnTierClass.java
 */
package fr.danakube.danacobblegen.Chat;

import fr.danakube.danacobblegen.API.Tier;
import fr.danakube.danacobblegen.Files.Lang;
import org.bukkit.entity.Player;

/**
 * @author Philip
 *
 */
public class ChatReturnTierClass implements ChatReturn{

	private Player p;
	private Tier tier;
	private ChatReturnType type = ChatReturnType.CLASS;
	
	public ChatReturnTierClass(Player p, Tier tier) {
		this.p = p;
		this.tier = tier;
	}
	
	@Override
	public Player getPlayer() {
		return this.p;
	}

	@Override
	public void setPlayer(Player p) {
		this.p = p;
	}

	@Override
	public Tier getTier() {
		return this.tier;
	}

	@Override
	public void setTier(Tier tier) {
		this.tier = tier;
	}

	@Override
	public ChatReturnType getType() {
		return this.type;
	}

	@Override
	public void setType(ChatReturnType type) {
		this.type = type;
	}

	@Override
	public String validInput(String input) {
		if(input.trim().equals("")) {
			return Lang.CHATINPUT_INVALID.toString(input);
		}else if(input.contains(" ")) {
			return Lang.CHATINPUT_INVALID_NOSPACE.toString(input);
		}
		return "VALID";
	}

}
