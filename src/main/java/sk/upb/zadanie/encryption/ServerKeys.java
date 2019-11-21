package sk.upb.zadanie.encryption;

import org.springframework.stereotype.Component;
import sk.upb.zadanie.storage.StorageService;

import java.util.List;

@Component
public class ServerKeys {
    private final StorageService storageService;
    private String publicKey;
    private String privateKey;


    public ServerKeys(StorageService storageService) {
        this.storageService = storageService;
        List<String> key = storageService.getServerKeys("server_keys.csv");
        publicKey = key.get(0);
        privateKey = key.get(1);
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }
}
