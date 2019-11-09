package sk.upb.zadanie;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
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
import sk.upb.zadanie.storage.Cookies;
import sk.upb.zadanie.storage.FileNotFoundException;
import sk.upb.zadanie.storage.StorageService;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class FileUploadController {
    private final StorageService storageService;
    private final IEncryptionService encryptionService;
    private final Cookies cookies;

    @Autowired
    public FileUploadController(StorageService storageService, IEncryptionService encryptionService, Cookies cookies) {
        this.storageService = storageService;
        this.encryptionService = encryptionService;
        this.cookies = cookies;
    }

    //NOT OOP FFS,
    @GetMapping({"/"})
    public String listUploadedFiles(Model model, HttpServletRequest request) throws IOException {
        model.addAttribute("files", this.storageService.loadAll().map((path) -> {
            return MvcUriComponentsBuilder.fromMethodName(FileUploadController.class, "serveFile", new Object[]{path.getFileName().toString()}).build().toString();
        }).collect(Collectors.toList()));
        String allCookies = cookies.readAllCookies(request);
        if (allCookies.contains("userName=")){
            return "uploadForm";
        } else {
            return "redirect:/login";
        }
    }

    /****************FUJ OOP TREBA*********************/
    @GetMapping({"/login"})
    public String login(Model model, HttpServletRequest request){
        String allCookies = cookies.readAllCookies(request);
        if (allCookies.contains("userName=")){
            return "redirect:/";
        } else {
            return "login";
        }
        // neviem co je ten model, ale nechal som to hu
//        model.addAttribute("files", this.storageService.loadAll().map((path) -> {
//            return MvcUriComponentsBuilder.fromMethodName(FileUploadController.class, "serveFile", new Object[]{path.getFileName().toString()}).build().toString();
//        }).collect(Collectors.toList()));
    }

    @PostMapping({"/login"})
    public String login(@RequestParam("user") String userName, @RequestParam("password") String password, RedirectAttributes redirectAttributes, HttpServletResponse response) {
        //treba v csv kontroloval, ci existuje user a ci sedi heslo, -> na to osobitny kontroler by trebalo a bude vraciat T/F
        List<String[]> data = storageService.convertCSVToData("users.csv");
        for (String[] row : data) {
            if (userName.equals(row[1])) {
                if (password.equals(row[2])) {
                    redirectAttributes.addFlashAttribute("login", "Prihlaseny: " + userName);
                    cookies.setCookie(response, userName);
                    return "redirect:/";
                } else {
                    redirectAttributes.addFlashAttribute("login", "Zle heslo !");
                    return "redirect:/login";
                }
            }
        }
        redirectAttributes.addFlashAttribute("login", "Nenasiel sa user!");
        return "redirect:/login";
    }
    /*************************************/

    /****************FUJ OOP TREBA*********************/
    @GetMapping({"/register"})
    public String register(Model model, HttpServletRequest request) throws IOException {
        String allCookies = cookies.readAllCookies(request);
        if (allCookies.contains("userName=")){
            return "redirect:/";
        } else {
            return "register";
        }
        // neviem co je ten model, ale nechal som to hu
//        model.addAttribute("files", this.storageService.loadAll().map((path) -> {
//            return MvcUriComponentsBuilder.fromMethodName(FileUploadController.class, "serveFile", new Object[]{path.getFileName().toString()}).build().toString();
//        }).collect(Collectors.toList()));
    }

    @PostMapping({"/register"})
    public String register(@RequestParam("user") String userName, @RequestParam("password") String password, @RequestParam("conFirmpassword") String conFirmpassword, RedirectAttributes redirectAttributes) {
        //treba v csv kontroloval, ci existuje user a ci heslo je rovnake ako confirmHeslo a ci to nie Slabe heslo, -> na to osobitny kontroler by trebalo a bude vraciat T/F
        if (!userName.equals("Skuska") && password.equals(conFirmpassword)) {
            redirectAttributes.addFlashAttribute("login", "Prihlaseny: " + userName);
            return "redirect:/";
        }
        else {
            redirectAttributes.addFlashAttribute("loginBad", "Registracia neuspesna!");
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
//        Resource file = this.storageService.loadAsResource(this.storageService.getRootLocation() + "\\..\\src\\main\\upb_decypher.jar", false);
        Resource file = this.storageService.loadAsResource("upb_decypher.jar", false);

        return ((BodyBuilder) ResponseEntity.ok().header("Content-Disposition", new String[]{"attachment; filename=\"" + file.getFilename() + "\""})).body(file);
    }

    @PostMapping({"/generate_key"})
    public String generateKeys(RedirectAttributes redirectAttributes) throws java.io.FileNotFoundException {

        redirectAttributes.addFlashAttribute("public_key", encryptionService.generatePublicKey());
        redirectAttributes.addFlashAttribute("private_key", encryptionService.generatePrivateKey());

        return "redirect:/";
    }

    @PostMapping({"/generate_file"})
    @ResponseBody
    public ResponseEntity<Resource> serveFileWithKeys(@RequestParam("public_key") String public_key, @RequestParam("private_key") String private_key) throws IOException {
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
        //switch pre encrypt metodu alebo decrypt
        switch (action) {
            case "encrypt-rsa":
                this.encryptionService.encryptRSA(file, this.storageService.load(file.getOriginalFilename()), key);
                break;
            case "decrypt-rsa":
                storageService.store(file, "Decrypted-" + file.getOriginalFilename());
                this.encryptionService.decryptRSA(file, this.storageService.load("Decrypted-" + file.getOriginalFilename()), key);
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