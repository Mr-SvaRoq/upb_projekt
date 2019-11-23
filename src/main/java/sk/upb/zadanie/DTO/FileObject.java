package sk.upb.zadanie.DTO;

import java.util.List;

public class FileObject {
    public String fileName;
    public String fileOwner;
    public List<String> Comments;

    public FileObject() {
    }

    public void setComments(List<String> comments) {
        Comments = comments;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileOwner(String fileOwner) {
        this.fileOwner = fileOwner;
    }
}
