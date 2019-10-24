package sk.upb.zadanie.encryption;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.security.*;
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
    private final SecretKey key;
    private final SecretKey mac;
    private byte[] iv;
    private byte[] cipher;

    private final RSAHandler rsaHandler = new RSAHandler();
    private PublicKey publicKey;
    private PrivateKey privateKey;

    private String encrypted;

    //TODO zmenit, vyriesit ako to bude presne


    public FileSystemEncryptionService() throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.key = this.cipherHandler.generateSecretKey();
        this.mac = this.cipherHandler.generateMacKey();
        GenerateKeys generate = new GenerateKeys(512);
        generate.createKeys();
        this.publicKey = generate.getPublicKey();
        this.privateKey = generate.getPrivateKey();

    }

    public void encrypt(MultipartFile file, Path filePath) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        //TODO tu bude problem s velkym suborom podla mojho nazoru
        System.out.println("----------------Begin encrypt----------------");
        byte[] plainText = file.getBytes();
        this.iv = this.cipherHandler.generateInitialVector();
        this.cipher = this.cipherHandler.doEncrypt(iv, key, mac, plainText); //TODO FFS toto je zle, ale ze brutal, NOP !, FML, moja chzba, tato premenna by mala byt len lokalna, nie atribut
        String cipherAssString = new String(cipher);
        System.out.println("--------------Cipher -------");
        System.out.println(this.cipher);
        System.out.println("Cipher of plain text: " + cipherAssString);
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
        FileUtils.writeByteArrayToFile(new File(filePath.toString()), myByteArray);
//        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
//        writer.write(text);
//        writer.close();
    }

    public void decrypt(MultipartFile file, Path filePath) throws IOException, NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        byte[] plainText = file.getBytes(); // TODO platinText not same as this.cipher
        System.out.println("----------------Begin decrypt----------------");
        if (plainText == this.cipher) {
            System.out.println("TRUE");
            System.out.println(plainText);
            System.out.println("--------------Hore plaint ------- Dole cipher -------");
            System.out.println(this.cipher);
        }  else {
            System.out.println("FALSE");
            System.out.println(plainText);
            System.out.println("--------------Hore plaint ------- Dole cipher -------");
            System.out.println(this.cipher);
        }

        byte[] plain = cipherHandler.decrypt(plainText, iv, key, mac);
        String decipheredText = new String(plain);
        this.writeToFile(decipheredText, filePath);
        System.out.println("----------------done decrypt----------------");
    }

    @Override
    public void encryptRSA(MultipartFile file, Path filePath) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {
        byte[] plainText = file.getBytes();
        this.iv = this.cipherHandler.generateInitialVector();
        this.cipher = cipherHandler.doEncrypt(iv, key, mac, plainText); //TODO THIS
        this.encrypted = rsaHandler.encryptText(key.getEncoded(), publicKey); //TODO THIS sifrovanie kluca
        String cipherAssString = new String(cipher); //vypis
//        this.writeToFile(cipherAssString, filePath);
        this.writeToFileByte(this.cipher, filePath);

    }

    @Override
    public void decryptRSA(MultipartFile file, Path filePath) throws IOException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        byte[] plainText = file.getBytes();
        byte[] decrypted = rsaHandler.decryptText(this.encrypted.getBytes(), privateKey); //TODO THIS

        SecretKey originalKey = new SecretKeySpec(decrypted, 0, decrypted.length, "AES"); //TODO THIS - desifrovanie

        byte[] plain = cipherHandler.decrypt(plainText, iv, originalKey, mac); //TODO THIS
        String decipheredText = new String(plain);
        this.writeToFile(decipheredText, filePath);
    }
}