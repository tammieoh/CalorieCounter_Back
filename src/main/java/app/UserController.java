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
                PreparedStatement stmt = conn.prepareStatement("INSERT INTO Users (Username, Password, Email) VALUES (?,?,?)");
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
            newObject.put("Name", rs.getString("Name"));
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
            String query = "SELECT * FROM Foods WHERE Name = (?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {
                String retrieveCal = rs.getString("Calories(kcal)");
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
//    @RequestMapping(value = "/addToWishList", method = RequestMethod.POST) // <-- setup the endpoint URL at /hello with the HTTP POST method
//    public ResponseEntity<String> addToWishList(HttpServletRequest request) {
//        String nameToPull = request.getParameter("username");
//        String clothingName = request.getParameter("productName");
//        int userId = 70;
//        int productId = 71;
//
//        HttpHeaders responseHeaders = new HttpHeaders();
//        responseHeaders.set("Content-Type", "application/json");
//
//
////		for(int i = 0; i < app.MyServer.listOfUsers.size(); i++) {
////			if(app.MyServer.listOfUsers.get(i).username.equals(nameToPull)) {
////				Clothing product = new Clothing();
////				product.name = clothingName;
////				app.MyServer.listOfUsers.get(i).wishList.addProduct(product);
////				return new ResponseEntity("{\"message\":\"" + clothingName + " has been added to wish list\"}", responseHeaders, HttpStatus.OK);
////			}
////		}
//
//        try {
//            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Shopping?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", "123");
//            String query = "SELECT Id FROM Users WHERE username = (?)";
//            PreparedStatement stmt = conn.prepareStatement(query);
//            stmt.setString(1, nameToPull);
//            ResultSet rs =  stmt.executeQuery();
//            if (!rs.isBeforeFirst() ) {
//                return new ResponseEntity("{\"message\":\"" + nameToPull + " not found \"}", responseHeaders, HttpStatus.BAD_REQUEST);
//            }
//            while (rs.next()) { // this goes through every single row of data
//                userId = rs.getInt("Id");
//                System.out.println("inside while loop, userid:" + userId);
//            }
//            String productQuery = "SELECT productId FROM Products WHERE name = (?)";
//            stmt = conn.prepareStatement(productQuery);
//            stmt.setString(1, clothingName);
//            rs =  stmt.executeQuery();
//            if (!rs.isBeforeFirst() ) {
//                return new ResponseEntity("{\"message\":\"" + clothingName + " not found \"}", responseHeaders, HttpStatus.BAD_REQUEST);
//            }
//            while (rs.next()) { // this goes through every single row of data
//                productId = rs.getInt("productId");
//            }
//            query = "INSERT INTO WishList VALUES (?,?,?)";
//            System.out.println("userID: " + userId);
//            System.out.println("productId: " + productId);
//            stmt = conn.prepareStatement(query);
//            stmt.setNull(1, java.sql.Types.NULL);
//            stmt.setString(2, Integer.toString(userId));
//            stmt.setString(3, Integer.toString(productId));
//            stmt.execute();
//            query = "COMMIT";
//        } catch (SQLException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
////			ResultSet rs = stmt.executeQuery();
////			PreparedStatement stmt1 = conn.prepareStatement("INSERT INTO Shopping.WishList VALUES (?,?)");
////			stmt.setInt(1, userId);
////			stmt.setInt(2, productId);
////			stmt.execute();
////			} catch (SQLException e) {
////				// TODO Auto-generated catch block
////				e.printStackTrace();
////			}
////
//        return new ResponseEntity("{\"message\":\"" + clothingName + " has been found \"}", responseHeaders, HttpStatus.OK);
//    }

//   @RequestMapping(value = "/showWishList", method = RequestMethod.GET) // <-- setup the endpoint URL at /hello with the HTTP POST method
//    public ResponseEntity<ArrayList<Clothing>> showWishList(HttpServletRequest request) {
//        int userIdtoPull = Integer.parseInt(request.getParameter("userId"));
//        int productId = 0;
//        System.out.println("show wishlist request received");
//        HttpHeaders responseHeaders = new HttpHeaders();
//        responseHeaders.set("Content-Type", "application/json");
////		for(int i = 0; i < app.MyServer.listOfUsers.size(); i++) {
////			if(app.MyServer.listOfUsers.get(i).username.equals(nameToPull)) {
////				ShoppingCart userCart = app.MyServer.listOfUsers.get(i).shoppingCart;
////				ArrayList<Clothing> a1 = userCart.listOfProducts;
////				GsonBuilder gsonBuilder = new GsonBuilder();
////				Gson gson = gsonBuilder.create();
////				String JSONObject = gson.toJson(app.MyServer.listOfUsers.get(i).shoppingCart.listOfProducts);
////				return new ResponseEntity(JSONObject, responseHeaders, HttpStatus.OK);
////			}
////		}
//        try {
//            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Shopping?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", Clothing.SQL_PASSWORD);
//            String query = "SELECT WishList.productId, Products.name, Products.price FROM WishList JOIN Products ON WishList.productId=Products.productId WHERE WishList.userId=(?)";
//            PreparedStatement stmt = conn.prepareStatement(query);
//            stmt.setInt(1, userIdtoPull);
//            ResultSet rs =  stmt.executeQuery();
//            if (!rs.isBeforeFirst() ) {
//                return new ResponseEntity("{\"message\":\"" + userIdtoPull + " not found \"}", responseHeaders, HttpStatus.BAD_REQUEST);
//            }
//            JSONArray searchResultsArray = new JSONArray();
//            while (rs.next()) { // this goes through every single row of data
//                JSONObject newObject = new JSONObject();
//                newObject.put("productId", rs.getString("productId"));
//                newObject.put("name", rs.getString("name"));
//                newObject.put("price", rs.getString("price"));
//                searchResultsArray.put(newObject);
//            }
//            if (searchResultsArray.isEmpty()) {
//                return new ResponseEntity("{\"message\":\" nothing in wishlist \"}", responseHeaders, HttpStatus.OK);
//            } else {
//                return new ResponseEntity(searchResultsArray.toString(), responseHeaders, HttpStatus.OK);
//            }
//
////			String wishListQuery = "SELECT Products.productId, Products.name FROM ShoppingCart JOIN Products ON ShoppingCart.productId=Products.productId WHERE ShoppingCart.productId = (?)";
////			stmt = conn.prepareStatement(wishListQuery);
////			stmt.setString(1, Integer.toString(productId));
////			rs =  stmt.executeQuery();
////			if (!rs.isBeforeFirst() ) {
////				return new ResponseEntity("{\"message\":\"" + "nothing in shopping cart \"}", responseHeaders, HttpStatus.BAD_REQUEST);
////			}
////			JSONArray searchResultsArray = new JSONArray();
////			while(rs.next()) {
////				JSONObject newObject = new JSONObject();
////				newObject.put("name", rs.getString("name"));
////				newObject.put("description", rs.getString("description"));
////				newObject.put("price", rs.getString("price"));
////				searchResultsArray.put(newObject);
////			}
////			return new ResponseEntity(searchResultsArray.toString(), responseHeaders, HttpStatus.OK);
////			my code
////			GsonBuilder gsonBuilder = new GsonBuilder();
////			Gson gson = gsonBuilder.create();
////			String JSONObject = gson.toJson(app.MyServer.listOfUsers.get(i).shoppingCart.listOfProducts);
////			return new ResponseEntity(JSONObject, responseHeaders, HttpStatus.OK);
//        } catch (SQLException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return new ResponseEntity("error", responseHeaders, HttpStatus.OK);
//    }
//    @RequestMapping(value = "/showProducts", method = RequestMethod.GET) // <-- setup the endpoint URL at /hello with the HTTP POST method
//    public ResponseEntity<ArrayList<Clothing>> showProducts(HttpServletRequest request) {
//        System.out.println("show products request received");
//        HttpHeaders responseHeaders = new HttpHeaders();
//        responseHeaders.set("Content-Type", "application/json");
//        JSONArray productResultsArray = new JSONArray();
//        try {
//            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Shopping?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", Clothing.SQL_PASSWORD);
//            String query = "SELECT * FROM Shopping.Products";
//            PreparedStatement stmt = conn.prepareStatement(query);
//            ResultSet rs = stmt.executeQuery();
//
//            while (rs.next()) {
//                JSONObject newObject = new JSONObject();
//                newObject.put("productId", rs.getString("productId"));
//                newObject.put("name", rs.getString("name"));
//                newObject.put("price", rs.getInt("price"));
//                newObject.put("description", rs.getString("description"));
//                productResultsArray.put(newObject);
//            }
//        } catch (SQLException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        if (productResultsArray.isEmpty()) {
//            return new ResponseEntity("{\"message\":\" nothing in product table \"}", responseHeaders, HttpStatus.OK);
//        } else {
//            return new ResponseEntity(productResultsArray.toString(), responseHeaders, HttpStatus.OK);
//        }
//    }

//    @RequestMapping(value = "/addToShoppingCart", method = RequestMethod.POST) // <-- setup the endpoint URL at /hello with the HTTP POST method
//    public ResponseEntity<String> addToShoppingCart(HttpServletRequest request) {
//        String nameToPull = request.getParameter("username");
//        String clothingName = request.getParameter("productName");
//        int userId = 0;
//        int productId = 0;
//
//        HttpHeaders responseHeaders = new HttpHeaders();
//        responseHeaders.set("Content-Type", "application/json");
//
//
////		for(int i = 0; i < app.MyServer.listOfUsers.size(); i++) {
////			if(app.MyServer.listOfUsers.get(i).username.equals(nameToPull)) {
////				Clothing product = new Clothing();
////				product.name = clothingName;
////				app.MyServer.listOfUsers.get(i).wishList.addProduct(product);
////				return new ResponseEntity("{\"message\":\"" + clothingName + " has been added to wish list\"}", responseHeaders, HttpStatus.OK);
////			}
////		}
//
//        try {
//            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Shopping?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", Clothing.SQL_PASSWORD);
//            String query = "SELECT Id FROM Users WHERE username = (?)";
//            PreparedStatement stmt = conn.prepareStatement(query);
//            stmt.setString(1, nameToPull);
//            ResultSet rs =  stmt.executeQuery();
//            if (!rs.isBeforeFirst() ) {
//                return new ResponseEntity("{\"message\":\"" + nameToPull + " not found \"}", responseHeaders, HttpStatus.BAD_REQUEST);
//            }
//            while (rs.next()) { // this goes through every single row of data
//                userId = rs.getInt("Id");
//                System.out.println("inside while loop, userid:" + userId);
//            }
//            String productQuery = "SELECT productId FROM Products WHERE name = (?)";
//            stmt = conn.prepareStatement(productQuery);
//            stmt.setString(1, clothingName);
//            rs =  stmt.executeQuery();
//            if (!rs.isBeforeFirst() ) {
//                return new ResponseEntity("{\"message\":\"" + clothingName + " not found \"}", responseHeaders, HttpStatus.BAD_REQUEST);
//            }
//            while (rs.next()) { // this goes through every single row of data
//                productId = rs.getInt("productId");
//            }
//            query = "INSERT INTO ShoppingCart VALUES (?,?,?)";
//            stmt = conn.prepareStatement(query);
//            stmt.setNull(1, java.sql.Types.NULL);
//            stmt.setString(2, Integer.toString(userId));
//            stmt.setString(3, Integer.toString(productId));
//            stmt.execute();
//            query = "COMMIT";
//        } catch (SQLException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
////			ResultSet rs = stmt.executeQuery();
////			PreparedStatement stmt1 = conn.prepareStatement("INSERT INTO Shopping.WishList VALUES (?,?)");
////			stmt.setInt(1, userId);
////			stmt.setInt(2, productId);
////			stmt.execute();
////			} catch (SQLException e) {
////				// TODO Auto-generated catch block
////				e.printStackTrace();
////			}
////
//        return new ResponseEntity("{\"message\":\"" + clothingName + " has been found \"}", responseHeaders, HttpStatus.OK);
//    }
//
//    @RequestMapping(value = "/showShoppingCart", method = RequestMethod.GET) // <-- setup the endpoint URL at /hello with the HTTP POST method
//    public ResponseEntity<ArrayList<Clothing>> showShoppingCart(HttpServletRequest request) {
//        int userIdtoPull = Integer.parseInt(request.getParameter("userId"));
//        int productId = 0;
//        System.out.println("show shopping cart request received");
//        HttpHeaders responseHeaders = new HttpHeaders();
//        responseHeaders.set("Content-Type", "application/json");
////		for(int i = 0; i < app.MyServer.listOfUsers.size(); i++) {
////			if(app.MyServer.listOfUsers.get(i).username.equals(nameToPull)) {
////				ShoppingCart userCart = app.MyServer.listOfUsers.get(i).shoppingCart;
////				ArrayList<Clothing> a1 = userCart.listOfProducts;
////				GsonBuilder gsonBuilder = new GsonBuilder();
////				Gson gson = gsonBuilder.create();
////				String JSONObject = gson.toJson(app.MyServer.listOfUsers.get(i).shoppingCart.listOfProducts);
////				return new ResponseEntity(JSONObject, responseHeaders, HttpStatus.OK);
////			}
////		}
//        try {
//            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Shopping?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", Clothing.SQL_PASSWORD);
//            String query = "SELECT ShoppingCart.productId, Products.name, Products.price FROM ShoppingCart JOIN Products ON ShoppingCart.productId=Products.productId WHERE ShoppingCart.userId=(?)";
//            PreparedStatement stmt = conn.prepareStatement(query);
//            stmt.setInt(1, userIdtoPull);
//            ResultSet rs =  stmt.executeQuery();
//            if (!rs.isBeforeFirst() ) {
//                return new ResponseEntity("{\"message\":\"" + userIdtoPull + " not found \"}", responseHeaders, HttpStatus.BAD_REQUEST);
//            }
//            JSONArray searchResultsArray = new JSONArray();
//            while (rs.next()) { // this goes through every single row of data
//                JSONObject newObject = new JSONObject();
//                newObject.put("productId", rs.getString("productId"));
//                newObject.put("name", rs.getString("name"));
//                newObject.put("price", rs.getString("price"));
//                searchResultsArray.put(newObject);
//            }
//            if (searchResultsArray.isEmpty()) {
//                return new ResponseEntity("{\"message\":\" nothing in shopping cart \"}", responseHeaders, HttpStatus.OK);
//            } else {
//                return new ResponseEntity(searchResultsArray.toString(), responseHeaders, HttpStatus.OK);
//            }
//
////			String wishListQuery = "SELECT Products.productId, Products.name FROM ShoppingCart JOIN Products ON ShoppingCart.productId=Products.productId WHERE ShoppingCart.productId = (?)";
////			stmt = conn.prepareStatement(wishListQuery);
////			stmt.setString(1, Integer.toString(productId));
////			rs =  stmt.executeQuery();
////			if (!rs.isBeforeFirst() ) {
////				return new ResponseEntity("{\"message\":\"" + "nothing in shopping cart \"}", responseHeaders, HttpStatus.BAD_REQUEST);
////			}
////			JSONArray searchResultsArray = new JSONArray();
////			while(rs.next()) {
////				JSONObject newObject = new JSONObject();
////				newObject.put("name", rs.getString("name"));
////				newObject.put("description", rs.getString("description"));
////				newObject.put("price", rs.getString("price"));
////				searchResultsArray.put(newObject);
////			}
////			return new ResponseEntity(searchResultsArray.toString(), responseHeaders, HttpStatus.OK);
////			my code
////			GsonBuilder gsonBuilder = new GsonBuilder();
////			Gson gson = gsonBuilder.create();
////			String JSONObject = gson.toJson(app.MyServer.listOfUsers.get(i).shoppingCart.listOfProducts);
////			return new ResponseEntity(JSONObject, responseHeaders, HttpStatus.OK);
//        } catch (SQLException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return new ResponseEntity("error", responseHeaders, HttpStatus.OK);
//    }
//    @RequestMapping(value = "/moveToShoppingCart", method = RequestMethod.POST)
//    public ResponseEntity<String> moveToShoppingCart(HttpServletRequest request) {
//        String nameToPull = request.getParameter("username");
//        String clothingName = request.getParameter("productName");
//        int userId = 0;
//        int productId = 0;
//
//        HttpHeaders responseHeaders = new HttpHeaders();
//        responseHeaders.set("Content-Type", "application/json");
//
//        try {
//            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Shopping?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", Clothing.SQL_PASSWORD);
//            String query = "SELECT Id FROM Users WHERE username = (?)";
//            PreparedStatement stmt = conn.prepareStatement(query);
//            stmt.setString(1, nameToPull);
//            ResultSet rs =  stmt.executeQuery();
//            if (!rs.isBeforeFirst() ) {
//                return new ResponseEntity("{\"message\":\"" + nameToPull + " not found \"}", responseHeaders, HttpStatus.BAD_REQUEST);
//            }
//            while (rs.next()) { // this goes through every single row of data
//                userId = rs.getInt("Id");
//                System.out.println("inside while loop, userid:" + userId);
//            }
//            String productQuery = "SELECT productId FROM Products WHERE name = (?)";
//            stmt = conn.prepareStatement(productQuery);
//            stmt.setString(1, clothingName);
//            rs =  stmt.executeQuery();
//            if (!rs.isBeforeFirst() ) {
//                return new ResponseEntity("{\"message\":\"" + clothingName + " not found \"}", responseHeaders, HttpStatus.BAD_REQUEST);
//            }
//            while (rs.next()) { // this goes through every single row of data
//                productId = rs.getInt("productId");
//            }
//            String wishListQuery = "SELECT productId FROM WishList WHERE productId = (?)";
//            stmt = conn.prepareStatement(wishListQuery);
//            stmt.setString(1, Integer.toString(productId));
//            rs =  stmt.executeQuery();
//            if (!rs.isBeforeFirst() ) {
//                return new ResponseEntity("{\"message\":\"" + clothingName + "2 not found \"}", responseHeaders, HttpStatus.BAD_REQUEST);
//            }
//            while (rs.next()) { // this goes through every single row of data
//                productId = rs.getInt("productId");
//            }
//            query = "INSERT INTO ShoppingCart VALUES (?,?,?)";
//            stmt = conn.prepareStatement(query);
//            stmt.setNull(1, java.sql.Types.NULL);
//            stmt.setString(2, Integer.toString(userId));
//            stmt.setString(3, Integer.toString(productId));
//            stmt.execute();
//            query = "COMMIT";
//        } catch (SQLException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return new ResponseEntity("{\"message\":\"" + clothingName + " has been moved to shopping cart \"}", responseHeaders, HttpStatus.OK);
//    }
//


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