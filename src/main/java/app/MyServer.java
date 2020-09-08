package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.ArrayList;

@SpringBootApplication
public class MyServer {
	public static HashMap<String, String> users = new HashMap<String, String>();
	public static HashMap<String, User> tokenHashmap = new HashMap<String, User>();
	public static ArrayList<User> tokensArrayList = new ArrayList<User>();
	public static ArrayList<User> listOfUsers = new ArrayList<User>();
	
	public static void main(String[] args) {
		SpringApplication.run(MyServer.class, args);
	}
}
