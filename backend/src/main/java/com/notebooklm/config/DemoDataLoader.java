package com.notebooklm.config;

import com.notebooklm.model.User;
import com.notebooklm.repository.UserRepository;
import com.notebooklm.service.DocumentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class DemoDataLoader implements CommandLineRunner {

    private final DocumentService documentService;
    private final UserRepository userRepository;

    public DemoDataLoader(DocumentService documentService, UserRepository userRepository) {
        this.documentService = documentService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if demo user already exists
        if (userRepository.findByEmail("demo@example.com").isPresent()) {
            return; // Demo data already loaded
        }

        // Create demo user
        User demoUser = new User();
        demoUser.setId(UUID.randomUUID());
        demoUser.setEmail("demo@example.com");
        demoUser.setCreatedAt(Instant.now());
        userRepository.save(demoUser);

        // Import demo documents
        Path demoPath = Paths.get("../demo");
        if (!Files.exists(demoPath)) {
            System.out.println("Demo folder not found, skipping demo data import");
            return;
        }

        try (Stream<Path> paths = Files.walk(demoPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".md"))
                 .forEach(path -> {
                     try {
                         byte[] content = Files.readAllBytes(path);
                         String filename = path.getFileName().toString();
                         
                         documentService.importFile(
                             demoUser.getId(), 
                             filename, 
                             "text/markdown", 
                             content
                         );
                         
                         System.out.println("Imported demo document: " + filename);
                     } catch (Exception e) {
                         System.err.println("Failed to import " + path + ": " + e.getMessage());
                     }
                 });
        }
    }
}