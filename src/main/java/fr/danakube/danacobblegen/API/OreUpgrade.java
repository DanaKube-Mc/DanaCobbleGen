package fr.danakube.danacobblegen.API;

import fr.danakube.danacobblegen.Requirements.Requirement;
import java.util.List;

public class OreUpgrade {
    private final int level;
    private final List<Requirement> requirements;
    private final double percentage;

    public OreUpgrade(int level, List<Requirement> requirements, double percentage) {
        this.level = level;
        this.requirements = requirements;
        this.percentage = percentage;
    }

    public int getLevel() {
        return level;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public double getPercentage() {
        return percentage;
    }
}
