package pl.lukbol.dyplom.controllers;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import pl.lukbol.dyplom.classes.Conversation;
import pl.lukbol.dyplom.classes.Message;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.repositories.ConversationRepository;
import pl.lukbol.dyplom.repositories.MessageRepository;
import pl.lukbol.dyplom.repositories.RoleRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.MessageService;
import pl.lukbol.dyplom.services.UserService;
import pl.lukbol.dyplom.utilities.AuthenticationUtils;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class ChatController {

    @Autowired
    private MessageService messageService;

    private UserRepository userRepository;
    @Autowired
    private UserController userController;

    private MessageRepository messageRepository;

    private ConversationRepository conversationRepository;

    public ChatController(UserRepository userRepository, ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @MessageMapping("/sendToConversation/{conversationId}")
    @SendTo("/topic/employees")
    @Transactional
    public Message sendMessageToClient(@DestinationVariable Long conversationId, Message message) {
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation != null) {
            messageService.sendMessage(message.getSender(), conversation, message.getContent(), message.getMessageDate());

            return message;
        } else {
            System.out.println("Konwersacja o ID " + conversationId + " nie istnieje.");
            return null;
        }
    }

    @MessageMapping("/sendToEmployees")
    @SendTo("/topic/employees")
    @Transactional
    public Message sendMessageToEmployees(Message message) {
        String clientEmail = message.getSender().getEmail();

        User client = userRepository.findByEmail(clientEmail);

        List<Conversation> conversations = conversationRepository.findConversationByClient_Id(client.getId());

        if (conversations == null || conversations.isEmpty()) {
            conversations = new ArrayList<>();


            Conversation conversation = new Conversation();
            conversation.setClient(client);
            conversation.setName(client.getName());
            conversation.setOdczyt(false);
            conversation = conversationRepository.save(conversation);
            conversations.add(conversation);
            client.setConversations(conversations);
            userRepository.save(client);

        }

        for (Conversation conversation : conversations) {
            messageService.sendMessage(message.getSender(), conversation, message.getContent(), message.getMessageDate());
            conversation.getSeenByUserIds().clear();
            conversation.setOdczyt(false);
            conversationRepository.save(conversation);
        }
        return message;
    }

    @GetMapping("/api/conversation")
    public ResponseEntity<List<Message>> getClientConversation(Authentication authentication) {
        User usr = userRepository.findByEmail(AuthenticationUtils.checkmail(authentication.getPrincipal()));

        List<Conversation> conversations = conversationRepository.findConversationByClient_Id(usr.getId());
        if (!conversations.isEmpty()) {
            List<Message> messages = messageRepository.findByConversation(conversations.get(0));
            if (!messages.isEmpty()) {
                return ResponseEntity.ok(messages);
            } else {
                return ResponseEntity.notFound().build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/api/employee/conversations")
    public ResponseEntity<List<Message>> getAllEmployeeConversationMessages(Authentication authentication) {
        // Pobierz użytkownika na podstawie danych z autoryzacji
        User usr = userRepository.findByEmail(AuthenticationUtils.checkmail(authentication.getPrincipal()));

        // Znajdź wszystkie konwersacje, w których uczestniczy użytkownik
        List<Conversation> conversations = conversationRepository.findByParticipants_Id(usr.getId());

        // Lista do agregacji wiadomości ze wszystkich konwersacji
        List<Message> allMessages = new ArrayList<>();

        // Dla każdej konwersacji pobierz wiadomości i dodaj je do listy allMessages
        conversations.forEach(conversation -> {
            List<Message> messages = messageRepository.findByConversation(conversation);
            allMessages.addAll(messages);
        });

        if (!allMessages.isEmpty()) {
            return ResponseEntity.ok(allMessages);
        } else {
            return ResponseEntity.notFound().build();
        }
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
    public ResponseEntity<Map<String, Object>> createConversation(Authentication authentication, @RequestParam("name") String name,
                                                                  @RequestParam("participantIds") String participantIds) {
        User usr = userRepository.findByEmail(AuthenticationUtils.checkmail(authentication.getPrincipal()));
        try {
            List<Long> participant = Arrays.stream(participantIds.split(","))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            List<User> participants = userRepository.findByIdIn(participant);
            participants.add(usr);
            Conversation newConversation = new Conversation(name, participants, new ArrayList<>(), false);
            conversationRepository.save(newConversation);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Konwersacja utworzona pomyślnie.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Błąd podczas tworzenia konwersacji: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/markConversationAsRead/{conversationId}")
    public ResponseEntity<?> markAllMessagesAsRead(Authentication authentication, @PathVariable Long conversationId) {
        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);
        User usr = userRepository.findByEmail(AuthenticationUtils.checkmail(authentication.getPrincipal()));
        if (conversationOptional.isPresent()) {
            Conversation conversation = conversationOptional.get();
            Set<String> users = conversation.getSeenByUserIds();
            users.add(usr.getId().toString());
            conversation.setSeenByUserIds(users);
            conversationRepository.save(conversation);

            return ResponseEntity.ok("Wiadomości zostały oznaczone jako przeczytane.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/clearSeenByUserIds/{conversationId}")
    public ResponseEntity<?> clearSeenByUserIds(@PathVariable Long conversationId) {
        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);

        if (conversationOptional.isPresent()) {
            Conversation conversation = conversationOptional.get();
            conversation.getSeenByUserIds().clear();
            conversationRepository.save(conversation);

            return ResponseEntity.ok("Lista przeczytanych została wyczyszczona.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/checkIfConversationRead/{conversationId}")
    public ResponseEntity<Boolean> checkIfConversationRead(Authentication authentication, @PathVariable Long conversationId) {
        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);
        User user = userRepository.findByEmail(AuthenticationUtils.checkmail(authentication.getPrincipal()));

        if (conversationOptional.isPresent() && user != null) {
            Conversation conversation = conversationOptional.get();
            Set<String> seenByUserIds = conversation.getSeenByUserIds();
            boolean isRead = seenByUserIds.contains(user.getId().toString());
            return ResponseEntity.ok(isRead);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/getConversationParticipants/{conversationId}")
    public ResponseEntity<List<User>> getConversationParticipants(@PathVariable Long conversationId) {
        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);
        if (conversationOptional.isPresent()) {
            Conversation conversation = conversationOptional.get();
            // Tu zakładam, że masz sposób na pobranie uczestników konwersacji z obiektu konwersacji
            List<User> participants = conversation.getParticipants();
            if (participants.isEmpty())
            {
                participants.add(conversation.getClient());
            }
            return ResponseEntity.ok(participants);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/hide/{conversationId}")
    public ResponseEntity<?> hideConversation(@PathVariable Long conversationId) {
        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);

        if (conversationOptional.isPresent()) {
            Conversation conversation = conversationOptional.get();
            if (conversation.isOdczyt())
            {
                conversation.setOdczyt(false);
            }
            else
            {
                conversation.setOdczyt(true);
            }
           // Zakładam, że 'odczyt' to pole w klasie Conversation oznaczające, czy konwersacja została ukryta/przeczytana
            conversationRepository.save(conversation);

            return ResponseEntity.ok().body("Konwersacja została ukryta.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}


