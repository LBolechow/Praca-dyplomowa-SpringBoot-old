package pl.lukbol.dyplom.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import pl.lukbol.dyplom.classes.Notification;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.exceptions.UserNotFoundException;
import pl.lukbol.dyplom.repositories.NotificationRepository;
import pl.lukbol.dyplom.repositories.UserRepository;

import java.util.Date;
import java.util.List;

@Service
public class NotificationService {

    private UserRepository userRepository;

    private NotificationRepository notificationRepository;

    public NotificationService(UserRepository userRepository, NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void removeAlerts(String userEmail) {
        User usr = userRepository.findByEmail(userEmail);
        if (usr != null) {
            usr.getNotifications().clear();
            notificationRepository.deleteAllByUserId(usr.getId());
            userRepository.save(usr);
        } else {
            throw new UserNotFoundException(usr.getId());
        }
    }
    @Transactional
    public void createNotification(String userEmail, String message, List<Long> participantIds) {
        User sender = userRepository.findByEmail(userEmail);
        List<User> participants = userRepository.findByIdIn(participantIds);

        Date currentDate = new Date();
        for (User participant : participants) {
            Notification notification = new Notification(message, currentDate, participant, sender.getName());
            participant.getNotifications().add(notification);
            userRepository.save(participant);
        }
    }
}
