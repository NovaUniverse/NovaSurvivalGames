package net.novauniverse.games.survivalgames;

import java.io.File;
import java.io.IOException;

import net.zeeraa.novacore.spigot.language.LanguageReader;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONException;
import org.json.JSONObject;

import net.novauniverse.games.survivalgames.commands.StartSGCountdown;
import net.novauniverse.games.survivalgames.debug.DebugCommands;
import net.novauniverse.games.survivalgames.game.SurvivalGames;
import net.novauniverse.games.survivalgames.map.mapmodules.extendedspawnlocationconfig.ExtendedSpawnLocationConfig;
import net.novauniverse.games.survivalgames.map.mapmodules.extendedspawnlocationconfig.IWrapedMaterial;
import net.novauniverse.games.survivalgames.map.mapmodules.extendedspawnlocationconfig.WrapedMaterial;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.commons.utils.JSONFileUtils;
import net.zeeraa.novacore.spigot.NovaCore;
import net.zeeraa.novacore.spigot.abstraction.events.VersionIndependentPlayerAchievementAwardedEvent;
import net.zeeraa.novacore.spigot.command.CommandRegistry;
import net.zeeraa.novacore.spigot.gameengine.NovaCoreGameEngine;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.GameManager;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodule.MapModuleManager;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.mapselector.selectors.RandomMapSelector;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.mapselector.selectors.guivoteselector.GUIMapVote;
import net.zeeraa.novacore.spigot.gameengine.module.modules.gamelobby.GameLobby;
import net.zeeraa.novacore.spigot.module.ModuleManager;
import net.zeeraa.novacore.spigot.module.modules.compass.CompassTracker;
import net.zeeraa.novacore.spigot.module.modules.compass.event.CompassTrackingEvent;

public class NovaSurvivalGames extends JavaPlugin implements Listener {
	public static final IWrapedMaterial DEFAULT_EXTENDED_SPAWN_FLOOR_MATERIAL = new WrapedMaterial(Material.BARRIER);
	private static NovaSurvivalGames instance;

	public static NovaSurvivalGames getInstance() {
		return instance;
	}

	private boolean allowReconnect;
	private boolean combatTagging;
	private boolean autoStartCountdown;
	private int reconnectTime;

	private boolean useExtendedSpawnLocations;
	private boolean disableEarlyBlockBreakCheck;

	private boolean disableDefaultEndSound;
	private boolean disableBuiltInCountdownSound;
	private boolean disableActionbar;
	private boolean disableChatCountdown;

	private SurvivalGames game;

	public boolean isDisableActionbar() {
		return disableActionbar;
	}

	public boolean isDisableBuiltInCountdownSound() {
		return disableBuiltInCountdownSound;
	}

	public void setDisableActionbar(boolean disableActionbar) {
		this.disableActionbar = disableActionbar;
	}

	public void setDisableBuiltInCountdownSound(boolean disableBuiltInCountdownSound) {
		this.disableBuiltInCountdownSound = disableBuiltInCountdownSound;
	}

	public boolean isAllowReconnect() {
		return allowReconnect;
	}

	public boolean isCombatTagging() {
		return combatTagging;
	}

	public int getReconnectTime() {
		return reconnectTime;
	}

	public SurvivalGames getGame() {
		return game;
	}

	public boolean isUseExtendedSpawnLocations() {
		return useExtendedSpawnLocations;
	}

	public void setUseExtendedSpawnLocations(boolean useExtendedSpawnLocations) {
		this.useExtendedSpawnLocations = useExtendedSpawnLocations;
	}

	public boolean isAutoStartCountdown() {
		return autoStartCountdown;
	}

	public boolean isDisableEarlyBlockBreakCheck() {
		return disableEarlyBlockBreakCheck;
	}

	public boolean isDisableDefaultEndSound() {
		return disableDefaultEndSound;
	}

	public void setDisableDefaultEndSound(boolean disableDefaultEndSound) {
		this.disableDefaultEndSound = disableDefaultEndSound;
	}

	public boolean isDisableChatCountdown() {
		return disableChatCountdown;
	}
	
	public void setDisableChatCountdown(boolean disableChatCountdown) {
		this.disableChatCountdown = disableChatCountdown;
	}
	
	@Override
	public void onEnable() {
		NovaSurvivalGames.instance = this;

		saveDefaultConfig();

		boolean disableNovaCoreGameLobby = getConfig().getBoolean("disable_novacore_gamelobby");

		disableActionbar = getConfig().getBoolean("disable_actionbar");
		disableBuiltInCountdownSound = getConfig().getBoolean("disable_built_in_countdown_sound");

		allowReconnect = getConfig().getBoolean("allow_reconnect");
		combatTagging = getConfig().getBoolean("combat_tagging");
		autoStartCountdown = getConfig().getBoolean("auto_start_countdown");
		reconnectTime = getConfig().getInt("player_elimination_delay");

		useExtendedSpawnLocations = getConfig().getBoolean("extended_spawn_location");
		disableEarlyBlockBreakCheck = getConfig().getBoolean("disable_early_block_break_check");

		disableDefaultEndSound = getConfig().getBoolean("disable_default_end_sound");
		
		disableChatCountdown = getConfig().getBoolean("disable_chat_countdown");

		File mapFolder = new File(this.getDataFolder().getPath() + File.separator + "Maps");
		File worldFolder = new File(this.getDataFolder().getPath() + File.separator + "Worlds");
		File lootTableFolder = new File(this.getDataFolder().getPath() + File.separator + "LootTables");

		String mapDataDirectoryNameOverride = "SurvivalGames";

		File overridesFile = new File(this.getDataFolder().getPath() + File.separator + "overrides.json");
		if (overridesFile.exists()) {
			try {
				JSONObject overrides = JSONFileUtils.readJSONObjectFromFile(overridesFile);

				if (overrides.has("map_data_folder_name")) {
					mapDataDirectoryNameOverride = overrides.getString("map_data_folder_name");
					Log.info("SurvivalGames", "Map data folder name override set to: " + mapDataDirectoryNameOverride);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.error("Failed to read overrides from file " + overridesFile.getAbsolutePath());
			}
		}

		if (NovaCoreGameEngine.getInstance().getRequestedGameDataDirectory() != null) {
			mapFolder = new File(NovaCoreGameEngine.getInstance().getRequestedGameDataDirectory().getAbsolutePath() + File.separator + mapDataDirectoryNameOverride + File.separator + "Maps");
			worldFolder = new File(NovaCoreGameEngine.getInstance().getRequestedGameDataDirectory().getAbsolutePath() + File.separator + mapDataDirectoryNameOverride + File.separator + "Worlds");
			lootTableFolder = new File(NovaCoreGameEngine.getInstance().getRequestedGameDataDirectory().getAbsolutePath() + File.separator + mapDataDirectoryNameOverride + File.separator + "LootTables");
		}

		File mapOverrides = new File(this.getDataFolder().getPath() + File.separator + "map_overrides.json");
		if (mapOverrides.exists()) {
			Log.info("SurvivalGames", "Trying to read map overrides file");
			try {
				JSONObject mapFiles = JSONFileUtils.readJSONObjectFromFile(mapOverrides);

				boolean relative = mapFiles.getBoolean("relative");

				mapFolder = new File((relative ? this.getDataFolder().getPath() + File.separator : "") + mapFiles.getString("maps_folder"));
				worldFolder = new File((relative ? this.getDataFolder().getPath() + File.separator : "") + mapFiles.getString("worlds_folder"));
				lootTableFolder = new File((relative ? this.getDataFolder().getPath() + File.separator : "") + mapFiles.getString("loot_tables_folder"));

				Log.info("SurvivalGames", "New paths:");
				Log.info("SurvivalGames", "Map folder: " + mapFolder.getAbsolutePath());
				Log.info("SurvivalGames", "World folder: " + worldFolder.getAbsolutePath());
				Log.info("SurvivalGames", "Loot table folder:" + lootTableFolder.getAbsolutePath());
			} catch (JSONException | IOException e) {
				e.printStackTrace();
				Log.error("SurvivalGames", "Failed to read map overrides from file " + mapOverrides.getAbsolutePath());
			}
		}

		GameManager.getInstance().setUseCombatTagging(combatTagging);

		try {
			FileUtils.forceMkdir(getDataFolder());
			Log.info("NovaSurvivalGames", "Loading language files...");
			try {
				LanguageReader.readFromJar(this.getClass(), "/lang/en-us.json");
			} catch (Exception e) {
				e.printStackTrace();
			}
			FileUtils.forceMkdir(mapFolder);
			FileUtils.forceMkdir(worldFolder);
			FileUtils.forceMkdir(lootTableFolder);
		} catch (IOException e1) {
			e1.printStackTrace();
			Log.fatal("SurvivalGames", "Failed to setup data directory");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		ModuleManager.enable(GameManager.class);

		if (!disableNovaCoreGameLobby) {
			ModuleManager.enable(GameLobby.class);
		}

		ModuleManager.enable(CompassTracker.class);

		MapModuleManager.addMapModule("novauniverse.survivalgames.extendedspawnlocation.config", ExtendedSpawnLocationConfig.class);

		this.game = new SurvivalGames(this);

		GameManager.getInstance().loadGame(game);

		Bukkit.getServer().getPluginManager().registerEvents(this, this);

		if (!disableNovaCoreGameLobby) {
			GUIMapVote mapSelector = new GUIMapVote();
			GameManager.getInstance().setMapSelector(mapSelector);
			Bukkit.getServer().getPluginManager().registerEvents(mapSelector, this);
		} else {
			GameManager.getInstance().setMapSelector(new RandomMapSelector());
		}

		NovaCore.getInstance().getLootTableManager().loadAll(lootTableFolder);

		CommandRegistry.registerCommand(new StartSGCountdown());

		Log.info(getName(), "Scheduled loading maps from " + mapFolder.getPath());
		GameManager.getInstance().readMapsFromFolderDelayed(mapFolder, worldFolder);

		new DebugCommands();
	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll((Plugin) this);
		Bukkit.getScheduler().cancelTasks(this);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onCompassTracking(CompassTrackingEvent e) {
		boolean enabled = false;
		if (GameManager.getInstance().isEnabled()) {
			if (GameManager.getInstance().hasGame()) {
				if (GameManager.getInstance().getActiveGame().hasStarted()) {
					enabled = true;
				}
			}
		}
		e.setCancelled(!enabled);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onVersionIndependantPlayerAchievementAwarded(VersionIndependentPlayerAchievementAwardedEvent e) {
		e.setCancelled(true);
	}
}