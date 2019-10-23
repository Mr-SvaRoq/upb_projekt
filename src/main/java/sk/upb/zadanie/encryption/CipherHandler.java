package sk.upb.zadanie.encryption;

import java.nio.ByteBuffer;
import java.security.*;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class CipherHandler {
    SecureRandom secureRandom = new SecureRandom();

    CipherHandler() {
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

    byte[] doEncrypt(final byte[] iv, final SecretKey secretKey, final SecretKey macKey, final byte[] plainText) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcm = new GCMParameterSpec(128, iv);
        final Mac hmac = Mac.getInstance("HmacSHA256");
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
        hmac.init(macKey);
        hmac.update(iv);
        hmac.update(cipherText);

        byte [] mac = hmac.doFinal();

        return this.concatCipherToSingleMessage(iv, cipherText, mac);
    }

    byte[] decrypt(final byte[] cipherText, final byte[] initialVector, final SecretKey key, final SecretKey macKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        // java.lang.NullPointerException: null
        ByteBuffer buf = ByteBuffer.wrap(cipherText);

        int ivLength = buf.getInt();

        byte [] iv = new byte[ivLength];
        buf.get(iv);

        int macLength = (buf.get());
        byte [] mac = new byte[macLength];
        buf.get(mac);

        byte [] cipherT = new byte[buf.remaining()];
        buf.get(cipherT);

        final Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(macKey);
        hmac.update(iv);
        hmac.update(cipherT);
        byte [] refMac = hmac.doFinal();

        if (!MessageDigest.isEqual(refMac, mac)) {
            throw new SecurityException("could not authenticate");
        }

        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));

        return cipher.doFinal(cipherT);

    }

    private byte[] concatCipherToSingleMessage(final byte[] iv, final byte[] cipherText, final byte [] mac) {
        ByteBuffer buffer = ByteBuffer.allocate(4 + iv.length + 1 + mac.length + cipherText.length);
        buffer.putInt(iv.length);
        buffer.put(iv);
        buffer.put((byte) mac.length);
        buffer.put(mac);
        buffer.put(cipherText);
        return buffer.array();
    }
}
