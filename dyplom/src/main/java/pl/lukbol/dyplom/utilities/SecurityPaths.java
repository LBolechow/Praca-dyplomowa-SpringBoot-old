package pl.lukbol.dyplom.utilities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Getter
@NoArgsConstructor
public class SecurityPaths {
    public final String[] CLIENT_EMPLOYEE_ADMIN_PATHS = {
            "/user", "/profile/**", "/currentDate", "/clientChat", "/ws-chat/**", "/ws-chat",
            "/api/conversation", "/sendToEmployees", "/topic/employees", "/app", "/topic/**",
            "/employeeChat", "/conversation/**", "/sendToConversation/**",
            "/api/conversation/**/latest-message", "/index", "/user/activateMail",
            "/user/checkCode", "/user/orders", "/removeAlerts", "/create-notification"
    };

    public final String[] ADMIN_PATHS = {
            "/admin/**", "/search-users", "/panel_administratora", "/users/delete/**",
            "/users/update/**", "/users/add", "/add-price", "/delete-price/{id}", "/update-price/{id}"
    };

    public final String[] ADMIN_EMPLOYEE_PATHS = {
            "/order/add", "/users", "/caldendar", "/daily", "/daily/**", "/order/getDailyOrders",
            "/users/findByRole", "/order/checkAvailability", "/order/getOrderDetails/{id}",
            "/order/edit/{id}", "/order/checkAvailabilityNextDay", "/order/delete/{id}",
            "/materials", "/order/search", "/material/{id}", "/order/otherEmployee/{orderId}",
            "/user/employees-and-admins", "/users/employees-and-admins", "/api/createConversation",
            "/api/employee/conversations", "/clearSeenByUserIds/", "/markConversationAsRead/{conversationId}",
            "/checkIfConversationRead/{conversationId}", "/get_conversations",
            "/getConversationParticipants/{conversationId}", "/hide/{conversationId}", "/checkSeen/{conversationId}"
    };

    public final String[] PERMIT_ALL_PATHS = {
            "/register", "/error", "/webjars/**", "/githubprivacyerror.html", "/css/**",
            "/static/**", "/images/**", "/fonts/**", "/scripts/**", "/error", "/login",
            "/", "/user2", "/favicon", "/usersonline", "/user/profile/{id}", "/get_message",
            "/favicon.ico", "/price_list", "/locked", "/api/conversation", "/ordersList",
            "/order", "/order/**", "/order/checkOrder/{idCode}", "/prices", "/send-new-password"
    };
}
