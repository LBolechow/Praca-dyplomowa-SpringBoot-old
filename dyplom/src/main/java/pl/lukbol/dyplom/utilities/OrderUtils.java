package pl.lukbol.dyplom.utilities;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pl.lukbol.dyplom.classes.Order;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.repositories.MaterialRepository;
import pl.lukbol.dyplom.repositories.OrderRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.OrderService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OrderUtils {


    private UserRepository userRepository;

    private OrderRepository orderRepository;

    public OrderUtils(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }
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


}
