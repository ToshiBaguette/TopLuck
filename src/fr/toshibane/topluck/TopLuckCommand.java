package fr.toshibane.topluck;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class TopLuckCommand implements CommandExecutor {

	private TopLuck topluck;
	
	public TopLuckCommand(TopLuck topluck) {
		this.topluck = topluck;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {

		if (sender instanceof Player) {  // Nous ne permettons pas aux command blocks d'executer la commande
			Player player = (Player) sender;
			if (label.equalsIgnoreCase("topluck")) {
				if (player.hasPermission("topluck.admin.use")) {
					if (args.length != 0) {
						if (!args[0].equalsIgnoreCase("reload")) {
							// Si on a donné un argument, nous devons vérifier s'il s'agit d'un joueur
							Player target = Bukkit.getPlayer(args[0]);
							if (target != null) {
								PlayerTopLuck targetTopLuck = this.topluck.getPlayerTopLuckFrom(target);
								String maxMined = targetTopLuck.getMaxNbMined();
								
								String toSend = topluck.getConfig("MessageSolo");
								toSend = ChatColor.YELLOW + toSend;
								toSend = toSend.replace("%PLAYER%", ChatColor.GREEN + args[0] + ChatColor.YELLOW);
								toSend = toSend.replace("%BLOCK%", ChatColor.GREEN + maxMined + ChatColor.YELLOW);
								toSend = toSend.replace("%TOPLUCK", ChatColor.GREEN + "" + targetTopLuck.getPercent(Material.matchMaterial(maxMined)) + ChatColor.YELLOW);
								
								sender.sendMessage(toSend);
								return true;
								
							}else {  // Le joueur spécifé est introuvable
								String toSend = ChatColor.RED + topluck.getConfig("OfflineMessage");
								toSend = toSend.replace("%PLAYER%", ChatColor.BOLD + args[0] + ChatColor.RESET + ChatColor.RED);
								
								player.sendMessage(toSend);
								return false;
							}
						}
					}else {
						// Ici, on demande le Tableau TopLuck
						TopLuckTable table = new TopLuckTable(topluck);
						table.computePage();
						table.openInventory(player);
						return true;
					}
				}else {
					return false;
				}
			}
		}
		if (label.equalsIgnoreCase("topluck") && args[0].equalsIgnoreCase("reload")) {
			// Dans ce cas, on nous demande de reload le plugin
			topluck.reload();
			sender.sendMessage("[TopLuck] " + topluck.getConfig("PluginReloaded"));
			return true;
		}
		return false;
	}
}
