package pl.lukbol.dyplom.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    private static final String SMTP_HOST = "smtp.mailtrap.io";
    private static final int SMTP_PORT = 587;
    private static final String SMTP_USERNAME = "0d63142c9e9d9d";
    private static final String SMTP_PASSWORD = "32607c89b1ff56";

    private static final String PROP_PROTOCOL = "mail.transport.protocol";
    private static final String PROP_SMTP_AUTH = "mail.smtp.auth";
    private static final String PROP_STARTTLS = "mail.smtp.starttls.enable";
    private static final String PROP_DEBUG = "mail.debug";

    private static final String PROTOCOL_VALUE = "smtp";
    private static final String TRUE = "true";

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(SMTP_HOST);
        mailSender.setPort(SMTP_PORT);
        mailSender.setUsername(SMTP_USERNAME);
        mailSender.setPassword(SMTP_PASSWORD);

        Properties props = mailSender.getJavaMailProperties();
        props.put(PROP_PROTOCOL, PROTOCOL_VALUE);
        props.put(PROP_SMTP_AUTH, TRUE);
        props.put(PROP_STARTTLS, TRUE);
        props.put(PROP_DEBUG, TRUE);

        return mailSender;
    }
}
