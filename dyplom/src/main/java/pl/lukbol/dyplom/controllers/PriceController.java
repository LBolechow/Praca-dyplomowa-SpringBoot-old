package pl.lukbol.dyplom.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pl.lukbol.dyplom.classes.Price;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.exceptions.UserNotFoundException;
import pl.lukbol.dyplom.repositories.ConversationRepository;
import pl.lukbol.dyplom.repositories.MessageRepository;
import pl.lukbol.dyplom.repositories.PriceRepository;
import pl.lukbol.dyplom.repositories.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class PriceController {
    private PriceRepository priceRepository;

    public PriceController(PriceRepository priceRepository) {
        this.priceRepository = priceRepository;

    }
    @GetMapping("/prices")
    @ResponseBody
    public ResponseEntity<List<Price>> getAllPrices() {
        List<Price> prices = priceRepository.findAll();
        return ResponseEntity.ok(prices);
    }
    @PostMapping("/add-price")
    public ResponseEntity<Map<String, Object>> addPrice(@RequestParam("item") String item, @RequestParam("price") String price) {
        Price newPrice = new Price(item, price);
        priceRepository.save(newPrice);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Poprawnie utworzono użytkownika.");
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/delete-price/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {

        Optional<Price> priceOptional = priceRepository.findById(id);

        if (priceOptional.isPresent()) {
            priceRepository.delete(priceOptional.get());
        } else {
            throw new UserNotFoundException(id);
        }
    }
    @PutMapping("/update-price/{id}")
    public ResponseEntity<?> updatePrice(@PathVariable Long id,
                                         @RequestParam("item") String newItem,
                                         @RequestParam("price") String newPrice) {

        return priceRepository.findById(id)
                .map(price -> {
                    price.setItem(newItem);
                    price.setPrice(newPrice);
                    priceRepository.save(price);
                    return ResponseEntity.ok().build();
                })
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
