package send;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
				String url = "jdbc:sqlite:/var/db/pmr.db";
				connection = DriverManager.getConnection(url);
				String sql = "INSERT INTO User(Username, Email, PasswordHash, PasswordSalt, PhoneNumber, Keywords) VALUES(?,?,?,?,?,?)";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setString(1, userName);
				preparedStatement.setString(2, email);
				preparedStatement.setString(3, passHash);
				preparedStatement.setString(4, passSalt);
				preparedStatement.setString(5, phoneNumber);
				preparedStatement.setString(6, "");
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
		
		
		@RequestMapping(value="login")
		public ResponseEntity<String> login(@RequestParam(value="userName", required=true) String userName,
				@RequestParam(value="passHash", required=true) String passHash,
				@RequestParam(value="passSalt", required=true) String passSalt){
			boolean success = false;
		
			Connection connection = null;
			ResultSet resultSet = null;
			Statement statement = null;
			try{
				String url = "jdbc:sqlite:/var/db/pmr.db";
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
				responseEntity = new ResponseEntity<>("true", HttpStatus.OK);
			else
				responseEntity = new ResponseEntity<>("false", HttpStatus.OK);

			return responseEntity;
		}
		
		@RequestMapping(value="recover")
		public ResponseEntity<String> recover(@RequestParam(value="email", required=true) String email){
			boolean success = false;
			Connection connection = null;
			ResultSet resultSet = null;
			Statement statement = null;
			try{
				String url = "jdbc:sqlite:/var/db/pmr.db";
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
				responseEntity = new ResponseEntity<>("true", HttpStatus.OK);
				System.out.println("Sending recovery email to " + email);
			}
			else{
				responseEntity = new ResponseEntity<>("false", HttpStatus.OK);
				System.out.println("Email not in db - not doing anything");
			}	
			
			return responseEntity;
		}
}
