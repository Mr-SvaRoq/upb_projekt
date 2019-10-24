package sk.upb.zadanie.encryption;

import java.io.*;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileSystemEncryptionService implements IEncryptionService {
    private final CipherHandler cipherHandler = new CipherHandler();
    private final SecretKey secretKey;
    private final SecretKey mac;
    private byte[] iv;
    private byte[] cipher;

    private final RSAHandler rsaHandler = new RSAHandler();
//    private PublicKey publicKey;
//    private PrivateKey privateKey;

    private String encrypted;

//    private String publicKeySkuska;

    private GenerateKeys generate;

    //TODO zmenit, vyriesit ako to bude presne


    public FileSystemEncryptionService() throws NoSuchPaddingException, NoSuchAlgorithmException, IOException {
        this.secretKey = this.cipherHandler.generateSecretKey();
        this.mac = this.cipherHandler.generateMacKey();
        this.generate = new GenerateKeys(512);
        this.generate.createKeys();
        //this.publicKey = generate.getPublicKey();
//        this.privateKey = generate.getPrivateKey();
//        StringWriter stringWriter = new StringWriter();
//        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
//        jcaPEMWriter.writeObject(this.privateKey);
//        jcaPEMWriter.close();
//        System.out.println(stringWriter.getBuffer().toString());

//        this.publicKeySkuska = Base64.getEncoder().encodeToString(this.publicKey.getEncoded());
//        this.publicKeySkuska = this.publicKey.getEncoded();

//        System.out.println(this.publicKeySkuska);
//
//        System.out.println("----------------BEGIN public KEYS----------------");
//        System.out.println(new String(publicKey.getEncoded()));
//        System.out.println("----------------END KEYS----------------");
//        System.out.println("----------------BEGIN private KEYS----------------");
//        System.out.println(new String(privateKey.getEncoded()));
//        System.out.println("----------------END KEYS----------------");
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

    public void encrypt(MultipartFile file, Path filePath) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        //TODO tu bude problem s velkym suborom podla mojho nazoru
        System.out.println("----------------Begin encrypt----------------");
        byte[] plainText = file.getBytes();
        this.iv = this.cipherHandler.generateInitialVector();
        this.cipher = this.cipherHandler.doEncrypt(iv, secretKey, mac, plainText); //TODO FFS toto je zle, ale ze brutal, NOP !, FML, moja chzba, tato premenna by mala byt len lokalna, nie atribut
        String cipherAssString = new String(cipher);
//        System.out.println("--------------Cipher -------");
//        System.out.println(this.cipher);
//        System.out.println("Cipher of plain text: " + cipherAssString);
//        this.writeToFile(cipherAssString, filePath);
        this.writeToFileByte(this.cipher, filePath);
        System.out.println("----------------End encrypt----------------");
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
//        FileUtils.writeByteArrayToFile(new File(filePath.toString()), myByteArray);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(text);
        writer.close();
    }

    public void writeToFileByte(byte[] myByteArray, Path filePath) throws IOException {
//        File file = new File(filePath.toString());
        System.out.println(filePath.toString().split("upload-dir"));

        String[] path = filePath.toString().split("upload-dir");
        String newPath = path[0] + "PublicKeys-" + path[1];

        FileUtils.writeByteArrayToFile(new File(filePath.toString()), myByteArray);
//        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
//        writer.write(text);
//        writer.close();
    }

    public void decrypt(MultipartFile file, Path filePath) throws IOException, NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        byte[] plainText = file.getBytes(); // TODO platinText not same as this.cipher
//        System.out.println("----------------Begin decrypt----------------");
//        if (plainText == this.cipher) {
//            System.out.println("TRUE");
//            System.out.println(plainText);
//            System.out.println("--------------Hore plaint ------- Dole cipher -------");
//            System.out.println(this.cipher);
//        }  else {
//            System.out.println("FALSE");
//            System.out.println(plainText);
//            System.out.println("--------------Hore plaint ------- Dole cipher -------");
//            System.out.println(this.cipher);
//        }

        byte[] plain = cipherHandler.decrypt(plainText, iv, secretKey, mac);
        String decipheredText = new String(plain);
        this.writeToFile(decipheredText, filePath);
        System.out.println("----------------done decrypt----------------");
    }

    @Override
    public String encryptRSA(MultipartFile file, Path filePath, String key) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, InvalidKeySpecException {
        byte[] plainText = file.getBytes();
        this.iv = this.cipherHandler.generateInitialVector();
        PublicKey publicKey = rsaHandler.getPublic(Base64.getDecoder().decode(key));


        this.cipher = cipherHandler.doEncrypt(iv, secretKey, mac, plainText); //TODO THIS

        this.writeToFileByte(this.cipher, filePath);

        return rsaHandler.encryptText(secretKey.getEncoded(), publicKey); //TODO THIS sifrovanie kluca

//        this.encrypted = rsaHandler.encryptText(secretKey.getEncoded(), rsaHandler.getPublic(publicKeySkuska)); //TODO THIS sifrovanie kluca
//        this.encrypted = rsaHandler.encryptText(key.getEncoded(), rsaHandler.getPublic(Base64.getDecoder().decode(publicKeySkuska))); //TODO THIS sifrovanie kluca

//        rsaHandler.getPublic(this.publicKeySkuska.getBytes());
//        this.writeToFile(cipherAssString, filePath);

    }

    @Override
    public SecretKey decryptSecretKey(final String key, final String encryptedKey) throws InvalidKeySpecException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException {
        PrivateKey privateKey = rsaHandler.getPrivate(Base64.getDecoder().decode(key));
        byte[] decrypted = rsaHandler.decryptText(encryptedKey.getBytes(), privateKey);
        return new SecretKeySpec(decrypted, 0, decrypted.length, "AES"); //TODO THIS - desifrovanie
    }

    @Override
    public void decryptRSA(MultipartFile file, Path filePath, SecretKey originalKey) throws IOException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        byte[] plainText = file.getBytes();

        byte[] plain = cipherHandler.decrypt(plainText, iv, originalKey, mac); //TODO THIS
        String decipheredText = new String(plain);
        this.writeToFile(decipheredText, filePath);
    }
}