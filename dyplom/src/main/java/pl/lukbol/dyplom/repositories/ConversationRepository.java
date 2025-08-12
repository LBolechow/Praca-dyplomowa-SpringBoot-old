package pl.lukbol.dyplom.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.lukbol.dyplom.classes.Conversation;
import pl.lukbol.dyplom.classes.User;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Conversation findByClient(User client);

    List<Conversation> findConversationByClient_Id(Long clientId);

    List<Conversation> findByParticipants_Id(Long userId);


}
