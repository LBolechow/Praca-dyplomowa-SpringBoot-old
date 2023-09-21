package pl.lukbol.dyplom.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.lukbol.dyplom.classes.Order;


import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long>
{
    List<Order> findAll();
}
