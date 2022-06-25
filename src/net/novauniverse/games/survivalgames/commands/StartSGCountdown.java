package net.novauniverse.games.survivalgames.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import net.novauniverse.games.survivalgames.NovaSurvivalGames;
import net.zeeraa.novacore.spigot.command.AllowedSenders;
import net.zeeraa.novacore.spigot.command.NovaCommand;

public class StartSGCountdown extends NovaCommand {
	public StartSGCountdown() {
		super("startsgcountdown", NovaSurvivalGames.getInstance());

		setAllowedSenders(AllowedSenders.ALL);
		setPermission("novauniverse.survivalgames.command.startsgcountdown");
		setPermissionDefaultValue(PermissionDefault.OP);
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		if (NovaSurvivalGames.getInstance().getGame().hasStarted()) {
			return false;
		}

		if (NovaSurvivalGames.getInstance().getGame().isCountdownStarted()) {
			return false;
		}

		NovaSurvivalGames.getInstance().getGame().startCountdown();
		sender.sendMessage(ChatColor.GREEN + "Countdown started");

		return true;
	}
}