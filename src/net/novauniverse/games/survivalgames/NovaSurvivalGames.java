package net.novauniverse.games.survivalgames;

import java.io.File;
import java.io.IOException;

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

import net.novauniverse.games.survivalgames.debug.DebugCommands;
import net.novauniverse.games.survivalgames.game.SurvivalGames;
import net.novauniverse.games.survivalgames.map.mapmodules.extendedspawnlocationconfig.ExtendedSpawnLocationConfig;
import net.novauniverse.games.survivalgames.map.mapmodules.extendedspawnlocationconfig.IWrapedMaterial;
import net.novauniverse.games.survivalgames.map.mapmodules.extendedspawnlocationconfig.WrapedMaterial;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.commons.utils.JSONFileUtils;
import net.zeeraa.novacore.spigot.NovaCore;
import net.zeeraa.novacore.spigot.abstraction.events.VersionIndependantPlayerAchievementAwardedEvent;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.GameManager;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodule.MapModuleManager;
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
	private int reconnectTime;

	private boolean useExtendedSpawnLocations;

	private SurvivalGames game;

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

	@Override
	public void onEnable() {
		NovaSurvivalGames.instance = this;

		saveDefaultConfig();

		allowReconnect = getConfig().getBoolean("allow_reconnect");
		combatTagging = getConfig().getBoolean("combat_tagging");
		reconnectTime = getConfig().getInt("player_elimination_delay");

		useExtendedSpawnLocations = getConfig().getBoolean("extended_spawn_location");

		File mapFolder = new File(this.getDataFolder().getPath() + File.separator + "Maps");
		File worldFolder = new File(this.getDataFolder().getPath() + File.separator + "Worlds");
		File lootTableFolder = new File(this.getDataFolder().getPath() + File.separator + "LootTables");

		File mapOverrides = new File(this.getDataFolder().getPath() + File.separator + "map_overrides.json");
		if (mapOverrides.exists()) {
			Log.info("Trying to read map overrides file");
			try {
				JSONObject mapFiles = JSONFileUtils.readJSONObjectFromFile(mapOverrides);

				boolean relative = mapFiles.getBoolean("relative");

				mapFolder = new File((relative ? this.getDataFolder().getPath() + File.separator : "") + mapFiles.getString("maps_folder"));
				worldFolder = new File((relative ? this.getDataFolder().getPath() + File.separator : "") + mapFiles.getString("worlds_folder"));
				lootTableFolder = new File((relative ? this.getDataFolder().getPath() + File.separator : "") + mapFiles.getString("loot_tables_folder"));

				Log.info("New paths:");
				Log.info("Map folder: " + mapFolder.getAbsolutePath());
				Log.info("World folder: " + worldFolder.getAbsolutePath());
				Log.info("Loot table folder:" + lootTableFolder.getAbsolutePath());
			} catch (JSONException | IOException e) {
				e.printStackTrace();
				Log.error("Failed to read map overrides from file " + mapOverrides.getAbsolutePath());
			}
		}

		GameManager.getInstance().setUseCombatTagging(combatTagging);

		try {
			FileUtils.forceMkdir(getDataFolder());
			FileUtils.forceMkdir(mapFolder);
			FileUtils.forceMkdir(worldFolder);
			FileUtils.forceMkdir(lootTableFolder);
		} catch (IOException e1) {
			e1.printStackTrace();
			Log.fatal("Failed to setup data directory");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		ModuleManager.enable(GameManager.class);
		ModuleManager.enable(GameLobby.class);
		ModuleManager.enable(CompassTracker.class);

		MapModuleManager.addMapModule("novauniverse.survivalgames.extendedspawnlocation.config", ExtendedSpawnLocationConfig.class);

		this.game = new SurvivalGames();

		GameManager.getInstance().loadGame(game);

		GUIMapVote mapSelector = new GUIMapVote();

		GameManager.getInstance().setMapSelector(mapSelector);

		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		Bukkit.getServer().getPluginManager().registerEvents(mapSelector, this);

		NovaCore.getInstance().getLootTableManager().loadAll(lootTableFolder);

		Log.info("SurvivalGames", "Loading maps from " + mapFolder.getPath());
		GameManager.getInstance().readMapsFromFolder(mapFolder, worldFolder);

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
	public void onVersionIndependantPlayerAchievementAwarded(VersionIndependantPlayerAchievementAwardedEvent e) {
		e.setCancelled(true);
	}
}