package sk.upb.zadanie.encryption;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CipherHandler {
    SecureRandom secureRandom = new SecureRandom();

    public CipherHandler() {
    }

    public SecretKey generateSecretKey() {
        byte[] key = new byte[16];
        this.secureRandom.nextBytes(key);
        return new SecretKeySpec(key, "AES");
    }

    public byte[] generateInitialVector() {
        byte[] iv = new byte[12];
        this.secureRandom.nextBytes(iv);
        return iv;
    }

    public byte[] doEncrypt(final byte[] iv, final SecretKey secretKey, final byte[] plainText) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcm = new GCMParameterSpec(128, iv);
        cipher.init(1, secretKey, gcm);
        byte[] cipherText = null;

        try {
            cipherText = cipher.doFinal(plainText);
        } catch (IllegalBlockSizeException var8) {
            var8.printStackTrace();
        } catch (BadPaddingException var9) {
            var9.printStackTrace();
        }

        return this.concatCipherToSingleMessage(iv, cipherText);
    }

    public String doDecrypt(String strToDecrypt, final SecretKey secretKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(2, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception var4) {
            System.out.println("Error while decrypting: " + var4.toString());
            return null;
        }
    }

    private byte[] concatCipherToSingleMessage(final byte[] iv, final byte[] cipherText) {
        ByteBuffer buffer = ByteBuffer.allocate(4 + iv.length + cipherText.length);
        buffer.putInt(iv.length);
        buffer.put(iv);
        buffer.put(cipherText);
        return buffer.array();
    }
}
