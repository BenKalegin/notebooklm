package com.notebooklm.controller;

import com.notebooklm.model.Document;
import com.notebooklm.service.DocumentService;
import com.notebooklm.util.UserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/docs")
@CrossOrigin(origins = "*")
public class DocumentController {
    
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile[] files) throws Exception {
        UUID userId = UserContext.getCurrentUser().getId();

        if (files.length == 1) {
            Document doc = documentService.upload(userId, files[0]);
            return ResponseEntity.ok(doc);
        }

        List<Document> documents = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            try {
                Document doc = documentService.upload(userId, file);
                documents.add(doc);
            } catch (Exception e) {
                System.err.println("Failed to upload " + file.getOriginalFilename() + ": " + e.getMessage());
            }
        }
        return ResponseEntity.ok(documents);
    }
    
    @GetMapping
    public List<Document> list() {
        return documentService.listDocuments(UserContext.getCurrentUser().getId());
    }
    
    @GetMapping("/{docId}")
    public Document get(@PathVariable UUID docId) {
        Document doc = documentService.getDocument(docId);
        if (!doc.getUserId().equals(UserContext.getCurrentUser().getId())) {
            throw new RuntimeException("Unauthorized");
        }
        return doc;
    }
    
    @GetMapping("/{docId}/raw")
    public String getRaw(@PathVariable UUID docId) {
        Document doc = get(docId);
        return doc.getRawMarkdown();
    }
}
