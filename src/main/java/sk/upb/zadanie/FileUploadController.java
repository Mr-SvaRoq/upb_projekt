package sk.upb.zadanie;

    import java.io.IOException;
    import java.security.InvalidAlgorithmParameterException;
    import java.security.InvalidKeyException;
    import java.security.NoSuchAlgorithmException;
    import java.sql.SQLOutput;
    import java.util.stream.Collectors;

    import org.apache.commons.io.FilenameUtils;
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
    import sk.upb.zadanie.encryption.EncryptionService;
    import sk.upb.zadanie.storage.FileNotFoundException;
    import sk.upb.zadanie.storage.StorageService;

    import javax.crypto.NoSuchPaddingException;

@Controller
public class FileUploadController {
    private final StorageService storageService;
    private final EncryptionService encryptionService;

    @Autowired
    public FileUploadController(StorageService storageService, EncryptionService encryptionService) {
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

    @GetMapping({"/generate_key"})
//    public ResponseEntity<Resource> generateKeyFile() throws java.io.FileNotFoundException {
//
//        Resource file = this.storageService.loadAsResource(filename);
//        return ((BodyBuilder)ResponseEntity.ok().header("Content-Disposition", new String[]{"attachment; filename=\"" + file.getFilename() + "\""})).body(file);
//    }

    @PostMapping({"/"})
    public String handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("action") String action,RedirectAttributes redirectAttributes) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        this.storageService.store(file,storageService.createUniqueName(file.getOriginalFilename()));
//        switch(action) {
//            case "encrypt":
//                this.encryptionService.encrypt(file, this.storageService.load(file.getOriginalFilename()));
//                break;
//            case "decrypt":
//                this.encryptionService.decrypt(file, this.storageService.load(file.getOriginalFilename()));
//                break;
//            default:
//                System.out.println("Nieco sa dojebalo...");
//        }
//        redirectAttributes.addFlashAttribute("message", "You successfully uploaded " + file.getOriginalFilename() + "!");
        return "redirect:/project";
    }

    @ExceptionHandler({FileNotFoundException.class})
    public ResponseEntity<?> handleStorageFileNotFound(FileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }
}
