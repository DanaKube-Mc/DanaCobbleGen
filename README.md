![alt text](https://www.spigotmc.org/attachments/headerv2-jpg.476352/)
# CustomCobbleGen (v2.0.0 Dynamic Edition)
This is a Spigot plugin that changes cobblestone generators in Minecraft.

[![HitCount](http://hits.dwyl.com/phil14052/CustomCobbleGen.svg)](http://hits.dwyl.com/phil14052/CustomCobbleGen)

## 
With this simple plugin, you can change all aspects of the cobblestone generator, as known from Skyblocks servers. 

**NEW IN v2.0.0:** The plugin has been completely rewritten! We have moved away from the old "Tier" system and now feature a **Dynamic Percentage System**. Players no longer buy static tiers; instead, they start with a base generator and can individually unlock and upgrade the generation rates of different ores using money, XP or Island Levels.

### Features:
- **Dynamic Ore Upgrades**: Players can individually upgrade the percentage chance of specific ores spawning.
- **100% Customizable GUI**: Customize size, title, slots, lores, and background items completely via `gui.yml`.
- Plug and play installation (Comes with pre-setup default ores)
- Vault support (Buy upgrades with money)
- Disable specific worlds
- Placeholder API support
- Money, XP, and Level requirements for unlocking and upgrading ores
- uSkyBlock, BentoBox (bSkyBlock, AcidIsland + Addons), and SuperiorSkyblock2 Support

### Local GUI Placeholders (`gui.yml`)
When customizing your `gui.yml`, you can use the following local placeholders in your lore to display dynamic information about the player's generator:
- `%level%` : The player's current level for a specific ore.
- `%current_percentage%` : The current spawn chance percentage for a specific ore.
- `%next_percentage%` : The spawn chance percentage for the next level.
- `%requirements%` : Automatically formats and displays the cost (Money, XP, etc.) required to unlock or upgrade an ore based on your `lang.yml`.
- `%rates%` : Automatically lists all the active spawn rates for the generator.

### Commands & Permissions:
- `/ccg admin setlevel <player> <modeId> <oreId> <amount>` : Force a player's ore level to a specific amount. *(Permission: `customcobblegen.admin.level`)*
- `/ccg admin addlevel <player> <modeId> <oreId> <amount>` : Add levels to a player's ore. *(Permission: `customcobblegen.admin.level`)*
- `/ccg admin removelevel <player> <modeId> <oreId> <amount>` : Remove levels from a player's ore. *(Permission: `customcobblegen.admin.level`)*
*(Note: A level of `-1` means the ore is locked, `0` means unlocked but not upgraded)*

### Requirements:
- If you want to use money as a payment, then you need to have **Vault** installed.
- If you want to use the plugins placeholder in other plugins, then you need **PlaceholderAPI**.
- If you want to use Island Levels as a requirement, then you need to have either **BentoBox**, **SuperiorSkyblock2** or **uSkyBlock** installed.

### Placeholders (PlaceholderAPI):
The plugin supports PlaceholderAPI. With the v2.0.0 update, the format has changed to match the dynamic ores system:
- `%customcobblegen_ore_level_<modeId>_<oreId>%` : Returns the player's level for an ore.
- `%customcobblegen_ore_percentage_<modeId>_<oreId>%` : Returns the player's percentage for an ore.

*(Example: `%customcobblegen_ore_percentage_0_DIAMOND_ORE%`)*

### Future features:
https://trello.com/b/ibJ3WwHW/customcobblegen-plugin

I am open to new ideas/features. You can write to me over Spigot or on Github.

### Found an issue? Report it here: 
https://github.com/phil14052/CustomCobbleGen/issues
