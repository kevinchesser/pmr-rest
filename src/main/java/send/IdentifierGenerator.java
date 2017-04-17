package send;

import java.security.SecureRandom;
import java.math.BigInteger;

public class IdentifierGenerator {
	 private SecureRandom random = new SecureRandom();

	 public String nextSessionId() {
	   return new BigInteger(130, random).toString(32);
	 }
}
