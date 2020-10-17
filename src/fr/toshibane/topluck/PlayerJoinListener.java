package fr.toshibane.topluck;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

	private TopLuck topluck;
	private String[] ores;
	
	
	public PlayerJoinListener(TopLuck topluck, String[] ores) {
		this.topluck = topluck;
		this.ores = ores;
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		topluck.loadPlayer(event.getPlayer(), ores);
	}
	
}
