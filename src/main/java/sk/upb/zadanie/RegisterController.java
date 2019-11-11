package sk.upb.zadanie;

import org.passay.*;
import org.passay.dictionary.ArrayWordList;
import org.passay.dictionary.WordListDictionary;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class RegisterController {
    private final StorageService storageService;
    private final Cookies cookies;
    private final HashingHandler hashingHandler;
    private int counter = 0;

    @Autowired
    public RegisterController(StorageService storageService, Cookies cookies, HashingHandler hashingHandler) {
        this.storageService = storageService;
        this.cookies = cookies;
        this.hashingHandler = hashingHandler;
    }

    @GetMapping({"/register"})
    public String register(HttpServletRequest request) {
        String allCookies = cookies.readAllCookies(request);
        if ( allCookies.contains("userName=")  && allCookies.contains("userPassword=")) {
            return "redirect:/";
        } else {
            return "register";
        }
    }

    @PostMapping({"/register"})
    public String register(@RequestParam("user") String userName, @RequestParam("password") String password, @RequestParam("conFirmpassword") String conFirmpassword, @RequestParam("public_key") String public_key, RedirectAttributes redirectAttributes, HttpServletResponse response) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        //treba v csv kontroloval, ci existuje user a ci heslo je rovnake ako confirmHeslo a ci to nie Slabe heslo, -> na to osobitny kontroler by trebalo a bude vraciat T/F

        List<String[]> data = storageService.convertCSVToData("users.csv");
        for (String[] row : data) {
            if (userName.equals(row[0])) {
                redirectAttributes.addFlashAttribute("loginBad", "uzivatel existuje: " + userName);
                return "redirect:/register";
            }
        }

        //TODO OOP, treba dat vonku, to ja spravim
        File file = new File("10-million-password-list-top-10000");
        BufferedReader br = new BufferedReader(new FileReader(file));
        List<String> listOfWords = new ArrayList<String>();
        String st;
        while ((st = br.readLine()) != null) {
            listOfWords.add(st);
        }
        List<String> sorted = listOfWords.stream().sorted().collect(Collectors.toList());
        String []words = new String[listOfWords.size()];
        words = sorted.toArray(words);

        WordListDictionary wordListDictionary = new WordListDictionary(
                new ArrayWordList(words));

        List<Rule> rules = new ArrayList<>();
        rules.add(new LengthRule(10, 50));
        rules.add(new WhitespaceRule());
        rules.add(new CharacterRule(EnglishCharacterData.UpperCase, 1));
        rules.add(new CharacterRule(EnglishCharacterData.LowerCase, 1));
        rules.add(new CharacterRule(EnglishCharacterData.Digit, 1));
        rules.add(new CharacterRule(EnglishCharacterData.Special, 1));
        rules.add(new DictionaryRule(wordListDictionary));

        PasswordValidator validator = new PasswordValidator(rules);
        PasswordData passwordData = new PasswordData(password);
        RuleResult result = validator.validate(passwordData);

        if (result.isValid()) {
            System.out.println("Password validated.");
        } else {
            System.out.println("Invalid Password: " + validator.getMessages(result));
        }

        //
//        if (encryptionService.checkPublicKey(public_key.getBytes()) == null) {
//            redirectAttributes.addFlashAttribute("loginBad", "Zly public key!");
//            return "redirect:/register";
//        }

        if (password.equals(conFirmpassword) && result.isValid()) {
            String secureHashPassWord = hashingHandler.getPasswordHash(password);
            String[] newLine = {userName, secureHashPassWord, public_key} ;
            data.add(newLine);
            this.storageService.convertDataToCSV(data, "users.csv");

            redirectAttributes.addFlashAttribute("login", "Prihlaseny: " + userName);
            cookies.setCookieUserNamePassword(response, userName, secureHashPassWord);
            return "redirect:/";
        }
        else {
            redirectAttributes.addFlashAttribute("loginBad", "Nezhodne hesla!");
            return "redirect:/register";
        }
    }

    @ExceptionHandler({FileNotFoundException.class})
    public ResponseEntity<?> handleStorageFileNotFound(FileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}