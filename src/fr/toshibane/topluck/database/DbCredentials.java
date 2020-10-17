package fr.toshibane.topluck.database;

public class DbCredentials {

	private String host, user, password, name;
	private int port;
	
	public DbCredentials(String host, String user, String password, String name, int port) {
		this.host = host;
		this.user = user;
		this.password = password;
		this.name = name;
		this.port = port;
	}
	
	public String toURI() {
		final StringBuilder sb = new StringBuilder();
		sb.append("jdbc:mysql://").append(host).append(":").append(port).append("/").append(name);
		return sb.toString();
	}
	
	
	public String getHost() {
		return this.host;
	}
	public String getUser() {
		return this.user;
	}
	public String getPassword() {
		return this.password;
	}
	public String getName() {
		return this.name;
	}
	public int port() {
		return this.port;
	}
	
}
