package fr.danakube.danacobblegen.GUI;

import com.cryptomorin.xseries.XMaterial;
import fr.danakube.danacobblegen.API.DynamicOre;
import fr.danakube.danacobblegen.API.OreUpgrade;
import fr.danakube.danacobblegen.CustomCobbleGen;
import fr.danakube.danacobblegen.Files.Lang;
import fr.danakube.danacobblegen.Files.Setting;
import fr.danakube.danacobblegen.Managers.DynamicGeneratorManager;
import fr.danakube.danacobblegen.Requirements.Requirement;
import fr.danakube.danacobblegen.Requirements.RequirementType;
import fr.danakube.danacobblegen.Utils.ItemLib;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GUIManager {

	private static GUIManager instance = null;
	private final ItemStack backgroundItem = new ItemLib(XMaterial.GRAY_STAINED_GLASS_PANE.parseMaterial(), 1, (short) 7, " ").create();
	private final DynamicGeneratorManager dgm = DynamicGeneratorManager.getInstance();
	private final CustomCobbleGen plugin = CustomCobbleGen.getInstance();
	
	public static GUIManager getInstance() {
		if (instance == null) instance = new GUIManager();
		return instance;
	}

	public class MainGUI {
		private final int guiSize = 54; // 6 rows
		private final CustomHolder ch = new CustomHolder(guiSize, Lang.GUI_PREFIX.toString());
		private final Player player;
		private final int modeId = 0; // Option B: Direct to default generator

		public MainGUI(Player p) {
			player = p;
			Map<String, DynamicOre> modeOres = dgm.getDynamicOresByMode().getOrDefault(modeId, new HashMap<>());
			
			int slot = 10;
			for (DynamicOre ore : modeOres.values()) {
                // Ensure we don't go out of bounds
                if (slot > 43) break;
                if (slot % 9 == 8) slot += 2; // skip last col and first col of next row

				int level = plugin.getPlayerDatabase().getPlayerData(p.getUniqueId()).getOreLevel(modeId, ore.getId());
				ItemStack item = new ItemStack(ore.getIcon());
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', ore.getDisplayName()));
				List<String> lore = new ArrayList<>();
				lore.add(" ");

				if (level < 0) {
					// Locked
					lore.add(ChatColor.RED + "Locked");
					lore.add(ChatColor.GRAY + "Requirements to unlock:");
					for (Requirement r : ore.getUnlockRequirements()) {
						if (r.getRequirementType() == RequirementType.MONEY) {
							lore.add(ChatColor.YELLOW + "$ " + r.getRequirementValue());
						} else if (r.getRequirementType() == RequirementType.XP) {
							lore.add(ChatColor.GREEN + "" + r.getRequirementValue() + " XP Levels");
						}
					}
					meta.setLore(lore);
					item.setItemMeta(meta);
					Icon icon = new Icon(item);
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
							player1.sendMessage(Lang.PREFIX.toString() + ChatColor.GREEN + "You have unlocked " + ChatColor.translateAlternateColorCodes('&', ore.getDisplayName()) + "!");
							new MainGUI(player1).open();
						} else {
							player1.sendMessage(Lang.PREFIX.toString() + Lang.GUI_CAN_NOT_AFFORD.toString());
						}
					});
					ch.setIcon(slot, icon);
				} else {
					// Unlocked
					double currentPercentage = ore.getStartPercentage();
					if (level > 0) {
						OreUpgrade currentUpgrade = ore.getUpgrade(level);
						if (currentUpgrade != null) currentPercentage = currentUpgrade.getPercentage();
					}
					lore.add(ChatColor.GREEN + "Unlocked");
					lore.add(ChatColor.GRAY + "Current Level: " + ChatColor.AQUA + level);
					lore.add(ChatColor.GRAY + "Current Percentage: " + ChatColor.AQUA + currentPercentage + "%");
					
					OreUpgrade nextUpgrade = ore.getUpgrade(level + 1);
					if (nextUpgrade == null) {
						lore.add(" ");
						lore.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "MAX LEVEL");
						meta.setLore(lore);
						item.setItemMeta(meta);
						Icon icon = new Icon(item);
						ch.setIcon(slot, icon);
					} else {
						lore.add(" ");
						lore.add(ChatColor.GRAY + "Next Level Percentage: " + ChatColor.AQUA + nextUpgrade.getPercentage() + "%");
						lore.add(ChatColor.GRAY + "Requirements to upgrade:");
						for (Requirement r : nextUpgrade.getRequirements()) {
							if (r.getRequirementType() == RequirementType.MONEY) {
								lore.add(ChatColor.YELLOW + "$ " + r.getRequirementValue());
							} else if (r.getRequirementType() == RequirementType.XP) {
								lore.add(ChatColor.GREEN + "" + r.getRequirementValue() + " XP Levels");
							}
						}
						meta.setLore(lore);
						item.setItemMeta(meta);
						Icon icon = new Icon(item);
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
								player1.sendMessage(Lang.PREFIX.toString() + ChatColor.GREEN + "You have upgraded " + ChatColor.translateAlternateColorCodes('&', ore.getDisplayName()) + "!");
								new MainGUI(player1).open();
							} else {
								player1.sendMessage(Lang.PREFIX.toString() + Lang.GUI_CAN_NOT_AFFORD.toString());
							}
						});
						ch.setIcon(slot, icon);
					}
				}
				slot++;
			}

			// Add buffer and total stats item
			ItemStack statsItem = new ItemStack(Material.BOOK);
			ItemMeta statsMeta = statsItem.getItemMeta();
			statsMeta.setDisplayName(ChatColor.GOLD + "Generator Stats");
			List<String> statsLore = new ArrayList<>();
			Map<Material, Double> rates = dgm.getRatesForPlayer(p.getUniqueId(), modeId);
			statsLore.add(" ");
			statsLore.add(ChatColor.GRAY + "Current Generation Rates:");
			for (Map.Entry<Material, Double> entry : rates.entrySet()) {
				statsLore.add(ChatColor.YELLOW + " - " + entry.getKey().name() + ": " + String.format("%.2f", entry.getValue()) + "%");
			}
			statsMeta.setLore(statsLore);
			statsItem.setItemMeta(statsMeta);
			ch.setIcon(4, new Icon(statsItem));

			for (int i = 0; i < guiSize; i++) {
				if (ch.getIcon(i) == null) {
					ch.setIcon(i, new Icon(backgroundItem));
				}
			}
		}

		public void open() {
			Inventory inventory = ch.getInventory();
			player.openInventory(inventory);
		}
	}
}
