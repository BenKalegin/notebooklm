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
    public ResponseEntity<Document> upload(@RequestParam("file") MultipartFile file) throws Exception {
        UUID userId = UserContext.getCurrentUser().getId();
        Document doc = documentService.upload(userId, file);
        return ResponseEntity.ok(doc);
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
