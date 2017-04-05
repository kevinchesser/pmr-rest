package send;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
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

import com.sendgrid.*;
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

@RestController
public class UserController{

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

			long resetTime = System.nanoTime() + 157700000000000000L;  //add one year in nanoseconds
			long loginResetTime = System.nanoTime() + 3600000000000L;  //add one hour in nanoseconds
			try{
				//String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "INSERT INTO User(Username, Email, PasswordHash, PasswordSalt, PhoneNumber, Keywords, "
						+ "ResetToken, ResetExpiration, ReceiveTexts, ReceiveEmails, LoginKey, LoginKeyExpiration, ServerPasswordSalt)"
						+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
				//String sql = "INSERT INTO User(Username, Email, PasswordHash, PasswordSalt, PhoneNumber, Keywords, "
				//		+ "ResetToken, ResetExpiration, ReceiveTexts, ReceiveEmails, LoginKey, LoginKeyExpiration)"
				//		+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, userName);
				preparedStatement.setString(2, email);
				preparedStatement.setString(3, passHash);
				preparedStatement.setString(4, passSalt);
				preparedStatement.setString(5, phoneNumber);
				preparedStatement.setString(6, "");
				preparedStatement.setString(7, "");
				preparedStatement.setString(8, "");
				preparedStatement.setFloat(9, resetTime);
				preparedStatement.setFloat(10, resetTime);
				preparedStatement.setString(11, loginKey);
				preparedStatement.setFloat(12, loginResetTime);
				preparedStatement.setString(13,  saltString);
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
//				String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "Select * from User WHERE Username='" + userName + "';";
				System.out.println(sql);
				statement = connection.createStatement();
				resultSet = statement.executeQuery(sql);
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
		//TODO:Refactor with backend salting and hashing
		@RequestMapping(value="/login")
		public ResponseEntity<String> login(@RequestParam(value="userName", required=true) String userName,
				@RequestParam(value="passHash", required=true) String passHash,
				@RequestParam(value="loginKey", required=true) String loginKey){
			boolean success = false;
		
			Connection connection = null;
			ResultSet resultSet = null;
			Statement statement = null;
			try{
//				String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "Select * from User WHERE Username='" + userName + "' AND PasswordHash='" + passHash + "';";
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
			updateLoginKey(userName, loginKey);

			ResponseEntity responseEntity;
			if(success)
				responseEntity = new ResponseEntity<>("true", HttpStatus.OK);
			else
				responseEntity = new ResponseEntity<>("false", HttpStatus.OK);

			return responseEntity;
		}
		
		@RequestMapping(value="/sendSettings")
		public ResponseEntity<String> sendSettings(@RequestParam(value="userName", required=true) String userName,
				@RequestParam(value="loginKey", required=true) String loginKey){
			boolean success = false;
		
			Connection connection = null;
			ResultSet resultSet = null;
			Statement statement = null;
			float receiveEmails = 0;
			float receiveTexts = 0;
			NotificationSettings notificationSettings = new NotificationSettings();
			try{
//				String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "Select ReceiveEmails, ReceiveTexts from User WHERE Username='" + userName + "' AND LoginKey='" + loginKey + "';";
				System.out.println(sql);
				statement = connection.createStatement();
				resultSet = statement.executeQuery(sql);
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
						statement.close();
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
			ResultSet resultSet = null;
			Statement statement = null;

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
//				String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "UPDATE User SET ReceiveTexts = " + receiveTexts + ", ReceiveEmails = " + receiveEmails + " WHERE Username = '" + userName + "';";
				statement = connection.createStatement();
				statement.executeUpdate(sql);
				System.out.println("Connection successful");
				success = true;
			} catch (SQLException e){
				System.out.println(e.getMessage());
				success = false;
			} finally {
				try{
					if (connection != null){
						statement.close();
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
			Statement statement = null;
			
			try{
				//String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "Select * from User WHERE Email='" + email + "';";
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
				updateResetToken(email, resetToken, timeString);

				/*Hash hash = new Hash();
				Hex hex = new Hex();
				String hashedToken = hash.sha256(resetToken, hex);
				*/
				
				Email from = new Email("");
				String subject = "PMR Password reset request";
				//Email to = new Email(email);
				Email to = new Email(email);
				Content content = new Content("text/plain", "Hello, please click this link to take you to a password reset page"
						+ "\npmr.com/resetpassword?token=" + resetToken + "&email=" + email);
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
		//TODO:Refactor with backend salting and hashing
		@RequestMapping(value="/resetpassword")
		public ResponseEntity<String> resetPassword(@RequestParam(value = "token", required=true)String token,
				@RequestParam(value = "email", required=true) String email,
				@RequestParam(value = "passwordHash", required=true) String passwordHash,
				@RequestParam(value = "passwordSalt", required=true) String passwordSalt){
			
			long currentTime = System.nanoTime(); //get current time in nanoseconds
			//String timeString = Long.toString(time);
			String resetString = "";
			boolean initialSuccess = false;
			boolean secondarySuccess = false;
			Connection connection = null;
			ResultSet resultSet = null;
			Statement statement = null;
			
			try{
				//String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "Select ResetExpiration from User WHERE Email='" + email + "';";
				System.out.println(sql);
				statement = connection.createStatement();
				resultSet = statement.executeQuery(sql);
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
						statement.close();
						connection.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}
			ResponseEntity responseEntity;

			if(initialSuccess){
				long resetTime = Long.parseLong(resetString);
				if(currentTime - resetTime < 0){
					System.out.println("token still valid");
					try{
						//String url = "jdbc:sqlite:/var/db/pmr.db";
						String url = "jdbc:sqlite:../server/db/pmr.db";
						connection = DriverManager.getConnection(url);
						String sql = "UPDATE User SET PasswordHash = '" + passwordHash + "', PasswordSalt = '" + passwordSalt + "', ResetExpiration = '0' WHERE Email = '" + email + "';";
						System.out.println(sql);
						statement = connection.createStatement();
						statement.executeQuery(sql);
						/*PreparedStatement preparedStatement = connection.prepareStatement(sql);
						preparedStatement.setString(1, token);
						preparedStatement.setString(2, timeExpiration);
						preparedStatement.setString(3, email);
						preparedStatement.executeUpdate(); */
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
		public ResponseEntity<String> retrievePlayers(@RequestParam (value="userName", required = true) String userName, 
				@RequestParam(value="loginKey", required=true) String loginKey){
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

			boolean validLoginKey = checkLoginKey(userName, loginKey);

			ResponseEntity responseEntity;
			if(success && validLoginKey){
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
			
			////////////////////////////////////////////////
			
			boolean success = false;
			PlayerList playerList = new PlayerList();
			ArrayList<Player> players = new ArrayList<Player>();
			Connection connection = null;
			ResultSet resultSet = null;
			Statement statement = null;
			
			
			try{
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "Select * from User WHERE Username='" + userName + "';";
				statement = connection.createStatement();
				resultSet = statement.executeQuery(sql);
				if(resultSet.next()){ //for each record in the result set need to iterate over all entries delimited by &
					System.out.println("Result set is " + resultSet.getString("Keywords"));
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
						statement.close();
						connection.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}
			
			////////////////////////////////////////////
			
			
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
			ResultSet resultSet = null;
			Statement statement = null;
			keywords = keywords.replace("|", "&");
			keywords = keywords.replace("'", "''");
			System.out.println(keywords);
			
			try{
//				String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "UPDATE User SET Keywords = '" + keywords + "' WHERE Username = '" + username + "';";
				statement = connection.createStatement();
				statement.executeUpdate(sql);
				System.out.println("Connection successful");
				success = true;
			} catch (SQLException e){
				System.out.println(e.getMessage());
				success = false;
			} finally {
				try{
					if (connection != null){
						statement.close();
						connection.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}


			boolean validLoginKey = checkLoginKey(username, loginKey);
			
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
			ResultSet resultSet = null;
			Statement statement = null;
			
			try{
//				String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "UPDATE User SET LoginKey = '\"\"' WHERE Username = '" + username + "' AND LoginKey = '" + loginKey +"';";
				statement = connection.createStatement();
				statement.executeUpdate(sql);
				System.out.println("Connection successful");
				success = true;
			} catch (SQLException e){
				System.out.println(e.getMessage());
				success = false;
			} finally {
				try{
					if (connection != null){
						statement.close();
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
		
		
		public boolean checkLoginKey(String userName, String loginKey){
			boolean success = false;
			Connection connection = null;
			ResultSet resultSet = null;
			Statement statement = null;
			try{
//				String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "Select * from User WHERE Username='" + userName + "' AND LoginKey='" + loginKey + "';";
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
			
			return success;
		}
		
		public void updateLoginKey(String username, String loginKey){
			Connection connection = null;
			Statement statement = null;
			ResultSet resultSet = null;
			long loginResetTime = System.nanoTime() + 3600000000000L;  //add one hour in nanoseconds

			try{
				//String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "UPDATE User SET LoginKey = '" + loginKey + "', LoginKeyExpiration = '" + loginResetTime + "' WHERE Username = '" + username + "';";
				System.out.println(sql);
				statement = connection.createStatement();
				statement.executeQuery(sql);
				/*PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, token);
				preparedStatement.setString(2, timeExpiration);
				preparedStatement.setString(3, email);
				preparedStatement.executeUpdate(); */
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
			Statement statement = null;
			ResultSet resultSet = null;
			System.out.println(email + " " + token + " " + timeExpiration);
			
			try{
				//String url = "jdbc:sqlite:/var/db/pmr.db";
				String url = "jdbc:sqlite:../server/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "UPDATE User SET ResetToken = '" + token + "', ResetExpiration = '" + timeExpiration + "' WHERE Email = '" + email + "';";
				System.out.println(sql);
				statement = connection.createStatement();
				statement.executeQuery(sql);
				/*PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, token);
				preparedStatement.setString(2, timeExpiration);
				preparedStatement.setString(3, email);
				preparedStatement.executeUpdate(); */
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
			System.out.println(values[0]);
			System.out.println(values[1]);
			return values;
		}

}
