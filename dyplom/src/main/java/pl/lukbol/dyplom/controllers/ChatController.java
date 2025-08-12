package pl.lukbol.dyplom.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.lukbol.dyplom.classes.Conversation;
import pl.lukbol.dyplom.classes.Message;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.repositories.ConversationRepository;
import pl.lukbol.dyplom.repositories.MessageRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.ChatService;

import java.util.List;
import java.util.Map;

@RestController
public class ChatController {

    private UserRepository userRepository;

    private MessageRepository messageRepository;

    private ConversationRepository conversationRepository;

    private ChatService chatService;

    public ChatController(UserRepository userRepository, ConversationRepository conversationRepository, MessageRepository messageRepository, ChatService chatService) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.chatService = chatService;
    }

    @MessageMapping("/sendToConversation/{conversationId}")
    @SendTo("/topic/employees")
    public Message sendMessageToClient(@DestinationVariable Long conversationId, Message message) {
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation != null) {
            return chatService.sendMessageToClient(conversation, message);
        } else {
            System.out.println("Konwersacja o ID " + conversationId + " nie istnieje.");
            return null;
        }
    }

    @MessageMapping("/sendToEmployees")
    @SendTo("/topic/employees")
    public Message sendMessageToEmployees(Message message) {
        return chatService.sendMessageToEmployees(message);
    }

    @GetMapping("/api/conversation")
    public ResponseEntity<List<Message>> getClientConversation(Authentication authentication) {
        return chatService.getClientConversation(authentication);
    }

    @GetMapping("/api/employee/conversations")
    public ResponseEntity<List<Message>> getAllEmployeeConversationMessages(Authentication authentication) {
        return chatService.getAllEmployeeConversationMessages(authentication);
    }

    @GetMapping("/get_conversations")
    public List<Conversation> getAllConversations() {
        List<Conversation> conversations = conversationRepository.findAll();
        return conversations;
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<Message>> getMessagesForConversation(@PathVariable Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);

        if (conversation != null) {
            List<Message> messages = messageRepository.findByConversation(conversation);
            return ResponseEntity.ok(messages);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/api/conversation/{conversationId}/latest-message")
    public ResponseEntity<Message> getLatestMessageForConversation(@PathVariable Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);

        if (conversation != null) {
            Message latestMessage = messageRepository.findTopByConversationOrderByMessageDateDesc(conversation);

            if (latestMessage != null) {
                return ResponseEntity.ok(latestMessage);
            } else {
                return ResponseEntity.notFound().build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/api/createConversation")
    public ResponseEntity<Map<String, Object>> createConversation(Authentication authentication,
                                                                  @RequestParam("name") String name,
                                                                  @RequestParam("participantIds") String participantIds) {
        return chatService.createConversation(authentication, name, participantIds);
    }

    @PostMapping("/markConversationAsRead/{conversationId}")
    public ResponseEntity<?> markAllMessagesAsRead(Authentication authentication, @PathVariable Long conversationId) {
        return chatService.markConversationAsRead(authentication, conversationId);
    }

    @PutMapping("/clearSeenByUserIds/{conversationId}")
    public ResponseEntity<?> clearSeenByUserIds(@PathVariable Long conversationId) {
        return chatService.clearSeenByUserIds(conversationId);
    }

    @GetMapping("/checkIfConversationRead/{conversationId}")
    public ResponseEntity<Boolean> checkIfConversationRead(Authentication authentication, @PathVariable Long conversationId) {
        return chatService.checkIfConversationRead(authentication, conversationId);
    }

    @GetMapping("/getConversationParticipants/{conversationId}")
    public ResponseEntity<List<User>> getConversationParticipants(@PathVariable Long conversationId) {
        return chatService.getConversationParticipants(conversationId);
    }

    @GetMapping("/checkSeen/{conversationId}")
    public ResponseEntity<List<User>> getParticipantsBySeen(@PathVariable Long conversationId) {
        return chatService.getParticipantsBySeen(conversationId);
    }

    @PostMapping("/hide/{conversationId}")
    public ResponseEntity<?> hideConversation(@PathVariable Long conversationId) {
        return chatService.hideConversation(conversationId);
    }
}


