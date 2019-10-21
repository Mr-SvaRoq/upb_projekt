package sk.upb.zadanie.storage;

public class FileNotFoundException extends StorageException {
    public FileNotFoundException(String message) {
        super(message);
    }

    public FileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

