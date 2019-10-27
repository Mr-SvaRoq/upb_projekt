package sk.upb.zadanie.encryption;

import java.nio.ByteBuffer;
import java.security.*;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class CipherHandler {
    SecureRandom secureRandom = new SecureRandom();
    RSAHandler rsaHandler = new RSAHandler();

    CipherHandler() throws NoSuchPaddingException, NoSuchAlgorithmException {
    }

    SecretKey generateSecretKey() {
        byte[] key = new byte[16];
        this.secureRandom.nextBytes(key);
        return new SecretKeySpec(key, "AES");
    }

    byte[] generateInitialVector() {
        byte[] iv = new byte[12];
        this.secureRandom.nextBytes(iv);
        return iv;
    }

    SecretKey generateMacKey() {
        byte [] key = new byte [32];
        secureRandom.nextBytes(key);
        return new SecretKeySpec(key, "HmacSHA256");
    }

    byte[] doEncrypt(final byte[] iv, final SecretKey secretKey, final PublicKey publicKey, final byte[] plainText) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcm = new GCMParameterSpec(128, iv);
//        final Mac hmac = Mac.getInstance("HmacSHA256");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcm);
        byte[] cipherText = null;

        try {
            cipherText = cipher.doFinal(plainText);
        } catch (IllegalBlockSizeException var8) {
            var8.printStackTrace();
        } catch (BadPaddingException var9) {
            var9.printStackTrace();
        }

        //mac authentication
//        hmac.init(macKey);
//        hmac.update(iv);
//        hmac.update(cipherText);

//        byte [] mac = hmac.doFinal();

        byte [] encryptedKey = rsaHandler.encryptText(secretKey.getEncoded(), publicKey);

        return this.concatCipherToSingleMessage(iv, cipherText, encryptedKey);
    }

    byte[] decrypt(final byte[] cipherText, final PrivateKey privateKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        // java.lang.NullPointerException: null
        ByteBuffer buf = ByteBuffer.wrap(cipherText);

        int ivLength = buf.getInt();

        byte [] iv = new byte[ivLength];
        buf.get(iv);

//        int macLength = (buf.get()); //TODO tu je chybam dava negativny macLength, e.g. macLength = -17
//        byte [] mac = new byte[macLength];
//        buf.get(mac);


        int encryptedKeyLength = (buf.get()); //TODO tu je chybam dava negativny macLength, e.g. macLength = -17
        byte [] encryptedKey = new byte[encryptedKeyLength];
        buf.get(encryptedKey);

        byte [] cipherT = new byte[buf.remaining()];
        buf.get(cipherT);

//        final Mac hmac = Mac.getInstance("HmacSHA256");
//        hmac.init(macKey);
//        hmac.update(iv);
//        hmac.update(cipherT);
//        byte [] refMac = hmac.doFinal();

//        if (!MessageDigest.isEqual(refMac, mac)) {
//            throw new SecurityException("could not authenticate");
//        }

        byte[] decrypted = rsaHandler.decryptText(encryptedKey, privateKey);
        SecretKey originalKey = new SecretKeySpec(decrypted, 0, decrypted.length, "AES");

        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, originalKey, new GCMParameterSpec(128, iv));

        return cipher.doFinal(cipherT);

    }

    private byte[] concatCipherToSingleMessage(final byte[] iv, final byte[] cipherText, final byte[] encrypredKey) {
        ByteBuffer buffer = ByteBuffer.allocate(4 + iv.length + 1 + encrypredKey.length + cipherText.length);
        buffer.putInt(iv.length);
        buffer.put(iv);
        buffer.put((byte) encrypredKey.length);
        buffer.put(encrypredKey);
        buffer.put(cipherText);
        return buffer.array();
    }
}
