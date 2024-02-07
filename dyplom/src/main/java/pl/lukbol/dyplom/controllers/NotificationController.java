package pl.lukbol.dyplom.controllers;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.lukbol.dyplom.classes.Conversation;
import pl.lukbol.dyplom.classes.Notification;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.repositories.NotificationRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.utilities.AuthenticationUtils;

import java.util.*;
import java.util.stream.Collectors;

import static pl.lukbol.dyplom.utilities.AuthenticationUtils.checkmail;

@Controller
public class NotificationController {
    private UserRepository userRepository;

    private NotificationRepository notificationRepository;

    public NotificationController(UserRepository userRepository, NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    @DeleteMapping(value="/removeAlerts", consumes = {"*/*"})
    public ResponseEntity<?> removeAlerts(Authentication authentication) {
        User usr = userRepository.findByEmail(checkmail(authentication.getPrincipal()));
        usr.getNotifications().clear();
        notificationRepository.deleteAllByUserId(usr.getId());
        userRepository.save(usr);

        return ResponseEntity.ok().body("Powiadomienia zostały usunięte.");
    }
    @PostMapping("/create-notification")
    public ResponseEntity<Map<String, Object>> createNotification(Authentication authentication, @RequestParam("text") String message,
                                                                  @RequestParam("participantIds") String participantIds) {

        User usr = userRepository.findByEmail(checkmail(authentication.getPrincipal()));
        try {
            List<Long> participant = Arrays.stream(participantIds.split(","))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            List<User> participants = userRepository.findByIdIn(participant);
            for (User user : participants) {

                List<Notification> a = user.getNotifications();
                a.add(new Notification(message, new Date(), user, usr.getName()));
                user.setNotifications(a);
                userRepository.save(user);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notyfikacja utworzona pomyślnie.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Błąd podczas tworzenia notyfikacji: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
