package com.notebooklm.controller;

import com.notebooklm.model.Chat;
import com.notebooklm.model.ChatMessage;
import com.notebooklm.service.ChatService;
import com.notebooklm.service.dto.AssistantResponse;
import com.notebooklm.util.UserContext;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chats")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public Chat createChat(@RequestBody(required = false) CreateChatRequest req) {
        String title = (req != null) ? req.title() : null;
        return chatService.createChat(UserContext.getCurrentUser().getId(), title);
    }
    
    @GetMapping
    public List<Chat> list() {
        return chatService.listChats(UserContext.getCurrentUser().getId());
    }
    
    @GetMapping("/{chatId}")
    public List<ChatMessage> getMessages(@PathVariable UUID chatId) {
        return chatService.getMessages(chatId);
    }
    
    @PostMapping("/{chatId}/messages")
    public AssistantResponse sendMessage(@PathVariable UUID chatId, @RequestBody SendMessageRequest req) {
        return chatService.answer(chatId, UserContext.getCurrentUser().getId(), req.message());
    }
    
    public record CreateChatRequest(String title) {}
    public record SendMessageRequest(String message) {}
}
