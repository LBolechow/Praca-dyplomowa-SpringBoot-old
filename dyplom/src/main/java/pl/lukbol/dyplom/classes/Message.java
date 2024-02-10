package pl.lukbol.dyplom.classes;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Entity;
import jakarta.persistence.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
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
    private Date messageDate = new Date();;
    private String content;

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    @JsonBackReference
    private Conversation conversation;



    public Message() {
    }



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

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getMessageDate() {
        return messageDate;
    }

    public void setMessageDate(Date messageDate) {
        this.messageDate = messageDate;
    }
    public String getFormattedMessageDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm | dd.MM");
        return dateFormat.format(this.messageDate);
    }
}