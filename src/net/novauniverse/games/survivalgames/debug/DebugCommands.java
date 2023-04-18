package net.novauniverse.games.survivalgames.debug;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;

import net.novauniverse.games.survivalgames.SurvivalGamesPlugin;
import net.zeeraa.novacore.spigot.command.AllowedSenders;
import net.zeeraa.novacore.spigot.debug.DebugCommandRegistrator;
import net.zeeraa.novacore.spigot.debug.DebugTrigger;

public class DebugCommands {
	public DebugCommands() {
		DebugCommandRegistrator.getInstance().addDebugTrigger(new DebugTrigger() {
			@Override
			public void onExecute(CommandSender sender, String commandLabel, String[] args) {
				if (SurvivalGamesPlugin.getInstance().getGame().hasActiveMap()) {
					SurvivalGamesPlugin.getInstance().getGame().setCages(true);
					sender.sendMessage(ChatColor.GREEN + "ok");
				} else {
					sender.sendMessage(ChatColor.RED + "No map");
				}
			}

			@Override
			public PermissionDefault getPermissionDefault() {
				return PermissionDefault.OP;
			}

			@Override
			public String getPermission() {
				return "novauniverse.debug.hungergames.spawncages";
			}

			@Override
			public String getName() {
				return "spawncages";
			}

			@Override
			public AllowedSenders getAllowedSenders() {
				return AllowedSenders.ALL;
			}
		});

		DebugCommandRegistrator.getInstance().addDebugTrigger(new DebugTrigger() {
			@Override
			public void onExecute(CommandSender sender, String commandLabel, String[] args) {
				if (SurvivalGamesPlugin.getInstance().getGame().hasActiveMap()) {
					SurvivalGamesPlugin.getInstance().getGame().setCages(false);
					sender.sendMessage(ChatColor.GREEN + "ok");
				} else {
					sender.sendMessage(ChatColor.RED + "No map");
				}
			}

			@Override
			public PermissionDefault getPermissionDefault() {
				return PermissionDefault.OP;
			}

			@Override
			public String getPermission() {
				return "novauniverse.debug.hungergames.despawncages";
			}

			@Override
			public String getName() {
				return "despawncages";
			}

			@Override
			public AllowedSenders getAllowedSenders() {
				return AllowedSenders.ALL;
			}
		});
	}
}