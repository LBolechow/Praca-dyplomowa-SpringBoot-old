package pl.lukbol.dyplom.classes;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private boolean odczyt;

    @ElementCollection
    private Set<String> seenByUserIds = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "conversation_users",
            joinColumns = @JoinColumn(name = "conversation_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> participants;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Message> messages;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private User client;

    public Conversation(String name, List<User> participants, List<Message> messages, boolean odczyt) {
        this.name = name;
        this.participants = participants;
        this.messages = messages;
        this.odczyt = odczyt;
    }
}


