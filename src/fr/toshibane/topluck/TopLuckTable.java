package fr.toshibane.topluck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class TopLuckTable implements Listener {

	private Inventory inv;
	private TopLuck topluck;
	private int page;
	
	
	public TopLuckTable(TopLuck topluck) {
		this.topluck = topluck;
		this.page = 0;
		this.inv = Bukkit.createInventory(null, 45, ChatColor.YELLOW + "TopLuck");
	}
	
	@SuppressWarnings("deprecation")
	public void computePage() {
		inv.clear();
		
		List<PlayerTopLuck> playersOrder = new ArrayList<>(this.topluck.getPlayers());
		
		playersOrder.sort(Collections.reverseOrder());
		
		int max = 36;
		boolean icones = true;
		if (topluck.getPlayers().size() < max) {
			max = topluck.getPlayers().size();
			icones = false;
		}
		
		Material type = Material.matchMaterial(topluck.isNewVersion() ? "PLAYER_HEAD" : "SKULL_ITEM");
		
		for (int i = 0; i < max; i++) {
			PlayerTopLuck player = playersOrder.get(i + max * page);
			ItemStack item = new ItemStack(type, 1);
			if (!topluck.isNewVersion())
				item.setDurability((short) 3);
			
			SkullMeta meta = (SkullMeta) item.getItemMeta();
			meta.setOwner(player.getPlayer().getName());
			meta.setDisplayName(player.getPlayer().getName());
			
			List<String> lore = new ArrayList<>();
			for (String mat : player.getMinedBlocks().keySet()) {
				if (mat != "all") {
					lore.add(mat + " : " + player.getPercent(Material.matchMaterial(mat)) + "%");
				}
					
			}
			meta.setLore(lore);
			
			item.setItemMeta(meta);
			inv.addItem(item);
		}
		
		if (icones) {  // Icones de navigations
			if (page > 0) { // S'il y a une page précédente
				ItemStack back = new ItemStack(Material.ARROW, 1);
				
				ItemMeta meta = back.getItemMeta();
				
				meta.setDisplayName(topluck.getConfig("PreviousPage"));
				
				back.setItemMeta(meta);
			
				inv.setItem(36, back);
			}
			if (page < topluck.getPlayers().size() / 36) {  // S'il y a une page suivante
				ItemStack next = new ItemStack(Material.ARROW, 1);
				
				ItemMeta meta = next.getItemMeta();
				
				meta.setDisplayName(topluck.getConfig("NextPage"));
				
				next.setItemMeta(meta);
			
				inv.setItem(44, next);
			}
			
			ItemStack actualPage = new ItemStack(Material.PAPER, 1);
			ItemMeta meta = actualPage.getItemMeta();
			meta.setDisplayName((page + 1) + " / " + ((topluck.getPlayers().size() - 1) / 36));
			actualPage.setItemMeta(meta);
			inv.setItem(40, actualPage);
		}
		
	}
	
	public void openInventory(Player p) {
		p.openInventory(inv);
	}
	
	public void openDetails(Player player, Player target) {
		Inventory details = Bukkit.createInventory(null, 27, target.getName());
		PlayerTopLuck targetTL = topluck.getPlayerTopLuckFrom(target);
		
		
		// Téléportation au joueur
		ItemStack tp = new ItemStack(Material.ENDER_PEARL, 1);
		ItemMeta tpMeta = tp.getItemMeta();
		tpMeta.setDisplayName(topluck.getConfig("Teleport"));
		tp.setItemMeta(tpMeta);
		
		// Voir l'inventaire du joueur
		ItemStack seeInv = new ItemStack(Material.CHEST, 1);
		ItemMeta seeInvMeta = seeInv.getItemMeta();
		seeInvMeta.setDisplayName(topluck.getConfig("SeeInventory"));
		seeInv.setItemMeta(seeInvMeta);
		
		// Voir l'enderchest du joueur
		ItemStack seeEnder = new ItemStack(Material.ENDER_CHEST, 1);
		ItemMeta seeEnderMeta = seeEnder.getItemMeta();
		seeEnderMeta.setDisplayName(topluck.getConfig("SeeEnderchest"));
		seeEnder.setItemMeta(seeEnderMeta);
		
		// Avertir Publiquement
		ItemStack publicWarn = new ItemStack(Material.BLAZE_ROD, 1);
		ItemMeta publicWMeta = publicWarn.getItemMeta();
		publicWMeta.setDisplayName(topluck.getConfig("PublicWarn"));
		publicWarn.setItemMeta(publicWMeta);
		
		// Avertir en Privé
		ItemStack privateWarn = new ItemStack(Material.STICK, 1);
		ItemMeta privateWM = privateWarn.getItemMeta();
		privateWM.setDisplayName(topluck.getConfig("PrivateWarn"));
		privateWarn.setItemMeta(privateWM);
		
		// Tous ses minerais comptabilisés
		int i = 0;
		for (String mat : targetTL.getMinedBlocks().keySet()) {
			if (mat != "all") {
				Material material = Material.matchMaterial(mat);
				
				ItemStack item = new ItemStack(material, 1);
				ItemMeta meta = item.getItemMeta();
				
				List<String> lore = new ArrayList<>();
				lore.add(topluck.getConfig("TotalMined") + " : "+ targetTL.getMinedBlocks().get(mat));
				lore.add(topluck.getConfig("PercentageMined") + " : " + targetTL.getPercent(material));
				meta.setLore(lore);
				
				item.setItemMeta(meta);
				details.setItem(i + 9, item);
				
				i += 1;
			}
			
		}
		
		details.addItem(tp, seeInv, seeEnder, publicWarn, privateWarn);
		player.openInventory(details);
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		Material headType = Material.matchMaterial(topluck.isNewVersion() ? "PLAYER_HEAD" : "SKULL_ITEM");
		
		if (event.getView().getTitle().equals(ChatColor.YELLOW + "TopLuck") && event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() != null) {
			if (event.getCurrentItem().getType() == Material.ARROW) {
				if (event.getCurrentItem().getItemMeta().getDisplayName().equals(topluck.getConfig("NextPage"))) {
					page += 1;
					computePage();
				} else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(topluck.getConfig("PreviousPage"))) {
					page -= 1;
					computePage();
				}
			}else if (event.getCurrentItem().getType() == headType) {
				openDetails((Player) event.getWhoClicked(), Bukkit.getPlayer(event.getCurrentItem().getItemMeta().getDisplayName()));
			}
			event.setCancelled(true);
		}else if (Bukkit.getPlayer(event.getView().getTitle()) != null && event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() != null) {
			// Si nous sommes dans le menu de détails
			String nameItem = event.getCurrentItem().getItemMeta().getDisplayName();
			Player target = Bukkit.getPlayer(event.getView().getTitle());
			
			if(nameItem == null) {
				event.setCancelled(true);
				return;
			}
			
			if (nameItem.equals(topluck.getConfig("Teleport"))) {
				event.getWhoClicked().teleport(Bukkit.getPlayer(event.getView().getTitle()));
				event.getWhoClicked().setGameMode(GameMode.SPECTATOR);
			}else if (nameItem.equals(topluck.getConfig("SeeInventory"))) {
				event.getWhoClicked().openInventory(target.getInventory());
			}else if (nameItem.equals(topluck.getConfig("SeeEnderchest"))) {
				event.getWhoClicked().openInventory(target.getEnderChest());
			}else if (nameItem.equals(topluck.getConfig("PublicWarn"))) {
				String toSend = ChatColor.RED + "" + ChatColor.BOLD + topluck.getConfig("PublicWarnMessage");
				toSend = toSend.replace("%PLAYER%", target.getName());
				
				topluck.getServer().broadcastMessage(toSend);
			}else if (nameItem.equals(topluck.getConfig("PrivateWarn"))) {
				String toSend = ChatColor.RED + topluck.getConfig("PrivateWarnMessage");
				target.sendMessage(toSend);
			}
			event.setCancelled(true);
		}
	}
	
}
