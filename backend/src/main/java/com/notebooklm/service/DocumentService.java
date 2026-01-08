package com.notebooklm.service;

import com.notebooklm.model.*;
import com.notebooklm.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ChunkEmbeddingRepository embeddingRepository;
    private final EmbeddingService embeddingService;
    private final MarkdownChunker chunker;

    public DocumentService(DocumentRepository documentRepository, 
                           DocumentChunkRepository chunkRepository,
                           ChunkEmbeddingRepository embeddingRepository,
                           EmbeddingService embeddingService,
                           MarkdownChunker chunker) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingService = embeddingService;
        this.chunker = chunker;
    }

    public Document upload(UUID userId, MultipartFile file) throws IOException, NoSuchAlgorithmException {
        return importFile(userId, file.getOriginalFilename(), file.getContentType(), file.getBytes());
    }

    public Document importFile(UUID userId, String filename, String contentType, byte[] bytes) throws IOException, NoSuchAlgorithmException {
        String md5 = calculateMD5(bytes);

        Optional<Document> existing = documentRepository.findByUserIdAndMd5(userId, md5);
        if (existing.isPresent()) {
            return existing.get();
        }

        Document doc = createDocument(userId, filename, contentType, md5, bytes);
        
        // Ingest synchronously for MVP
        ingest(doc);
        
        return doc;
    }

    @Transactional
    protected Document createDocument(UUID userId, String filename, String contentType, String md5, byte[] bytes) {
        Document doc = new Document();
        doc.setId(UUID.randomUUID());
        doc.setUserId(userId);
        doc.setFilename(filename);
        doc.setTitle(filename);
        doc.setContentType(contentType);
        doc.setMd5(md5);
        doc.setSizeBytes((long) bytes.length);
        doc.setRawMarkdown(new String(bytes, StandardCharsets.UTF_8));
        doc.setStatus("UPLOADING");
        doc.setUploadedAt(Instant.now());
        return documentRepository.save(doc);
    }
    
    private void ingest(Document doc) {
        try {
            List<MarkdownChunker.ChunkResult> chunks = chunker.chunk(doc.getRawMarkdown());
            int index = 0;
            for (MarkdownChunker.ChunkResult chunkData : chunks) {
                // Generate embedding (External call)
                String embeddingString = embeddingService.embedToString(chunkData.text());
                
                // Save chunk and embedding (DB Transaction)
                saveChunkAndEmbedding(doc, chunkData, index++, embeddingString);
            }
            updateStatus(doc, "INGESTED", null);
        } catch (Exception e) {
            updateStatus(doc, "FAILED", e.getMessage());
            throw new RuntimeException(e); // Propagate
        }
    }

    @Transactional
    protected void saveChunkAndEmbedding(Document doc, MarkdownChunker.ChunkResult chunkData, int index, String embeddingString) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(UUID.randomUUID());
        chunk.setDocumentId(doc.getId());
        chunk.setUserId(doc.getUserId());
        chunk.setChunkIndex(index);
        chunk.setHeadingPath(chunkData.headingPath());
        chunk.setChunkText(chunkData.text());
        chunk.setStartChar(chunkData.startChar());
        chunk.setEndChar(chunkData.endChar());
        chunk.setTokenCountEst(chunkData.tokenEstimate());
        chunk.setCreatedAt(Instant.now());
        
        chunkRepository.save(chunk);
        
        embeddingRepository.saveEmbedding(chunk.getId(), embeddingString, "text-embedding-ada-002");
    }

    @Transactional
    protected void updateStatus(Document doc, String status, String error) {
        doc.setStatus(status);
        doc.setErrorMessage(error);
        documentRepository.save(doc);
    }

    private String calculateMD5(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    public Document getDocument(UUID docId) {
        return documentRepository.findById(docId).orElseThrow();
    }
    
    public List<Document> listDocuments(UUID userId) {
        return documentRepository.findByUserId(userId);
    }
}
