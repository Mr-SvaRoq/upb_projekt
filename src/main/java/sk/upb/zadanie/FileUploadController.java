package sk.upb.zadanie;

    import java.io.IOException;
    import java.nio.ByteBuffer;
    import java.nio.file.Files;
    import java.security.InvalidAlgorithmParameterException;
    import java.security.InvalidKeyException;
    import java.security.NoSuchAlgorithmException;
    import java.security.spec.InvalidKeySpecException;
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
        //toto pridava nazov suboru a 2 kluce do nasej DB
        //este by podla mna bolo super, ak by sme mali aj DB sifrovanu, lebo to mozu hodnotit

        //tu sa zoberie doterajsi zoznam ulozenych suborov
//        List<String[]> dataCSV = this.storageService.convertCSVToData("db.csv");
//        //tu sa generuje nazov noveho suboru
//        String newFilename = storageService.createUniqueName(file.getOriginalFilename());
//        //tu sa prida novy zaznam do listu
//        dataCSV.add(new String[]{ newFilename, "Key1", "Key2" });
//        //zapis do DB
//        this.storageService.convertDataToCSV(dataCSV);
//
//        //toto je len testovaci output a sucasne navod na sposob, ako iterovat cez DB a ziskavat data
//
//        //nacitanie listu
//        List<String[]> dataCSV2 = this.storageService.convertCSVToData("db.csv");
//        //iterovanie
//        for (String[] temp : dataCSV2) {
//            //tu sa len pouziva metoda toString() kvoli vypisu, ale realne by tu mal byt vnoreny cyklus alebo pristup cez indexy
//            System.out.println(Arrays.toString(temp));
//        }







        //tento kod je zakomentovany len kvoli testovacim ucelom
        //ukladanie suboru s novym menom
//        this.storageService.store(file,newFilename);
        //switch pre encrypt metodu alebo decrypt
        switch(action) {
//            case "encrypt":
//                this.encryptionService.encrypt(file, this.storageService.load(file.getOriginalFilename(), false));
//                break;
//            case "decrypt":
//                this.encryptionService.decrypt(file, this.storageService.load(file.getOriginalFilename(), true));
//                break;
            case "encrypt-rsa":
                System.out.println("encrypt-rsa");
                String secretKey = this.encryptionService.encryptRSA(file, this.storageService.load(file.getOriginalFilename(), false), key);

                List<String[]> dataCSV = this.storageService.convertCSVToData("db.csv");
                String unique = storageService.createUniqueName();
                dataCSV.add(new String[]{ unique, secretKey });
                this.storageService.convertDataToCSV(dataCSV);

                Files.setAttribute(this.storageService.load(file.getOriginalFilename(), false ), "user:key", unique.getBytes());

//                System.out.println((Files.getAttribute(this.storageService.load(file.getOriginalFilename(), false), "user:key")));
//                System.out.println();

                break;
            case "decrypt-rsa":
                System.out.println("decrypt-rsa");
;
                String id = new String((byte[]) Files.getAttribute(this.storageService.load(file.getOriginalFilename(), false), "user:key"));

                String secretKey2 = "";

                List<String[]> dataCSV2 = this.storageService.convertCSVToData("db.csv");
                for (String[] temp : dataCSV2) {
                    if (temp[0].equals(id)) {
                        secretKey2 = temp[1];
                        break;
                    }
                }

                if(secretKey2.equals("")) {
                    throw new FileNotFoundException("File not Found");
                }


                SecretKey original = encryptionService.decryptSecretKey(key, secretKey2);
                this.encryptionService.decryptRSA(file, this.storageService.load(file.getOriginalFilename(), true), original);
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
