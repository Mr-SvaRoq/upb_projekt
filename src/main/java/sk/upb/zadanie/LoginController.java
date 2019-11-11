package sk.upb.zadanie;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sk.upb.zadanie.encryption.IEncryptionService;
import sk.upb.zadanie.password.HashingHandler;
import sk.upb.zadanie.password.ValidationHandler;
import sk.upb.zadanie.storage.Cookies;
import sk.upb.zadanie.storage.FileNotFoundException;
import sk.upb.zadanie.storage.StorageService;

import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Controller
public class LoginController {
    private final StorageService storageService;
    private final Cookies cookies;
    private final HashingHandler hashingHandler;
    private final ValidationHandler validationHandler;
    private int counter = 0;

    @Autowired
    public LoginController(StorageService storageService, Cookies cookies, HashingHandler hashingHandler, ValidationHandler validationHandler) {
        this.storageService = storageService;
        this.cookies = cookies;
        this.hashingHandler = hashingHandler;
        this.validationHandler = validationHandler;
    }

    @GetMapping({"/login"})
    public String login(Model model, HttpServletRequest request) {
        if (counter  == 5) {
            TimerTask task = new TimerTask() {
                public void run() {
                    System.out.println("I am coming");
                    counter = 0;
                }
            };
            Timer timer = new Timer("Timer");

            long delay = 10000L;
            timer.schedule(task, delay);
            return "chyba";
        }

        String allCookies = cookies.readAllCookies(request);
        List<String[]> data = storageService.convertCSVToData("users.csv");

        for (String[] row : data) {
            if (allCookies.contains("userName=") && allCookies.contains("userPassword=") && cookies.getCookieValue(request, "userName").equals(row[0]) && cookies.getCookieValue(request, "userPassword").equals(row[1])) {
                return "redirect:/";
            }
        }
        return "login";

//        if (allCookies.contains("userName=") && allCookies.contains("userPassword=")) {
//            return "redirect:/";
//        } else {
//            return "login";
//        }
    }

    @PostMapping({"/login"})
    public String login(@RequestParam("user") String userName, @RequestParam("password") String password, RedirectAttributes redirectAttributes, HttpServletResponse response) throws InvalidKeySpecException, NoSuchAlgorithmException {
        //treba v csv kontroloval, ci existuje user a ci sedi heslo, -> na to osobitny kontroler by trebalo a bude vraciat T/F
        List<String[]> data = storageService.convertCSVToData("users.csv");
        for (String[] row : data) {
            if (userName.equals(row[0])) {
//                if (password.equals(row[1])) {
                String hashPassword = hashingHandler.getPasswordHash(password);
                if (validationHandler.validatePassword(hashPassword, row[1])) {
//                if (hashPassword.) {
                    redirectAttributes.addFlashAttribute("login", "Prihlaseny: " + userName);
                    cookies.setCookieUserNamePassword(response, userName, hashPassword);
                    counter = 0;
                    return "redirect:/";
                } else {
                    redirectAttributes.addFlashAttribute("login", "Zle heslo !");
                    counter++;
                    return "redirect:/login";
                }
            }
        }
        redirectAttributes.addFlashAttribute("login", "Nenasiel sa user!");
        counter++;
        return "redirect:/login";
    }

    @ExceptionHandler({FileNotFoundException.class})
    public ResponseEntity<?> handleStorageFileNotFound(FileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}