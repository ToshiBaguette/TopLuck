package fr.toshibane.topluck;

import java.io.Serializable;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;

public class PlayerTopLuck implements Serializable, Comparable<PlayerTopLuck> {

	private static final long serialVersionUID = 5949336241317201772L;
	private Player player;
	private HashMap<String, Integer> minedBlocks;
	
	public PlayerTopLuck(Player player, HashMap<String, Integer> minedBlocks) {
		this.player = player;
		this.minedBlocks = minedBlocks;
	}
	
	public PlayerTopLuck(PlayerTopLuck playerTL) {
		this.player = playerTL.getPlayer();
		this.minedBlocks = playerTL.getMinedBlocks();
	}
	

	public boolean isLooked(Material m) {
		for (String key : minedBlocks.keySet()) {
			if (key.equalsIgnoreCase(m.name())) {
				return true;
			}
		}
		return false;
	}
	
	public void mine(Material m) {
		if (isLooked(m)) {
			// Si on enregistre le type de blocs que le joueur vient de miner
			minedBlocks.put(m.name().toLowerCase(), minedBlocks.get(m.name().toLowerCase()) + 1);
		}
		minedBlocks.put("all", getNbMinedBlocks() + 1);  // "all" contiendra la somme de tous les blocs minés
	}
	
	public double getPercent(Material m) {
		if (isLooked(m)) {
			if (getNbMinedBlocks() > 0)
				return round((double) minedBlocks.get(m.name().toLowerCase()) / getNbMinedBlocks() * 100, 2);
			return (double)minedBlocks.get(m.name().toLowerCase());
		}
		return 0;
	}
	
	public int getNbMinedBlocks() {
		return minedBlocks.get("all");
	}
	
	public String getMaxNbMined() {
		int nb = 0;
		String material = "";
		for (String key : this.minedBlocks.keySet()) {
			if (this.minedBlocks.get(key) > nb && key != "all") {
				material = key;
				nb = this.minedBlocks.get(key);
			}
		}
		return material;
	}
	
	public double allPercentage() {
		double percentage = 0;
		for (String key : minedBlocks.keySet()) {
			if (!key.equalsIgnoreCase("allBlocks")) {
				percentage += minedBlocks.get(key);
			}
		}
		
		percentage /= getNbMinedBlocks();
		return percentage;
	}
	
	// GETTERS
	public Player getPlayer() {
		return this.player;
	}
	public HashMap<String, Integer> getMinedBlocks() {
		return this.minedBlocks;
	}

	@Override
	public int compareTo(PlayerTopLuck p) {
		if (allPercentage() == p.allPercentage()) return 0;
		if (allPercentage() > p.allPercentage()) return 1;
		return -1;
	}
	
	public static double round(double value, int places) {
		if (places < 0) throw new IllegalArgumentException();

		long factor = (long) Math.pow(10, places);
		value = value * factor;
		long tmp = Math.round(value);
		return (double) tmp / factor;
	}

	
	
}
