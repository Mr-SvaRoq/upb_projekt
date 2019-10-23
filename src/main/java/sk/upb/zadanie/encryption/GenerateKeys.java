package sk.upb.zadanie.encryption;

import java.security.*;

public class GenerateKeys {
	private KeyPairGenerator keyGen;
	private KeyPair pair;
	private PrivateKey privateKey;
	private PublicKey publicKey;
	
	public GenerateKeys(int length) throws NoSuchAlgorithmException {
		this.keyGen = KeyPairGenerator.getInstance("RSA");
		this.keyGen.initialize(length);
	}
	
	public void createKeys() {
		this.pair = this.keyGen.generateKeyPair();
		this.privateKey = pair.getPrivate();
		this.publicKey = pair.getPublic();
	}
	
	public PrivateKey getPrivateKey() {
		return this.privateKey;
	}
	
	public PublicKey getPublicKey() {
		return this.publicKey;
	}
	
	
}
