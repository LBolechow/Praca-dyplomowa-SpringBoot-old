package pl.lukbol.dyplom.controllers;

import jakarta.transaction.Transactional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pl.lukbol.dyplom.classes.Material;
import pl.lukbol.dyplom.classes.Order;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.exceptions.UserNotFoundException;
import pl.lukbol.dyplom.repositories.MaterialRepository;
import pl.lukbol.dyplom.repositories.OrderRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.utilities.AuthenticationUtils;
import pl.lukbol.dyplom.utilities.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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


    public OrderController(UserRepository userRepository, OrderRepository orderRepository, MaterialRepository materialRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.materialRepository = materialRepository;
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



    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }


    private String formatTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return sdf.format(date);
    }


        private boolean isWorkingDay(Calendar calendar) {
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            return dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY;
        }

    @GetMapping("/index")
    public String showUserOrders(Authentication authentication, Model model) {
        String userEmail = AuthenticationUtils.checkmail(authentication.getPrincipal());
        User user = userRepository.findByEmail(userEmail);

        if (user != null) {
            List<Order> userOrders = orderRepository.findOrdersByUserEmail(user.getEmail());

            model.addAttribute("userOrders", userOrders);
        }

        return "index";
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
    public ResponseEntity<List<Map<String, Object>>> getDailyOrders(
            Authentication authentication,
            @RequestParam(name = "selectedDate", required = false) String selectedDateStr
    ) {
        try {
            LocalDate selectedDate;

            if (selectedDateStr != null && !selectedDateStr.isEmpty()) {
                DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendOptional(DateTimeFormatter.ofPattern("dd.MM.yyYY"))
                        .appendOptional(DateTimeFormatter.ofPattern("d.MM.yy"))
                        .appendOptional(DateTimeFormatter.ofPattern("dd.M.yy"))
                        .appendOptional(DateTimeFormatter.ofPattern("d.M.yy"))
                        .toFormatter();
                selectedDate = LocalDate.parse(selectedDateStr, formatter);
            } else {
                selectedDate = LocalDate.now();
            }

            LocalDateTime startOfDay = selectedDate.atStartOfDay();
            LocalDateTime endOfDay = selectedDate.atTime(23, 59, 59);

            List<Order> orders;

            String userEmail = AuthenticationUtils.checkmail(authentication.getPrincipal());
            User user = userRepository.findByEmail(userEmail);

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));

            if (isAdmin) {

                orders = orderRepository.findByEndDateBetween(
                        Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant()),
                        Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant())
                );
            } else {

                String username = user.getName();
                orders = orderRepository.findByEmployeeNameAndEndDateBetween(
                        username,
                        Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant()),
                        Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant())
                );

            }

            List<Map<String, Object>> ordersData = new ArrayList<>();

            for (Map<String, Object> order : ordersData) {
                System.out.println("Order:");
                for (Map.Entry<String, Object> entry : order.entrySet()) {
                    System.out.println(entry.getKey() + ": " + entry.getValue());
                }
                System.out.println("-------------------------");
            }

            for (Order order : orders) {
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("id", order.getId());
                orderData.put("description", order.getDescription());
                orderData.put("clientName", order.getClientName());
                orderData.put("employeeName", order.getEmployeeName());
                orderData.put("status", order.isStatus());
                orderData.put("startDate", order.getStartDate());
                orderData.put("endDate", order.getEndDate());
                ordersData.add(orderData);
            }

            return new ResponseEntity<>(ordersData, HttpStatus.OK);
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Błąd parsowania daty");
            errorResponse.put("message", e.getMessage());
            return new ResponseEntity<>(Collections.singletonList(errorResponse), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Błąd podczas przetwarzania żądania");
            errorResponse.put("message", e.getMessage());
            return new ResponseEntity<>(Collections.singletonList(errorResponse), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PostMapping(value = "/order/add", consumes = {"application/json"})
    @ResponseBody
    public void addOrder(Authentication authentication, @RequestBody Map<String, Object> request) {
        String selectedUser = (String) request.get("selectedUser");
        List<User> usr = userRepository.findByNameContainingIgnoreCase(selectedUser);
        User user = usr.get(0);
        Date convertedDate = DateUtils.currentDate();

        String description = (String) request.get("description");
        String clientName = (String) request.get("clientName");
        String email = (String) request.get("email");



        String phoneNumber = (String) request.get("phoneNumber");

        String startDateString = (String) request.get("startDate");
        Date startDate = parseDateString(startDateString);


        String endDateString = (String) request.get("endDate");
        Date endDate = parseDateString(endDateString);


        String hour = (String) request.get("hours");
        double hours =  Double.parseDouble(hour);

        List<String> items = (List<String>) request.get("items");



        int price = Integer.parseInt((String) request.get("price"));


        Order newOrder = new Order(description, clientName, email, phoneNumber, usr.get(0).getName(), startDate, endDate, true, price, hours, null, user);

        List<Material> materials = new ArrayList<>();
        for (String item : items) {
            Material material = new Material(item, newOrder, false);
            materials.add(material);
        }
        newOrder.setMaterials(materials);

        orderRepository.save(newOrder);
    }
    @GetMapping("/order/getOrderDetails/{id}")
    public ResponseEntity<Map<String, Object>> getOrderDetails(@PathVariable Long id) {
        try {
            Optional<Order> optionalOrder = orderRepository.findById(id);

            if (optionalOrder.isPresent()) {
                Order order = optionalOrder.get();

                Map<String, Object> orderDetails = new HashMap<>();
                orderDetails.put("id", order.getId());
                orderDetails.put("description", order.getDescription());
                orderDetails.put("clientName", order.getClientName());
                orderDetails.put("email", order.getClientEmail());
                orderDetails.put("phoneNumber", order.getPhoneNumber());
                orderDetails.put("startDate", formatDate(order.getStartDate()));
                orderDetails.put("startTime", formatTime(order.getStartDate()));
                orderDetails.put("endDate", formatDate(order.getEndDate()));
                orderDetails.put("endTime", formatTime(order.getEndDate()));
                orderDetails.put("employee", order.getUser().getName());
                orderDetails.put("hours", order.getDuration());
                orderDetails.put("price", order.getPrice());
                orderDetails.put("materials", order.getMaterials());
                orderDetails.put("status", order.isStatus());


                return new ResponseEntity<>(orderDetails, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    private Date parseDateString(String dateString) {
        Date date = null;
        try {
            date = DateUtils.parseDate(dateString, "yyyy-MM-dd HH:mm:ss");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }
    @Transactional
    @PostMapping(value = "/order/edit/{id}", consumes = {"application/json"})
    @ResponseBody
    public void editOrder(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Optional<Order> optionalOrder = orderRepository.findById(id);

        if (optionalOrder.isPresent()) {
            Order order = optionalOrder.get();
            materialRepository.deleteAllByOrder(order);

            String selectedUser = (String) request.get("selectedUser");
            List<User> users = userRepository.findByNameContainingIgnoreCase(selectedUser);
            User user = users.isEmpty() ? null : users.get(0);
            order.setEmployeeName(user.getName());

            order.setDescription((String) request.get("description"));
            order.setClientName((String) request.get("clientName"));
            order.setClientEmail((String) request.get("email"));
            order.setPhoneNumber((String) request.get("phoneNumber"));

            String startDateString = (String) request.get("startDate");
            Date startDate = parseDateString(startDateString);
            order.setStartDate(startDate);

            String endDateString = (String) request.get("endDate");
            Date endDate = parseDateString(endDateString);
            order.setEndDate(endDate);

            double hours = Double.parseDouble((String) request.get("hours"));
            order.setDuration(hours);

            int price = Integer.parseInt((String) request.get("price"));
            order.setPrice(price);

            Boolean status = (Boolean) request.get("status");
            order.setStatus(status);

            List<String> items = (List<String>) request.get("items");
            List<Material> newMaterials = new ArrayList<>();
            for (String item : items) {
                Material material = new Material(item, order, false);
                newMaterials.add(material);
            }
            order.setMaterials(newMaterials);

            orderRepository.save(order);
        }
    }
    @GetMapping("/order/checkAvailabilityNextDay/{orderId}")
    public ResponseEntity<Map<String, Object>> checkAvailabilityNextDay(@PathVariable Long orderId, @RequestParam double durationHours) {
        try {
            Map<String, Object> response = new HashMap<>();

            Optional<Order> optionalOrder = orderRepository.findById(orderId);

            if (optionalOrder.isPresent()) {
                Order order = optionalOrder.get();

                Date orderEndDate = order.getEndDate();
                Calendar currentDateTime = Calendar.getInstance();
                currentDateTime.setTime(orderEndDate);

                if (currentDateTime.get(Calendar.HOUR_OF_DAY) >= 16) {
                    currentDateTime.add(Calendar.DAY_OF_MONTH, 1);
                    currentDateTime.set(Calendar.HOUR_OF_DAY, 8);
                    currentDateTime.set(Calendar.MINUTE, 0);
                }

                while (true) {
                    if (isWorkingDay(currentDateTime)) {
                        Calendar endDateTime = (Calendar) currentDateTime.clone();
                        int durationMinutes = (int) (durationHours * 60);
                        endDateTime.add(Calendar.MINUTE, durationMinutes);

                        if (endDateTime.get(Calendar.HOUR_OF_DAY) >= 16) {
                            currentDateTime.add(Calendar.DAY_OF_MONTH, 1);
                            currentDateTime.set(Calendar.HOUR_OF_DAY, 8);
                            currentDateTime.set(Calendar.MINUTE, 0);
                            continue;
                        }

                        List<User> availableUsers = findAvailableUsersWithEndDateTime(currentDateTime.getTime(), endDateTime.getTime(), durationMinutes);

                        if (!availableUsers.isEmpty()) {
                            User suggestedUser = availableUsers.get(0);

                            response.put("status", "success");
                            response.put("startDate", formatDate(currentDateTime.getTime()));
                            response.put("startTime", formatTime(currentDateTime.getTime()));
                            response.put("endDate", formatDate(endDateTime.getTime()));
                            response.put("endTime", formatTime(endDateTime.getTime()));
                            response.put("suggestedUser", suggestedUser.getName());
                            response.put("durationMinutes", durationMinutes);

                            return new ResponseEntity<>(response, HttpStatus.OK);
                        }
                    }

                    currentDateTime.add(Calendar.DAY_OF_MONTH, 1);
                    currentDateTime.set(Calendar.HOUR_OF_DAY, 8);
                    currentDateTime.set(Calendar.MINUTE, 0);
                }
            } else {
                response.put("status", "error");
                response.put("message", "Zamówienie o podanym identyfikatorze nie zostało znalezione.");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/order/checkAvailability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @RequestParam double durationHours,
            @RequestParam(required = false, defaultValue = "8") int startHour) {

        try {
            Map<String, Object> response = new HashMap<>();

            Calendar currentDateTime = Calendar.getInstance();

            if (currentDateTime.get(Calendar.HOUR_OF_DAY) > startHour ||
                    (currentDateTime.get(Calendar.HOUR_OF_DAY) == startHour && currentDateTime.get(Calendar.MINUTE) > 0)) {
                currentDateTime.add(Calendar.DAY_OF_MONTH, 1);  // Przesunięcie na następny dzień
            }

            currentDateTime.set(Calendar.HOUR_OF_DAY, startHour);
            currentDateTime.set(Calendar.MINUTE, 0);
            currentDateTime.set(Calendar.SECOND, 0);

            while (currentDateTime.get(Calendar.HOUR_OF_DAY) < 16) {
                if (isWorkingDay(currentDateTime)) {
                    Calendar endDateTime = (Calendar) currentDateTime.clone();
                    int durationMinutes = (int) (durationHours * 60);
                    endDateTime.add(Calendar.MINUTE, durationMinutes);

                    if (endDateTime.get(Calendar.HOUR_OF_DAY) > 16) {
                        endDateTime.set(Calendar.HOUR_OF_DAY, 16);
                        endDateTime.set(Calendar.MINUTE, 0);
                    }

                    if (endDateTime.after(Calendar.getInstance())) {
                        List<User> availableUsers = findAvailableUsersWithEndDateTime(currentDateTime.getTime(), endDateTime.getTime(), durationMinutes);

                        if (!availableUsers.isEmpty()) {
                            User suggestedUser = availableUsers.get(0);

                            response.put("status", "success");
                            response.put("startDate", formatDate(currentDateTime.getTime()));
                            response.put("startTime", formatTime(currentDateTime.getTime()));
                            response.put("endDate", formatDate(endDateTime.getTime()));
                            response.put("endTime", formatTime(endDateTime.getTime()));
                            response.put("suggestedUser", suggestedUser.getName());
                            response.put("durationMinutes", durationMinutes);

                            return new ResponseEntity<>(response, HttpStatus.OK);
                        }
                    }
                }

                currentDateTime.set(Calendar.HOUR_OF_DAY, startHour);
                currentDateTime.add(Calendar.DAY_OF_MONTH, 1);
            }

            response.put("status", "error");
            response.put("message", "Brak dostępnych pracowników w ramach dni roboczych.");

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<User> findAvailableUsersWithEndDateTime(Date taskStartDateTime, Date taskEndDateTime, int durationMinutes) {
        List<String> roleNamesToSearch = Arrays.asList("ROLE_EMPLOYEE", "ROLE_ADMIN");

        List<User> availableUsers = new ArrayList<>();

        List<User> allUsers = userRepository.findAll();  // Zakładam, że masz metodę findAll w userRepository

        for (User user : allUsers) {
            boolean isAvailable = true;

            Collection<Role> userRoles = user.getRoles();
            List<String> userRoleNames = userRoles.stream()
                    .map(Role::getName)
                    .collect(Collectors.toList());

            if (userRoleNames.stream().anyMatch(roleNamesToSearch::contains)) {

                List<Order> userOrders = orderRepository.findByEmployeeNameAndEndDateAfterAndStartDateBefore(user.getName(), taskStartDateTime, taskEndDateTime);

                for (Order order : userOrders) {
                    Date orderStartDate = order.getStartDate();
                    Date orderEndDate = order.getEndDate();

                    if ((taskEndDateTime.after(orderStartDate) && taskEndDateTime.before(orderEndDate)) ||
                            (taskStartDateTime.after(orderStartDate) && taskStartDateTime.before(orderEndDate)) ||
                            (taskStartDateTime.before(orderStartDate) && taskEndDateTime.after(orderEndDate))) {
                        isAvailable = false;
                        break;
                    }
                }

                if (isAvailable) {
                    availableUsers.add(user);
                }
            }
        }

        return availableUsers;
    }
    @DeleteMapping("/order/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable Long id) {

        Optional<Order> orderOptional = orderRepository.findById(id);

        if (orderOptional.isPresent()) {
            orderRepository.delete(orderOptional.get());
        } else {
            throw new UserNotFoundException(id);
        }
    }
    @GetMapping("/order/search")
    public ResponseEntity<List<Order>> searchOrdersByStartDateBetweenWithMaterials(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));

        LocalDateTime startDateTime = LocalDateTime.of(fromDate, LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(toDate, LocalTime.MAX);

        Date startDate = Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant());

        List<Order> matchingOrders;
        String userEmail = AuthenticationUtils.checkmail(authentication.getPrincipal());
        User user = userRepository.findByEmail(userEmail);

        if (isAdmin) {

            matchingOrders = orderRepository.findByStartDateBetweenWithMaterials(startDate, endDate);
        } else {

            String employeeName = user.getName();
            matchingOrders = orderRepository.findByEmployeeNameAndStartDateBetweenWithMaterials(employeeName, startDate, endDate);
        }

        return ResponseEntity.ok(matchingOrders);
    }

    @PatchMapping("/material/{materialId}")
    public ResponseEntity<Void> updateMaterialCheckedState(
            @PathVariable Long materialId,
            @RequestBody Map<String, Boolean> requestBody) {

        boolean checked = requestBody.getOrDefault("checked", false);

        Optional<Material> optionalMaterial = materialRepository.findById(materialId);

        if (optionalMaterial.isPresent()) {
            Material material = optionalMaterial.get();
            material.setChecked(checked);

            materialRepository.save(material);

            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
