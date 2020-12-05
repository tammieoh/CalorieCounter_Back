package app;

import org.springframework.web.bind.annotation.*;

import org.springframework.http.*;

import javax.servlet.http.*;

import java.security.MessageDigest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.json.JSONArray;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.nio.charset.Charset;

//import org.springframework.security.crypto.bcrypt.BCrypt;


@CrossOrigin
@RestController
public class UserController {
    final String SQLPASSWORD = System.getenv("key");
    int num = 0;
    @RequestMapping(value = "/registerUser", method = RequestMethod.POST) // <-- setup the endpoint URL at /hello with the HTTP POST method
    public ResponseEntity<String> register(@RequestBody String payload, HttpServletRequest request) {
        JSONObject payloadObj = new JSONObject(payload); // type into body

        String username = payloadObj.getString("username"); //Grabbing name and age parameters from URL
        String password = payloadObj.getString("password");
        String email = payloadObj.getString("email");

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");

        MessageDigest digest = null;
        String hashedKey = null;

        hashedKey = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt());
        System.out.println("registering user" + username + " password " + password);
//        User u1 = new User(username, hashedKey, email);

        // connecting database to backend
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Comps?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", SQLPASSWORD);
//            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Shopping?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", Clothing.SQL_PASSWORD);
            System.out.println(conn);
            PreparedStatement check_stmt = conn.prepareStatement("SELECT EMAIL FROM Users WHERE EMAIL=?");
            check_stmt.setString(1,email);
            PreparedStatement check_stmt2 = conn.prepareStatement("SELECT USERNAME FROM Users WHERE USERNAME=?");
            check_stmt2.setString(1,username);
            ResultSet rs = check_stmt.executeQuery();
            ResultSet rs2 = check_stmt2.executeQuery();
            if(rs.next()) {
                //System.out.println("Email already exists");
                JSONObject responseObj = new JSONObject();
                responseObj.put("message", "Account already exists");
                System.out.println(responseObj.toString());
                return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
            }
            else if(rs2.next()) {
                //System.out.println("Email already exists");
                JSONObject responseObj = new JSONObject();
                responseObj.put("message", username + " already exists");
                System.out.println(responseObj.toString());
                return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.BAD_REQUEST);
            }
            else {
                PreparedStatement stmt = conn.prepareStatement("INSERT INTO Users (Username, Password, Email, Calories) VALUES (?,?,?, 0)");
//            stmt.setNull(1, java.sql.Types.NULL);
                stmt.setString(1, username);
                stmt.setString(2, hashedKey);
                stmt.setString(3, email);
                stmt.execute();
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            JSONObject responseObj = new JSONObject();
//            responseObj.put("username", username);
            responseObj.put("message", e.getMessage());
            return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        //Returns the response with a String, headers, and HTTP status
        JSONObject responseObj = new JSONObject();

        responseObj.put("username", username);
        responseObj.put("message", "user registered");
        return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.OK);
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST) // <-- setup the endpoint URL at /hello with the HTTP POST method
    public ResponseEntity<String> login(@RequestBody String payload, HttpServletRequest request) {
        System.out.println("inside login API");
        JSONObject payloadObj = new JSONObject(payload); // type into body

        String username = payloadObj.getString("username"); //Grabbing name and age parameters from URL
        String password = payloadObj.getString("password");

		/*Creating http headers object to place into response entity the server will return.
		This is what allows us to set the content-type to application/json or any other content-type
		we would want to return */
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");

        MessageDigest digest = null;
        String hashedKey = null;
        Connection conn;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Comps?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", SQLPASSWORD
            );
            System.out.println(conn);
            String query = "SELECT username, password FROM Users WHERE username = (?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (!rs.isBeforeFirst()) {
                System.out.println("no username");
                return new ResponseEntity("{\"message\":\"username not registered\"}", responseHeaders, HttpStatus.BAD_REQUEST);
            } else {
                String loginPassword = "";
                username = "";
                String email = "";
                while (rs.next()) { // this goes through every single row of data
                    username = rs.getString("Username");
                    loginPassword = rs.getString("Password");
                }
                String token;
                if (BCrypt.checkpw(password, loginPassword)) {
                    token = getSaltString();
                    User user = new User(username, token, email);
                    if (MyServer.tokensArrayList.size() == 100) {
                        User userToRemove = MyServer.tokensArrayList.remove(99);
                        MyServer.tokenHashmap.remove(userToRemove.getUsername());
                    }
                    MyServer.tokensArrayList.add(0, user);
                    user.setToken(token);
                    MyServer.tokenHashmap.put(username, user);

                    JSONObject responseObj = new JSONObject();
                    responseObj.put("token", token);
                    responseObj.put("message", "user logged in");
                    System.out.println("sent back to android");
                    return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.OK);
                }
                else {
                    System.out.println("hello");
                    return new ResponseEntity("{\"message\":\"username/password combination is incorrect\"}", responseHeaders, HttpStatus.BAD_REQUEST);
                }
            }
        }
        catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            JSONObject responseObj = new JSONObject();
//            responseObj.put("username", username);
            responseObj.put("message", e.getMessage());
            return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
//        return new ResponseEntity("{\"message\":\"username not registered\"}", responseHeaders, HttpStatus.BAD_REQUEST);
    }

    @RequestMapping(value = "/calories", method = RequestMethod.POST) // <-- setup the endpoint URL at /hello with the HTTP POST method
    public ResponseEntity<String> caloriecalculator(@RequestBody String payload, HttpServletRequest request) {
        System.out.println("inside calorie calculator API");
        JSONObject payloadObj = new JSONObject(payload); // type into body

        String calories = payloadObj.getString("calories"); //Grabbing name and age parameters from URL
        String username = payloadObj.getString("username");

        System.out.println(calories);
        System.out.println(username);
		/*Creating http headers object to place into response entity the server will return.
		This is what allows us to set the content-type to application/json or any other content-type
		we would want to return */
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");

        MessageDigest digest = null;
        String hashedKey = null;
        Connection conn;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Comps?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", SQLPASSWORD);
            System.out.println(conn);
            PreparedStatement check_stmt = conn.prepareStatement("SELECT username FROM Users WHERE username=?");
            check_stmt.setString(1, username);
            ResultSet rs = check_stmt.executeQuery();

            if (!rs.isBeforeFirst()) {
                System.out.println("no username");
                return new ResponseEntity("{\"message\":\"username not registered\"}", responseHeaders, HttpStatus.BAD_REQUEST);
            } else {
                PreparedStatement stmt = conn.prepareStatement("UPDATE Users SET calories=? WHERE username=?");
//            stmt.setNull(1, java.sql.Types.NULL);
                stmt.setString(1, calories);
                stmt.setString(2, username);
                stmt.execute();
            }
        }
        catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            JSONObject responseObj = new JSONObject();
//            responseObj.put("username", username);
            responseObj.put("message", e.getMessage());
            System.out.println(e.getMessage());
            return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        JSONObject responseObj = new JSONObject();

        responseObj.put("calories", calories);
        responseObj.put("message", "calories saved");
        System.out.println(calories);
        return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.OK);
//        return new ResponseEntity("{\"message\":\"username not registered\"}", responseHeaders, HttpStatus.BAD_REQUEST);
    }

        @RequestMapping(value = "/validateToken", method = RequestMethod.GET) // <-- setup the endpoint URL at /hello with the HTTP POST method
    public ResponseEntity<String> validateToken(HttpServletRequest request) {
        String username = request.getParameter("username"); //Grabbing name and age parameters from URL
        String token = request.getParameter("token");

		/*Creating http headers object to place into response entity the server will return.
		This is what allows us to set the content-type to application/json or any other content-type
		we would want to return */
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");

        User user = MyServer.tokenHashmap.get(username);
        if (MyServer.tokensArrayList.size() == 100) {
            User userToRemove = MyServer.tokensArrayList.remove(99);
            MyServer.tokenHashmap.remove(userToRemove.getUsername());
        }
        MyServer.tokensArrayList.add(0, user);
        MyServer.tokenHashmap.put(username, user);

        if (user.getToken().equals(token)) {
            //continue with code...
            MyServer.tokensArrayList.remove(user);
            MyServer.tokensArrayList.add(0, user);
            return new ResponseEntity("{\"message\":\"Valid Token\"}", responseHeaders, HttpStatus.OK);
        }
        else {
            return new ResponseEntity("{\"message\":\"Bad Token\"}", responseHeaders, HttpStatus.BAD_REQUEST);
        }
    }
//    @RequestMapping(value = "/search", method = RequestMethod.GET)
//    public ResponseEntity<String> search(HttpServletRequest request) {
//        String searchString = request.getParameter("Name");
////        System.out.println(searchString);
//        searchString = "%"+searchString+"%";
////        System.out.println(searchString);
//        HttpHeaders responseHeaders = new HttpHeaders();
//        responseHeaders.set("Content-Type", "application/json");
//
//        try {
//            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Comps?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", SQLPASSWORD);
//            String query = "SELECT * FROM Comps.Foods WHERE Name LIKE ?";
//            PreparedStatement stmt = conn.prepareStatement(query);
//            stmt.setString(1, searchString);
//            ResultSet rs =  stmt.executeQuery();
//            JSONArray searchResultsArray = new JSONArray();
//            while(rs.next()) {
//                JSONObject newObject = new JSONObject();
//                newObject.put("Name", rs.getString("Name"));
////                System.out.println(newObject);
////                newObject.put("description", rs.getString("description"));
////                newObject.put("price", rs.getString("price"));
////                newObject.put("color", rs.getString("color"));
////                newObject.put("fabricType", rs.getString("fabricType"));
////                newObject.put("rating", rs.getString("rating"));
////                newObject.put("img", rs.getString("img"));
//                searchResultsArray.put(newObject);
//            }
//            return new ResponseEntity(searchResultsArray.toString(), responseHeaders, HttpStatus.OK);
//        } catch (SQLException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return new ResponseEntity("{\"message\":\"" + " error \"}", responseHeaders, HttpStatus.OK);
//
//    }
@RequestMapping(value = "/getFoods", method = RequestMethod.GET)
public ResponseEntity<String> getFoods(HttpServletRequest request) {
//    String searchString = request.getParameter("Name");
//        System.out.println(searchString);
//    searchString = "%"+searchString+"%";
//        System.out.println(searchString);
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "application/json");

    try {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Comps?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", SQLPASSWORD);
        String query = "SELECT * FROM Comps.Foods";
        PreparedStatement stmt = conn.prepareStatement(query);
//        stmt.setString(1, searchString);
        ResultSet rs =  stmt.executeQuery();
        JSONArray searchResultsArray = new JSONArray();
        while(rs.next()) {
            JSONObject newObject = new JSONObject();
            newObject.put("Item", rs.getString("Item"));
//                System.out.println(newObject);
//                newObject.put("description", rs.getString("description"));
//                newObject.put("price", rs.getString("price"));
//                newObject.put("color", rs.getString("color"));
//                newObject.put("fabricType", rs.getString("fabricType"));
//                newObject.put("rating", rs.getString("rating"));
//                newObject.put("img", rs.getString("img"));
            searchResultsArray.put(newObject);
        }
        System.out.println("we good");
        System.out.println(searchResultsArray.get(0).toString());
        return new ResponseEntity(searchResultsArray.toString(), responseHeaders, HttpStatus.OK);
    } catch (SQLException e) {
        // TODO Auto-generated catch block
        System.out.println("Hello error alert");
        e.printStackTrace();
    }
    System.out.println("yikes");
    return new ResponseEntity("{\"message\":\"" + " error \"}", responseHeaders, HttpStatus.OK);
}
    @RequestMapping(value = "/getUserCalories", method = RequestMethod.POST)
    public ResponseEntity<String> getUserCalories(@RequestBody String payload, HttpServletRequest request) {
//    String searchString = request.getParameter("Name");
//        System.out.println(searchString);
//    searchString = "%"+searchString+"%";
//        System.out.println(searchString);
        System.out.println("in get Usercalories api");
        JSONObject payloadObj = new JSONObject(payload);
        String username = payloadObj.getString("username");
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");

        Connection conn;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Comps?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", SQLPASSWORD
            );
            System.out.println(conn);
            String query = "SELECT * FROM Users WHERE Username = (?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {
                String retrieveCal = rs.getString("Calories");
                JSONObject responseObj = new JSONObject();
                responseObj.put("userCalories", retrieveCal);
                System.out.println("Calories: " + retrieveCal);
                return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.OK);
            }
        }
        catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            JSONObject responseObj = new JSONObject();
//            responseObj.put("username", username);
            responseObj.put("message", e.getMessage());
            System.out.println("error");
            return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        JSONObject responseObj = new JSONObject();
        responseObj.put("calories", "No calories listed");
        System.out.println("Calories: 0");
        return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.OK);
    }


    @RequestMapping(value = "/getCalories", method = RequestMethod.POST)
    public ResponseEntity<String> getCalories(@RequestBody String payload, HttpServletRequest request) {
//    String searchString = request.getParameter("Name");
//        System.out.println(searchString);
//    searchString = "%"+searchString+"%";
//        System.out.println(searchString);
        System.out.println("in get calories api");
        JSONObject payloadObj = new JSONObject(payload);
        String name = payloadObj.getString("item");
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");

        Connection conn;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Comps?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", SQLPASSWORD
            );
            System.out.println(conn);
            String query = "SELECT * FROM Foods WHERE Item = (?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {
                String retrieveCal = rs.getString("Calories");
                JSONObject responseObj = new JSONObject();
                responseObj.put("calories", retrieveCal);
                System.out.println("Calories: " + retrieveCal);
                return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.OK);
            }
        }
        catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            JSONObject responseObj = new JSONObject();
//            responseObj.put("username", username);
            responseObj.put("message", e.getMessage());
            System.out.println("error");
            return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        JSONObject responseObj = new JSONObject();
        responseObj.put("calories", "No calories listed");
        System.out.println("Calories: --");
        return new ResponseEntity(responseObj.toString(), responseHeaders, HttpStatus.OK);
    }

    @RequestMapping(value = "/connectToDB", method = RequestMethod.GET) // <-- setup the endpoint URL at /hello with the HTTP POST method
    public ResponseEntity<String> connectToDB(HttpServletRequest request) {
        String nameToPull = request.getParameter("username");
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");
        Connection conn = null;
        JSONArray usersArray = new JSONArray();
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/classdb?useUnicode=true&characterEncoding=UTF-8", "root", "password");
            // get Connection method to get IP address port, username, and password
            String transactionQuery = "START TRANSACTION";
            PreparedStatement stmt = null;

//	    		String query = "INSERT VALUES INTO users (?, ?)";
            String query = "SELECT id, username FROM Users WHERE username=?"; // question mark means that you can set the value to anything
            stmt = conn.prepareStatement(query);
            stmt.execute();
            stmt = null; // use prepared statement to convert the query into a database statement
            stmt = conn.prepareStatement(query); // this is returned to execute a query
            stmt.setString(1, nameToPull);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) { // this goes through every single row of data
                String name = rs.getString("username");
                int userID = rs.getInt("id");

                JSONObject obj = new JSONObject();
                obj.put("name", name);
                obj.put("id", userID);
                usersArray.put(obj);
            }

            query = "COMMIT";
            stmt = conn.prepareStatement(query);

        } catch (SQLException e ) {
        } finally {
            try {
                if (conn != null) { conn.close(); }
            }catch(SQLException se) {

            }

        }
        return new ResponseEntity(usersArray.toString(), responseHeaders, HttpStatus.OK);
    }

    public static String bytesToHex(byte[] in) {
        StringBuilder builder = new StringBuilder();
        for(byte b: in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    protected String getSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 18) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;
    }
    public String generateRandomString(int length) {
        byte[] array = new byte[length];
        new Random().nextBytes(array);
        String generatedString = new String(array, Charset.forName("UTF-8"));

        return generatedString;
    }
}