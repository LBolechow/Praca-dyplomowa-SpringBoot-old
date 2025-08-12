package pl.lukbol.dyplom.services;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import pl.lukbol.dyplom.classes.Conversation;
import pl.lukbol.dyplom.classes.Message;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.controllers.UserController;
import pl.lukbol.dyplom.repositories.ConversationRepository;
import pl.lukbol.dyplom.repositories.MessageRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.utilities.AuthenticationUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {
    @Autowired
    private MessageService messageService;

    private UserRepository userRepository;
    @Autowired
    private UserController userController;

    private MessageRepository messageRepository;

    private ConversationRepository conversationRepository;

    public ChatService(UserRepository userRepository, ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public Message sendMessageToClient(Conversation conversation, Message message) {
        messageService.sendMessage(message.getSender(), conversation, message.getContent(), message.getMessageDate());
        return message;
    }

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

    public ResponseEntity<List<Message>> getClientConversation(Authentication authentication) {
        User user = userRepository.findByEmail(AuthenticationUtils.checkmail(authentication.getPrincipal()));

        List<Conversation> conversations = conversationRepository.findConversationByClient_Id(user.getId());
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

    public ResponseEntity<List<Message>> getAllEmployeeConversationMessages(Authentication authentication) {
        User user = userRepository.findByEmail(AuthenticationUtils.checkmail(authentication.getPrincipal()));
        List<Conversation> conversations = conversationRepository.findByParticipants_Id(user.getId());

        List<Message> allMessages = new ArrayList<>();
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

    @Transactional
    public ResponseEntity<Map<String, Object>> createConversation(Authentication authentication, String name, String participantIds) {
        User user = userRepository.findByEmail(AuthenticationUtils.checkmail(authentication.getPrincipal()));
        try {
            List<Long> participantsIdsList = Arrays.stream(participantIds.split(","))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            List<User> participants = userRepository.findAllById(participantsIdsList);
            participants.add(user);
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

    @Transactional
    public ResponseEntity<?> markConversationAsRead(Authentication authentication, Long conversationId) {
        User user = userRepository.findByEmail(AuthenticationUtils.checkmail(authentication.getPrincipal()));
        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);

        if (conversationOptional.isPresent()) {
            Conversation conversation = conversationOptional.get();
            Set<String> users = conversation.getSeenByUserIds();
            users.add(user.getId().toString());
            conversation.setSeenByUserIds(users);
            conversationRepository.save(conversation);

            return ResponseEntity.ok("Wiadomości zostały oznaczone jako przeczytane.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Transactional
    public ResponseEntity<?> clearSeenByUserIds(Long conversationId) {
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

    public ResponseEntity<Boolean> checkIfConversationRead(Authentication authentication, Long conversationId) {
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

    public ResponseEntity<List<User>> getConversationParticipants(Long conversationId) {
        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);
        if (conversationOptional.isPresent()) {
            Conversation conversation = conversationOptional.get();
            List<User> participants = new ArrayList<>(conversation.getParticipants());
            if (participants.isEmpty()) {
                participants.add(conversation.getClient());
            }
            return ResponseEntity.ok(participants);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<List<User>> getParticipantsBySeen(Long conversationId) {
        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);
        if (conversationOptional.isPresent()) {
            Conversation conversation = conversationOptional.get();
            List<User> participants = conversation.getParticipants();
            Set<String> seenList = conversation.getSeenByUserIds();
            List<User> seenParticipants = participants.stream()
                    .filter(user -> seenList.contains(user.getId().toString()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(seenParticipants);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Transactional
    public ResponseEntity<?> hideConversation(Long conversationId) {
        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);

        if (conversationOptional.isPresent()) {
            Conversation conversation = conversationOptional.get();
            boolean currentlyRead = conversation.isOdczyt();
            conversation.setOdczyt(!currentlyRead);
            conversationRepository.save(conversation);

            if (currentlyRead) {
                return ResponseEntity.ok("Przywrócono konwersację.");
            } else {
                return ResponseEntity.ok("Ukryto konwersację.");
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
