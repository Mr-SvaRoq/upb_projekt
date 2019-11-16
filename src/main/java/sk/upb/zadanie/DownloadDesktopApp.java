package sk.upb.zadanie;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import sk.upb.zadanie.encryption.IEncryptionService;
import sk.upb.zadanie.password.HashingHandler;
import sk.upb.zadanie.password.ValidationHandler;
import sk.upb.zadanie.storage.Cookies;
import sk.upb.zadanie.storage.FileNotFoundException;
import sk.upb.zadanie.storage.StorageService;

import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;

@Controller
public class DownloadDesktopApp {
    private final StorageService storageService;
    private final IEncryptionService encryptionService;
    private final Cookies cookies;
    private final HashingHandler hashingHandler;
    private final ValidationHandler validationHandler;
    private int counter = 0;

    @Autowired
    public DownloadDesktopApp(StorageService storageService, IEncryptionService encryptionService, Cookies cookies, HashingHandler hashingHandler, ValidationHandler validationHandler) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.storageService = storageService;
        this.encryptionService = encryptionService;
        this.cookies = cookies;
        this.hashingHandler = hashingHandler;
        this.validationHandler = validationHandler;
    }

    @PostMapping({"/download"})
    @ResponseBody
    public ResponseEntity<Resource> serveFile() throws java.io.FileNotFoundException {
        Resource file = this.storageService.loadAsResource("upb_decypher.jar", false);
        return ((BodyBuilder) ResponseEntity.ok().header("Content-Disposition", new String[]{"attachment; filename=\"" + file.getFilename() + "\""})).body(file);
    }

    @ExceptionHandler({FileNotFoundException.class})
    public ResponseEntity<?> handleStorageFileNotFound(FileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}