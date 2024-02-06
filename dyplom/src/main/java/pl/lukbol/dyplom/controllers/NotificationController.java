package pl.lukbol.dyplom.controllers;

import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.repositories.NotificationRepository;
import pl.lukbol.dyplom.repositories.UserRepository;

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
    public String removeAlerts(Authentication authentication) {
        User usr = userRepository.findByEmail(checkmail(authentication.getPrincipal()));
        usr.getNotifications().clear();
        notificationRepository.deleteAllByUserId(usr.getId());
        userRepository.save(usr);

        return "success";
    }

}
