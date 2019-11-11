package sk.upb.zadanie;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sk.upb.zadanie.encryption.IEncryptionService;
import sk.upb.zadanie.password.HashingHandler;
import sk.upb.zadanie.password.ValidationHandler;
import sk.upb.zadanie.storage.Cookies;
import sk.upb.zadanie.storage.FileNotFoundException;
import sk.upb.zadanie.storage.StorageService;

import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServletResponse;
import java.security.NoSuchAlgorithmException;

@Controller
public class LogoutController {
    private final StorageService storageService;
    private final IEncryptionService encryptionService;
    private final Cookies cookies;
    private final HashingHandler hashingHandler;
    private final ValidationHandler validationHandler;
    private int counter = 0;

    @Autowired
    public LogoutController(StorageService storageService, IEncryptionService encryptionService, Cookies cookies, HashingHandler hashingHandler, ValidationHandler validationHandler) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.storageService = storageService;
        this.encryptionService = encryptionService;
        this.cookies = cookies;
        this.hashingHandler = hashingHandler;
        this.validationHandler = validationHandler;
    }

    @PostMapping({"/logOut"})
    public String logOut(RedirectAttributes redirectAttributes, HttpServletResponse response) {
        redirectAttributes.addFlashAttribute("logout", "Odhlaseny ");
        cookies.deleteCookie(response);
        return "redirect:/login";
    }

    @ExceptionHandler({FileNotFoundException.class})
    public ResponseEntity<?> handleStorageFileNotFound(FileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}