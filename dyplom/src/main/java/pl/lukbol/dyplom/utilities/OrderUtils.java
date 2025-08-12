package pl.lukbol.dyplom.utilities;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pl.lukbol.dyplom.classes.*;
import pl.lukbol.dyplom.repositories.OrderRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.OrderService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderUtils {


    public static final int WORKDAY_START_HOUR = 8;
    public static final int WORKDAY_END_HOUR = 16;
    public static final ZoneId WARSAW_ZONE = ZoneId.of("Europe/Warsaw");
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }


    public String formatTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return sdf.format(date);
    }


    public boolean isWorkingDay(Calendar calendar) {
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY;
    }

    public Date parseDateString(String dateString) {
        Date date = null;
        try {
            date = DateUtils.parseDate(dateString, "yyyy-MM-dd HH:mm:ss");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public List<User> findAvailableUsersWithEndDateTime(Date taskStartDateTime, Date taskEndDateTime, int durationMinutes) {
        List<String> roleNamesToSearch = Arrays.asList("ROLE_EMPLOYEE", "ROLE_ADMIN");

        List<User> availableUsers = new ArrayList<>();

        List<User> allUsers = userRepository.findAll();

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

    public List<User> findAvailableUserWithEndDateTime(Long employeeId, Date taskStartDateTime, Date taskEndDateTime, int durationMinutes) {
        List<String> roleNamesToSearch = Arrays.asList("ROLE_EMPLOYEE", "ROLE_ADMIN");

        List<User> availableUsers = new ArrayList<>();

        Optional<User> optionalUser = userRepository.findById(employeeId);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

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

    public boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));
    }

    public OrderService.DateRange convertToDateRange(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime startDateTime = LocalDateTime.of(fromDate, LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(toDate, LocalTime.MAX);

        return new OrderService.DateRange(
                Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant())
        );
    }

    public List<Order> filterInProgressOrders(List<Order> orders) {
        return orders.stream()
                .filter(order -> "W trakcie".equals(order.getStatus()))
                .collect(Collectors.toList());
    }

    public List<User> findAvailableUsersWithoutEmployee(Long orderId, Date taskStartDateTime, Date taskEndDateTime, int durationMinutes) {
        List<String> roleNamesToSearch = Arrays.asList("ROLE_EMPLOYEE", "ROLE_ADMIN");

        List<User> availableUsers = new ArrayList<>();

        Optional<Order> optionalOrder = orderRepository.findById(orderId);

        if (optionalOrder.isPresent()) {
            Order order = optionalOrder.get();

            String employeeNameOnOrder = order.getEmployeeName();

            List<User> allUsersExceptEmployee = userRepository.findAllByNameNot(employeeNameOnOrder);

            for (User user : allUsersExceptEmployee) {
                boolean isAvailable = true;

                Collection<Role> userRoles = user.getRoles();
                List<String> userRoleNames = userRoles.stream()
                        .map(Role::getName)
                        .collect(Collectors.toList());

                if (userRoleNames.stream().anyMatch(roleNamesToSearch::contains)) {
                    List<Order> userOrders = orderRepository.findByEmployeeNameAndEndDateAfterAndStartDateBefore(user.getName(), taskStartDateTime, taskEndDateTime);

                    for (Order userOrder : userOrders) {
                        Date orderStartDate = userOrder.getStartDate();
                        Date orderEndDate = userOrder.getEndDate();

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
        }

        return availableUsers;
    }

    public User findUserByName(String name) {
        return userRepository.findByNameContainingIgnoreCase(name).get(0);
    }

    public void updateOrderFields(Order order, Map<String, Object> request) {
        User user = findUserByName((String) request.get("selectedUser"));

        order.setEmployeeName(user != null ? user.getName() : null);
        order.setDescription((String) request.get("description"));
        order.setClientName((String) request.get("clientName"));
        order.setClientEmail((String) request.get("email"));
        order.setPhoneNumber((String) request.get("phoneNumber"));
        order.setStartDate(parseDateString((String) request.get("startDate")));
        order.setEndDate(parseDateString((String) request.get("endDate")));
        order.setDuration(Double.parseDouble((String) request.get("hours")));
        order.setPrice(Integer.parseInt((String) request.get("price")));
        order.setStatus((String) request.get("status"));
    }

    public void addNotificationAboutNewOrder(User user) {
        List<Notification> notifications = new ArrayList<>(user.getNotifications());
        notifications.add(new Notification("Pojawiło się nowe zlecenie!", new Date(), user, "System"));
        user.setNotifications(notifications);
        userRepository.save(user);
    }

    public List<Material> createMaterialsForOrder(List<String> items, Order order) {
        return items.stream()
                .map(item -> new Material(item, order, false))
                .collect(Collectors.toList());
    }

    public Calendar prepareInitialStartDateTime(int startHour) {
        Calendar calendar = Calendar.getInstance();

        if (calendar.get(Calendar.HOUR_OF_DAY) > startHour ||
                (calendar.get(Calendar.HOUR_OF_DAY) == startHour && calendar.get(Calendar.MINUTE) > 0)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        calendar.set(Calendar.HOUR_OF_DAY, startHour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        return calendar;
    }

    public void moveToNextDayAtStartHour(Calendar calendar, int startHour) {
        calendar.set(Calendar.HOUR_OF_DAY, startHour);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
    }

    public Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return response;
    }

    public ResponseEntity<Map<String, Object>> processNextDayAvailability(Order order, double durationHours) {
        Calendar currentDateTime = prepareOrderEndCalendar(order);

        while (true) {
            if (isWorkingDay(currentDateTime)) {
                Calendar endDateTime = calculateEndDateTime(currentDateTime, durationHours);

                if (endDateTime.get(Calendar.HOUR_OF_DAY) >= WORKDAY_END_HOUR) {
                    moveToNextDayAtStartHour(currentDateTime, WORKDAY_START_HOUR);
                    continue;
                }

                User user = userRepository.findByName(order.getEmployeeName());
                List<User> availableUsers = findAvailableUserWithEndDateTime(
                        user.getId(), currentDateTime.getTime(), endDateTime.getTime(), (int) (durationHours * 60));

                if (!availableUsers.isEmpty()) {
                    return ResponseEntity.ok(buildAvailabilityResponse(
                            availableUsers.get(0),
                            currentDateTime.getTime(),
                            endDateTime.getTime(),
                            (int) (durationHours * 60)
                    ));
                }
            }
            moveToNextDayAtStartHour(currentDateTime, WORKDAY_START_HOUR);
        }
    }

    public Calendar prepareOrderEndCalendar(Order order) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(order.getEndDate());

        if (calendar.get(Calendar.HOUR_OF_DAY) >= WORKDAY_END_HOUR) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, WORKDAY_START_HOUR);
            calendar.set(Calendar.MINUTE, 0);
        }

        return calendar;
    }

    public Calendar calculateEndDateTime(Calendar startDateTime, double durationHours) {
        Calendar endDateTime = (Calendar) startDateTime.clone();
        int durationMinutes = (int) (durationHours * 60);
        endDateTime.add(Calendar.MINUTE, durationMinutes);

        if (endDateTime.get(Calendar.HOUR_OF_DAY) > WORKDAY_END_HOUR) {
            endDateTime.set(Calendar.HOUR_OF_DAY, WORKDAY_END_HOUR);
            endDateTime.set(Calendar.MINUTE, 0);
        }

        return endDateTime;
    }

    public boolean isValidTimeSlot(Calendar startDateTime, Calendar endDateTime) {
        return endDateTime.after(Calendar.getInstance());
    }

    public Map<String, Object> findAlternativeEmployeeAvailability(Order order, double durationHours) {
        Calendar currentDateTime = prepareOrderEndCalendar(order);

        while (true) {
            if (isWorkingDay(currentDateTime)) {
                Calendar endDateTime = calculateEndDateTime(currentDateTime, durationHours);

                if (endDateTime.get(Calendar.HOUR_OF_DAY) >= WORKDAY_END_HOUR) {
                    moveToNextDayAtStartHour(currentDateTime, WORKDAY_START_HOUR);
                    continue;
                }

                List<User> availableUsers = findAvailableUsersWithoutEmployee(
                        order.getId(), currentDateTime.getTime(), endDateTime.getTime(), (int) (durationHours * 60));

                if (!availableUsers.isEmpty()) {
                    return buildAvailabilityResponse(
                            availableUsers.get(0),
                            currentDateTime.getTime(),
                            endDateTime.getTime(),
                            (int) (durationHours * 60)
                    );
                }
            }
            moveToNextDayAtStartHour(currentDateTime, WORKDAY_START_HOUR);
        }
    }

    public Map<String, Object> buildAvailabilityResponse(User user, Date startDate, Date endDate, int durationMinutes) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("startDate", formatDate(startDate));
        response.put("startTime", formatTime(startDate));
        response.put("endDate", formatDate(endDate));
        response.put("endTime", formatTime(endDate));
        response.put("suggestedUser", user.getName());
        response.put("durationMinutes", durationMinutes);
        return response;
    }

    public List<Order> findOrdersForAdmin(OrderService.DateRange dateRange) {
        return orderRepository.findByEndDateBetween(dateRange.start(), dateRange.end());
    }

    public List<Order> findOrdersForUser(String username, OrderService.DateRange dateRange) {
        return orderRepository.findByEmployeeNameAndEndDateBetween(username, dateRange.start(), dateRange.end());
    }

    private ZonedDateTime adjustForDST(ZonedDateTime dateTime) {
        LocalDateTime localDateTime = dateTime.toLocalDateTime();
        boolean isWinterTime = localDateTime.isBefore(LocalDate.of(localDateTime.getYear(), 3, 31).atTime(2, 0)) ||
                localDateTime.isAfter(LocalDate.of(localDateTime.getYear(), 10, 30).atTime(2, 0));

        return isWinterTime
                ? dateTime.plusSeconds(3600)
                : dateTime.plusSeconds(7200);
    }

    private Map<String, Object> convertOrderToCalendarData(Order order) {
        Map<String, Object> orderData = new HashMap<>();
        ZonedDateTime startZoned = adjustForDST(order.getStartDate().toInstant().atZone(WARSAW_ZONE));
        ZonedDateTime endZoned = adjustForDST(order.getEndDate().toInstant().atZone(WARSAW_ZONE));

        orderData.put("id", order.getId());
        orderData.put("title", order.getDescription());
        orderData.put("start", startZoned.toEpochSecond() * 1000);
        orderData.put("end", endZoned.toEpochSecond() * 1000);
        orderData.put("clientName", order.getClientName());
        orderData.put("employeeName", order.getEmployeeName());
        orderData.put("status", order.getStatus());

        return orderData;
    }

    public List<Map<String, Object>> convertOrdersToCalendarData(List<Order> orders) {
        return orders.stream()
                .map(this::convertOrderToCalendarData)
                .collect(Collectors.toList());
    }


}
