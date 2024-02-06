package pl.lukbol.dyplom.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.lukbol.dyplom.classes.Price;

public interface PriceRepository extends JpaRepository<Price, Long> {
}
