package fr.toshibane.topluck;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import fr.toshibane.topluck.database.DbConnection;
import fr.toshibane.topluck.database.DbCredentials;

public class TopLuck extends JavaPlugin {
	
	private DbConnection connection;
	private List<PlayerTopLuck> players = new ArrayList<>();
	private YamlConfiguration config;
	private boolean isNewVersion;
	
	
	@Override
	public void onEnable() {
		System.out.println("[TopLuck] Starting...");
		isNewVersion = Arrays.stream(Material.values()).map(Material::name).collect(Collectors.toList()).contains("PLAYER_HEAD");
		
		config = DataConfig();  // On récupère notre fichier de configuration
		String[] ores = config.getString("IdOres").split(",");
		
		int nbOres = ores.length;
		
		
		
		if (config.getBoolean("UseDatabase")) {  // Si on se connecte à une base de données
			connection = new DbConnection(new DbCredentials(config.getString("DatabaseHost"), 
					config.getString("DatabaseUser"),
					config.getString("DatabasePassword"),
					config.getString("DatabaseName"),
					config.getInt("DatabasePort")));
			try {
				connection.verifTable(ores);
			} catch (SQLException e) {
				System.out.println("[TopLuck] An error occured while attempting to verify the database :\n" + e.getMessage());
			}
		}
		
		getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, ores), this);
		getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
		getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
		getServer().getPluginManager().registerEvents(new TopLuckTable(this), this);
		getCommand("topluck").setExecutor(new TopLuckCommand(this));
		
		System.out.println("[TopLuck] Started.");
	}
	
	@Override
	public void onDisable() {
		System.out.println("[TopLuck] Stopping...");
		
		if (config.getBoolean("UseDatabase")) {
			System.out.println("[TopLuck] Closing connection to database...");
			connection.close();
		}
		
		savePlayers(players);
		System.out.println("[TopLuck] Stopped.");
	}
	
	public void reload() {
		System.out.println("[TopLuck] Reloading...");
		config = YamlConfiguration.loadConfiguration(new File(getDataFolder() + "/" + "config.yml"));
		
		String[] ores = config.getString("IdOres").split(",");
		try {
			connection.verifTable(ores);
		} catch (SQLException e) {
			System.out.println("[TopLuck] An error occured while attempting to verify the database :\n" + e.getMessage());
		}
		
	}
	
	public DbConnection getConnection() {
		return connection;
	}
	public String getConfig(String key) {
		return config.getString(key);
	}
	public boolean isNewVersion() {
		return this.isNewVersion;
	}
	public List<PlayerTopLuck> getPlayers() {
		return players;
	}
	public void addPlayer(PlayerTopLuck player) {
		players.add(player);
	}
	public PlayerTopLuck getPlayerTopLuckFrom(Player player) {
		int i = 0;
		while (i < players.size() && players.get(i).getPlayer() != player) {
			i+=1;
		}
		if (i == players.size()) {
			String[] ores = config.getString("IdOres").split(",");
			loadPlayer(player, ores);
		}
		return players.get(i);
	}
	public void removePlayer(Player player) {
		PlayerTopLuck playerTopLuck = getPlayerTopLuckFrom(player);
		players.remove(players.indexOf(playerTopLuck));
		if (playerTopLuck != null) {
			try {
				if (config.getBoolean("UseDatabase")) {
					savePlayerDB(playerTopLuck);
				}else {
					try {
						savePlayerF(playerTopLuck);
					} catch (IOException e) {
						System.out.println("[TopLuck] An error occured while saving a player data : \n" + e.getMessage());
					}
				}
				
			} catch (SQLException e) {
				System.out.println("[TopLuck] An error occured while saving a player data : \n" + e.getMessage());
			}
		}
	}
	
	public YamlConfiguration DataConfig() {
		File configFile = new File(getDataFolder() + "/" + "config.yml");
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		
		String[] defaultKeys = {"IdOres", "MessageSolo", "OfflineMessage", "PrivateWarnMessage", "PublicWarnMessage", 
		                            "PluginReloaded", "UseDatabase", "DatabaseHost", "DatabasePort", "DatabaseUser", "DatabasePassword", "DatabaseName",
		                            "Teleport", "SeeInventory", "SeeEnderchest", "PrivateWarn", "PublicWarn", "TotalMined", "PercentageMined",
		                            "NextPage", "PreviousPage"};
		
		String[] defaultValues = {
				"diamond_ore,gold_ore",
				"Le joueur %PLAYER% a miné %TOPLUCK% de %BLOCK%.",
				"Le joueur %PLAYER% n'est pas connecté actuellement.",
				"Vous avez été avertit en raison d'un trop grand nombre de minerais minés.",
				"Attention, %PLAYER%, votre chance sembre très suspecte !",
				"Configuration rechargée.",
				"false",
				"127.0.0.1",
				"3306",
				"user",
				"password",
				"topluck",
				"Se téléporter",
				"Voir l'inventaire",
				"Voir l'enderchest",
				"Avertir discrètement le joueur",
				"Avertir publiquement le joueur",
				"Total Miné",
				"Pourcentage Miné",
				"Page suivante",
				"Page précédante"
		};
		
		if (!configFile.exists()) {
			// Dans le cas ou le fichier de configuration n'existe pas, on créé tous les champs avec des valeurs par défaut
			System.out.println("[TopLuck] Creating configuration file...");
			
			for (int i = 0; i < defaultValues.length; i++) {
				config.set(defaultKeys[i], defaultValues[i]);
			}
		}else {
			// On doit vérifier que chaque ligne est existante, dans le cas contraire nous devons créer celles manquantes
			
			for (int i = 0; i < defaultValues.length; i++) {
				if (!config.contains(defaultKeys[i]))
					config.set(defaultKeys[i], defaultValues[i]);
			}
		}
		
		try {
			config.save(configFile);
		}catch (IOException e) {
			System.out.println("[TopLuck] An error occured while saving configuration :\n" + e.getMessage());
		}
		return config;
	}
	
	public void savePlayerDB(PlayerTopLuck player) throws SQLException {
		// Si nous sauvegardons dans une base de données
		Connection co = connection.getConnection();
		
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE players SET");
		boolean first = true;
		for (String name: player.getMinedBlocks().keySet()) {
			if (name != "all") {
				if (!first) {
					sb.append(", ");
				}else {
					sb.append(" ");
					first = false;
				}
				sb.append(name).append("=").append(player.getMinedBlocks().get(name));
			}
		}
		sb.append(", allBlocks=" + player.getMinedBlocks().get("all"));
		sb.append(" WHERE uuid='").append(player.getPlayer().getUniqueId()).append("'");
		
		System.out.println(sb.toString());
		
		PreparedStatement updateUser = co.prepareStatement(sb.toString());
		updateUser.executeUpdate();
			
	}
	
	public void savePlayerF(PlayerTopLuck player) throws IOException {
		File saveFile = new File(getDataFolder() + "/save/" + player.getPlayer().getUniqueId() + ".yml");
		YamlConfiguration playerSave = YamlConfiguration.loadConfiguration(saveFile);
		
		for (String ore : player.getMinedBlocks().keySet()) {
			playerSave.set(ore, player.getMinedBlocks().get(ore));
		}
		playerSave.save(saveFile);
	}
	
	public void createPlayerDB(PlayerTopLuck player) {
		if (config.getBoolean("UseDatabase")) {
			// Si nous sauvegardons dans une base de données
			Connection co = connection.getConnection();
			
			StringBuilder addQuery = new StringBuilder();
			addQuery.append("INSERT INTO player VALUES ?");
			for (int i = 0; i < player.getMinedBlocks().size(); i++) {
				addQuery.append(", ?");
			}
			
			PreparedStatement addUser;
			try {
				addUser = co.prepareStatement(addQuery.toString());
				addUser.setString(1, player.getPlayer().getUniqueId().toString());
				for (int i = 2; i <= player.getMinedBlocks().size() + 1; i++) {
					addUser.setInt(i, (int) player.getMinedBlocks().values().toArray()[i]);
				}
				addUser.executeUpdate();
			} catch (SQLException e) {
				System.out.println("[TopLuck] An error occured while saving a player data :\n" + e.getMessage());
			}
		} else {
			// Sinon, nous passons par un système de fichiers
			File saveFile = new File(getDataFolder() + "/save/" + player.getPlayer().getUniqueId() + ".yml");
			YamlConfiguration playerSave = YamlConfiguration.loadConfiguration(saveFile);
			
			for (String ore : player.getMinedBlocks().keySet()) {
				playerSave.set(ore, player.getMinedBlocks().get(ore));
			}
		}
	}
	
	public void savePlayers(List<PlayerTopLuck> players) {
		for (PlayerTopLuck ptl: players) {
			if (config.getBoolean("UseDatabase")) {
				try {
					// On vérifie si le joueur existe dans la base de données
					PreparedStatement testStatement = connection.getConnection().prepareStatement("SELECT uuid FROM players WHERE uuid=?");
					testStatement.setString(1, ptl.getPlayer().getUniqueId().toString());
					ResultSet resultSet = testStatement.executeQuery();
					
					if (resultSet.next()) {
						savePlayerDB(ptl);
					}else {
						createPlayerDB(ptl);
					}
				} catch (SQLException e) {
					System.out.println("[TopLuck] An error occured while saving players data : \n" + e.getMessage());
				}
			}else {
				try {
					savePlayerF(ptl);
				} catch (IOException e) {
					System.out.println("[TopLuck] An error occured while saving players data : \n" + e.getMessage());
				}
			}
			
		}
	}
	
	private void loadFromDb(Player player, String[] ores) throws SQLException {
		Connection dbCo = connection.getConnection();
		
		StringBuilder getQuery = new StringBuilder();
		getQuery.append("SELECT uuid, allBlocks");
		for (int i = 0; i < ores.length; i++) {
			getQuery.append(", " + ores[i].toLowerCase());
		}
		getQuery.append(" FROM players WHERE uuid=?");
		
		PreparedStatement preparedStatement = dbCo.prepareStatement(getQuery.toString());
		preparedStatement.setString(1, player.getUniqueId().toString());
		
		ResultSet resultSet = preparedStatement.executeQuery();
		
		if (resultSet.next()) {
			HashMap<String, Integer> hm = new HashMap<>();
			
			for (int i = 0; i < ores.length; i++) {
				hm.put(ores[i].toLowerCase(), resultSet.getInt(ores[i].toLowerCase()));
			}
			hm.put("all", resultSet.getInt("allBlocks"));
			
			addPlayer(new PlayerTopLuck(player, hm));
		} else {
			StringBuilder addQuery = new StringBuilder();
			addQuery.append("INSERT INTO players(uuid, allBlocks");
			for (int i = 0; i < ores.length; i++) {
				addQuery.append(", "+  ores[i]);
			}
			addQuery.append(") VALUES ('").append(player.getUniqueId().toString()).append("'");

			for (int i = 0; i <= ores.length; i++) {
				addQuery.append(", 0");
				
			}
			addQuery.append(")");
			
			PreparedStatement addUser = dbCo.prepareStatement(addQuery.toString());

			addUser.executeUpdate();
			
			HashMap<String, Integer> hm = new HashMap<>();
			for (int i = 0; i < ores.length; i++) {
				hm.put(ores[i].toLowerCase(), 0);
			}
			hm.put("all", 0);
			addPlayer(new PlayerTopLuck(player, hm));
		}
	}
	
	private void loadFromFile(Player player, String[] ores) {
		File playerFile = new File(getDataFolder() + "/save/" + player.getPlayer().getUniqueId() + ".yml");
		YamlConfiguration playerSave = YamlConfiguration.loadConfiguration(playerFile);
		
		if (playerFile.exists()) {
			HashMap<String, Integer> hm = new HashMap<>();
			for (int i = 0; i < ores.length; i++) {
				hm.put(ores[i].toLowerCase(), playerSave.getInt(ores[i].toLowerCase()));
			}
			hm.put("all", playerSave.getInt("all"));
			PlayerTopLuck ptl = new PlayerTopLuck(player, hm);
			players.add(ptl);
		}else {
			HashMap<String, Integer> hm = new HashMap<>();
			for (int i = 0; i < ores.length; i++) {
				hm.put(ores[i].toLowerCase(), 0);
			}
			hm.put("all", 0);
			
			PlayerTopLuck ptl = new PlayerTopLuck(player, hm);
			players.add(ptl);
		}
	}
	
	public void loadPlayer(Player player, String[] ores)  {
		if (config.getBoolean("UseDatabase")) {
			try {
				loadFromDb(player, ores);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}else {
			loadFromFile(player, ores);
		}
	}
	
}
