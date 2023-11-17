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
}
