package pl.lukbol.dyplom.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.lukbol.dyplom.classes.Conversation;
import pl.lukbol.dyplom.classes.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversation(Conversation conversation);

    Message findTopByConversationOrderByMessageDateDesc(Conversation conversation);


    void deleteBySenderId(Long id);

    List<Message> findByConversationId(Long conversationId);
}
