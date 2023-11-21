package pl.lukbol.dyplom.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.lukbol.dyplom.classes.Order;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long>
{
    List<Order> findAll();

    List<Order> findByEndDate(Date endDate);

    List<Order> findByStartDateBetweenOrEndDateBetween(Date startDate, Date endDate, Date startDate1, Date endDate1);

    @Query("SELECT o FROM Order o WHERE o.endDate BETWEEN :startDate AND :endDate")
    List<Order> findByEndDateBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
