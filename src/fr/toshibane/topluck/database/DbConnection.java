package fr.toshibane.topluck.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DbConnection {

	private DbCredentials credentials;
	private Connection connection;
	
	public DbConnection(DbCredentials credentials) {
		this.credentials = credentials;
		this.connect();
	}
	
	private void connect() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			this.connection = DriverManager.getConnection(this.credentials.toURI(), this.credentials.getUser(), this.credentials.getPassword());
		} catch (SQLException | ClassNotFoundException e) {
			System.out.println("[TopLuck] An error occured while connecting to the database : \n" + e.getMessage());
		}
	}
	
	public void close() {
		try {
			if (this.connection != null && !this.connection.isClosed()) {
				this.connection.close();
			}
		} catch (SQLException e) {
			System.out.print("[TopLuck] An error occured while disconnecting from the database :\n" + e.getMessage());
		}
			
	}
	
	public DbCredentials getCredentials() {
		return this.credentials;
	}
	
	public boolean isIn(String[] ores, String ore) {
		for (int i = 0; i < ores.length; i++) {
			if (ores[i].equalsIgnoreCase(ore))
				return true;
		}
		return false;
	}
	public boolean isIn(ResultSetMetaData rMeta, String ore) throws SQLException {
		for (int i = 1; i <= rMeta.getColumnCount(); i++) {
			if (rMeta.getColumnName(i).equalsIgnoreCase(ore))
				return true;
		}
		return false;
	}
	
	public void verifTable(String[] ores) throws SQLException {
		DatabaseMetaData dbm = connection.getMetaData();
		ResultSet tables = dbm.getTables(null, null, "players", null);
		
		if (tables.next()) {
			StringBuilder getQuery = new StringBuilder();
			getQuery.append("SELECT * FROM players");
			
			PreparedStatement query = connection.prepareStatement(getQuery.toString());
			
			ResultSet result = query.executeQuery();
			ResultSetMetaData rMeta = result.getMetaData();
			
			int nbColumns = rMeta.getColumnCount();
			boolean isExact = nbColumns == (ores.length + 2);
			
			if (isExact) {
				for (int i = 1; i <= nbColumns; i++) {
					String cName = rMeta.getColumnName(i);
					if (!cName.equalsIgnoreCase("allBlocks") && !cName.equalsIgnoreCase("uuid")) {
						if (!isIn(ores, cName)) {
							isExact = false;
							break;
						}
					}
				}
			}
			
			if (!isExact) {
				fixTable(ores, rMeta);
			}
		}else {
			// Si la table players n'existe pas, on la créé
			StringBuilder sb = new StringBuilder();
			sb.append("CREATE TABLE players (uuid VARCHAR(36), allBlocks INT DEFAULT 0");
			for (int i = 0; i < ores.length; i++) {
				sb.append(", ").append(ores[i]).append(" INT DEFAULT 0");
			}
			sb.append(")");
			connection.prepareStatement(sb.toString()).executeUpdate();
		}
		
		
	}
	
	public void deleteColumns(List<String> toDelete) throws SQLException {
		boolean first = true;
		StringBuilder sb = new StringBuilder();
		sb.append("ALTER TABLE players ");
		for (int i = 0; i < toDelete.size(); i++) {
			if (!first) {
				sb.append(", ");
			}else {
				first = false;
			}
			sb.append("DROP ").append(toDelete.get(i));
		}
		
		connection.prepareStatement(sb.toString()).executeUpdate();
	}
	
	public void addColumns(List<String> toAdd) throws SQLException {
		boolean first = true;
		StringBuilder sb = new StringBuilder();
		sb.append("ALTER TABLE players ");
		for (int i = 0; i < toAdd.size(); i++) {
			if (!first) {
				sb.append(", ");
			}else {
				first = false;
			}
			sb.append("ADD ").append(toAdd.get(i)).append(" INT DEFAULT 0");
		}
		
		connection.prepareStatement(sb.toString()).executeUpdate();
	}
	
	public void fixTable(String[] ores, ResultSetMetaData rMeta) throws SQLException {
		int colCount = rMeta.getColumnCount();
		
		// Si nous n'avons pas un la colonne uuid
		boolean isUuidPresent = false;
		boolean isAllBlocksPresent = false;
		for (int i = 1; i <= colCount; i++) {
			if (rMeta.getColumnName(i).equalsIgnoreCase("uuid")) {
				isUuidPresent = true;
			}else if (rMeta.getColumnName(i).equalsIgnoreCase("allBlocks")) {
				isAllBlocksPresent = true;
			}
		}
		if (!isUuidPresent) {  // On remet tout le monde à 0
			connection.prepareStatement("ALTER TABLE players ADD uuid VARCHAR(36)").executeUpdate();
			connection.prepareStatement("DELETE FROM players");
		}
		
		// Si nous n'avons pas la colonne allBlocks
		if (!isAllBlocksPresent) {
			connection.prepareStatement("ALTER TABLE players ADD allBlocks INT DEFAULT 0").executeUpdate();
			connection.prepareStatement("DELETE FROM players");
		}
		
		
		// Si nous avons supprimé un minerai à vérifier
		List<String> toDelete = new ArrayList<>();
		for (int i = 1; i  <= colCount; i++) {
			String cName = rMeta.getColumnName(i);
			if (cName.equalsIgnoreCase("uuid") && cName.equalsIgnoreCase("allBlocks") && !isIn(ores, cName)) {
				toDelete.add(cName);
			}
		}
	
		// Si nous avons ajouté une minerai à vérifier
		List<String> toAdd = new ArrayList<>();
		for (int i = 0; i < ores.length; i++) {
			if (!isIn(rMeta, ores[i])) {
				toAdd.add(ores[i]);
			}
		}
		deleteColumns(toDelete);
		addColumns(toAdd);
	}
	
	public Connection getConnection() {
		try {
			if (this.connection == null || this.connection.isClosed()) {
				connect();
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return this.connection;
	}
	
}
