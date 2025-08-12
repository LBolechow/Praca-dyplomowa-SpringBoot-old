package pl.lukbol.dyplom.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import pl.lukbol.dyplom.classes.Price;
import pl.lukbol.dyplom.exceptions.UserNotFoundException;
import pl.lukbol.dyplom.repositories.PriceRepository;
import pl.lukbol.dyplom.services.PriceService;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PriceController {
    private final PriceRepository priceRepository;

    private final PriceService priceService;

    @GetMapping("/prices")
    @ResponseBody
    public ResponseEntity<List<Price>> getAllPrices() {
        return ResponseEntity.ok(priceService.getAllPrices());
    }

    @PostMapping("/add-price")
    public ResponseEntity<Map<String, Object>> addPrice(@RequestParam("item") String item, @RequestParam("price") String price) {
        return ResponseEntity.ok(priceService.addPrice(item, price));
    }

    @DeleteMapping("/delete-price/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        priceService.deleteUser(id);
    }

    @PutMapping("/update-price/{id}")
    public ResponseEntity<?> updatePrice(@PathVariable Long id,
                                         @RequestParam("item") String newItem,
                                         @RequestParam("price") String newPrice) {
        try {
            priceService.updatePrice(id, newItem, newPrice);
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
