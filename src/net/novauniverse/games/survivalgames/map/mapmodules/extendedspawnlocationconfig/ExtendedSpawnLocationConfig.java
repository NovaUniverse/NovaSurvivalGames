package net.novauniverse.games.survivalgames.map.mapmodules.extendedspawnlocationconfig;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.json.JSONObject;

import net.novauniverse.games.survivalgames.NovaSurvivalGames;
import net.zeeraa.novacore.commons.log.Log;
import net.zeeraa.novacore.spigot.abstraction.enums.ColoredBlockType;
import net.zeeraa.novacore.spigot.module.modules.game.map.mapmodule.MapModule;

public class ExtendedSpawnLocationConfig extends MapModule {
	private IWrapedMaterial floorMaterial;
	private boolean keepAfterStart;
	private boolean disabled;

	public ExtendedSpawnLocationConfig(JSONObject json) {
		super(json);

		if (json.has("floor_material")) {
			String materialString = json.getString("floor_material");
			try {
				if (materialString.startsWith("COLOREDBLOCK:")) {
					String[] data = materialString.split(":");
					ColoredBlockType type = ColoredBlockType.valueOf(data[1]);
					DyeColor color = DyeColor.valueOf(data[2]);

					floorMaterial = new ColoredWrapedMaterial(color, type);
				} else {
					floorMaterial = new WrapedMaterial(Material.valueOf(materialString));
				}
				
			} catch (Exception e) {
				Log.error("ExtendedSpawnLocationConfig", "Failed to parse material " + materialString + ". " + e.getClass().getName() + " " + e.getMessage());
			}
		} else {
			floorMaterial = NovaSurvivalGames.DEFAULT_EXTENDED_SPAWN_FLOOR_MATERIAL;
		}

		if (json.has("keep_after_start")) {
			keepAfterStart = json.getBoolean("keep_after_start");
		} else {
			keepAfterStart = false;
		}
		
		if (json.has("disabled")) {
			disabled = json.getBoolean("disabled");
		} else {
			disabled = false;
		}
	}

	public IWrapedMaterial getFloorMaterial() {
		return floorMaterial;
	}

	public boolean isKeepAfterStart() {
		return keepAfterStart;
	}
	
	public boolean isDisabled() {
		return disabled;
	}
}