package pl.lukbol.dyplom.controllers;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pl.lukbol.dyplom.classes.*;
import pl.lukbol.dyplom.exceptions.UserNotFoundException;
import pl.lukbol.dyplom.repositories.MaterialRepository;
import pl.lukbol.dyplom.repositories.OrderRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.OrderService;
import pl.lukbol.dyplom.utilities.AuthenticationUtils;
import pl.lukbol.dyplom.utilities.DateUtils;
import pl.lukbol.dyplom.utilities.GenerateCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class OrderController {
    private UserRepository userRepository;

    private OrderRepository orderRepository;

    private MaterialRepository materialRepository;

    private OrderService orderService;


    public OrderController(UserRepository userRepository, OrderRepository orderRepository, MaterialRepository materialRepository, OrderService orderService) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.materialRepository = materialRepository;
        this.orderService = orderService;

    }

    @GetMapping("/currentDate")
    public Map<String, Object> getCurrentDate() {
        return orderService.getCurrentDate();
    }


    @GetMapping("/index")
    public String showUserOrders(Authentication authentication, Model model) {
        String userEmail = AuthenticationUtils.checkmail(authentication.getPrincipal());
        List<Order> userOrders = orderService.getUserOrders(userEmail);

        if (userOrders != null) {
            model.addAttribute("userOrders", userOrders);
        }

        return "index";
    }
    @GetMapping(value = "/order/getDailyOrders")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getDailyOrders(
            Authentication authentication,
            @RequestParam(name = "start", required = false) String start,
            @RequestParam(name = "end", required = false) String end
    ) {
        try {
            String userEmail = AuthenticationUtils.checkmail(authentication.getPrincipal());
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));

            List<Map<String, Object>> ordersData = orderService.getDailyOrders(start, end, userEmail, isAdmin);
            return new ResponseEntity<>(ordersData, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Błąd podczas przetwarzania żądania");
            errorResponse.put("message", e.getMessage());
            return new ResponseEntity<>(Collections.singletonList(errorResponse), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/order/add", consumes = {"application/json"})
    public ResponseEntity<Order> addOrder(Authentication authentication, @RequestBody Map<String, Object> request) {
        Order newOrder = orderService.addOrder(request);
        return ResponseEntity.ok(newOrder);
    }
    @GetMapping("/order/getOrderDetails/{id}")
    public ResponseEntity<Map<String, Object>> getOrderDetails(@PathVariable Long id) {
        Map<String, Object> orderDetails = orderService.getOrderDetails(id);

        if (orderDetails != null) {
            return new ResponseEntity<>(orderDetails, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    @Transactional
    @PostMapping(value = "/order/edit/{id}", consumes = {"application/json"})
    @ResponseBody
    public void editOrder(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        orderService.editOrder(id, request);
    }

    @GetMapping("/order/checkAvailability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @RequestParam double durationHours,
            @RequestParam(required = false, defaultValue = "8") int startHour) {
        return orderService.checkAvailability(durationHours, startHour);
    }

    @GetMapping("/order/checkAvailabilityNextDay/{orderId}")
    public ResponseEntity<Map<String, Object>> checkAvailabilityNextDay(
            @PathVariable Long orderId,
            @RequestParam double durationHours) {

        return orderService.checkAvailabilityNextDay(orderId, durationHours);
    }
    @GetMapping("/order/otherEmployee/{orderId}")
    public ResponseEntity<Map<String, Object>> checkAvailabilityOtherEmployee(
            @PathVariable Long orderId,
            @RequestParam double durationHours) {
        return ResponseEntity.ok(orderService.checkAvailabilityOtherEmployee(orderId, durationHours));
    }


    @DeleteMapping("/order/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable Long id) {

        orderService.deleteOrder(id);
    }
    @GetMapping("/order/search")
    public ResponseEntity<List<Order>> searchOrdersByStartDateBetweenWithMaterials(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication) {

        List<Order> filteredOrders = orderService.searchOrdersByStartDateBetweenWithMaterials(fromDate, toDate, authentication);

        return ResponseEntity.ok(filteredOrders);
    }

    @PatchMapping("/material/{materialId}")
    public ResponseEntity<Void> updateMaterialCheckedState(
            @PathVariable Long materialId,
            @RequestBody Map<String, Boolean> requestBody) {

        boolean checked = requestBody.getOrDefault("checked", false);

        orderService.updateMaterialCheckedState(materialId, checked);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/order/checkOrder/{idCode}")
    public ResponseEntity<Order> getOrderDetails(@PathVariable String idCode) {
        Order order = orderService.getOrderDetails(idCode);

        if (order != null) {
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


}
