package com.notebooklm;

import com.notebooklm.model.Document;
import com.notebooklm.model.User;
import com.notebooklm.repository.UserRepository;
import com.notebooklm.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

@SpringBootTest
@Testcontainers
class DemoDataImportTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg15").asCompatibleSubstituteFor("postgres"))
        .withDatabaseName("notebooklm")
        .withUsername("user")
        .withPassword("password");

    @Autowired
    private DocumentService documentService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void importDemoDocuments() throws IOException, NoSuchAlgorithmException {
        // Create demo user
        User demoUser = new User();
        demoUser.setId(UUID.randomUUID());
        demoUser.setEmail("demo@example.com");
        demoUser.setCreatedAt(Instant.now());
        userRepository.save(demoUser);

        // Import all markdown files from demo folder
        Path demoPath = Paths.get("C:\\Users\\vkalegin\\repos\\test\\notebooklm\\demo");
        
        try (Stream<Path> paths = Files.walk(demoPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".md"))
                 .forEach(path -> {
                     try {
                         byte[] content = Files.readAllBytes(path);
                         String filename = path.getFileName().toString();
                         
                         Document doc = documentService.importFile(
                             demoUser.getId(), 
                             filename, 
                             "text/markdown", 
                             content
                         );
                         
                         System.out.println("Imported: " + filename + " -> " + doc.getStatus());
                     } catch (Exception e) {
                         System.err.println("Failed to import " + path + ": " + e.getMessage());
                     }
                 });
        }
    }
}