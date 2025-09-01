package pl.lukbol.dyplom.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.lukbol.dyplom.classes.Price;
import pl.lukbol.dyplom.exceptions.UserNotFoundException;
import pl.lukbol.dyplom.repositories.PriceRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
@Service
public class PriceService {

    private PriceRepository priceRepository;

    public PriceService(PriceRepository priceRepository) {
        this.priceRepository = priceRepository;

    }
    public List<Price> getAllPrices() {
        List<Price> prices = priceRepository.findAll();
        return prices;
    }
    @Transactional
    public Map<String, Object> addPrice(String item, String price) {
        Price newPrice = new Price(item, price);
        priceRepository.save(newPrice);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Poprawnie utworzono użytkownika.");
        return response;
    }
    @Transactional
    public void deleteUser(Long id) {

        Optional<Price> priceOptional = priceRepository.findById(id);

        if (priceOptional.isPresent()) {
            priceRepository.delete(priceOptional.get());
        } else {
            throw new UserNotFoundException(id);
        }
    }
    @Transactional
    public void updatePrice(Long id, String newItem, String newPrice) {
        Price price = priceRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        price.setItem(newItem);
        price.setPrice(newPrice);
        priceRepository.save(price);
    }
}
