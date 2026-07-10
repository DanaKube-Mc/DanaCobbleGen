package fr.danakube.danacobblegen.API;

import org.bukkit.inventory.ItemStack;
import fr.danakube.danacobblegen.Requirements.Requirement;
import java.util.List;
import java.util.Map;

public class DynamicOre {
    private final String id;
    private final String displayName;
    private final ItemStack iconItem;
    private final int supportedGenerationMode;
    private final int slot;
    private final List<Requirement> unlockRequirements;
    private final double startPercentage;
    private final Map<Integer, OreUpgrade> upgrades;

    public DynamicOre(String id, String displayName, ItemStack iconItem, int slot, int supportedGenerationMode, List<Requirement> unlockRequirements, double startPercentage, Map<Integer, OreUpgrade> upgrades) {
        this.id = id;
        this.displayName = displayName;
        this.iconItem = iconItem;
        this.slot = slot;
        this.supportedGenerationMode = supportedGenerationMode;
        this.unlockRequirements = unlockRequirements;
        this.startPercentage = startPercentage;
        this.upgrades = upgrades;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ItemStack getIconItem() {
        return iconItem.clone();
    }

    public int getSupportedGenerationMode() {
        return supportedGenerationMode;
    }

    public int getSlot() {
        return slot;
    }

    public List<Requirement> getUnlockRequirements() {
        return unlockRequirements;
    }

    public double getStartPercentage() {
        return startPercentage;
    }

    public Map<Integer, OreUpgrade> getUpgrades() {
        return upgrades;
    }

    public OreUpgrade getUpgrade(int level) {
        if (upgrades == null) return null;
        return upgrades.get(level);
    }

    public int getMaxLevel() {
        if (upgrades == null || upgrades.isEmpty()) return 0;
        int max = 0;
        for (int level : upgrades.keySet()) {
            if (level > max) max = level;
        }
        return max;
    }
}
