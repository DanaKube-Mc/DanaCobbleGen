package fr.danakube.danacobblegen.Requirements;

import fr.danakube.danacobblegen.Files.Lang;
import org.bukkit.entity.Player;
import java.util.List;

public class XpRequirement implements Requirement{
	
	private final int xpNeeded;
	
	public XpRequirement(int xpNeeded) {
		if(xpNeeded < 0) xpNeeded = 0;
		this.xpNeeded = xpNeeded;
	}
	
	@Override
	public boolean furfillsRequirement(Player p) {
		return p.getLevel() >= this.getRequirementValue();
	}

	@Override
	public RequirementType getRequirementType() {
		return RequirementType.XP;
	}

	@Override
	public int getRequirementValue() {
		return this.xpNeeded;
	}

	@Override
	public List<String> addAvailableString(List<String> lore) {
		lore.add(Lang.GUI_PRICE_XP_AFFORD.toString());
		return lore;
	}

	@Override
	public List<String> addUnavailableString(List<String> lore) {
		lore.add(Lang.GUI_PRICE_XP_EXPENSIVE.toString());
		return lore;
	}

	@Override
	public void onPurchase(Player p) {
		int xpPriceInLevels = this.getRequirementValue();
		p.setLevel(p.getLevel()-xpPriceInLevels);
	}
	
	

}
