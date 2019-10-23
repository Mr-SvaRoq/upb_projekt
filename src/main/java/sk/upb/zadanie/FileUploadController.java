package sk.upb.zadanie;

    import java.io.IOException;
    import java.security.InvalidAlgorithmParameterException;
    import java.security.InvalidKeyException;
    import java.security.NoSuchAlgorithmException;
    import java.util.ArrayList;
    import java.util.Arrays;
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
    import sk.upb.zadanie.encryption.EncryptionService;
    import sk.upb.zadanie.storage.FileNotFoundException;
    import sk.upb.zadanie.storage.StorageService;

    import javax.crypto.BadPaddingException;
    import javax.crypto.IllegalBlockSizeException;
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


    //toto by bolo cool, ak by to vedelo vracat privatne kluce napr. ako string
//    @GetMapping({"/generate_key"})
//    public ResponseEntity<Resource> generateKeyFile() throws java.io.FileNotFoundException {
//
//        Resource file = this.storageService.loadAsResource(filename);
//        return ((BodyBuilder)ResponseEntity.ok().header("Content-Disposition", new String[]{"attachment; filename=\"" + file.getFilename() + "\""})).body(file);
//    }

    @PostMapping({"/"})
    public String handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("key") MultipartFile key, @RequestParam("action") String action,RedirectAttributes redirectAttributes) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        //toto pridava nazov suboru a 2 kluce do nasej DB
        //este by podla mna bolo super, ak by sme mali aj DB sifrovanu, lebo to mozu hodnotit

        //tu sa zoberie doterajsi zoznam ulozenych suborov
        List<String[]> dataCSV = this.storageService.convertCSVToData("db.csv");
        //tu sa generuje nazov noveho suboru
        String newFilename = storageService.createUniqueName(file.getOriginalFilename());
        //tu sa prida novy zaznam do listu
        dataCSV.add(new String[]{ newFilename, "Key1", "Key2" });
        //zapis do DB
        this.storageService.convertDataToCSV(dataCSV);


        //toto je len testovaci output a sucasne navod na sposob, ako iterovat cez DB a ziskavat data

        //nacitanie listu
        List<String[]> dataCSV2 = this.storageService.convertCSVToData("db.csv");
        //iterovanie
        for (String[] temp : dataCSV2) {
            //tu sa len pouziva metoda toString() kvoli vypisu, ale realne by tu mal byt vnoreny cyklus alebo pristup cez indexy
            System.out.println(Arrays.toString(temp));
        }

        //tento kod je zakomentovany len kvoli testovacim ucelom
        //ukladanie suboru s novym menom
//        this.storageService.store(file,newFilename);
        //switch pre encrypt metodu alebo decrypt
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
