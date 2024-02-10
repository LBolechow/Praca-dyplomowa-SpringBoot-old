package pl.lukbol.dyplom.classes;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private boolean odczyt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ElementCollection
    private Set<String> seenByUserIds = new HashSet<>();

    public Set<String> getSeenByUserIds() {
        return seenByUserIds;
    }

    public void setSeenByUserIds(Set<String> seenByUserIds) {
        this.seenByUserIds = seenByUserIds;
    }
    @ManyToMany
    @JoinTable(
            name = "conversation_users",
            joinColumns = @JoinColumn(name = "conversation_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> participants;

    public List<User> getParticipants() {
        return participants;
    }

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Message> messages;

    public void setParticipants(List<User> participants) {
        this.participants = participants;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    @ManyToOne
    @JoinColumn(name = "client_id")
    private User client;

    public User getClient() {
        return client;
    }

    public void setClient(User client) {
        this.client = client;
    }


    public Conversation() {
    }

    public Conversation(String name, List<User> participants, List<Message> messages, boolean odczyt) {
        this.name = name;
        this.participants = participants;
        this.messages = messages;
        this.odczyt = odczyt;
    }

    public boolean isOdczyt() {
        return odczyt;
    }

    public void setOdczyt(boolean odczyt) {
        this.odczyt = odczyt;
    }

}
