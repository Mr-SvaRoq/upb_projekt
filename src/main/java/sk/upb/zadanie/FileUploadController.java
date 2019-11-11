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
import sk.upb.zadanie.password.ValidationHandler;
import sk.upb.zadanie.storage.Cookies;
import sk.upb.zadanie.storage.FileNotFoundException;
import sk.upb.zadanie.storage.StorageService;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
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

    @Autowired
    public FileUploadController(StorageService storageService, IEncryptionService encryptionService, Cookies cookies, ValidationHandler validationHandler) {
        this.storageService = storageService;
        this.encryptionService = encryptionService;
        this.cookies = cookies;
        this.validationHandler = validationHandler;
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
                    List files_roots = this.storageService.loadAll().map((path) -> {
                        return MvcUriComponentsBuilder.fromMethodName(FileUploadController.class, "serveFile", new Object[]{path.getFileName().toString()}).build().toString();
                    }).collect(Collectors.toList());

                    List<List<String>> files = new ArrayList<>();

                    for (Object file_root : files_roots) {
                        List<String> file_data = new ArrayList<>();
                        if (cookies.getCookieValue(request, "userName").equals(storageService.getFileOwner(file_root.toString().substring(file_root.toString().lastIndexOf("/") + 1)))) {
                            file_data.add(file_root.toString());
                            file_data.add(storageService.getFileOwner(file_root.toString().substring(file_root.toString().lastIndexOf("/") + 1)));
                            files.add(file_data);
                            model.addAttribute("files", files);
                        }
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

    @GetMapping({"/files/{filename:.+}"})
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) throws java.io.FileNotFoundException {
        Resource file = this.storageService.loadAsResource(filename, true);
        return ((BodyBuilder) ResponseEntity.ok().header("Content-Disposition", new String[]{"attachment; filename=\"" + file.getFilename() + "\""})).body(file);
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
                    this.encryptionService.encryptRSA(file, this.storageService.load(filename), storageService.getUserKey(owner));
                }
                break;
            case "decrypt-rsa":
                if (!storageService.checkIfFileExist(storageService.load("Decrypted-" + file.getOriginalFilename()).toString())) {
                    filename = "Decrypted-" + file.getOriginalFilename();
                } else {
                    int i = 1;
                    while (storageService.checkIfFileExist(storageService.load("Decrypted-(" + i + ")-" + file.getOriginalFilename()).toString())) {
                        i++;
                    }
                    filename = "Decrypted-(" + i + ")-" + file.getOriginalFilename();
                }
                storageService.store(file, filename, cookies.getCookieValue(request, "userName"));
                if (storageService.getUserKey(cookies.getCookieValue(request, "userName")).equals("")) {
                    System.out.println("Nieco sa pokazilo...");
                } else {
                    this.encryptionService.decryptRSA(file, this.storageService.load(filename), owner);
                }
                break;
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