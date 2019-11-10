package sk.upb.zadanie.password;

import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

@Component
public class ValidationHandler {

	public ValidationHandler() {}
	
	public boolean validatePassword(final String original, final String stored) throws InvalidKeySpecException, NoSuchAlgorithmException {
//		String [] parts = stored.split(":");
//		int iterations = Integer.parseInt(parts[0]);
//		byte [] salt = fromHex(parts[1]);
		byte [] hash_stored = fromHex(stored);
		byte [] hash_original = fromHex(original);

//		PBEKeySpec spec = new PBEKeySpec(original.toCharArray(), salt, iterations, hash.length * 8);
//		SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
//
//		byte [] testHash = skf.generateSecret(spec).getEncoded();
		
		int diff = hash_stored.length ^ hash_original.length;
		for(int i = 0; i < hash_stored.length && i < hash_original.length; i++) {
			diff |= hash_stored[i] ^ hash_original[i];
		}
		
		return diff == 0;
		
	}
	
	private static byte[] fromHex(final String hex) {
		byte[] bytes = new byte[hex.length() / 2];
		for(int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) Integer.parseInt(hex.substring(2*i, 2*i + 2), 16);
		}
		return bytes;
	}
}
