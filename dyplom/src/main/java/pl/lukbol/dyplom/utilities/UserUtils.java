package pl.lukbol.dyplom.utilities;

import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.repositories.UserRepository;

@Component
public class UserUtils {

    private UserRepository userRepository;

    private final JavaMailSender mailSender;

    public UserUtils(UserRepository userRepository, JavaMailSender mailSender)
    {
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }
    public void sendResetEmail(String to, String newPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject("Twoje nowe hasło");
        message.setText("Twoje nowe hasło to: " + newPassword);
        message.setTo(to);
        mailSender.send(message);
    }
    public boolean emailExists(String email) {
        return userRepository.findByEmail(email) != null;
    }
}
