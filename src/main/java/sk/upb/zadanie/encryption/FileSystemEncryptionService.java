package sk.upb.zadanie.encryption;

import java.io.*;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileSystemEncryptionService implements IEncryptionService {
    private final CipherHandler cipherHandler = new CipherHandler();
    private SecretKey secretKey;
//    private final SecretKey mac;
    private byte[] iv;
    private byte[] cipher;

    private final RSAHandler rsaHandler = new RSAHandler();

    private GenerateKeys generate;

    public FileSystemEncryptionService() throws NoSuchPaddingException, NoSuchAlgorithmException, IOException {
        this.secretKey = this.cipherHandler.generateSecretKey();
//        this.mac = this.cipherHandler.generateMacKey();
        this.generate = new GenerateKeys(512);
        this.generate.createKeys();
    }

    @Override
    public void regenerate() throws NoSuchAlgorithmException {
        this.secretKey = this.cipherHandler.generateSecretKey();
//        this.mac = this.cipherHandler.generateMacKey();
//        this.generate = new GenerateKeys(512);
        this.generate.createKeys();
    }

    @Override
    public String generatePublicKey() {
        PublicKey publicKey = this.generate.getPublicKey();
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    @Override
    public String generatePrivateKey() {
        PrivateKey privateKey = generate.getPrivateKey();
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    public void writeToFile(String text, Path filePath) throws IOException {
        File file = new File(filePath.toString());
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(text);
        writer.close();
    }

    public void writeToFileByte(byte[] myByteArray, Path filePath) throws IOException {
        FileUtils.writeByteArrayToFile(new File(filePath.toString()), myByteArray);
    }

    @Override
    public byte[] encryptRSA(MultipartFile file, Path filePath, String key) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, InvalidKeySpecException {
        byte[] plainText = file.getBytes();
        this.iv = this.cipherHandler.generateInitialVector();
        PublicKey publicKey = rsaHandler.getPublic(Base64.getDecoder().decode(key));

        this.cipher = cipherHandler.doEncrypt(iv, secretKey, publicKey, plainText); //TODO iv sa nebude pouzivat, secret key bude danielka teraz robit, mac mozno pojde prec, lebo GCM pojde ma integritu
        this.writeToFileByte(this.cipher, filePath);
        return rsaHandler.encryptText(secretKey.getEncoded(), publicKey); //TODO THIS sifrovanie kluca
    }

    @Override
    public SecretKey decryptSecretKey(final String key, final String encryptedKey) throws InvalidKeySpecException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException {
        PrivateKey privateKey = rsaHandler.getPrivate(Base64.getDecoder().decode(key));
        byte[] decrypted = rsaHandler.decryptText(encryptedKey.getBytes(), privateKey);
        return new SecretKeySpec(decrypted, 0, decrypted.length, "AES"); //TODO THIS - desifrovanie
    }

    @Override
    public void decryptRSA(MultipartFile file, Path filePath, String privateKey) throws IOException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeySpecException {
        byte[] plainText = file.getBytes();
        PrivateKey newPrivateKey = rsaHandler.getPrivate(Base64.getDecoder().decode(privateKey));
        byte[] plain = cipherHandler.decrypt(plainText, newPrivateKey); //TODO THIS
        String decipheredText = new String(plain);
        this.writeToFile(decipheredText, filePath);
    }
}