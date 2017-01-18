package send;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.abstractj.kalium.*;
import org.abstractj.kalium.crypto.Hash;
import org.abstractj.kalium.encoders.Encoder;
import org.abstractj.kalium.encoders.Hex;
import org.abstractj.kalium.encoders.Raw;
import com.google.gson.*;

import com.sendgrid.*;
import java.io.IOException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

@RestController
public class UserController{

		@RequestMapping(value="/signup")
		public ResponseEntity<String> send(@RequestParam(value="userName", required=true) String userName,
				@RequestParam(value="email", required=true) String email,
				@RequestParam(value="passHash", required=true) String passHash,
				@RequestParam(value="passSalt", required=true) String passSalt,
				@RequestParam(value="phoneNumber", required=true) String phoneNumber) {
			
			boolean success = false;
			Connection connection = null;
			ResultSet resultSet = null;
			Statement statement = null;
			
			try{
				//String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "INSERT INTO User(Username, Email, PasswordHash, PasswordSalt, PhoneNumber, Keywords, "
						+ "ResetToken, ResetExpiration) VALUES(?,?,?,?,?,?,?,?)";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, userName);
				preparedStatement.setString(2, email);
				preparedStatement.setString(3, passHash);
				preparedStatement.setString(4, passSalt);
				preparedStatement.setString(5, phoneNumber);
				preparedStatement.setString(6, "");
				preparedStatement.setString(7, "");
				preparedStatement.setString(8, "");
				preparedStatement.executeUpdate(); //Need to do something when we try to insert a username/email that already exists
				success = true;
				System.out.println("Connection successful");
			} catch (SQLException e){
				System.out.println(e.getMessage());
				success = false;
			} finally {
				try{
					if (connection != null){
						connection.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}

			ResponseEntity responseEntity;
			if(success)
				responseEntity = new ResponseEntity<>("true", HttpStatus.OK);
			else
				responseEntity = new ResponseEntity<>("false", HttpStatus.OK);

			return responseEntity;
	}
		
		@RequestMapping(value="/checkAvailable")
		public ResponseEntity<String> checkAvailable(@RequestParam(value="username", required = false) String username){
			return null;
			
		}
		
		
		@RequestMapping(value="/login")
		public ResponseEntity<String> login(@RequestParam(value="userName", required=true) String userName,
				@RequestParam(value="passHash", required=true) String passHash,
				@RequestParam(value="passSalt", required=true) String passSalt){
			boolean success = false;
		
			Connection connection = null;
			ResultSet resultSet = null;
			Statement statement = null;
			try{
//				String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "Select * from User WHERE Username='" + userName + "' AND PasswordHash='" + passHash + "' AND PasswordSalt='" + passSalt + "';";
				System.out.println(sql);
				statement = connection.createStatement();
				resultSet = statement.executeQuery(sql);
				if(resultSet.next()){
					success = true;
				} else{
					success = false;
				}
				System.out.println("Connection successful");
			} catch (SQLException e){
				System.out.println(e.getMessage());
			} finally {
				try{
					if (connection != null){
						resultSet.close();
						statement.close();
						connection.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}

			ResponseEntity responseEntity;
			if(success)
				responseEntity = new ResponseEntity<>(userName, HttpStatus.OK);
			else
				responseEntity = new ResponseEntity<>("false", HttpStatus.OK);

			return responseEntity;
		}
		
		@RequestMapping(value="/reset")
		public ResponseEntity<String> recover(@RequestParam(value="email", required=true) String email) throws IOException {
			boolean success = false;
			Connection connection = null;
			ResultSet resultSet = null;
			Statement statement = null;
			
			try{
				//String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "Select * from User WHERE Email=" + email + ";";
				System.out.println(sql);
				statement = connection.createStatement();
				resultSet = statement.executeQuery(sql);
				if(resultSet.next()){
					success = true;
				} else{
					success = false;
				}
				System.out.println("Connection successful");
			} catch (SQLException e){
				System.out.println(e.getMessage());
			} finally {
				try{
					if (connection != null){
						resultSet.close();
						statement.close();
						connection.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}

			ResponseEntity responseEntity;
			if(success){
				ResetIdentifierGenerator resetIdentifierGenerator = new ResetIdentifierGenerator();
				String resetToken = resetIdentifierGenerator.nextSessionId();
				long time = System.nanoTime() + 180000000000L;  //add thirty minutes in nanoseconds
				String timeString = Long.toString(time);
				System.out.println(resetToken + " " + timeString);
				updateResetToken(email, resetToken, timeString);

				/*Hash hash = new Hash();
				Hex hex = new Hex();
				String hashedToken = hash.sha256(resetToken, hex);
				*/
				
				Email from = new Email("");
				String subject = "PMR Password reset request";
				//Email to = new Email(email);
				Email to = new Email("com");
				Content content = new Content("text/plain", "Hello, please click this link to take you to a password reset page"
						+ "\npmr.com/resetpassword?token=" + resetToken);
				Mail mail = new Mail(from, subject, to, content);
				SendGrid sg = new SendGrid("");
				Request request = new Request();
				

				try {
				  request.method = Method.POST;
				  request.endpoint = "mail/send";
				  request.body = mail.build();
				  Response response = sg.api(request);
				  System.out.println(response.statusCode);
				  System.out.println(response.body);
				  System.out.println(response.headers);
				} catch (IOException ex) {
				  throw ex;
				}
				responseEntity = new ResponseEntity<>("true", HttpStatus.OK);
				System.out.println("Sending recovery email to " + email);
			}
			else{
				responseEntity = new ResponseEntity<>("false", HttpStatus.OK);
				System.out.println("Email not in db - not doing anything");
			}	
			
			return responseEntity;
		}
		
		
		@RequestMapping(value="/retrievePlayers") //Encode into UTF-8
		public ResponseEntity<String> retrievePlayers(){
			boolean success = false;
			PlayerList playerList = new PlayerList();
			ArrayList<Player> players = new ArrayList<Player>();
			Connection connection = null;
			ResultSet resultSet = null;
			Statement statement = null;
			
			
			try{
//				String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "Select * from Player;";
				statement = connection.createStatement();
				resultSet = statement.executeQuery(sql);
				if(resultSet.next()){
					Player initialPlayer = new Player(resultSet.getString("League"),
							resultSet.getString("Team"), resultSet.getString("Player"));
					players.add(initialPlayer);
					success = true;
					while(resultSet.next()){
						Player player = new Player(resultSet.getString("League"),
								resultSet.getString("Team"), resultSet.getString("Player"));
						players.add(player);
					}
					playerList.setList(players);
				} else{
					success = false;
				}
				System.out.println("Connection successful");
			} catch (SQLException e){
				System.out.println(e.getMessage());
			} finally {
				try{
					if (connection != null){
						resultSet.close();
						statement.close();
						connection.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}

			ResponseEntity responseEntity;
			if(success){
				Gson gson = new Gson();
				responseEntity = new ResponseEntity<>(gson.toJson(playerList), HttpStatus.OK);
			}
			else{
				responseEntity = new ResponseEntity<>("false", HttpStatus.OK);
			}

			return responseEntity;
		}
		
		@RequestMapping(value="/retrieveFavorites")
		public ResponseEntity<String> retrieveFavorites(){
			PlayerList playerList = new PlayerList();
			ArrayList<Player> favorites = new ArrayList<Player>();
			Player player = new Player("", "", "Virgil van Dijk");
			Player player1 = new Player("", "", "Jos� Fonte");
			Player player2 = new Player("", "", "Florin Gardos");
			Player player3 = new Player("", "", "Maya Yoshida");
			favorites.add(player);
			favorites.add(player1);
			favorites.add(player2);
			favorites.add(player3);
			playerList.setList(favorites);
			Gson gson = new Gson();
			ResponseEntity responseEntity;
			responseEntity = new ResponseEntity<>(gson.toJson(playerList), HttpStatus.OK); 
			return responseEntity;
		}
		
		@RequestMapping(value="/sendFavorites")
		public ResponseEntity<String> addUserFavorites(){

			return null;
		}
		
		
		public void updateResetToken(String email, String token, String timeExpiration){
			Connection connection = null;
			Statement statement = null;
			System.out.println(email + " " + token + " " + timeExpiration);
			
			try{
				//String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "UPDATE User SET ResetToken = ? , ResetExpiration = ? WHERE Email = ?";
				System.out.println(sql);
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, token);
				preparedStatement.setString(2, timeExpiration);
				preparedStatement.setString(3, email);
				preparedStatement.executeUpdate(); //Need to do something when we try to insert a username/email that already exists
				System.out.println("Connection successful");
			} catch (SQLException e){
				System.out.println(e.getMessage());
			} /*finally {
				try{
					if (connection != null){
						connection.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}*/
			
		}
}
