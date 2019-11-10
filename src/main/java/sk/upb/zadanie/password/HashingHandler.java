package sk.upb.zadanie.password;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

@Component
public class HashingHandler {
	
	public HashingHandler() {}
	public static final String salt = "a7ddc9efc264a415dbbb180ac181854f";
	public static final int iterations = 1000;
	public String getPasswordHash(final String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
		char [] chars = password.toCharArray();
		PBEKeySpec spec = new PBEKeySpec(chars, fromHex(salt), iterations, 64 * 8);
		SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		byte[] hash = skf.generateSecret(spec).getEncoded();
		
		return toHex(hash);
		
	}
	
	private static String toHex(byte []array) {
		BigInteger bigInt = new BigInteger(1, array);
		String hex = bigInt.toString(16);
		int paddingLength = (array.length * 2) - hex.length();
		if(paddingLength > 0) {
			return String.format("%0" + paddingLength + "d", 0) + hex;
		} else {
			return hex;
		}
	}
	
	private static byte[] getSalt() throws NoSuchAlgorithmException {
		SecureRandom rnd = SecureRandom.getInstance("SHA1PRNG");
		byte [] salt = new byte [16];
		rnd.nextBytes(salt);
		return salt;
	}

	private static byte[] fromHex(final String hex) {
		byte[] bytes = new byte[hex.length() / 2];
		for(int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) Integer.parseInt(hex.substring(2*i, 2*i + 2), 16);
		}
		return bytes;
	}

}
