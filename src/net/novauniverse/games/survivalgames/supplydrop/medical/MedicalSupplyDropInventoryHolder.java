package net.novauniverse.games.survivalgames.supplydrop.medical;

import java.util.UUID;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MedicalSupplyDropInventoryHolder implements InventoryHolder {
	private UUID uuid;

	public MedicalSupplyDropInventoryHolder(UUID uuid) {
		this.uuid = uuid;
	}

	public UUID getUuid() {
		return uuid;
	}

	@Override
	public Inventory getInventory() {
		return null;
	}
}