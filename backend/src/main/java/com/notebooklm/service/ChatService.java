package com.notebooklm.service;

import com.notebooklm.model.*;
import com.notebooklm.repository.*;
import com.notebooklm.service.dto.AssistantResponse;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.parser.BeanOutputParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.Objects;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository messageRepository;
    private final MessageRetrievalRepository retrievalRepository;
    private final RetrievalService retrievalService;
    private final DocumentRepository documentRepository;
    private final ChatClient chatClient;
    private final int topK;

    public ChatService(ChatRepository chatRepository,
                       ChatMessageRepository messageRepository,
                       MessageRetrievalRepository retrievalRepository,
                       RetrievalService retrievalService,
                       DocumentRepository documentRepository,
                       ChatClient chatClient,
                       @org.springframework.beans.factory.annotation.Value("${app.retrieval.top-k:10}") int topK) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.retrievalRepository = retrievalRepository;
        this.retrievalService = retrievalService;
        this.documentRepository = documentRepository;
        this.chatClient = chatClient;
        this.topK = topK;
    }

    @Transactional
    public Chat createChat(UUID userId, String title) {
        Chat chat = new Chat();
        chat.setId(UUID.randomUUID());
        chat.setUserId(userId);
        chat.setTitle(title != null ? title : "New Chat");
        chat.setCreatedAt(Instant.now());
        chat.setUpdatedAt(Instant.now());
        return chatRepository.save(chat);
    }
    
    public List<Chat> listChats(UUID userId) {
        return chatRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }
    
    public List<ChatMessage> getMessages(UUID chatId) {
        return messageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
    }

    @Transactional
    public AssistantResponse answer(UUID chatId, UUID userId, String userMessageText) {
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        chat.setUpdatedAt(Instant.now());
        chatRepository.save(chat);

        // 1. Save User Message
        ChatMessage userMsg = new ChatMessage();
        userMsg.setId(UUID.randomUUID());
        userMsg.setChatId(chatId);
        userMsg.setUserId(userId);
        userMsg.setRole("USER");
        userMsg.setContent(userMessageText);
        userMsg.setCreatedAt(Instant.now());
        messageRepository.save(userMsg);
        
        // 2. Retrieve
        List<RetrievalService.RetrievedChunk> chunks = retrievalService.search(userId, null, userMessageText, topK);
        
        // 3. Construct Context
        StringBuilder context = new StringBuilder();
        
        for (RetrievalService.RetrievedChunk chunk : chunks) {
            Document doc = documentRepository.findById(chunk.docId()).orElse(null);
            String title = doc != null ? doc.getTitle() : "Unknown";
            String filename = doc != null ? doc.getFilename() : "Unknown";
            
            context.append(String.format("""
                [CONTEXT_CHUNK]
                DOC_ID: %s
                FILENAME: %s
                DOC_TITLE: %s
                CHUNK_ID: %s
                HEADING: %s
                TEXT:
                %s
                [/CONTEXT_CHUNK]
                """, chunk.docId(), filename, title, chunk.chunkId(), chunk.headingPath(), chunk.chunkText()));
        }
        
        // 4. System Prompt
        BeanOutputParser<AssistantResponse> outputParser = new BeanOutputParser<>(AssistantResponse.class);
        
        String systemText = """
            You are a helpful assistant that answers questions ONLY using the provided context chunks.
            If context does not contain the answer, say you don't know based on uploaded documents.
            Do not use external knowledge.
            Provide citations for each key statement.
            
            Note: "Shipment values" or "total values" can be found in documents like Invoices (Total Due), 
            Advance Ship Notices (Declared Total), or Bill of Ladings. 
            Consider documents from Acme Industrial as the sender/issuer if looking for shipments 'from' them.
            
            Output MUST match JSON schema exactly.
            {format}
            """;
        
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemText);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("format", outputParser.getFormat()));
        
        Message contextMessage = new SystemMessage("Context:\n" + context.toString());
        Message userMessage = new UserMessage(userMessageText);
        
        Prompt prompt = new Prompt(List.of(systemMessage, contextMessage, userMessage));
        
        // 5. Call AI
        String content = chatClient.call(prompt).getResult().getOutput().getContent();
        
        // 6. Parse
        AssistantResponse response;
        try {
             // Strip markdown code blocks if present
             String cleanContent = content.trim();
             if (cleanContent.startsWith("```json")) {
                 cleanContent = cleanContent.substring(7);
             } else if (cleanContent.startsWith("```")) {
                 cleanContent = cleanContent.substring(3);
             }
             if (cleanContent.endsWith("```")) {
                 cleanContent = cleanContent.substring(0, cleanContent.length() - 3);
             }
             cleanContent = cleanContent.trim();
             
             AssistantResponse rawResponse = outputParser.parse(cleanContent);
             
             // Map document IDs in citations from internal IDs to UUIDs
             List<AssistantResponse.Citation> mappedCitations = rawResponse.citations().stream()
                 .map(citation -> {
                     String docId = citation.document_id();
                     
                     // Try exact match first
                     List<Document> docs = documentRepository.findByUserIdAndInternalId(userId, "id: " + docId);
                     
                     // If not found and looks like a partial ID (contains DOC-), try partial match
                     if (docs.isEmpty() && docId.contains("DOC-")) {
                         String partialId = docId.substring(docId.indexOf("DOC-"));
                         docs = documentRepository.findByUserIdAndInternalId(userId, "id: " + partialId);
                     }
                     
                     String mappedDocId;
                     if (docs.isEmpty()) {
                         System.err.println("WARNING: Could not find document with internal ID: " + docId);
                         mappedDocId = docId;
                     } else {
                         mappedDocId = docs.get(0).getId().toString();
                         System.out.println("Mapped " + docId + " -> " + mappedDocId);
                     }
                     
                     return new AssistantResponse.Citation(mappedDocId, citation.chunk_id(), citation.quote());
                 })
                 .toList();
             
             response = new AssistantResponse(rawResponse.answer_markdown(), mappedCitations, rawResponse.confidence());
        } catch (Exception e) {
            response = new AssistantResponse("Error parsing model response: " + e.getMessage() + "\nRaw: " + content, List.of(), "LOW");
        }
        
        // 7. Save Assistant Message & Retrievals
        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setId(UUID.randomUUID());
        assistantMsg.setChatId(chatId);
        assistantMsg.setUserId(userId);
        assistantMsg.setRole("ASSISTANT");
        assistantMsg.setContent(response.answer_markdown());
        assistantMsg.setCreatedAt(Instant.now());
        messageRepository.save(assistantMsg);
        
        // Save retrievals
        int rank = 0;
        for (RetrievalService.RetrievedChunk chunk : chunks) {
            MessageRetrieval retrieval = new MessageRetrieval(assistantMsg.getId(), chunk.chunkId(), chunk.distance().floatValue(), rank++);
            retrievalRepository.save(retrieval);
        }
        
        return response;
    }
}
