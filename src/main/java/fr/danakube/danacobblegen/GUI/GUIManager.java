package fr.danakube.danacobblegen.GUI;

import com.cryptomorin.xseries.XMaterial;
import fr.danakube.danacobblegen.API.DynamicOre;
import fr.danakube.danacobblegen.API.OreUpgrade;
import fr.danakube.danacobblegen.CustomCobbleGen;
import fr.danakube.danacobblegen.Files.Lang;
import fr.danakube.danacobblegen.Managers.DynamicGeneratorManager;
import fr.danakube.danacobblegen.Requirements.Requirement;
import fr.danakube.danacobblegen.Requirements.RequirementType;
import fr.danakube.danacobblegen.Utils.ItemLib;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GUIManager {

	private static GUIManager instance = null;
	private final DynamicGeneratorManager dgm = DynamicGeneratorManager.getInstance();
	private final CustomCobbleGen plugin = CustomCobbleGen.getInstance();
	
	public static GUIManager getInstance() {
		if (instance == null) instance = new GUIManager();
		return instance;
	}

	public class MainGUI {
		private final Player player;
		private final int modeId = 0; // Directly to default generator mode
		private final CustomHolder ch;
		private final int guiSize;

		public MainGUI(Player p) {
			player = p;
			FileConfiguration guiConfig = plugin.guiConfig;

			// Load GUI Settings
			String title = Lang.color(guiConfig.getString("main-menu.title", "&3&lGenerator Upgrades"));
			guiSize = guiConfig.getInt("main-menu.size", 54);
			ch = new CustomHolder(guiSize, title);

			Map<String, DynamicOre> modeOres = dgm.getDynamicOresByMode().getOrDefault(modeId, new HashMap<>());
			List<Integer> oreSlots = guiConfig.getIntegerList("main-menu.ores.slots");
			if (oreSlots.isEmpty()) {
				// Default fallback
				oreSlots = Arrays.asList(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25);
			}

			List<Integer> availableSlots = new ArrayList<>(oreSlots);
			
			// Remove slots that are forcefully requested
			for (DynamicOre ore : modeOres.values()) {
				if (ore.getSlot() >= 0) {
					availableSlots.remove(Integer.valueOf(ore.getSlot()));
				}
			}

			int currentSlotIndex = 0;
			
			for (DynamicOre ore : modeOres.values()) {
				int slot;
				if (ore.getSlot() >= 0) {
					slot = ore.getSlot();
				} else {
					if (currentSlotIndex >= availableSlots.size()) break; // No more slots available
					slot = availableSlots.get(currentSlotIndex);
					currentSlotIndex++;
				}

				int level = plugin.getPlayerDatabase().getPlayerData(p.getUniqueId()).getOreLevel(modeId, ore.getId());
				ItemStack item = ore.getIconItem();
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(Lang.color(ore.getDisplayName()));

				List<String> rawLore;
				List<String> finalLore = new ArrayList<>();
				Icon icon = null;

				if (level < 0) {
					// Locked
					rawLore = guiConfig.getStringList("main-menu.ores.locked.lore");
					List<String> requirementsLines = new ArrayList<>();
					for (Requirement r : ore.getUnlockRequirements()) {
						requirementsLines.add(String.valueOf(r.getRequirementValue()));
					}

					for (String line : rawLore) {
						if (line.contains("%requirements%")) {
							finalLore.addAll(requirementsLines);
						} else {
							finalLore.add(Lang.color(line));
						}
					}

					meta.setLore(finalLore);
					item.setItemMeta(meta);
					icon = new Icon(item);
					icon.addClickAction(player1 -> {
						boolean canAfford = true;
						for (Requirement r : ore.getUnlockRequirements()) {
							if (!r.furfillsRequirement(player1)) {
								canAfford = false;
								break;
							}
						}
						if (canAfford) {
							for (Requirement r : ore.getUnlockRequirements()) r.onPurchase(player1);
							plugin.getPlayerDatabase().getPlayerData(player1.getUniqueId()).setOreLevel(modeId, ore.getId(), 0);
							player1.sendMessage(Lang.PREFIX.toString() + ChatColor.GREEN + "You have unlocked " + Lang.color(ore.getDisplayName()) + "!");
							new MainGUI(player1).open();
						} else {
							player1.sendMessage(Lang.PREFIX.toString() + Lang.GUI_CAN_NOT_AFFORD.toString());
						}
					});

				} else {
					// Unlocked
					double currentPercentage = ore.getStartPercentage();
					if (level > 0) {
						OreUpgrade currentUpgrade = ore.getUpgrade(level);
						if (currentUpgrade != null) currentPercentage = currentUpgrade.getPercentage();
					}

					OreUpgrade nextUpgrade = ore.getUpgrade(level + 1);
					if (nextUpgrade == null) {
						// Max Level
						rawLore = guiConfig.getStringList("main-menu.ores.max-level.lore");
						for (String line : rawLore) {
							line = line.replace("%level%", String.valueOf(level));
							line = line.replace("%current_percentage%", String.valueOf(currentPercentage));
							finalLore.add(Lang.color(line));
						}
						meta.setLore(finalLore);
						item.setItemMeta(meta);
						icon = new Icon(item);
					} else {
						// Upgradable
						rawLore = guiConfig.getStringList("main-menu.ores.unlocked.lore");
						List<String> requirementsLines = new ArrayList<>();
						for (Requirement r : nextUpgrade.getRequirements()) {
							requirementsLines.add(String.valueOf(r.getRequirementValue()));
						}

						for (String line : rawLore) {
							if (line.contains("%requirements%")) {
								finalLore.addAll(requirementsLines);
							} else {
								line = line.replace("%level%", String.valueOf(level));
								line = line.replace("%current_percentage%", String.valueOf(currentPercentage));
								line = line.replace("%next_percentage%", String.valueOf(nextUpgrade.getPercentage()));
								finalLore.add(Lang.color(line));
							}
						}
						
						meta.setLore(finalLore);
						item.setItemMeta(meta);
						icon = new Icon(item);
						icon.addClickAction(player1 -> {
							boolean canAfford = true;
							for (Requirement r : nextUpgrade.getRequirements()) {
								if (!r.furfillsRequirement(player1)) {
									canAfford = false;
									break;
								}
							}
							if (canAfford) {
								for (Requirement r : nextUpgrade.getRequirements()) r.onPurchase(player1);
								plugin.getPlayerDatabase().getPlayerData(player1.getUniqueId()).setOreLevel(modeId, ore.getId(), level + 1);
								player1.sendMessage(Lang.PREFIX.toString() + ChatColor.GREEN + "You have upgraded " + Lang.color(ore.getDisplayName()) + "!");
								new MainGUI(player1).open();
							} else {
								player1.sendMessage(Lang.PREFIX.toString() + Lang.GUI_CAN_NOT_AFFORD.toString());
							}
						});
					}
				}
				ch.setIcon(slot, icon);
				currentSlotIndex++;
			}

			// Add Stats Item
			if (guiConfig.getBoolean("main-menu.stats-item.enabled", true)) {
				int statsSlot = guiConfig.getInt("main-menu.stats-item.slot", 4);
				Material statsMat = XMaterial.matchXMaterial(guiConfig.getString("main-menu.stats-item.material", "BOOK")).orElse(XMaterial.BOOK).parseMaterial();
				ItemStack statsItem = new ItemStack(statsMat != null ? statsMat : Material.BOOK);
				if (statsMat == Material.PLAYER_HEAD) {
					String skullTexture = guiConfig.getString("main-menu.stats-item.skin", guiConfig.getString("main-menu.stats-item.skull", null));
					if (skullTexture != null && !skullTexture.isEmpty()) {
						statsItem = com.cryptomorin.xseries.profiles.builder.XSkull.createItem().profile(com.cryptomorin.xseries.profiles.objects.Profileable.detect(skullTexture)).apply();
					}
				}
				ItemMeta statsMeta = statsItem.getItemMeta();
				statsMeta.setDisplayName(Lang.color(guiConfig.getString("main-menu.stats-item.name", "&6Generator Stats")));
				
				List<String> rawStatsLore = guiConfig.getStringList("main-menu.stats-item.lore");
				List<String> finalStatsLore = new ArrayList<>();
				Map<Material, Double> rates = dgm.getRatesForPlayer(p.getUniqueId(), modeId);
				
				String rateFormat = guiConfig.getString("main-menu.stats-item.rates-format", "&e - %material%: %percentage%%");
				List<String> ratesLines = new ArrayList<>();
				for (Map.Entry<Material, Double> entry : rates.entrySet()) {
					String line = rateFormat
							.replace("%material%", entry.getKey().name())
							.replace("%percentage%", String.format(Locale.US, "%.2f", entry.getValue()));
					ratesLines.add(Lang.color(line));
				}

				for (String line : rawStatsLore) {
					if (line.contains("%rates%")) {
						finalStatsLore.addAll(ratesLines);
					} else {
						finalStatsLore.add(Lang.color(line));
					}
				}
				statsMeta.setLore(finalStatsLore);
				statsItem.setItemMeta(statsMeta);
				ch.setIcon(statsSlot, new Icon(statsItem));
			}

			// Add Filler Background
			if (guiConfig.getBoolean("main-menu.filler.enabled", true)) {
				Material fillerMat = XMaterial.matchXMaterial(guiConfig.getString("main-menu.filler.material", "GRAY_STAINED_GLASS_PANE")).orElse(XMaterial.GRAY_STAINED_GLASS_PANE).parseMaterial();
				String fillerName = Lang.color(guiConfig.getString("main-menu.filler.name", " "));
				ItemStack backgroundItem;
				if (fillerMat == Material.PLAYER_HEAD) {
					String skullTexture = guiConfig.getString("main-menu.filler.skin", guiConfig.getString("main-menu.filler.skull", null));
					if (skullTexture != null && !skullTexture.isEmpty()) {
						backgroundItem = com.cryptomorin.xseries.profiles.builder.XSkull.createItem().profile(com.cryptomorin.xseries.profiles.objects.Profileable.detect(skullTexture)).apply();
						ItemMeta bm = backgroundItem.getItemMeta();
						if (bm != null) {
							bm.setDisplayName(fillerName);
							backgroundItem.setItemMeta(bm);
						}
					} else {
						backgroundItem = new ItemLib(Material.PLAYER_HEAD, 1, (short) 3, fillerName).create();
					}
				} else {
					backgroundItem = new ItemLib(fillerMat != null ? fillerMat : Material.STONE, 1, (short) 7, fillerName).create();
				}
				List<Integer> fillerSlots = guiConfig.getIntegerList("main-menu.filler.slots");
				for (int i : fillerSlots) {
					if (i >= 0 && i < guiSize && ch.getIcon(i) == null) {
						ch.setIcon(i, new Icon(backgroundItem));
					}
				}
			}
		}

		public void open() {
			Inventory inventory = ch.getInventory();
			player.openInventory(inventory);
		}
	}
}
