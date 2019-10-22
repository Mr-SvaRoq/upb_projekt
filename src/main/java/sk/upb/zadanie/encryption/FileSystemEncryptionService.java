package sk.upb.zadanie.encryption;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Array;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileSystemEncryptionService implements EncryptionService {
    private final CipherHandler cipherHandler = new CipherHandler();
    private final SecretKey key;
    private final SecretKey mac;
    private byte[] iv;
    private byte[] cipher;


    //TODO zmenit, vyriesit ako to bude presne


    public FileSystemEncryptionService() {
        this.key = this.cipherHandler.generateSecretKey();
        this.mac = this.cipherHandler.generateMacKey();
    }

    public void encrypt(MultipartFile file, Path filePath) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        //TODO tu bude problem s velkym suborom podla mojho nazoru
        byte[] plainText = file.getBytes();
        this.iv = this.cipherHandler.generateInitialVector();
        this.cipher = this.cipherHandler.doEncrypt(iv, key, mac, plainText);
        String cipherAssString = new String(cipher);
        System.out.println("Cipher of plain text: " + cipherAssString);
        this.writeToFile(cipherAssString, filePath);
    }

    public String readFile(Path filePath) throws IOException {
        File file = new File(filePath.toString());
        String plainText = "";

        try {
//            String str;
//            BufferedReader buffer;
//            for(buffer = new BufferedReader(new FileReader(file)); (str = buffer.readLine()) != null; plainText = plainText + str) {
//            }
            //REFAKTOR KVOLI DEBUGGERU, ak chces, tak si to daj naspat
            BufferedReader buffer = new BufferedReader(new FileReader(file));
            String str;
            while ((str = buffer.readLine()) != null) {
                plainText += str;
            }
            buffer.close();
            return plainText;
        } catch (FileNotFoundException var6) {
            var6.printStackTrace();
            return plainText;
        }
    }

    public void writeToFile(String text, Path filePath) throws IOException {
        File file = new File(filePath.toString());
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(text);
        writer.close();
    }

    public void decrypt(MultipartFile file, Path filePath) throws IOException, NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        byte[] plainText = file.getBytes();
        byte[] plain = cipherHandler.decrypt(cipher, iv, key, mac);
        String decipheredText = new String(plain);
        writeToFile(decipheredText, filePath);
        System.out.println("----------------done decrypt----------------");
    }
}