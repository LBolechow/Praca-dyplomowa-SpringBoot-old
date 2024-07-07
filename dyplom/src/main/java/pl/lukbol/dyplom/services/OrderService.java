package pl.lukbol.dyplom.services;

import jakarta.transaction.Transactional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import pl.lukbol.dyplom.classes.*;
import pl.lukbol.dyplom.exceptions.UserNotFoundException;
import pl.lukbol.dyplom.repositories.MaterialRepository;
import pl.lukbol.dyplom.repositories.OrderRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.utilities.AuthenticationUtils;
import pl.lukbol.dyplom.utilities.DateUtils;
import pl.lukbol.dyplom.utilities.GenerateCode;
import pl.lukbol.dyplom.utilities.OrderUtils;


import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private UserRepository userRepository;

    private OrderRepository orderRepository;

    private MaterialRepository materialRepository;

    private OrderUtils orderUtils;

    public OrderService(UserRepository userRepository, OrderRepository orderRepository, MaterialRepository materialRepository, OrderUtils orderUtils) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.materialRepository = materialRepository;
        this.orderUtils = orderUtils;
    }
    public Date parseDateString(String dateString) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            return formatter.parse(dateString);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public Order addOrder(Map<String, Object> request) {
        String selectedUser = (String) request.get("selectedUser");
        List<User> usr = userRepository.findByNameContainingIgnoreCase(selectedUser);
        User user = usr.get(0);
        Date convertedDate = DateUtils.currentDate();

        String description = (String) request.get("description");
        String clientName = (String) request.get("clientName");
        String email = (String) request.get("email");
        String phoneNumber = (String) request.get("phoneNumber");
        String startDateString = (String) request.get("startDate");
        Date startDate = orderUtils.parseDateString(startDateString);
        String endDateString = (String) request.get("endDate");
        Date endDate = orderUtils.parseDateString(endDateString);
        String hour = (String) request.get("hours");
        double hours = Double.parseDouble(hour);
        List<String> items = (List<String>) request.get("items");
        int price = Integer.parseInt((String) request.get("price"));

        String generatedIdCode = GenerateCode.generateActivationCode();

        List<Notification> notifications = user.getNotifications();
        notifications.add(new Notification("Pojawiło się nowe zlecenie!", new Date(), user, "System"));
        user.setNotifications(notifications);

        userRepository.save(user);

        Order newOrder = new Order(description, clientName, email, phoneNumber, user.getName(), startDate, endDate, "W trakcie", price, hours, null, generatedIdCode);

        List<Material> materials = new ArrayList<>();
        for (String item : items) {
            Material material = new Material(item, newOrder, false);
            materials.add(material);
        }
        newOrder.setMaterials(materials);

        return orderRepository.save(newOrder);
    }



    public List<Map<String, Object>> getDailyOrders(String start, String end, String userEmail, boolean isAdmin) {
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        if (start != null && end != null) {
            try {
                startDateTime = LocalDateTime.parse(start, DateTimeFormatter.ISO_DATE_TIME);
                endDateTime = LocalDateTime.parse(end, DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException e) {
                startDateTime = LocalDate.parse(start).atStartOfDay();
                endDateTime = LocalDate.parse(end).atTime(23, 59, 59);
            }
        } else {
            startDateTime = LocalDateTime.now().with(LocalTime.of(0, 0, 0));
            endDateTime = LocalDateTime.now().with(LocalTime.of(23, 59, 59));
        }

        ZoneId zoneId = ZoneId.of("Europe/Warsaw");
        ZonedDateTime zonedStartDateTime = startDateTime.atZone(zoneId);
        ZonedDateTime zonedEndDateTime = endDateTime.atZone(zoneId);

        List<Order> orders;
        User user = userRepository.findByEmail(userEmail);

        if (isAdmin) {
            orders = orderRepository.findByEndDateBetween(
                    Date.from(zonedStartDateTime.toInstant()),
                    Date.from(zonedEndDateTime.toInstant())
            );
        } else {
            String username = user.getName();
            orders = orderRepository.findByEmployeeNameAndEndDateBetween(
                    username,
                    Date.from(zonedStartDateTime.toInstant()),
                    Date.from(zonedEndDateTime.toInstant())
            );
        }

        List<Map<String, Object>> ordersData = new ArrayList<>();

        for (Order order : orders) {
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("id", order.getId());
            orderData.put("title", order.getDescription());

            // Convert the start and end times to ZonedDateTime
            ZonedDateTime startZonedDateTime = order.getStartDate().toInstant().atZone(zoneId);
            ZonedDateTime endZonedDateTime = order.getEndDate().toInstant().atZone(zoneId);

            // Manually adjust for DST
            if (startZonedDateTime.toLocalDateTime().isBefore(LocalDate.of(startZonedDateTime.getYear(), 3, 31).atTime(2, 0)) ||
                    startZonedDateTime.toLocalDateTime().isAfter(LocalDate.of(startZonedDateTime.getYear(), 10, 30).atTime(2, 0))) {
                // Winter time (Standard Time)
                startZonedDateTime = startZonedDateTime.plusSeconds(3600);
                endZonedDateTime = endZonedDateTime.plusSeconds(3600);
            } else {
                // Summer time (Daylight Saving Time)
                startZonedDateTime = startZonedDateTime.plusSeconds(7200);
                endZonedDateTime = endZonedDateTime.plusSeconds(7200);
            }

            orderData.put("start", startZonedDateTime.toEpochSecond() * 1000);
            orderData.put("end", endZonedDateTime.toEpochSecond() * 1000);
            orderData.put("clientName", order.getClientName());
            orderData.put("employeeName", order.getEmployeeName());
            orderData.put("status", order.getStatus());
            ordersData.add(orderData);
        }

        return ordersData;
    }

    public List<Order> getUserOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail);

        if (user != null) {
            List<Order> userOrders = orderRepository.findOrdersByUserEmail(user.getEmail());
            userOrders.sort(Comparator.comparing(Order::getEndDate).reversed());
            return userOrders;
        }

        return null;
    }

    public Map<String, Object> getCurrentDate() {
        Map<String, Object> response = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Date currentDate = new Date();
        String formattedDate = sdf.format(currentDate);

        response.put("currentDate", formattedDate);

        return response;
    }

    public Map<String, Object> getOrderDetails(Long id) {
        try {
            Optional<Order> optionalOrder = orderRepository.findById(id);

            if (optionalOrder.isPresent()) {
                Order order = optionalOrder.get();
                String employeeName = order.getEmployeeName();
                User usr = userRepository.findByName(employeeName);

                Map<String, Object> orderDetails = new HashMap<>();

                orderDetails.put("id", order.getId());
                orderDetails.put("description", order.getDescription());
                orderDetails.put("clientName", order.getClientName());
                orderDetails.put("email", order.getClientEmail());
                orderDetails.put("phoneNumber", order.getPhoneNumber());
                orderDetails.put("startDate", orderUtils.formatDate(order.getStartDate()));
                orderDetails.put("startTime", orderUtils.formatTime(order.getStartDate()));
                orderDetails.put("endDate", orderUtils.formatDate(order.getEndDate()));
                orderDetails.put("endTime", orderUtils.formatTime(order.getEndDate()));
                orderDetails.put("employee", usr != null ? usr.getName() : "");
                orderDetails.put("hours", order.getDuration());
                orderDetails.put("price", order.getPrice());
                orderDetails.put("materials", order.getMaterials());
                orderDetails.put("status", order.getStatus());

                return orderDetails;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Transactional
    public void editOrder(Long id, Map<String, Object> request) {
        Optional<Order> optionalOrder = orderRepository.findById(id);

        if (optionalOrder.isPresent()) {
            Order order = optionalOrder.get();

            // Usunięcie materiałów powiązanych z zamówieniem
            materialRepository.deleteAllByOrder(order);

            // Zaktualizowanie pracownika
            String selectedUser = (String) request.get("selectedUser");
            List<User> users = userRepository.findByNameContainingIgnoreCase(selectedUser);
            User user = users.isEmpty() ? null : users.get(0);
            order.setEmployeeName(user != null ? user.getName() : null);

            // Zaktualizowanie pozostałych pól zamówienia
            order.setDescription((String) request.get("description"));
            order.setClientName((String) request.get("clientName"));
            order.setClientEmail((String) request.get("email"));
            order.setPhoneNumber((String) request.get("phoneNumber"));

            String startDateString = (String) request.get("startDate");
            Date startDate = orderUtils.parseDateString(startDateString);
            order.setStartDate(startDate);

            String endDateString = (String) request.get("endDate");
            Date endDate = orderUtils.parseDateString(endDateString);
            order.setEndDate(endDate);

            double hours = Double.parseDouble((String) request.get("hours"));
            order.setDuration(hours);

            int price = Integer.parseInt((String) request.get("price"));
            order.setPrice(price);

            String status = (String) request.get("status");
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
                if (orderUtils.isWorkingDay(currentDateTime)) {
                    Calendar endDateTime = (Calendar) currentDateTime.clone();
                    int durationMinutes = (int) (durationHours * 60);
                    endDateTime.add(Calendar.MINUTE, durationMinutes);

                    if (endDateTime.get(Calendar.HOUR_OF_DAY) > 16) {
                        endDateTime.set(Calendar.HOUR_OF_DAY, 16);
                        endDateTime.set(Calendar.MINUTE, 0);
                    }

                    if (endDateTime.after(Calendar.getInstance())) {
                        List<User> availableUsers = orderUtils.findAvailableUsersWithEndDateTime(currentDateTime.getTime(), endDateTime.getTime(), durationMinutes);

                        if (!availableUsers.isEmpty()) {
                            User suggestedUser = availableUsers.get(0);

                            response.put("status", "success");
                            response.put("startDate", orderUtils.formatDate(currentDateTime.getTime()));
                            response.put("startTime", orderUtils.formatTime(currentDateTime.getTime()));
                            response.put("endDate", orderUtils.formatDate(endDateTime.getTime()));
                            response.put("endTime", orderUtils.formatTime(endDateTime.getTime()));
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

    public ResponseEntity<Map<String, Object>> checkAvailabilityNextDay(
            Long orderId,
            double durationHours) {

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
                    if (orderUtils.isWorkingDay(currentDateTime)) {
                        Calendar endDateTime = (Calendar) currentDateTime.clone();
                        int durationMinutes = (int) (durationHours * 60);
                        endDateTime.add(Calendar.MINUTE, durationMinutes);

                        if (endDateTime.get(Calendar.HOUR_OF_DAY) >= 16) {
                            currentDateTime.add(Calendar.DAY_OF_MONTH, 1);
                            currentDateTime.set(Calendar.HOUR_OF_DAY, 8);
                            currentDateTime.set(Calendar.MINUTE, 0);
                            continue;
                        }
                        User user = userRepository.findByName(order.getEmployeeName());

                        List<User> availableUsers = orderUtils.findAvailableUserWithEndDateTime(user.getId(), currentDateTime.getTime(), endDateTime.getTime(), durationMinutes);

                        if (!availableUsers.isEmpty()) {
                            User suggestedUser = availableUsers.get(0);

                            response.put("status", "success");
                            response.put("startDate", orderUtils.formatDate(currentDateTime.getTime()));
                            response.put("startTime", orderUtils.formatTime(currentDateTime.getTime()));
                            response.put("endDate", orderUtils.formatDate(endDateTime.getTime()));
                            response.put("endTime", orderUtils.formatTime(endDateTime.getTime()));
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


    public Map<String, Object> checkAvailabilityOtherEmployee(Long orderId, double durationHours) {
        Map<String, Object> response = new HashMap<>();
        boolean foundUser = true;

        try {
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
                    if (orderUtils.isWorkingDay(currentDateTime)) {
                        Calendar endDateTime = (Calendar) currentDateTime.clone();
                        int durationMinutes = (int) (durationHours * 60);
                        endDateTime.add(Calendar.MINUTE, durationMinutes);

                        if (endDateTime.get(Calendar.HOUR_OF_DAY) >= 16) {
                            currentDateTime.add(Calendar.DAY_OF_MONTH, 1);
                            currentDateTime.set(Calendar.HOUR_OF_DAY, 8);
                            currentDateTime.set(Calendar.MINUTE, 0);
                            continue;
                        }

                        List<User> availableUsers = orderUtils.findAvailableUsersWithoutEmployee(order.getId(), currentDateTime.getTime(), endDateTime.getTime(), durationMinutes);

                        if (!availableUsers.isEmpty()) {
                            User suggestedUser = availableUsers.get(0);

                            response.put("status", "success");
                            response.put("startDate", orderUtils.formatDate(currentDateTime.getTime()));
                            response.put("startTime", orderUtils.formatTime(currentDateTime.getTime()));
                            response.put("endDate", orderUtils.formatDate(endDateTime.getTime()));
                            response.put("endTime", orderUtils.formatTime(endDateTime.getTime()));
                            response.put("suggestedUser", suggestedUser.getName());
                            response.put("durationMinutes", durationMinutes);
                            return response;
                        }
                    }

                    currentDateTime.add(Calendar.DAY_OF_MONTH, 1);
                    currentDateTime.set(Calendar.HOUR_OF_DAY, 8);
                    currentDateTime.set(Calendar.MINUTE, 0);
                }
            } else {
                // Jeśli zamówienie o podanym identyfikatorze nie zostało znalezione
                response.put("status", "error");
                response.put("message", "Zamówienie o podanym identyfikatorze nie zostało znalezione.");
                return response;
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Obsługa błędów
            response.put("status", "error");
            response.put("message", "Wystąpił błąd podczas przetwarzania żądania.");
            return response;
        }
    }


    @Transactional
    public void deleteOrder(@PathVariable Long id) {

        Optional<Order> orderOptional = orderRepository.findById(id);

        if (orderOptional.isPresent()) {
            orderRepository.delete(orderOptional.get());
        } else {
            throw new UserNotFoundException(id);
        }
    }

    public List<Order> searchOrdersByStartDateBetweenWithMaterials(LocalDate fromDate, LocalDate toDate, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));

        LocalDateTime startDateTime = LocalDateTime.of(fromDate, LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(toDate, LocalTime.MAX);

        Date startDate = Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant());

        String userEmail = AuthenticationUtils.checkmail(authentication.getPrincipal());
        User user = userRepository.findByEmail(userEmail);

        List<Order> matchingOrders;
        if (isAdmin) {
            matchingOrders = orderRepository.findByStartDateBetweenWithMaterials(startDate, endDate);
        } else {
            String employeeName = user.getName();
            matchingOrders = orderRepository.findByEmployeeNameAndStartDateBetweenWithMaterials(employeeName, startDate, endDate);
        }

        List<Order> filteredOrders = matchingOrders.stream()
                .filter(order -> "W trakcie".equals(order.getStatus()))
                .collect(Collectors.toList());

        return filteredOrders;
    }

    public void updateMaterialCheckedState(Long materialId, boolean checked) {
        Optional<Material> optionalMaterial = materialRepository.findById(materialId);

        optionalMaterial.ifPresent(material -> {
            material.setChecked(checked);
            materialRepository.save(material);
        });
    }

}


