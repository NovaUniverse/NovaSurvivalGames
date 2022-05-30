package net.novauniverse.games.survivalgames.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.meta.FireworkMeta;

import net.novauniverse.games.survivalgames.NovaSurvivalGames;
import net.novauniverse.games.survivalgames.map.mapmodules.extendedspawnlocationconfig.ExtendedSpawnLocationConfig;
import net.novauniverse.games.survivalgames.map.mapmodules.extendedspawnlocationconfig.IWrapedMaterial;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.commons.timers.TickCallback;
import net.zeeraa.novacore.commons.utils.Callback;
import net.zeeraa.novacore.spigot.NovaCore;
import net.zeeraa.novacore.spigot.abstraction.VersionIndependantUtils;
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependantSound;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.GameEndReason;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.MapGame;
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.elimination.PlayerQuitEliminationAction;
import net.zeeraa.novacore.spigot.language.LanguageManager;
import net.zeeraa.novacore.spigot.teams.Team;
import net.zeeraa.novacore.spigot.teams.TeamManager;
import net.zeeraa.novacore.spigot.timers.BasicTimer;
import net.zeeraa.novacore.spigot.utils.PlayerUtils;
import net.zeeraa.novacore.spigot.utils.RandomFireworkEffect;

public class SurvivalGames extends MapGame implements Listener {
	private boolean started;
	private boolean ended;

	private final boolean randomStartLocation = false;
	private final int countdownTime = 20;

	private Map<UUID, Location> usedStartLocation;
	private Map<Team, Location> teamSpawnLocation;
	private Map<Location, Material> temporaryCageBlocks;

	private boolean countdownOver;

	public SurvivalGames() {
		super(NovaSurvivalGames.getInstance());

		this.started = false;
		this.ended = false;

		this.countdownOver = false;

		this.usedStartLocation = new HashMap<>();
		this.teamSpawnLocation = new HashMap<>();
		this.temporaryCageBlocks = new HashMap<>();
	}

	@Override
	public String getName() {
		return "survivalgames";
	}

	public String getDisplayName() {
		return "SurvivalGames";
	}

	@Override
	public PlayerQuitEliminationAction getPlayerQuitEliminationAction() {
		return NovaSurvivalGames.getInstance().isAllowReconnect() ? PlayerQuitEliminationAction.DELAYED : PlayerQuitEliminationAction.INSTANT;
	}

	@Override
	public int getPlayerEliminationDelay() {
		return NovaSurvivalGames.getInstance().getReconnectTime();
	}

	@Override
	public boolean eliminateIfCombatLogging() {
		return NovaSurvivalGames.getInstance().isCombatTagging();
	}

	@Override
	public boolean eliminatePlayerOnDeath(Player player) {
		return true;
	}

	@Override
	public boolean isPVPEnabled() {
		return countdownOver;
	}

	@Override
	public boolean autoEndGame() {
		return true;
	}

	@Override
	public boolean hasStarted() {
		return started;
	}

	@Override
	public boolean hasEnded() {
		return ended;
	}

	@Override
	public boolean isFriendlyFireAllowed() {
		return false;
	}

	@Override
	public boolean canAttack(LivingEntity attacker, LivingEntity target) {
		return countdownOver;
	}

	public boolean setCages(boolean state) {
		if (!hasActiveMap()) {
			return false;
		}

		if (!state) {
			// Clean up cage floor blocks
			temporaryCageBlocks.forEach((location, material) -> location.getBlock().setType(material));
			temporaryCageBlocks.clear();
		}

		getActiveMap().getStarterLocations().forEach(location -> setStartCage(location, state));

		return true;
	}

	public void setStartCage(Location location, boolean state) {
		Material cageMaterial = state ? Material.BARRIER : Material.AIR;

		if (NovaSurvivalGames.getInstance().isUseExtendedSpawnLocations()) {
			// Box
			for (int x = -2; x < 3; x++) {
				for (int y = 0; y < 4; y++) {
					for (int z = -2; z < 3; z++) {
						location.clone().add(x, y, z).getBlock().setType(cageMaterial);
					}
				}
			}

			// Inside
			if (state == true) {
				for (int x = -1; x < 2; x++) {
					for (int y = 0; y < 3; y++) {
						for (int z = -1; z < 2; z++) {
							location.clone().add(x, y, z).getBlock().setType(Material.AIR);
						}
					}
				}
			}

			if (state) {
				// Cage floor
				ExtendedSpawnLocationConfig config = (ExtendedSpawnLocationConfig) this.getActiveMap().getMapData().getMapModule(ExtendedSpawnLocationConfig.class);

				IWrapedMaterial floorMaterial = NovaSurvivalGames.DEFAULT_EXTENDED_SPAWN_FLOOR_MATERIAL;
				boolean keepFloor = false;
				if (config != null) {
					if (config.isDisabled()) {
						return;
					}
					floorMaterial = config.getFloorMaterial();
					keepFloor = config.isKeepAfterStart();
				}

				// Bottom
				Location floor = location.clone().add(0, -1, 0);
				for (int x = -1; x <= 1; x++) {
					for (int z = -1; z <= 1; z++) {
						Location l = floor.clone().add(x, 0, z);

						Block block = l.getBlock();

						if (block.isLiquid() || !block.getType().isSolid()) {
							if (!keepFloor) {
								temporaryCageBlocks.put(l, block.getType());
							}
							floorMaterial.setBlock(block);
						}
					}
				}
			}
		} else {
			// Use old version
			location.clone().add(1, 0, 0).getBlock().setType(cageMaterial);
			location.clone().add(-1, 0, 0).getBlock().setType(cageMaterial);
			location.clone().add(0, 0, 1).getBlock().setType(cageMaterial);
			location.clone().add(0, 0, -1).getBlock().setType(cageMaterial);

			location.clone().add(1, 1, 0).getBlock().setType(cageMaterial);
			location.clone().add(-1, 1, 0).getBlock().setType(cageMaterial);
			location.clone().add(0, 1, 1).getBlock().setType(cageMaterial);
			location.clone().add(0, 1, -1).getBlock().setType(cageMaterial);

			location.clone().add(0, 2, 0).getBlock().setType(cageMaterial);
		}
	}

	public void tpToSpectator(Player player) {
		NovaCore.getInstance().getVersionIndependentUtils().resetEntityMaxHealth(player);
		player.setHealth(20);
		player.setGameMode(GameMode.SPECTATOR);
		if (hasActiveMap()) {
			player.teleport(getActiveMap().getSpectatorLocation());
		}
	}

	public void tpToArena(Player player) {
		if (hasActiveMap()) {
			if (NovaSurvivalGames.getInstance().isUseExtendedSpawnLocations()) {
				// Use new version
				Team team = TeamManager.getTeamManager().getPlayerTeam(player);
				if (team != null) {
					if (teamSpawnLocation.containsKey(team)) {
						this.tpToArena(player, teamSpawnLocation.get(team));
						return;
					} else {
						for (int i = 0; i < getActiveMap().getStarterLocations().size(); i++) {
							Location location = getActiveMap().getStarterLocations().get(i);

							if (teamSpawnLocation.containsValue(location)) {
								// Already in use
								continue;
							}

							teamSpawnLocation.put(team, location);
							this.tpToArena(player, location);
							return;
						}
					}
				}
			} else {
				// Use old version
				for (int i = 0; i < getActiveMap().getStarterLocations().size(); i++) {
					Location location = getActiveMap().getStarterLocations().get(i);

					if (usedStartLocation.containsValue(location)) {
						// Already in use
						continue;
					}

					this.tpToArena(player, location);
					return;
				}
			}

			// Use fallback
			Log.warn("SurvivalGames", "Using random location for " + player.getName() + " since for some reason getting an empty spawn location for them failed");
			Location backupLocation = getActiveMap().getStarterLocations().get(this.random.nextInt(getActiveMap().getStarterLocations().size()));
			this.tpToArena(player, backupLocation);
		} else {
			Log.error("SurvivalGames", "SurvivalGames#tpToArena() called without map");
		}
	}

	/**
	 * Teleport a player to a provided start location
	 * 
	 * @param player   {@link Player} to teleport
	 * @param location {@link Location} to teleport the player to
	 */
	protected void tpToArena(Player player, Location location) {
		player.teleport(location.getWorld().getSpawnLocation());
		usedStartLocation.put(player.getUniqueId(), location);
		PlayerUtils.clearPlayerInventory(player);
		PlayerUtils.clearPotionEffects(player);
		PlayerUtils.resetPlayerXP(player);
		player.setHealth(player.getMaxHealth());
		player.setSaturation(20);
		player.setFoodLevel(20);
		player.teleport(location);
		player.setGameMode(GameMode.SURVIVAL);
	}

	@Override
	public void onEnd(GameEndReason reason) {
		ended = true;

		getActiveMap().getStarterLocations().forEach(location -> {
			Firework fw = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
			FireworkMeta fwm = fw.getFireworkMeta();

			fwm.setPower(2);
			fwm.addEffect(RandomFireworkEffect.randomFireworkEffect());

			if (random.nextBoolean()) {
				fwm.addEffect(RandomFireworkEffect.randomFireworkEffect());
			}

			fw.setFireworkMeta(fwm);
		});

		Bukkit.getServer().getOnlinePlayers().forEach(player -> {
			VersionIndependantUtils.get().resetEntityMaxHealth(player);
			player.setFoodLevel(20);
			PlayerUtils.clearPlayerInventory(player);
			PlayerUtils.resetPlayerXP(player);
			player.setGameMode(GameMode.SPECTATOR);
			VersionIndependantUtils.get().playSound(player, player.getLocation(), VersionIndependantSound.WITHER_DEATH, 1F, 1F);
		});
	}

	@Override
	public void onStart() {
		if (started) {
			return;
		}

		this.setDropItemsOnCombatLog(true);

		ArrayList<Player> toTeleport = new ArrayList<Player>();
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			if (players.contains(player.getUniqueId())) {
				Log.trace(player.getName() + " is in the player list");
				toTeleport.add(player);
			} else {
				PlayerUtils.clearPlayerInventory(player);
				tpToSpectator(player);
			}
		}

		getActiveMap().getWorld().setGameRuleValue("doMobSpawning", "false");
		getActiveMap().getWorld().setDifficulty(Difficulty.PEACEFUL);

		Bukkit.getScheduler().scheduleSyncDelayedTask(NovaSurvivalGames.getInstance(), new Runnable() {
			@Override
			public void run() {
				getActiveMap().getWorld().setDifficulty(Difficulty.NORMAL);
			}
		}, 200L);

		if (randomStartLocation) {
			Collections.shuffle(getActiveMap().getStarterLocations());
			Collections.shuffle(toTeleport);
		} else {
			if (NovaCore.getInstance().hasTeamManager()) {
				ArrayList<UUID> teamOrder = new ArrayList<UUID>();

				for (Team team : NovaCore.getInstance().getTeamManager().getTeams()) {
					teamOrder.add(team.getTeamUuid());
				}

				Collections.shuffle(teamOrder);

				ArrayList<Player> toTeleportReal = new ArrayList<Player>();

				for (UUID uuid : teamOrder) {
					for (Player pt : toTeleport) {
						if (NovaCore.getInstance().getTeamManager().getPlayerTeam(pt).getTeamUuid().equals(uuid)) {
							toTeleportReal.add(pt);
						}
					}
				}
				toTeleport = toTeleportReal;
			}
		}

		Log.debug("Final toTeleport size: " + toTeleport.size());

		getActiveMap().getStarterLocations().forEach(location -> setStartCage(location, true));

		toTeleport.forEach(player -> tpToArena(player));

		this.started = true;

		Bukkit.getServer().getOnlinePlayers().forEach(player -> VersionIndependantUtils.get().sendTitle(player, "", ChatColor.GOLD + "Starting in " + countdownTime + " seconds", 10, 20, 10));
		
		BasicTimer startTimer = new BasicTimer(countdownTime, 20L);
		startTimer.addFinishCallback(new Callback() {
			@Override
			public void execute() {
				countdownOver = true;
				// Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD +
				// "May the odds be ever in your favour");

				setCages(false);

				Bukkit.getServer().getOnlinePlayers().forEach(player -> {
					player.setFoodLevel(20);
					player.setSaturation(20);
					VersionIndependantUtils.get().playSound(player, player.getLocation(), VersionIndependantSound.NOTE_PLING, 1F, 1F);
				});

				sendBeginEvent();
			}
		});

		startTimer.addTickCallback(new TickCallback() {
			@Override
			public void execute(long timeLeft) {
				Bukkit.getServer().getOnlinePlayers().forEach(player -> {
					VersionIndependantUtils.get().playSound(player, player.getLocation(), VersionIndependantSound.NOTE_PLING, 1F, 1.3F);
					VersionIndependantUtils.get().sendActionBarMessage(player, LanguageManager.getString(player, "novacore.game.starting_in", timeLeft));
				});
				Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Starting in: " + ChatColor.AQUA + ChatColor.BOLD + timeLeft);
			}
		});

		startTimer.start();
	}

	@Override
	public void onPlayerRespawn(Player player) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(NovaCore.getInstance(), new Runnable() {
			@Override
			public void run() {
				Log.trace(getName(), "Calling tpToSpectator(" + player.getName() + ")");
				tpToSpectator(player);
			}
		}, 5L);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoin(PlayerJoinEvent e) {
		if (!hasStarted()) {
			PlayerUtils.clearPotionEffects(e.getPlayer());
		}
	}
}