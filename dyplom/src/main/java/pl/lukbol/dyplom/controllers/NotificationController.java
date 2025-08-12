package pl.lukbol.dyplom.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.repositories.NotificationRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.NotificationService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static pl.lukbol.dyplom.utilities.AuthenticationUtils.checkmail;

@Controller
public class NotificationController {

    private static final String ALERTS_REMOVED_MSG = "Powiadomienia zostały usunięte.";
    private static final String NOTIFICATION_CREATED_SUCCESS_MSG = "Notyfikacja utworzona pomyślnie.";
    private static final String NOTIFICATION_CREATED_ERROR_MSG_PREFIX = "Błąd podczas tworzenia notyfikacji: ";

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public NotificationController(UserRepository userRepository, NotificationRepository notificationRepository, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    @DeleteMapping(value = "/removeAlerts", consumes = {"*/*"})
    public ResponseEntity<String> removeAlerts(Authentication authentication) {
        User usr = userRepository.findByEmail(checkmail(authentication.getPrincipal()));
        notificationService.removeAlerts(usr);
        return ResponseEntity.ok(ALERTS_REMOVED_MSG);
    }

    @PostMapping("/create-notification")
    public ResponseEntity<Map<String, Object>> createNotification(
            Authentication authentication,
            @RequestParam("text") String message,
            @RequestParam("participantIds") String participantIds) {

        String userEmail = checkmail(authentication.getPrincipal());
        Map<String, Object> response = new HashMap<>();

        try {
            List<Long> participantIdsList = Arrays.stream(participantIds.split(","))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            notificationService.createNotification(userEmail, message, participantIdsList);

            response.put("success", true);
            response.put("message", NOTIFICATION_CREATED_SUCCESS_MSG);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", NOTIFICATION_CREATED_ERROR_MSG_PREFIX + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
