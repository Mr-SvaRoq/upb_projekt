package sk.upb.zadanie.encryption;

import java.io.IOException;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface IEncryptionService {
    String generatePublicKey();

    void regenerate() throws NoSuchAlgorithmException;

    String generatePrivateKey();

    void writeToFile(String text, Path filePath) throws IOException;

    void writeToFileByte(byte[] myByteArray, Path filePath) throws IOException;

    byte[] encryptRSA(MultipartFile file, Path filePath, String key) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, InvalidKeySpecException;

    SecretKey decryptSecretKey(String key, String encodedSecretKey) throws InvalidKeySpecException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException;

//    void decryptRSA(MultipartFile file, Path filePath, String privateKey) throws IOException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeySpecException;

    Resource decryptRSA(byte[] file, String privateKey) throws InvalidKeySpecException, NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException, IOException;

    Resource reDecryptRSAWithUsersPublicKey(byte[] file, String userPublicKey, String serverPrivateKey) throws InvalidKeySpecException, NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException, IOException;

    PublicKey checkPublicKey(byte[] publicKey);
}
