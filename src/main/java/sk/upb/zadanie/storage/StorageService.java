package sk.upb.zadanie.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public interface StorageService {
    void init();

    void store(MultipartFile file, String newName);

    Stream<Path> loadAll();

    Path load(String filename, boolean decMode);

    Resource loadAsResource(String filename) throws java.io.FileNotFoundException;

    String createUniqueName(String filename);

    //zmeni stringove pole nazvu suboru, kluca a kluca na normalizovany string pre CSV
    //samostatne sa nepouziva, sucast funkcie convertDataToCSV
    String convertLineToCSVFormat(String[] data);

    //skontroluje string a popripade escapne potrebne charaktre
    //samostatne sa nepouziva, sucast funkcie convertDataToCSV
    String escapeSpecialCharacters(String data);

    //zapise List stringov do csv
    Boolean convertDataToCSV(List<String[]> data);

    //Dostane List z csv
    List<String[]> convertCSVToData(String filename);

    void deleteAll();
}
