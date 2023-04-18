package net.novauniverse.games.survivalgames.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import net.novauniverse.games.survivalgames.SurvivalGamesPlugin;
import net.zeeraa.novacore.spigot.command.AllowedSenders;
import net.zeeraa.novacore.spigot.command.NovaCommand;

public class StartSGCountdown extends NovaCommand {
	public StartSGCountdown() {
		super("startsgcountdown", SurvivalGamesPlugin.getInstance());

		setAllowedSenders(AllowedSenders.ALL);
		setPermission("novauniverse.survivalgames.command.startsgcountdown");
		setPermissionDefaultValue(PermissionDefault.OP);
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		if (!SurvivalGamesPlugin.getInstance().getGame().hasStarted()) {
			sender.sendMessage(ChatColor.RED + "Game has not started yet");
			return false;
		}

		if (SurvivalGamesPlugin.getInstance().getGame().isCountdownStarted()) {
			sender.sendMessage(ChatColor.RED + "Countdown already started");
			return false;
		}

		SurvivalGamesPlugin.getInstance().getGame().startCountdown();
		sender.sendMessage(ChatColor.GREEN + "Countdown started");

		return true;
	}
}