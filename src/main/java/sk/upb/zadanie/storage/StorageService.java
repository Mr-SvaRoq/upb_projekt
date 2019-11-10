package sk.upb.zadanie.storage;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Component
public interface StorageService {
    void init();

    void store(MultipartFile file, String newName, String owner);

    Stream<Path> loadAll();

    Path load(String filename);

    Boolean checkIfFileExist(String filename);

    Resource loadAsResource(String filename, boolean path) throws java.io.FileNotFoundException;

    String createUniqueName();

    //zmeni stringove pole nazvu suboru, kluca a kluca na normalizovany string pre CSV
    //samostatne sa nepouziva, sucast funkcie convertDataToCSV
    String convertLineToCSVFormat(String[] data);

    //skontroluje string a popripade escapne potrebne charaktre
    //samostatne sa nepouziva, sucast funkcie convertDataToCSV
    String escapeSpecialCharacters(String data);

    //Dostane List z csv
    List<String[]> convertCSVToData(String filename);

    void convertDataToCSV(List<String[]> data, String filename);

    void deleteAll(String filename);

    String getUserKey(String user);

    String getFileOwner(String filename);

    Path getRootLocation();
}
