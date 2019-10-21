package sk.upb.zadanie;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import sk.upb.zadanie.storage.StorageProperties;
import sk.upb.zadanie.storage.StorageService;

@EnableConfigurationProperties({StorageProperties.class})
@SpringBootApplication
public class ZadanieApplication {
    public ZadanieApplication() {}

    public static void main(String[] args) {
        SpringApplication.run(ZadanieApplication.class, args);
    }
    @Bean
    CommandLineRunner init(StorageService storageService) {
        return (args) -> {
            System.out.println("UPB-zadanie");
            storageService.deleteAll();
            storageService.init();
        };
    }
}
