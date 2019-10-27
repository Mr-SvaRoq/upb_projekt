package sk.upb.zadanie;

import java.io.IOException;
    import java.nio.ByteBuffer;
    import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
    import java.util.Arrays;
import java.util.Base64;
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
    import sk.upb.zadanie.storage.FileNotFoundException;
    import sk.upb.zadanie.storage.StorageService;

    import javax.crypto.BadPaddingException;
    import javax.crypto.IllegalBlockSizeException;
    import javax.crypto.NoSuchPaddingException;
    import javax.crypto.SecretKey;

@Controller
public class FileUploadController {
    private final StorageService storageService;
    private final IEncryptionService encryptionService;

    @Autowired
    public FileUploadController(StorageService storageService, IEncryptionService encryptionService) {
        this.storageService = storageService;
        this.encryptionService = encryptionService;
    }

    @GetMapping({"/project"})
    public String listUploadedFiles(Model model) throws IOException {
        model.addAttribute("files", this.storageService.loadAll().map((path) -> {
            return MvcUriComponentsBuilder.fromMethodName(FileUploadController.class, "serveFile", new Object[]{path.getFileName().toString()}).build().toString();
        }).collect(Collectors.toList()));
        return "uploadForm";
    }

    @GetMapping({"/files/{filename:.+}"})
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) throws java.io.FileNotFoundException {
        Resource file = this.storageService.loadAsResource(filename);
        return ((BodyBuilder)ResponseEntity.ok().header("Content-Disposition", new String[]{"attachment; filename=\"" + file.getFilename() + "\""})).body(file);
    }

    @PostMapping({"/generate_key"})
    public String generateKeys(RedirectAttributes redirectAttributes) throws java.io.FileNotFoundException {

        redirectAttributes.addFlashAttribute("public_key",encryptionService.generatePublicKey());
        redirectAttributes.addFlashAttribute("private_key",encryptionService.generatePrivateKey());

        return "redirect:/project";
    }

    @PostMapping({"/"})
    public String handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("key") String key, @RequestParam("action") String action,RedirectAttributes redirectAttributes) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IOException, BadPaddingException, IllegalBlockSizeException, InvalidKeySpecException {
        //switch pre encrypt metodu alebo decrypt
        switch(action) {
            case "encrypt-rsa":
                this.encryptionService.encryptRSA(file, this.storageService.load(file.getOriginalFilename(), false), key);
//                String secretKey = this.encryptionService.encryptRSA(file, this.storageService.load(file.getOriginalFilename(), false), key);
//                Files.setAttribute(this.storageService.load(file.getOriginalFilename(), false ), "user:key", secretKey.getBytes());
                break;
            case "decrypt-rsa":
                //TODO save

//                String secretKey2 = new String((byte[]) Files.getAttribute(this.storageService.load(file.getOriginalFilename(), false), "user:key"));
//                SecretKey original = encryptionService.decryptSecretKey(key, secretKey2);
                this.encryptionService.decryptRSA(file, this.storageService.load(file.getOriginalFilename(), true), key);
                break;
            default:
                System.out.println("Nieco sa pokazilo...");
        }
        redirectAttributes.addFlashAttribute("message", "You successfully uploaded " + file.getOriginalFilename() + "!");
        return "redirect:/project";
    }

    @ExceptionHandler({FileNotFoundException.class})
    public ResponseEntity<?> handleStorageFileNotFound(FileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }
}
