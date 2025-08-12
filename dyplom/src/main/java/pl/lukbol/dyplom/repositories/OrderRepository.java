package pl.lukbol.dyplom.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.lukbol.dyplom.classes.Order;

import java.util.Date;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAll();

    List<Order> findByEndDate(Date endDate);

    List<Order> findByStartDateBetweenOrEndDateBetween(Date startDate, Date endDate, Date startDate1, Date endDate1);

    @Query("SELECT o FROM Order o WHERE o.endDate BETWEEN :startDate AND :endDate")
    List<Order> findByEndDateBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.materials m " +
            "WHERE o.startDate BETWEEN :startDate AND :endDate")
    List<Order> findByStartDateBetweenWithMaterials(
            @Param("startDate") Date startDate, @Param("endDate") Date endDate);

    List<Order> findByEmployeeNameAndEndDateAfterAndStartDateBefore(String name, Date taskEndDateTime, Date taskEndDateTime1);

    @Query("SELECT o FROM Order o WHERE o.clientEmail = :userEmail")
    List<Order> findOrdersByUserEmail(@Param("userEmail") String userEmail);

    @Query("SELECT o FROM Order o WHERE o.employeeName = :employeeName AND o.endDate BETWEEN :startDate AND :endDate")
    List<Order> findByEmployeeNameAndEndDateBetween(
            @Param("employeeName") String employeeName,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    @Query("SELECT o FROM Order o WHERE o.employeeName = :employeeName AND o.startDate BETWEEN :startDate AND :endDate")
    List<Order> findByEmployeeNameAndStartDateBetweenWithMaterials(
            @Param("employeeName") String employeeName,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    boolean existsByidCode(String generateActivationCode);

    Order findByIdCode(String idCode);


}
