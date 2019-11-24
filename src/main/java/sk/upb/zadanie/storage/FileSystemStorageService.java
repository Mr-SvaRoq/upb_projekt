package sk.upb.zadanie.storage;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileSystemStorageService implements StorageService {

    private final Path rootLocation;

    @Autowired
    public FileSystemStorageService(StorageProperties properties) {
        this.rootLocation = Paths.get(properties.getLocation());
    }

    @Override
    public Boolean checkIfFileExist(String filePath) {
        File f = new File(filePath);
        if(f.exists() && !f.isDirectory()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void store(MultipartFile file, String newName, String owner) {
        String newFilename = newName;
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file " + filename);
            }
            if (filename.contains("..")) {
                // This is a security check
                throw new StorageException(
                        "Cannot store file with relative path outside current directory "
                                + filename);
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, this.rootLocation.resolve(newFilename),
                        StandardCopyOption.REPLACE_EXISTING);
                List<String[]> files = convertCSVToData("files.csv");
                String[] newLine = {newName, owner} ;
                files.add(newLine);
                this.convertDataToCSV(files, "files.csv");
            }
        }
        catch (IOException e) {
            throw new StorageException("Failed to store file " + filename, e);
        }
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootLocation, 1)
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(this.rootLocation::relativize);
        }
        catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }

    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename, boolean path) throws java.io.FileNotFoundException {
        try {
            Path file;
            if (path) {
                file = load(filename);
            } else {
                file = Paths.get(filename);
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            else {
                throw new java.io.FileNotFoundException(
                        "Could not read file: " + filename);

            }
        }
        catch (MalformedURLException e) {
            throw new FileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public String createUniqueName() {
        Random rand = new Random();
        int num = 1000000 + rand.nextInt(9000000);
        return Integer.toString(num);
    }

    @Override
    public String getUserKey(String user) {
        List<String[]> data = convertCSVToData("users.csv");
        for (String[] user_data : data) {
            if (user_data[0].equals(user)) {
                return user_data[2];
            }
        }
        return "";
    }

    @Override
    public String convertLineToCSVFormat(String[] data) {
        return Stream.of(data)
                .map(this::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }

    @Override
    public String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    @Override
    public void convertDataToCSV(List<String[]> data, String filename) {
        File csvOutputFile = new File(filename);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            data.stream()
                    .map(this::convertLineToCSVFormat)
                    .forEach(pw::println);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFileOwner(String filename) {
        List<String[]> file_list = convertCSVToData("files.csv");
        for (String[] file_record : file_list) {
            if (file_record[0].equals(filename)) {
                return file_record[1];
            }
        }
        return null;
    }

    @Override
    public List<String[]> convertCSVToData(String filename) {
        List<String[]> records = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(new FileReader(filename))) {
            String[] values = null;
            while ((values = csvReader.readNext()) != null) {
                records.add(values);
            }
            return records;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteAll(String filename) {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(filename);
            writer.print("");
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init() {
        //toto je aby sa Rado nedojebal lebo sa mu to bude vzdy mazat
//        try {
//            Files.createDirectories(rootLocation);
//        }
//        catch (IOException e) {
//            throw new StorageException("Could not initialize storage", e);
//        }
    }

    @Override
    public Path getRootLocation() {
        return rootLocation;
    }

    @Override
    public List<String> getServerKeys(String csvFile) {
        List<String[]> keysFromCsv = convertCSVToData(csvFile);
        List<String> publicPrivateKeys = new ArrayList<>();
        int counter = 0;
        for (String[] row : keysFromCsv) {
            if (counter > 0 ) {
                throw new StorageException("Zly format filu, asi pravdepodobne");
            }
            publicPrivateKeys.add(row[0]);
            publicPrivateKeys.add(row[1]);
            counter++;
        }
        return publicPrivateKeys;
    }
}
