package pl.lukbol.dyplom.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.lukbol.dyplom.classes.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);

    User deleteUserById(Long id);

    Optional<User> findOptionalByEmail(String email);
    Boolean existsByEmail(String email);

    // @Query(nativeQuery=true,value="drop database")
    // void removedb4fun();


    @EntityGraph(attributePaths = "roles")
    List<User> findAll();
}