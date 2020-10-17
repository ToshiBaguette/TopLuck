package fr.toshibane.topluck;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener<BreakBlocEvent> implements Listener {

	private TopLuck topluck;
	
	public BlockBreakListener(TopLuck topluck) {
		this.topluck = topluck;
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		PlayerTopLuck ptl = topluck.getPlayerTopLuckFrom(e.getPlayer());
		
		ptl.mine(e.getBlock().getType());
	}
}
