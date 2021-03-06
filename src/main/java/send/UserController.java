package send;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.abstractj.kalium.*;
import org.abstractj.kalium.crypto.Hash;
import org.abstractj.kalium.crypto.Password;
import org.abstractj.kalium.encoders.Encoder;
import static org.abstractj.kalium.encoders.Encoder.HEX;
import org.abstractj.kalium.encoders.Hex;
import org.abstractj.kalium.encoders.Raw;
import com.google.gson.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.regex.Pattern;

//@CrossOrigin(origins = "https://peemr.localtunnel.me")
@CrossOrigin(origins = "*")
@RestController
public class UserController{

	private GmailService service = new GmailService();
	private static final String USER_DB_CONNECTION_STRING = "jdbc:sqlite:../server/db/user.db";
	private static final String PLAYER_DB_CONNECTION_STRING = "jdbc:sqlite:../server/db/player.db";
	
		@RequestMapping(value="/signup")
		public ResponseEntity<String> send(@RequestParam(value="userName", required=true) String userName,
				@RequestParam(value="email", required=true) String email,
				@RequestParam(value="passHash", required=true) String passHash,
				@RequestParam(value="passSalt", required=true) String passSalt,
				@RequestParam(value="phoneNumber", required=true) String phoneNumber,
				@RequestParam(value="loginKey", required=true) String loginKey) {
			
			boolean success = false;
			Connection connection = null;
			ResultSet resultSet = null;
			Statement statement = null;
			String[] values = new String[2]; //0 - salt, 1 - hash
			values = getPasswordHash(passHash);
			passHash = values[1];
			String saltString = values[0];
//			String saltString = "";
			IdentifierGenerator IdentifierGenerator = new IdentifierGenerator();
			String confirmToken = IdentifierGenerator.nextSessionId();

			long resetTime = System.nanoTime() + 157700000000000000L;  //add one year in nanoseconds
			long loginResetTime = System.nanoTime() + 3600000000000L;  //add one hour in nanoseconds
			try{
				String url = USER_DB_CONNECTION_STRING;
				connection = DriverManager.getConnection(url);
				String sql = "INSERT INTO User(Username, Email, PasswordHash, PasswordSalt, PhoneNumber, Keywords, "
						+ "ResetToken, ResetExpiration, ReceiveTexts, ReceiveEmails, LoginKey, LoginKeyExpiration, ServerPasswordSalt, "
						+ "ConfirmToken)"
						+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, userName);
				preparedStatement.setString(2, email);
				preparedStatement.setString(3, passHash);
				preparedStatement.setString(4, passSalt);
				preparedStatement.setString(5, phoneNumber);
				preparedStatement.setString(6, "");
				preparedStatement.setString(7, "");
				preparedStatement.setString(8, "");
				preparedStatement.setFloat(9, 0); //Account is not yet confirmed so we set to 0
				preparedStatement.setFloat(10, 0);//Account is not yet confirmed so we set to 0
				preparedStatement.setString(11, loginKey);
				preparedStatement.setFloat(12, loginResetTime);
				preparedStatement.setString(13,  saltString);
				preparedStatement.setString(14, confirmToken);
				preparedStatement.executeUpdate(); 
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

			if(success == true){
				GmailService.send(this.service.getService(), email, "pmridontcareifyourespond@gmail.com", "PMR Account Confirmation", 
						"Hello, please click this link to take you to confirm you account so you can start receiving notifications" +
	 					 "\nhttp://peemr.com:8080/confirmAccount?token=" + confirmToken + "&userName=" + userName);
			}

			ResponseEntity responseEntity;
			if(success)
				responseEntity = new ResponseEntity<>("true", HttpStatus.OK);
			else
				responseEntity = new ResponseEntity<>("false", HttpStatus.OK);

			return responseEntity;
	}

		@RequestMapping(value="/checkUser")
		public ResponseEntity<String> checkUser(@RequestParam(value="userName", required = true) String userName){
			boolean success = false;
			String salt = "";
		
			Connection connection = null;
			ResultSet resultSet = null;
			Statement statement = null;
			try{
				String url = USER_DB_CONNECTION_STRING;
				connection = DriverManager.getConnection(url);
				statement = connection.createStatement();
				String sql = "Select * from User WHERE Username = ?";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, userName);
				resultSet = preparedStatement.executeQuery();
				if(resultSet.next()){
					salt = resultSet.getString("PasswordSalt");
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
				responseEntity = new ResponseEntity<>(salt, HttpStatus.OK);
			else
				responseEntity = new ResponseEntity<>("false", HttpStatus.OK);

			return responseEntity;
		}

		@RequestMapping(value="/login")
		public ResponseEntity<String> login(@RequestParam(value="userName", required=true) String userName,
				@RequestParam(value="passHash", required=true) String passHash,
				@RequestParam(value="loginKey", required=true) String loginKey){
			int success = 0; //0 - failed credentials, 1 -- failed confirmation, 2 -- success
		
			Connection connection = null;
			ResultSet resultSet = null;
			float receiveEmails = 0;
			String serverSalt = "";
			
			try{
				String url = USER_DB_CONNECTION_STRING;
				connection = DriverManager.getConnection(url);
				String sql = "Select ServerPasswordSalt from User WHERE Username = ?";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, userName);
				resultSet = preparedStatement.executeQuery();
				if(resultSet.next()){
					serverSalt = resultSet.getString("ServerPasswordSalt");
					System.out.println("Server salt: " + serverSalt);
				}
				System.out.println("Connection successful");
			} catch (SQLException e){
				System.out.println(e.getMessage());
			} finally {
				try{
					if (connection != null){
						resultSet.close();
						connection.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}
		
			passHash = getPasswordHash(passHash, serverSalt);
			
			try{
				String url = USER_DB_CONNECTION_STRING;
				connection = DriverManager.getConnection(url);
				String sql = "Select ReceiveEmails from User WHERE Username = ? AND PasswordHash = ?";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, userName);
				preparedStatement.setString(2, passHash);
				resultSet = preparedStatement.executeQuery();
				if(resultSet.next()){
					receiveEmails = resultSet.getFloat("ReceiveEmails");
					System.out.println("emails: " + receiveEmails);
					success = 2;
				} else{
					success = 0;
				}
				System.out.println("Connection successful");
			} catch (SQLException e){
				System.out.println(e.getMessage());
			} finally {
				try{
					if (connection != null){
						resultSet.close();
						connection.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}
			updateLoginKey(userName, loginKey);
			if(receiveEmails == 0){
				success = 1;
			}

			ResponseEntity responseEntity;
			if(success == 0){
				responseEntity = new ResponseEntity<>("credentials", HttpStatus.OK);
			} else if(success == 1){
				responseEntity = new ResponseEntity<>("confirmation", HttpStatus.OK);
			} else if(success == 2){
				responseEntity = new ResponseEntity<>("true", HttpStatus.OK);
			} else{
				responseEntity = new ResponseEntity<>("false", HttpStatus.OK);
			}

			return responseEntity;
		}
		
		@RequestMapping(value="/sendSettings")
		public ResponseEntity<String> sendSettings(@RequestParam(value="userName", required=true) String userName,
				@RequestParam(value="loginKey", required=true) String loginKey){
			boolean success = false;
		
			Connection connection = null;
			ResultSet resultSet = null;
			float receiveEmails = 0;
			float receiveTexts = 0;
			NotificationSettings notificationSettings = new NotificationSettings();
			try{
				String url = USER_DB_CONNECTION_STRING;
				connection = DriverManager.getConnection(url);
				String sql = "Select ReceiveEmails, ReceiveTexts from User WHERE Username = ? AND LoginKey = ?";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, userName);
				preparedStatement.setString(2, loginKey);
				resultSet = preparedStatement.executeQuery();
				if(resultSet.next()){
					success = true;
					receiveEmails = resultSet.getFloat("ReceiveEmails");
					receiveTexts = resultSet.getFloat("ReceiveTexts");
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
						connection.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}
			Gson gson = new Gson();
			float currentTime = System.nanoTime();
			if(currentTime > receiveEmails){
				notificationSettings.setReceiveEmails(1);
			}else{
				notificationSettings.setReceiveEmails(0);
			}

			if(currentTime > receiveTexts){
				notificationSettings.setReceiveTexts(1);
			}else{
				notificationSettings.setReceiveTexts(0);
			}
			ResponseEntity responseEntity;
			if(success)
				responseEntity = new ResponseEntity<>(gson.toJson(notificationSettings), HttpStatus.OK);
			else
				responseEntity = new ResponseEntity<>("false", HttpStatus.OK);

			return responseEntity;
		}
		
		
		@RequestMapping(value="/updateSettings", method = RequestMethod.POST)
		public ResponseEntity<String> updateSettings(@RequestParam(value = "receiveEmails", required = true) float receiveEmails, 
				@RequestParam(value = "receiveTexts", required = true) float receiveTexts,
				@RequestParam(value = "suspendNotifs", required = true) float suspendNotifs,
				@RequestParam(value = "userName", required = true) String userName,
				@RequestParam(value = "loginKey", required = true) String loginKey){
			boolean success = false;
			Connection connection = null;

			if(receiveEmails == 1.0){
				receiveEmails = System.nanoTime() + (suspendNotifs * 3600000000000L);
			} else{
				receiveEmails = System.nanoTime() + 157700000000000000L;
			}
			if(receiveTexts == 1.0){
				receiveTexts = System.nanoTime() + (suspendNotifs * 3600000000000L);
			} else{
				receiveTexts = System.nanoTime() + 157700000000000000L;
			}
			
			try{
				String url = USER_DB_CONNECTION_STRING;
				connection = DriverManager.getConnection(url);
				String sql = "UPDATE User SET ReceiveTexts = ? , ReceiveEmails = ? WHERE Username = ?";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setFloat(1, receiveTexts);
				preparedStatement.setFloat(2, receiveEmails);
				preparedStatement.setString(3, userName);
				preparedStatement.executeUpdate();
				System.out.println("Connection successful");
				success = true;
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


			boolean validLoginKey = checkLoginKey(userName, loginKey);
			
			ResponseEntity responseEntity;
			if(success && validLoginKey){
				responseEntity = new ResponseEntity<>("true", HttpStatus.OK);
			}
			else{
				responseEntity = new ResponseEntity<>("false", HttpStatus.OK);
			}

			return responseEntity;
		}	
		
		@RequestMapping(value="/reset")
		public ResponseEntity<String> recover(@RequestParam(value="email", required=true) String email) throws IOException {
			boolean success = false;
			Connection connection = null;
			ResultSet resultSet = null;
			
			try{
				String url = USER_DB_CONNECTION_STRING;
				connection = DriverManager.getConnection(url);
				String sql = "Select * from User WHERE Email = ?";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, email);
				resultSet = preparedStatement.executeQuery();

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
						connection.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}

			ResponseEntity responseEntity;
			if(success){
				IdentifierGenerator IdentifierGenerator = new IdentifierGenerator();
				String resetToken = IdentifierGenerator.nextSessionId();
				long time = System.nanoTime() + 180000000000L;  //add thirty minutes in nanoseconds
				String timeString = Long.toString(time);
				updateResetToken(email, resetToken, timeString);

				GmailService.send(this.service.getService(), email, "pmridontcareifyourespond@gmail.com", "PMR Password Reset", 
						"Hello, please click this link to take you to a password reset page" +
	 					 "\nhttp://peemr.com/resetPassword?token=" + resetToken + "&email=" + email);


				responseEntity = new ResponseEntity<>("true", HttpStatus.OK);
				System.out.println("Sending recovery email to " + email);
			}
			else{
				responseEntity = new ResponseEntity<>("false", HttpStatus.OK);
				System.out.println("Email not in db - not doing anything");
			}	
			
			return responseEntity;
		}

		@RequestMapping(value="/resetpassword")
		public ResponseEntity<String> resetPassword(@RequestParam(value = "token", required=true)String token,
				@RequestParam(value = "email", required=true) String email,
				@RequestParam(value = "passwordHash", required=true) String passwordHash,
				@RequestParam(value = "passwordSalt", required=true) String passwordSalt){
			
			long currentTime = System.nanoTime(); //get current time in nanoseconds
			String resetString = "";
			boolean initialSuccess = false;
			boolean secondarySuccess = false;
			Connection connection = null;
			ResultSet resultSet = null;
			
			try{
				//String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = USER_DB_CONNECTION_STRING;
				connection = DriverManager.getConnection(url);
				String sql = "Select ResetExpiration from User WHERE Email = ?";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, email);
				resultSet = preparedStatement.executeQuery();

				if(resultSet.next()){
					initialSuccess = true;
					resetString = resultSet.getString("ResetExpiration");
				} else{
					initialSuccess = false;
				}
				System.out.println("Connection successful");
			} catch (SQLException e){
				System.out.println(e.getMessage());
			} finally {
				try{
					if (connection != null){
						resultSet.close();
						connection.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}
			ResponseEntity responseEntity;

			String[] values = new String[2]; //0 - salt, 1 - hash
			values = getPasswordHash(passwordHash);
			passwordHash = values[1];
			String saltString = values[0];
			
			if(initialSuccess){
				long resetTime = Long.parseLong(resetString);
				if(currentTime - resetTime < 0){
					System.out.println("token still valid");
					try{
						String url = USER_DB_CONNECTION_STRING;
						connection = DriverManager.getConnection(url);
						String sql = "UPDATE User SET PasswordHash = ?, PasswordSalt = ?, ResetExpiration = ?, ServerPasswordSalt WHERE Email = ?";
						PreparedStatement preparedStatement = connection.prepareStatement(sql);
						preparedStatement.setString(1, passwordHash);
						preparedStatement.setString(2, passwordSalt);
						preparedStatement.setString(3, "0");
						preparedStatement.setString(4, saltString);
						preparedStatement.setString(5, email);
						preparedStatement.executeUpdate();		
						System.out.println("Connection successful");
					} catch (SQLException e){
						System.out.println(e.getMessage());
					} finally {
						try{
							if (connection != null){
								resultSet.close();
								connection.close();
							}
						} catch (SQLException ex) {
							System.out.println(ex.getMessage());
						}
					} 
					responseEntity = new ResponseEntity<>("true", HttpStatus.OK);
				} else{
					System.out.println("token expired");
					responseEntity = new ResponseEntity<>("false", HttpStatus.OK);
				}
			} else{
				responseEntity = new ResponseEntity<>("false", HttpStatus.OK);
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
				String url = PLAYER_DB_CONNECTION_STRING;
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
		public ResponseEntity<String> retrieveFavorites(@RequestParam(value="userName", required=true) String userName,
				@RequestParam(value="loginKey", required=true) String loginKey){
			boolean success = false;
			PlayerList playerList = new PlayerList();
			ArrayList<Player> players = new ArrayList<Player>();
			Connection connection = null;
			ResultSet resultSet = null;
			
			try{
				String url = USER_DB_CONNECTION_STRING;
				connection = DriverManager.getConnection(url);
				String sql = "Select * from User WHERE Username = ?";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, userName);
				resultSet = preparedStatement.executeQuery();
				if(resultSet.next()){ //for each record in the result set need to iterate over all entries delimited by &
					String[] tokens = resultSet.getString("Keywords").split(Pattern.quote("&"));;
					for (String player : tokens) {
						Player aPlayer = new Player("", "", player);
						players.add(aPlayer);
					}
					success = true;
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
						connection.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}
			
			boolean validLoginKey = checkLoginKey(userName, loginKey);
			playerList.setList(players);
			Gson gson = new Gson();
			ResponseEntity responseEntity;
			if(success && validLoginKey){
				responseEntity = new ResponseEntity<>(gson.toJson(playerList), HttpStatus.OK); 
			}else{
				responseEntity = new ResponseEntity<>("false", HttpStatus.OK); 
			}
			return responseEntity;
		}
		
		@RequestMapping(value="/sendFavorites", method = RequestMethod.POST)
		public ResponseEntity<String> addUserFavorites(@RequestParam(value = "favorites", required = true) String keywords, 
				@RequestParam(value = "username", required = true) String username,
				@RequestParam(value = "loginKey", required = true) String loginKey){
			boolean success = false;
			PlayerList playerList = new PlayerList();
			ArrayList<Player> players = new ArrayList<Player>();
			Connection connection = null;
			keywords = keywords.replace("|", "&");
			keywords = keywords.replace("'", "''");
			System.out.println(keywords);
			
			boolean validLoginKey = checkLoginKey(username, loginKey);
			if(validLoginKey){
				try{
					String url = USER_DB_CONNECTION_STRING;
					connection = DriverManager.getConnection(url);
					String sql = "UPDATE User SET Keywords = ? WHERE Username = ?";
					PreparedStatement preparedStatement = connection.prepareStatement(sql);
					preparedStatement.setString(1, keywords);
					preparedStatement.setString(2, username);
					preparedStatement.executeUpdate();	
					System.out.println("Connection successful");
					success = true;
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
			}


			
			ResponseEntity responseEntity;
			if(success && validLoginKey){
				responseEntity = new ResponseEntity<>("true", HttpStatus.OK);
			}
			else{
				responseEntity = new ResponseEntity<>("false", HttpStatus.OK);
			}

			return responseEntity;
		}
		
		@RequestMapping(value="/logout", method = RequestMethod.POST)
		public ResponseEntity<String> logout(@RequestParam(value = "userName", required = true) String username, 
				@RequestParam(value = "loginKey", required = true) String loginKey){
			boolean success = false;
			Connection connection = null;
			
			try{
				String url = USER_DB_CONNECTION_STRING;
				connection = DriverManager.getConnection(url);
				String sql = "UPDATE User SET LoginKey = ? WHERE Username = ? AND LoginKey = ?";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, "");
				preparedStatement.setString(2, username);
				preparedStatement.setString(3, loginKey);
				preparedStatement.executeUpdate();	
				System.out.println("Connection successful");
				success = true;
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
			if(success){
				responseEntity = new ResponseEntity<>("true", HttpStatus.OK);
			}
			else{
				responseEntity = new ResponseEntity<>("false", HttpStatus.OK);
			}

			return responseEntity;
		}
		
		@RequestMapping(value="/confirmAccount", method = RequestMethod.GET)
		public ResponseEntity<String> confirmAccount(@RequestParam(value = "token", required = true) String confirmationToken, 
				@RequestParam(value = "userName", required = true) String username){
			boolean success = false;
			Connection connection = null;
			int count;
			long resetTime = System.nanoTime() + 157700000000000000L;  //add five years in nanoseconds
			
			try{
				String url = USER_DB_CONNECTION_STRING;
				connection = DriverManager.getConnection(url);
				String sql = "UPDATE User SET ReceiveTexts = ?, ReceiveEmails = ? WHERE Username = ? AND ConfirmToken = ?";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setFloat(1, resetTime);
				preparedStatement.setFloat(2, resetTime);
				preparedStatement.setString(3, username);
				preparedStatement.setString(4, confirmationToken);
				count = preparedStatement.executeUpdate();
				System.out.println("Connection successful");
				if(count == 1){
					success = true;
				}
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
			if(success){
				responseEntity = new ResponseEntity<>("Account has been confirmed and you can now receive notifications.", HttpStatus.OK);
			}
			else{
				responseEntity = new ResponseEntity<>("Error occured", HttpStatus.OK);
			}

			return responseEntity;
		}

		public boolean checkLoginKey(String userName, String loginKey){
			boolean success = false;
			Connection connection = null;
			ResultSet resultSet = null;
			try{
				String url = USER_DB_CONNECTION_STRING;
				connection = DriverManager.getConnection(url);
				String sql = "Select * from User WHERE Username = ? AND LoginKey = ?";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, userName);
				preparedStatement.setString(2, loginKey);
				resultSet = preparedStatement.executeQuery();
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
						connection.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}	
			
			return success;
		}
		
		public void updateLoginKey(String username, String loginKey){
			Connection connection = null;
			long loginResetTime = System.nanoTime() + 3600000000000L;  //add one hour in nanoseconds

			try{
				String url = USER_DB_CONNECTION_STRING;
				connection = DriverManager.getConnection(url);
				String sql = "UPDATE User SET LoginKey = ?, LoginKeyExpiration = ? WHERE Username = ?";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, loginKey);
				preparedStatement.setFloat(2, loginResetTime);
				preparedStatement.setString(3, username);
				preparedStatement.executeUpdate();
				System.out.println("Connection successful");
			} catch (SQLException e){
				System.out.println(e.getMessage());
			} finally {
				try{
					if (connection != null){
						connection.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}
		
			
		}
		
		public void updateResetToken(String email, String token, String timeExpiration){
			Connection connection = null;
			
			try{
				String url = USER_DB_CONNECTION_STRING;
				connection = DriverManager.getConnection(url);
				String sql = "UPDATE User SET ResetToken = ?, ResetExpiration = ? WHERE Email = ?";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, token);
				preparedStatement.setString(2, timeExpiration);
				preparedStatement.setString(3, email);
				preparedStatement.executeUpdate();
				System.out.println("Connection successful");
			} catch (SQLException e){
				System.out.println(e.getMessage());
			} finally {
				try{
					if (connection != null){
						connection.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}
			
		}
		
		public String[] getPasswordHash(String frontEndHash){
			Hash hash = new Hash();
			Salt salt = new Salt();
			String[] values = new String[2];
			String saltString = salt.generateSalt();
			String saltHash = frontEndHash + saltString;
			String backEndHash = hash.sha256(saltHash, HEX);
			values[0] = saltString; //Position 0 - Salt for Server
			values[1] = backEndHash; //Position 1 - Hash generated on backEND
			return values;
		}
		
		public String getPasswordHash(String frontEndHash, String backEndSalt){
			Hash hash = new Hash();
			String saltHash = frontEndHash + backEndSalt;
			String backEndHash = hash.sha256(saltHash, HEX);
			return backEndHash;
		}

}
