package sk.upb.zadanie.encryption;

import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import org.springframework.web.multipart.MultipartFile;

public interface EncryptionService {
    void encrypt(MultipartFile file, Path filePath) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException;

    String readFile(Path filePath) throws IOException;

    void writeToFile(String text, Path filePath) throws IOException;

    void decrypt(MultipartFile file, Path filePath) throws IOException;
}
