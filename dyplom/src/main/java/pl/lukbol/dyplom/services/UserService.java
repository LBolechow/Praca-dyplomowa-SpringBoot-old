package pl.lukbol.dyplom.services;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import pl.lukbol.dyplom.classes.Conversation;
import pl.lukbol.dyplom.classes.Notification;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.exceptions.UserNotFoundException;
import pl.lukbol.dyplom.repositories.ConversationRepository;
import pl.lukbol.dyplom.repositories.MessageRepository;
import pl.lukbol.dyplom.repositories.RoleRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.utilities.AuthenticationUtils;
import pl.lukbol.dyplom.utilities.GenerateCode;
import pl.lukbol.dyplom.utilities.UserUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String WELCOME_MESSAGE = "Witamy na stronie naszego zakładu krawieckiego!";
    private static final String ROLE_NAME_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_NAME_EMPLOYEE = "ROLE_EMPLOYEE";
    private static final String ROLE_NAME_CLIENT = "ROLE_CLIENT";

    private final PasswordEncoder passwordEncoder;
    private final UserUtils userUtils;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public ResponseEntity<Map<String, Object>> addUser(String name, String email, String password, String roleName) {
        if (userUtils.emailExists(email)) {
            return createResponse(false, WELCOME_MESSAGE, HttpStatus.BAD_REQUEST);
        }

        User newUser = createUser(name, email, password);
        Role role = roleRepository.findByName(roleName);
        newUser.setRoles(Collections.singletonList(role));
        addWelcomeNotification(newUser);

        userRepository.save(newUser);

        return createResponse(true, "Poprawnie utworzono użytkownika.", HttpStatus.OK);
    }

    public List<User> getUsersByRoles() {
        List<User> users = userRepository.findByRoles_NameContainingIgnoreCase(ROLE_NAME_ADMIN);
        users.addAll(userRepository.findByRoles_NameContainingIgnoreCase(ROLE_NAME_EMPLOYEE));
        return users;
    }

    public ModelAndView getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAll(pageable);

        ModelAndView modelAndView = new ModelAndView("admin");
        modelAndView.addObject("users", userPage.getContent());
        modelAndView.addObject("currentPage", userPage.getNumber());
        modelAndView.addObject("totalPages", userPage.getTotalPages());

        return modelAndView;
    }

    @Transactional
    public void registerUser(String name, String email, String password, HttpServletRequest req, HttpServletResponse resp) {
        if (userUtils.emailExists(email)) {
            req.getSession().setAttribute("message", "Użytkownik o takim adresie email już istnieje.");
            redirectToRegister(resp, req);
            return;
        }

        User newUser = createUser(name, email, password);
        Role clientRole = roleRepository.findByName(ROLE_NAME_CLIENT);
        newUser.setRoles(Collections.singletonList(clientRole));
        addWelcomeNotification(newUser);

        userRepository.save(newUser);

        req.getSession().setAttribute("message", "Poprawnie utworzono użytkownika.");
        redirectToRegister(resp, req);
    }

    public User getUserByEmail(Authentication authentication) {
        String email = AuthenticationUtils.checkmail(authentication.getPrincipal());
        return userRepository.findByEmail(email);
    }

    @Transactional
    public String changeProfile(Authentication authentication, String username, String password, String repeatPassword) {
        if (!password.equals(repeatPassword)) {
            return "Hasła nie są zgodne";
        }

        User usr = getUserByEmail(authentication);
        usr.setPassword(passwordEncoder.encode(password));
        usr.setName(username);
        usr.setEnabled(true);

        userRepository.save(usr);
        return "Zmiany zostały zapisane pomyślnie";
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        messageRepository.deleteBySenderId(id);

        removeClientFromConversations(id);
        removeUserFromConversations(user);

        userRepository.delete(user);
    }

    public List<User> getEmployeesAndAdmins(Authentication authentication) {
        User currentUser = getUserByEmail(authentication);
        List<User> users = userRepository.findUsersByRoles_NameIn(ROLE_NAME_EMPLOYEE, ROLE_NAME_ADMIN);
        users.removeIf(user -> user.getEmail().equalsIgnoreCase(currentUser.getEmail()));
        return users;
    }

    @Transactional
    public void updateUser(Long id, String newName, String newEmail, String newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setName(newName);
        user.setEmail(newEmail);

        Role role = roleRepository.findByName(newRole);
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + newRole);
        }
        user.setRoles(Collections.singletonList(role));

        userRepository.save(user);
    }

    public List<Map<String, Object>> searchUsers(String category, String searchText) {
        List<User> users;
        if ("name".equals(category)) {
            users = userRepository.findByNameContainingIgnoreCase(searchText);
        } else if ("email".equals(category)) {
            users = userRepository.findByEmailContainingIgnoreCase(searchText);
        } else if ("role".equals(category)) {
            users = userRepository.findByRoles_NameContainingIgnoreCase(searchText);
        } else {
            users = Collections.emptyList();
        }

        return users.stream()
                .map(user -> {
                    Map<String, Object> userWithRole = new HashMap<>();
                    userWithRole.put("id", user.getId());
                    userWithRole.put("name", user.getName());
                    userWithRole.put("email", user.getEmail());
                    userWithRole.put("role", user.getRoles().iterator().next().getName());
                    return userWithRole;
                })
                .collect(Collectors.toList());
    }


    public List<String> getEmployeeNames() {
        List<User> users = userRepository.findUsersByRoles_NameIn(ROLE_NAME_EMPLOYEE, ROLE_NAME_ADMIN);
        return users.stream()
                .map(User::getName)
                .distinct()
                .collect(Collectors.toList());
    }

    public ResponseEntity<?> sendNewPassword(Map<String, String> payload) {
        String email = payload.get("email");
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.badRequest().body("Nie znaleziono użytkownika z takim adresem e-mail.");
        }

        String newPassword = GenerateCode.generateActivationCode();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        userUtils.sendResetEmail(email, newPassword);
        return ResponseEntity.ok().body("Nowe hasło zostało wysłane.");
    }

    // --- Prywatne metody pomocnicze ---

    private User createUser(String name, String email, String password) {
        return new User(name, email, passwordEncoder.encode(password), false);
    }

    private void addWelcomeNotification(User user) {
        List<Notification> notifications = user.getNotifications();
        notifications.add(new Notification(WELCOME_MESSAGE, new Date(), user, "System"));
        user.setNotifications(notifications);
    }

    private ResponseEntity<Map<String, Object>> createResponse(boolean success, String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        return new ResponseEntity<>(response, status);
    }

    private void redirectToRegister(HttpServletResponse resp, HttpServletRequest req) {
        try {
            resp.sendRedirect(req.getContextPath() + "/register");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeClientFromConversations(Long clientId) {
        List<Conversation> conversationsToUpdate = conversationRepository.findAll()
                .stream()
                .filter(conversation -> conversation.getClient() != null && clientId.equals(conversation.getClient().getId()))
                .collect(Collectors.toList());

        for (Conversation conversation : conversationsToUpdate) {
            conversation.setClient(null);
            conversationRepository.save(conversation);
            conversationRepository.delete(conversation);
        }
    }

    private void removeUserFromConversations(User user) {
        List<Conversation> userConversations = conversationRepository.findByParticipants_Id(user.getId());
        for (Conversation conversation : userConversations) {
            conversation.getParticipants().remove(user);
            conversationRepository.save(conversation);
        }
    }

}


