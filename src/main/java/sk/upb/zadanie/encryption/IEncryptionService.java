package sk.upb.zadanie.encryption;

import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.springframework.web.multipart.MultipartFile;

public interface IEncryptionService {
    void encrypt(MultipartFile file, Path filePath) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException;

    String readFile(Path filePath) throws IOException;

    void writeToFile(String text, Path filePath) throws IOException;

    void decrypt(MultipartFile file, Path filePath) throws IOException, NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException;

    void encryptRSA(MultipartFile file, Path filePath) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException;

    void decryptRSA(MultipartFile file, Path filePath) throws IOException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException;
}
