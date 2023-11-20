package pl.lukbol.dyplom.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import pl.lukbol.dyplom.classes.Material;
import pl.lukbol.dyplom.classes.Order;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.repositories.OrderRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.utilities.AuthenticationUtils;
import pl.lukbol.dyplom.utilities.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

@Controller
public class OrderController {
    private UserRepository userRepository;

    private OrderRepository orderRepository;


    public OrderController(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/currentDate")
    public Map<String, Object> getCurrentDate() {
        Map<String, Object> response = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Date currentDate = new Date();
        String formattedDate = sdf.format(currentDate);

        response.put("currentDate", formattedDate);

        return response;
    }
    @PostMapping(value = "/order/add", consumes = {"application/json"})
    @ResponseBody
    public void addOrder(Authentication authentication, @RequestBody Map<String, Object> request) {
        User usr = userRepository.findByEmail(AuthenticationUtils.checkmail(authentication.getPrincipal()));
        Date convertedDate = DateUtils.currentDate();


        String description = (String) request.get("description");
        String clientName = (String) request.get("clientName");
        String email = (String) request.get("email");
        String phoneNumber = (String) request.get("phoneNumber");

        String endDateString = (String) request.get("endDate");
        Date endDate = null;
        try {
            endDate = DateUtils.parseDate(endDateString, "yyyy-MM-dd");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        int price = Integer.parseInt((String) request.get("price"));
        List<String> items = (List<String>) request.get("items");

        Order newOrder = new Order(description, clientName, email, phoneNumber, usr.getName(), convertedDate, endDate, true, price, null);

        List<Material> materials = new ArrayList<>();
        for (String item : items) {
            Material material = new Material(item, newOrder, false);
            materials.add(material);
        }
        newOrder.setMaterials(materials);

        orderRepository.save(newOrder);

    }
    @GetMapping(value ="/order/getAllOrders")
    public ResponseEntity<List<Order>> getAllOrders() {
        try {
            List<Order> orders = orderRepository.findAll();

            return new ResponseEntity<>(orders, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping(value = "/order/getDailyOrders")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getDailyOrders(@RequestParam(name = "selectedDate", required = false) String selectedDateStr) {
        try {
            LocalDate selectedDate;
            if (selectedDateStr != null && !selectedDateStr.isEmpty()) {
                DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendOptional(DateTimeFormatter.ofPattern("dd.MM.yy"))
                        .appendOptional(DateTimeFormatter.ofPattern("d.MM.yy"))
                        .appendOptional(DateTimeFormatter.ofPattern("dd.M.yy"))
                        .appendOptional(DateTimeFormatter.ofPattern("d.M.yy"))
                        .toFormatter();
                selectedDate = LocalDate.parse(selectedDateStr, formatter);

                // Konwersja LocalDate na Date
                Date endDate = Date.from(selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                System.out.println("endDate: " + endDate);

                List<Order> orders = orderRepository.findByEndDate(endDate);
                List<Map<String, Object>> ordersData = new ArrayList<>();

                for (Order order : orders) {
                    Map<String, Object> orderData = new HashMap<>();
                    orderData.put("description", order.getDescription());
                    orderData.put("employeeName", order.getEmployeeName());
                    orderData.put("status", order.isStatus()); // true = w trakcie, false = zakończone
                    orderData.put("endDate", order.getEndDate()); // dodaj endDate do danych
                    ordersData.add(orderData);
                }

                return new ResponseEntity<>(ordersData, HttpStatus.OK);
            } else {
                selectedDate = LocalDate.now();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Błąd podczas przetwarzania żądania");
            errorResponse.put("message", e.getMessage());
            return new ResponseEntity<>(Collections.singletonList(errorResponse), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK); // Dodana linia zwracająca pustą listę w przypadku braku danych
    }
}
