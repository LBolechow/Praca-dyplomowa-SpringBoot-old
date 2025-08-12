package pl.lukbol.dyplom.classes;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.text.SimpleDateFormat;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "HH:mm | dd.MM")
    private Date messageDate = new Date();

    private String content;

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    @JsonBackReference
    private Conversation conversation;

    public Message(User sender, String content, Conversation conversation, Date messageDate) {
        this.sender = sender;
        this.content = content;
        this.conversation = conversation;
        this.messageDate = messageDate;
    }

    public Message(Long id, User sender, User receiver, String content, Conversation conversation) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.conversation = conversation;
    }

    public String getFormattedMessageDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm | dd.MM");
        return dateFormat.format(this.messageDate);
    }
}