package fr.danakube.danacobblegen.Requirements;

import org.bukkit.entity.Player;

import java.util.List;

public interface Requirement {
	
	boolean furfillsRequirement(Player p);
	
	RequirementType getRequirementType();
	
	int getRequirementValue();
	
	List<String> addAvailableString(List<String> lore);
	
	List<String> addUnavailableString(List<String> lore);
	
	void onPurchase(Player p);
}
