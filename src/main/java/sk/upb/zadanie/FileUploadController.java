package sk.upb.zadanie;

import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sk.upb.zadanie.encryption.IEncryptionService;
import sk.upb.zadanie.encryption.RSAHandler;
import sk.upb.zadanie.password.HashingHandler;
import sk.upb.zadanie.password.ValidationHandler;
import sk.upb.zadanie.storage.Cookies;
import sk.upb.zadanie.storage.FileNotFoundException;
import sk.upb.zadanie.storage.StorageService;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.passay.CharacterRule;
import org.passay.DictionaryRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.Rule;
import org.passay.RuleResult;
import org.passay.WhitespaceRule;
import org.passay.dictionary.ArrayWordList;
import org.passay.dictionary.WordListDictionary;

@Controller
public class FileUploadController {
    private final StorageService storageService;
    private final IEncryptionService encryptionService;
    private final Cookies cookies;
    private final HashingHandler hashingHandler;
    private final ValidationHandler validationHandler;
    private int counter = 0;

    @Autowired
    public FileUploadController(StorageService storageService, IEncryptionService encryptionService, Cookies cookies, HashingHandler hashingHandler, ValidationHandler validationHandler) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.storageService = storageService;
        this.encryptionService = encryptionService;
        this.cookies = cookies;
        this.hashingHandler = hashingHandler;
        this.validationHandler = validationHandler;
    }

    //NOT OOP FFS,
    @GetMapping({"/"})
    public String listUploadedFiles(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes, HttpServletResponse response) throws InvalidKeySpecException, NoSuchAlgorithmException {
        //Skuska
        List<String[]> data = storageService.convertCSVToData("users.csv");
        model.addAttribute("files", this.storageService.loadAll().map((path) -> {
            return MvcUriComponentsBuilder.fromMethodName(FileUploadController.class, "serveFile", new Object[]{path.getFileName().toString()}).build().toString();
        }).collect(Collectors.toList()));
        String allCookies = cookies.readAllCookies(request);
        if ( !allCookies.contains("userName=")  || !allCookies.contains("userPassword=")) {
            return "redirect:/login";
        }

        for (String[] row : data) {
            if (cookies.getCookieValue(request, "userName").equals(row[0])) {
                if (validationHandler.validatePassword(cookies.getCookieValue(request, "userPassword"), row[1])) { //ak nesedi databaza a je uz zapisane cookies, cele je to na blb
                    model.addAttribute("login", "Prihlaseny: " + cookies.getCookieValue(request, "userName"));

                    List<List<String>> users = new ArrayList<>();

                    for (String[] user_data : data) {
                        List<String> user = new ArrayList<>();
                        user.add(user_data[0]);
                        user.add(user_data[2]);
                        users.add(user);
                        model.addAttribute("users", users);
                    }
                    counter = 0;
                    return "uploadForm";
                } else {
                    model.addAttribute("login", "Nastala chyba");
                    return "chyba";
                }
            }
        }
        return "redirect:/login";
    }

    /****************FUJ OOP TREBA*********************/
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
    /*************************************/

    /****************FUJ OOP TREBA*********************/
    @GetMapping({"/register"})
    public String register(Model model, HttpServletRequest request) throws IOException {
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

        //TODO tu pridat Danielkine metody, ok OOP zas tu nie je
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
    /*************************************/

    @GetMapping({"/files/{filename:.+}"})
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) throws java.io.FileNotFoundException {
        Resource file = this.storageService.loadAsResource(filename, true);
        return ((BodyBuilder)ResponseEntity.ok().header("Content-Disposition", new String[]{"attachment; filename=\"" + file.getFilename() + "\""})).body(file);
    }

    @PostMapping({"/download"})
    @ResponseBody
    public ResponseEntity<Resource> serveFile() throws java.io.FileNotFoundException {
        Resource file = this.storageService.loadAsResource("upb_decypher.jar", false);
        return ((BodyBuilder) ResponseEntity.ok().header("Content-Disposition", new String[]{"attachment; filename=\"" + file.getFilename() + "\""})).body(file);
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

    @PostMapping({"/"})
    public String handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("key") String key, @RequestParam("action") String action, RedirectAttributes redirectAttributes) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IOException, BadPaddingException, IllegalBlockSizeException, InvalidKeySpecException {
        String filename = "";
        switch (action) {
            case "encrypt-rsa":
                if (!storageService.checkIfFileExist(storageService.load(file.getOriginalFilename()).toString())) {
                    filename = file.getOriginalFilename();
                } else {
                    int i = 1;
                    while(storageService.checkIfFileExist(storageService.load("(" + i + ")-" + file.getOriginalFilename()).toString())) {
                        i++;
                    }
                    filename = "(" + i + ")-" + file.getOriginalFilename();
                }
                storageService.store(file, filename);
                this.encryptionService.encryptRSA(file, this.storageService.load(filename), key);
                break;
            case "decrypt-rsa":
                if (!storageService.checkIfFileExist(storageService.load("Decrypted-" + file.getOriginalFilename()).toString())) {
                    filename = "Decrypted-" + file.getOriginalFilename();
                } else {
                    int i = 1;
                    while(storageService.checkIfFileExist(storageService.load("Decrypted-(" + i + ")-" + file.getOriginalFilename()).toString())) {
                        i++;
                    }
                    filename = "Decrypted-(" + i + ")-" + file.getOriginalFilename();
                }
                storageService.store(file, filename);
                this.encryptionService.decryptRSA(file, this.storageService.load(filename), key);
                break;
            default:
                System.out.println("Nieco sa pokazilo...");
        }
        redirectAttributes.addFlashAttribute("message", "You successfully uploaded " + file.getOriginalFilename() + "!");
        return "redirect:/";
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