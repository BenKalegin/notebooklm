package com.notebooklm;

import com.notebooklm.model.User;
import com.notebooklm.repository.UserRepository;
import com.notebooklm.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest
class DemoDataImportTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void importDemoDocuments() throws Exception {
        importFromFolder("../demo", 1);
    }

    @Test
    void import10KDocuments() throws Exception {
        importFromFolder("../demo/demo10K", 8);
    }

    @Test
    void import100KDocuments() throws Exception {
        importFromFolder("../demo/demo100K", 16);
    }

    private void importFromFolder(String folderPath, int parallelThreads) throws Exception {
        User demoUser = new User();
        demoUser.setId(UUID.randomUUID());
        demoUser.setEmail("demo@example.com");
        demoUser.setCreatedAt(Instant.now());
        userRepository.save(demoUser);

        Path demoPath = Paths.get(folderPath);
        if (!Files.exists(demoPath)) {
            System.err.println("Path not found: " + demoPath.toAbsolutePath());
            return;
        }

        List<Path> files;
        try (Stream<Path> paths = Files.walk(demoPath)) {
            files = paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".md"))
                        .filter(path -> !path.getFileName().toString().equals("README.md"))
                        .collect(Collectors.toList());
        }

        System.out.println("Found " + files.size() + " documents to import");
        System.out.println("Using " + parallelThreads + " parallel threads");

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        Instant start = Instant.now();

        ExecutorService executor = Executors.newFixedThreadPool(parallelThreads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Path path : files) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    byte[] content = Files.readAllBytes(path);
                    String filename = path.getFileName().toString();
                    
                    documentService.importFile(
                        demoUser.getId(), 
                        filename, 
                        "text/markdown", 
                        content
                    );
                    
                    int count = success.incrementAndGet();
                    if (count % 100 == 0) {
                        long elapsed = Duration.between(start, Instant.now()).getSeconds();
                        double rate = count / (double) elapsed;
                        System.out.printf("Progress: %d/%d (%.1f docs/sec)%n", count, files.size(), rate);
                    }
                } catch (Exception e) {
                    failed.incrementAndGet();
                    System.err.println("Failed: " + path.getFileName() + " - " + e.getMessage());
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        long elapsed = Duration.between(start, Instant.now()).getSeconds();
        double rate = success.get() / (double) Math.max(elapsed, 1);
        
        System.out.println("\n=== Import Complete ===");
        System.out.println("Success: " + success.get());
        System.out.println("Failed: " + failed.get());
        System.out.println("Time: " + elapsed + " seconds");
        System.out.println("Rate: " + String.format("%.1f", rate) + " docs/sec");
    }
}