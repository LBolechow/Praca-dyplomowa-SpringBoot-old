package pl.lukbol.dyplom.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import pl.lukbol.dyplom.classes.Material;
import pl.lukbol.dyplom.classes.Order;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.exceptions.UserNotFoundException;
import pl.lukbol.dyplom.repositories.MaterialRepository;
import pl.lukbol.dyplom.repositories.OrderRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.utilities.AuthenticationUtils;
import pl.lukbol.dyplom.utilities.DateUtils;
import pl.lukbol.dyplom.utilities.GenerateCode;
import pl.lukbol.dyplom.utilities.OrderUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static pl.lukbol.dyplom.utilities.OrderUtils.WARSAW_ZONE;
import static pl.lukbol.dyplom.utilities.OrderUtils.WORKDAY_END_HOUR;

@Service
@RequiredArgsConstructor
public class OrderService {


    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MaterialRepository materialRepository;
    private final OrderUtils orderUtils;

    @Transactional
    public Order addOrder(Map<String, Object> request) {
        User user = orderUtils.findUserByName((String) request.get("selectedUser"));
        Date currentDate = DateUtils.currentDate();

        Order newOrder = createOrderFromRequest(request, user);
        orderUtils.addNotificationAboutNewOrder(user);

        List<Material> materials = orderUtils.createMaterialsForOrder((List<String>) request.get("items"), newOrder);
        newOrder.setMaterials(materials);

        return orderRepository.save(newOrder);
    }

    public List<Map<String, Object>> getDailyOrders(String start, String end, String userEmail, boolean isAdmin) {
        DateRange dateRange = parseDateRange(start, end);
        User user = userRepository.findByEmail(userEmail);

        List<Order> orders = isAdmin
                ? orderUtils.findOrdersForAdmin(dateRange)
                : orderUtils.findOrdersForUser(user.getName(), dateRange);

        return orderUtils.convertOrdersToCalendarData(orders);
    }

    public List<Order> getUserOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail);
        if (user == null) return null;

        List<Order> userOrders = orderRepository.findOrdersByUserEmail(user.getEmail());
        userOrders.sort(Comparator.comparing(Order::getEndDate).reversed());
        return userOrders;
    }

    public Map<String, Object> getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return Collections.singletonMap("currentDate", sdf.format(new Date()));
    }

    public Map<String, Object> getOrderDetails(Long id) {
        return orderRepository.findById(id)
                .map(this::buildOrderDetails)
                .orElse(null);
    }

    @Transactional
    public void editOrder(Long id, Map<String, Object> request) {
        orderRepository.findById(id).ifPresent(order -> {
            orderUtils.updateOrderFields(order, request);
            updateOrderMaterials(order, (List<String>) request.get("items"));
            orderRepository.save(order);
        });
    }

    public ResponseEntity<Map<String, Object>> checkAvailability(double durationHours, int startHour) {
        try {
            Calendar startDateTime = orderUtils.prepareInitialStartDateTime(startHour);

            while (startDateTime.get(Calendar.HOUR_OF_DAY) < WORKDAY_END_HOUR) {
                if (orderUtils.isWorkingDay(startDateTime)) {
                    Calendar endDateTime = orderUtils.calculateEndDateTime(startDateTime, durationHours);

                    if (orderUtils.isValidTimeSlot(startDateTime, endDateTime)) {
                        Optional<Map<String, Object>> availability = checkUserAvailability(startDateTime, endDateTime, durationHours);
                        if (availability.isPresent()) {
                            return ResponseEntity.ok(availability.get());
                        }
                    }
                }
                orderUtils.moveToNextDayAtStartHour(startDateTime, startHour);
            }

            return ResponseEntity.ok(orderUtils.createErrorResponse("Brak dostępnych pracowników w ramach dni roboczych."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    public ResponseEntity<Map<String, Object>> checkAvailabilityNextDay(Long orderId, double durationHours) {
        try {
            return orderRepository.findById(orderId)
                    .map(order -> orderUtils.processNextDayAvailability(order, durationHours))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(orderUtils.createErrorResponse("Zamówienie o podanym identyfikatorze nie zostało znalezione.")));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    public Map<String, Object> checkAvailabilityOtherEmployee(Long orderId, double durationHours) {
        try {
            return orderRepository.findById(orderId)
                    .map(order -> orderUtils.findAlternativeEmployeeAvailability(order, durationHours))
                    .orElseGet(() -> orderUtils.createErrorResponse("Zamówienie o podanym identyfikatorze nie zostało znalezione."));
        } catch (Exception e) {
            return orderUtils.createErrorResponse("Wystąpił błąd podczas przetwarzania żądania.");
        }
    }

    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.findById(id)
                .ifPresentOrElse(
                        orderRepository::delete,
                        () -> {
                            throw new UserNotFoundException(id);
                        }
                );
    }

    public List<Order> searchOrdersByStartDateBetweenWithMaterials(LocalDate fromDate, LocalDate toDate, Authentication authentication) {
        boolean isAdmin = orderUtils.isAdmin(authentication);
        DateRange dateRange = orderUtils.convertToDateRange(fromDate, toDate);
        String userEmail = AuthenticationUtils.checkmail(authentication.getPrincipal());

        List<Order> matchingOrders = isAdmin
                ? orderRepository.findByStartDateBetweenWithMaterials(dateRange.start(), dateRange.end())
                : orderRepository.findByEmployeeNameAndStartDateBetweenWithMaterials(
                userRepository.findByEmail(userEmail).getName(), dateRange.start(), dateRange.end());

        return orderUtils.filterInProgressOrders(matchingOrders);
    }

    public void updateMaterialCheckedState(Long materialId, boolean checked) {
        materialRepository.findById(materialId)
                .ifPresent(material -> {
                    material.setChecked(checked);
                    materialRepository.save(material);
                });
    }

    private Order createOrderFromRequest(Map<String, Object> request, User user) {
        Date startDate = orderUtils.parseDateString((String) request.get("startDate"));
        Date endDate = orderUtils.parseDateString((String) request.get("endDate"));

        return new Order(
                (String) request.get("description"),
                (String) request.get("clientName"),
                (String) request.get("email"),
                (String) request.get("phoneNumber"),
                user.getName(),
                startDate,
                endDate,
                "W trakcie",
                Integer.parseInt((String) request.get("price")),
                Double.parseDouble((String) request.get("hours")),
                null,
                GenerateCode.generateActivationCode()
        );
    }

    private DateRange parseDateRange(String start, String end) {
        if (start == null || end == null) {
            LocalDateTime now = LocalDateTime.now();
            return new DateRange(
                    Date.from(now.with(LocalTime.MIN).atZone(WARSAW_ZONE).toInstant()),
                    Date.from(now.with(LocalTime.MAX).atZone(WARSAW_ZONE).toInstant())
            );
        }

        try {
            LocalDateTime startDateTime = LocalDateTime.parse(start, DateTimeFormatter.ISO_DATE_TIME);
            LocalDateTime endDateTime = LocalDateTime.parse(end, DateTimeFormatter.ISO_DATE_TIME);
            return new DateRange(
                    Date.from(startDateTime.atZone(WARSAW_ZONE).toInstant()),
                    Date.from(endDateTime.atZone(WARSAW_ZONE).toInstant())
            );
        } catch (DateTimeParseException e) {
            return new DateRange(
                    Date.from(LocalDate.parse(start).atStartOfDay(WARSAW_ZONE).toInstant()),
                    Date.from(LocalDate.parse(end).atTime(23, 59, 59).atZone(WARSAW_ZONE).toInstant())
            );
        }
    }

    private Map<String, Object> buildOrderDetails(Order order) {
        Map<String, Object> details = new HashMap<>();
        User user = userRepository.findByName(order.getEmployeeName());

        details.put("id", order.getId());
        details.put("description", order.getDescription());
        details.put("clientName", order.getClientName());
        details.put("email", order.getClientEmail());
        details.put("phoneNumber", order.getPhoneNumber());
        details.put("startDate", orderUtils.formatDate(order.getStartDate()));
        details.put("startTime", orderUtils.formatTime(order.getStartDate()));
        details.put("endDate", orderUtils.formatDate(order.getEndDate()));
        details.put("endTime", orderUtils.formatTime(order.getEndDate()));
        details.put("employee", user != null ? user.getName() : "");
        details.put("hours", order.getDuration());
        details.put("price", order.getPrice());
        details.put("materials", order.getMaterials());
        details.put("status", order.getStatus());

        return details;
    }

    private void updateOrderMaterials(Order order, List<String> items) {
        materialRepository.deleteAllByOrder(order);
        List<Material> materials = orderUtils.createMaterialsForOrder(items, order);
        order.setMaterials(materials);
    }

    private Optional<Map<String, Object>> checkUserAvailability(Calendar startDateTime, Calendar endDateTime, double durationHours) {
        int durationMinutes = (int) (durationHours * 60);
        List<User> availableUsers = orderUtils.findAvailableUsersWithEndDateTime(
                startDateTime.getTime(), endDateTime.getTime(), durationMinutes);

        if (!availableUsers.isEmpty()) {
            return Optional.of(orderUtils.buildAvailabilityResponse(
                    availableUsers.get(0),
                    startDateTime.getTime(),
                    endDateTime.getTime(),
                    durationMinutes
            ));
        }
        return Optional.empty();
    }

    public record DateRange(Date start, Date end) {
    }


}


