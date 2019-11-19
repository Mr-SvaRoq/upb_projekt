package sk.upb.zadanie;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sk.upb.zadanie.encryption.IEncryptionService;
import sk.upb.zadanie.encryption.ServerKeys;
import sk.upb.zadanie.password.ValidationHandler;
import sk.upb.zadanie.storage.Cookies;
import sk.upb.zadanie.storage.FileNotFoundException;
import sk.upb.zadanie.storage.StorageService;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class FileUploadController {
    private final StorageService storageService;
    private final IEncryptionService encryptionService;
    private final Cookies cookies;
    private final ValidationHandler validationHandler;
    private final ServerKeys serverKeys;

    @Autowired
    public FileUploadController(StorageService storageService, IEncryptionService encryptionService, Cookies cookies, ValidationHandler validationHandler, ServerKeys serverKeys) {
        this.storageService = storageService;
        this.encryptionService = encryptionService;
        this.cookies = cookies;
        this.validationHandler = validationHandler;
        this.serverKeys = serverKeys;
    }

    @GetMapping({"/"})
    public String listUploadedFiles(Model model, HttpServletRequest request) throws InvalidKeySpecException, NoSuchAlgorithmException {
        List<String[]> data = storageService.convertCSVToData("users.csv");

        String allCookies = cookies.readAllCookies(request);
        if (!allCookies.contains("userName=") || !allCookies.contains("userPassword=")) {
            return "redirect:/login";
        }

        for (String[] row : data) {
            if (cookies.getCookieValue(request, "userName").equals(row[0])) {
                if (validationHandler.validatePassword(cookies.getCookieValue(request, "userPassword"), row[1])) { //ak nesedi databaza a je uz zapisane cookies, cele je to na blb
                    model.addAttribute("login", "Prihlaseny: " + cookies.getCookieValue(request, "userName"));

                    //Toto potrebovat nebudeme - minimalne v tejto forme

//                    List files_roots = this.storageService.loadAll().map((path) -> {
//                        return MvcUriComponentsBuilder.fromMethodName(FileUploadController.class, "fileDetail", new Object[]{path.getFileName().toString()}).build().toString();
//                    }).collect(Collectors.toList());

                    List files_roots = this.storageService.loadAll().collect(Collectors.toList());
                    List<List<String>> files = new ArrayList<>();

                    for (Object file_root : files_roots) {
                        List<String> file_data = new ArrayList<>();
                            file_data.add(file_root.toString());
                            file_data.add(storageService.getFileOwner(file_root.toString().substring(file_root.toString().lastIndexOf("/") + 1)));
                            files.add(file_data);
                            model.addAttribute("files", files);
                    }

                    List<List<String>> users = new ArrayList<>();

                    for (String[] user_data : data) {
                        List<String> user = new ArrayList<>();
                        user.add(user_data[0]);
                        users.add(user);
                        model.addAttribute("users", users);
                    }
                    return "uploadForm";
                } else {
                    model.addAttribute("login", "Nastala chyba");
                    return "chyba";
                }
            }
        }
        return "redirect:/login";
    }

    //Toto bude spracovavat podnet na stahovanie suborov, ale treba este pridat parameter toho, co sa to ma stiahnut sifrovane,
    // alebo nie... plus by sa mala preniest informacia o uzivatelovi, ci je lognuty, ci ma prava a hlavne kto to je,
    // lebo vsak potrebujes jeho public key
    @GetMapping({"/download/{filename:.+}"})
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) throws IOException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException {
        Path pathFile = this.storageService.load(filename);
        byte[] bytesOfFile = Files.readAllBytes(pathFile);
        //TODO decrypt rsa - problem, ze tam treba multipart file
        //TODO Navrh, bud zmenit parameter a dat tam get bytes a nejako nacitat file
        Resource fileToDownload = this.encryptionService.decryptRSA(bytesOfFile, serverKeys.getPrivateKey());
        return ((BodyBuilder)ResponseEntity.ok().header("Content-Disposition", new String[]{"attachment; filename=\"" + "Dec-" + filename + "\""})).body(fileToDownload);
    }

    //Toto je Radkova podstranka pre konkretny subor - tu sa caka na doplnenie db s komentarmi a pravami
    @GetMapping({"/files/{filename:.+}"})
    public String fileDetail(Model model, HttpServletRequest request, @PathVariable String filename) throws InvalidKeySpecException, NoSuchAlgorithmException {
        List<String[]> users_data = storageService.convertCSVToData("users.csv");
        String allCookies = cookies.readAllCookies(request);
        if ( !allCookies.contains("userName=")  || !allCookies.contains("userPassword=")) {
            return "redirect:/login";
        }
        List<String> file_data = new ArrayList<>();
        file_data.add(filename);
        file_data.add(storageService.getFileOwner(filename));

        model.addAttribute("file", file_data);

        for (String[] row : users_data) {
            if (cookies.getCookieValue(request, "userName").equals(row[0])) {
                if (validationHandler.validatePassword(cookies.getCookieValue(request, "userPassword"), row[1])) { //ak nesedi databaza a je uz zapisane cookies, cele je to na blb
                    model.addAttribute("login", cookies.getCookieValue(request, "userName"));
                    List files_roots = this.storageService.loadAll().map((path) -> {
                        return MvcUriComponentsBuilder.fromMethodName(FileUploadController.class, "serveFile", new Object[]{path.getFileName().toString()}).build().toString();
                    }).collect(Collectors.toList());

                    List<List<String>> comments = new ArrayList<>();
                    List<String[]> data_comments = storageService.convertCSVToData("comments.csv");

                    for (String[] comment_data : data_comments) {
                        if (comment_data[2].equals(filename)) {
                            List<String> comment = new ArrayList<>();
                            comment.add(comment_data[1]);
                            comment.add(comment_data[3]);
                            comments.add(comment);
                        }

                    }
                    model.addAttribute("comments", comments);


                    List<List<String>> users = new ArrayList<>();

                    for (String[] user_data : users_data) {
                        List<String> user = new ArrayList<>();
                        user.add(user_data[0]);
                        users.add(user);
                    }
                    model.addAttribute("users", users);

//                    counter = 0;
                    return "file";
                } else {
                    model.addAttribute("login", "Nastala chyba");
                    return "chyba";
                }
            }
        }
        return "redirect:/login";
    }

    public static boolean empty( final String s ) {
        // Null-safe, short-circuit evaluation.
        return s == null || s.trim().isEmpty();
    }

    //Zapisovanie komentarov do DB
    @PostMapping({"/files/comment"})
    public String newComment(Model model, HttpServletRequest request, @RequestParam("fileName") String fileName, @RequestParam("newComment") String newComment) throws InvalidKeySpecException, NoSuchAlgorithmException {
        if (!empty(fileName) && !empty(newComment) && storageService.checkIfFileExist(fileName)) {

            List<String[]> comments = storageService.convertCSVToData("comments.csv");

            String[] newLine = {Integer.toString(comments.size()), cookies.getCookieValue(request, "userName"), fileName, newComment} ;
            comments.add(newLine);
            storageService.convertDataToCSV(comments, "comments.csv");
        }

        return "redirect:/files/" + fileName;
    }

        //TODO zmenit /files/skuska
    @PostMapping({"/files/skuska"})
    public String newPrivileges(Model model, HttpServletRequest request, @RequestParam("fileName") String fileName, @RequestParam("owner") String owner, @RequestParam("newPrivileges") String newFileUser) throws InvalidKeySpecException, NoSuchAlgorithmException {
        //overit, ci nie su nahodou null parametre alebo prazdne stringy
        if (!empty(fileName) && !empty(owner) && !empty(newFileUser)) {
            //pozriet, ci owner ma fileName,
            List<String[]> data = storageService.convertCSVToData("privileges.csv");
            for (String[] row : data) {
                if(row[0].equals(fileName) && row[1].equals(owner) && row[2].equals(newFileUser)){
                    //TODO FrontEnd - Aby sa tento atribut zobrazil na stranke
                    model.addAttribute("error", "Pouzivatel: " + newFileUser + " uz ma prava k suboru: " + fileName);
                    return "redirect:/files/" + fileName;
                }
            }
            //ak sa prihlaseny user (cookies) zhoduje s ownerom fileu tak moze zapisovat prava
            String login = cookies.getCookieValue(request, "userName");
            String ownerOfFile = storageService.getFileOwner(fileName);
            if (cookies.getCookieValue(request, "userName").equals(storageService.getFileOwner(fileName))) {
                  //pridat novy riadok do privileges
                  String[] newLine = {fileName, owner, newFileUser};
                  data.add(newLine);
                  this.storageService.convertDataToCSV(data, "privileges.csv");
            }
        }
        //TODO redirect zatial na / mozno potom zmenit
        //return stranky, resp redirect
        return "redirect:/files/" + fileName;
    }



    @PostMapping({"/"})
    public String handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("owner") String owner, @RequestParam("action") String action, HttpServletRequest request, RedirectAttributes redirectAttributes) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IOException, BadPaddingException, IllegalBlockSizeException, InvalidKeySpecException {
        String filename = "";
        switch (action) {
            case "encrypt-rsa":
                if (!storageService.checkIfFileExist(storageService.load(file.getOriginalFilename()).toString())) {
                    filename = file.getOriginalFilename();
                } else {
                    int i = 1;
                    while (storageService.checkIfFileExist(storageService.load("(" + i + ")-" + file.getOriginalFilename()).toString())) {
                        i++;
                    }
                    filename = "(" + i + ")-" + file.getOriginalFilename();
                }
                storageService.store(file, filename, owner);
                if (storageService.getUserKey(owner).equals("")) {
                    System.out.println("Nieco sa pokazilo...");
                } else {
                    //TODO zmenit public key na server public key - 16.11.2019 -DNT - done
                    this.encryptionService.encryptRSA(file, this.storageService.load(filename), serverKeys.getPublicKey());
                }
                break;
//                //TODO DNT - dat prec, po suhlase timu
//            case "decrypt-rsa":
//                if (!storageService.checkIfFileExist(storageService.load("Decrypted-" + file.getOriginalFilename()).toString())) {
//                    filename = "Decrypted-" + file.getOriginalFilename();
//                } else {
//                    int i = 1;
//                    while (storageService.checkIfFileExist(storageService.load("Decrypted-(" + i + ")-" + file.getOriginalFilename()).toString())) {
//                        i++;
//                    }
//                    filename = "Decrypted-(" + i + ")-" + file.getOriginalFilename();
//                }
//                storageService.store(file, filename, cookies.getCookieValue(request, "userName"));
//                if (storageService.getUserKey(cookies.getCookieValue(request, "userName")).equals("")) {
//                    System.out.println("Nieco sa pokazilo...");
//                } else {
//                    this.encryptionService.decryptRSA(file, this.storageService.load(filename), owner);
//                }
//                break;
            default:
                System.out.println("Nieco sa pokazilo...");
        }
        redirectAttributes.addFlashAttribute("message", "You successfully uploaded " + file.getOriginalFilename() + "!");
        return "redirect:/";
    }

    @ExceptionHandler({FileNotFoundException.class})
    public ResponseEntity<?> handleStorageFileNotFound(FileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}