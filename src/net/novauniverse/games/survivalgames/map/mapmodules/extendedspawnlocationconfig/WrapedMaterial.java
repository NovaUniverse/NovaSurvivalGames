package net.novauniverse.games.survivalgames.map.mapmodules.extendedspawnlocationconfig;

import org.bukkit.Material;
import org.bukkit.block.Block;

public class WrapedMaterial implements IWrapedMaterial {
	private Material material;

	public WrapedMaterial(Material material) {
		this.material = material;
	}

	@Override
	public void setBlock(Block block) {
		block.setType(material);
	}
}