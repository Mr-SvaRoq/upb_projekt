package sk.upb.zadanie;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sk.upb.zadanie.encryption.IEncryptionService;
import sk.upb.zadanie.password.HashingHandler;
import sk.upb.zadanie.password.ValidationHandler;
import sk.upb.zadanie.storage.Cookies;
import sk.upb.zadanie.storage.FileNotFoundException;
import sk.upb.zadanie.storage.StorageService;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@Controller
public class GenerateKey {
    private final StorageService storageService;
    private final IEncryptionService encryptionService;

    @Autowired
    public GenerateKey(StorageService storageService, IEncryptionService encryptionService ) {
        this.storageService = storageService;
        this.encryptionService = encryptionService;
    }

    @PostMapping({"/generate_key"})
    public String generateKeys(@RequestParam("origin") String origin, RedirectAttributes redirectAttributes) throws java.io.FileNotFoundException, NoSuchAlgorithmException {

        encryptionService.regenerate();
        redirectAttributes.addFlashAttribute("public_key", encryptionService.generatePublicKey());
        redirectAttributes.addFlashAttribute("private_key", encryptionService.generatePrivateKey());

        return "redirect:/" + origin;
    }

    @PostMapping({"/generate_file"})
    @ResponseBody
    public ResponseEntity<Resource> serveFileWithKeys(@RequestParam("public_key_download") String public_key, @RequestParam("private_key_download") String private_key) throws IOException {
        File file = new File("keys.txt");

        if (file.createNewFile()) {
            System.out.println("File is created!");
        } else {
            System.out.println("File already exists.");
        }

        FileWriter writer = new FileWriter(file);
        writer.write("Public key - \n" + public_key + "\nPrivate key - \n" + private_key);
        writer.close();

        Resource resource = this.storageService.loadAsResource("keys.txt", false);

        return ((BodyBuilder) ResponseEntity.ok().header("Content-Disposition", new String[]{"attachment; filename=\"" + resource.getFilename() + "\""})).body(resource);
    }

    @ExceptionHandler({FileNotFoundException.class})
    public ResponseEntity<?> handleStorageFileNotFound(FileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}