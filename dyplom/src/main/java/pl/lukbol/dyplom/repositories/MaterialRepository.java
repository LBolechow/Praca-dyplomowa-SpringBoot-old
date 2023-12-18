package pl.lukbol.dyplom.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.lukbol.dyplom.classes.Material;
import pl.lukbol.dyplom.classes.Order;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findAll();

    void deleteAllByOrder(Order order);

    Optional<Material> findById(Long materialsId);
}
