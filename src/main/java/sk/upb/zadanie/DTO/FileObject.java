package sk.upb.zadanie.DTO;

import java.util.List;

public class FileObject {
    public String fileName;
    public String fileOwner;
    public List<String> comments;
    public List<String> allPrivileges;

    public FileObject() {
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileOwner(String fileOwner) {
        this.fileOwner = fileOwner;
    }

    public void setAllPrivileges(List<String> allPrivileges) {
        this.allPrivileges = allPrivileges;
    }
}
