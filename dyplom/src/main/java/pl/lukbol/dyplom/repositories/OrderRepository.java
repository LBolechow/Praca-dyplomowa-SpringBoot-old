package pl.lukbol.dyplom.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.lukbol.dyplom.classes.Order;


import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long>
{
    List<Order> findAll();

    List<Order> findByEndDate(Date endDate);
}
