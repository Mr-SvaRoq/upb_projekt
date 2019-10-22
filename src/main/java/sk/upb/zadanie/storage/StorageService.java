package sk.upb.zadanie.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface StorageService {
    void init();

    void store(MultipartFile file, String newName);

    Stream<Path> loadAll();

    Path load(String filename, boolean decMode);

    Resource loadAsResource(String filename) throws java.io.FileNotFoundException;

    String createUniqueName(String filename);

    void deleteAll();
}
