package app;

public class User {
	// instance variables
	private int id;
	private String username;
	private String token;
	private String password;
	private String email;
	private CalorieCounter calorieCounter;
	
	// constructor
	public User(String username, String password, String email) {
		this.username = username;
		this.password = password;
		this.email = email;
//		this.calorieCounter = calorieCounter;
	}


	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
//	// methods
//	public String getEmail(){
//		return email;
//	}
//	public String setEmail(){
//		this.email = email;
//	}
}
