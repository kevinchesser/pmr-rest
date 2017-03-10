package send;

public class Salt {

	public String generateSalt(){
		String salt = "";
		String characters = "abcdefghijklmnopqrstuvwxyz1234567890";
		for(int i = 0; i < 32; i++){
			salt += characters.charAt((int) Math.floor(Math.random() * characters.length()));
		}
		return salt;
	}
	
}
