package pl.lukbol.dyplom.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.lukbol.dyplom.classes.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    public void deleteAllByUserId(Long userId);
}
