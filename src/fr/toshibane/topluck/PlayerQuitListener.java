package fr.toshibane.topluck;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

	private TopLuck topluck;
	
	public PlayerQuitListener(TopLuck topluck) {
		this.topluck = topluck;
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		topluck.removePlayer(event.getPlayer());
	}
	
}
