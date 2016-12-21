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
			System.out.println("Singup executing");
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
			ResponseEntity<String> responseEntity = new ResponseEntity<>("Signup successful", HttpStatus.OK);
			return responseEntity;
	}
		
		
		@RequestMapping(value="login")
		public ResponseEntity<String> login(@RequestParam(value="userName", required=true) String userName){
			
			ResponseEntity responseEntity = new ResponseEntity<>("", HttpStatus.OK);
			return responseEntity;
		}
		
		@RequestMapping(value="recover")
		public ResponseEntity<String> recover(@RequestParam(value="email", required=true) String userName){
			
			ResponseEntity responseEntity = new ResponseEntity<>("", HttpStatus.OK);
			return responseEntity;
		}
}
