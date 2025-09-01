package pl.lukbol.dyplom.services;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
public class UserService {
    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserUtils userUtils;

    private UserRepository userRepository;

    private RoleRepository roleRepository;

    private ConversationRepository conversationRepository;

    private MessageRepository messageRepository;


    public UserService(UserRepository userRepository, RoleRepository roleRepository, ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }


    @Transactional
    public ResponseEntity<Map<String, Object>> addUser(String name, String email, String password, String roleName) {

        User newUser = new User(name, email, passwordEncoder.encode(password), false);

        Role role = roleRepository.findByName(roleName);

        if (userUtils.emailExists(newUser.getEmail())) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Użytkownik o takim adresie email już istnieje.");
            return ResponseEntity.badRequest().body(response);
        }
        List<Notification> notifications = newUser.getNotifications();
        notifications.add(new Notification("Witamy na stronie naszego zakładu krawieckiego!", new Date(), newUser, "System"));
        newUser.setNotifications(notifications);
        newUser.setRoles(Arrays.asList(role));

        userRepository.save(newUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Poprawnie utworzono użytkownika.");
        return ResponseEntity.ok(response);
    }
    public List<User> getUsersByRoles() {
        List<User> users = userRepository.findByRoles_NameContainingIgnoreCase("ROLE_ADMIN");
        users.addAll(userRepository.findByRoles_NameContainingIgnoreCase("ROLE_EMPLOYEE"));
        return users;
    }
    public ModelAndView getAllUsers(int page, int size) {
        ModelAndView modelAndView = new ModelAndView("admin");

        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAll(pageable);

        modelAndView.addObject("users", userPage.getContent());
        modelAndView.addObject("currentPage", userPage.getNumber());
        modelAndView.addObject("totalPages", userPage.getTotalPages());

        return modelAndView;
    }
    @Transactional
    public void registerUser(String name, String email, String password, HttpServletRequest req, HttpServletResponse resp) {
        User newUser = new User(name, email, passwordEncoder.encode(password), false);

        Role clientRole = roleRepository.findByName("ROLE_CLIENT");
        newUser.setRoles(Collections.singletonList(clientRole));

        List<Notification> notifications = newUser.getNotifications();
        notifications.add(new Notification("Witamy na stronie naszego zakładu krawieckiego!", new Date(), newUser, "System"));
        newUser.setNotifications(notifications);

        if (userUtils.emailExists(newUser.getEmail())) {
            req.getSession().setAttribute("message", "Użytkownik o takim adresie email już istnieje.");
        } else {
            userRepository.save(newUser);
            req.getSession().setAttribute("message", "Poprawnie utworzono użytkownika.");
        }
        try {
            resp.sendRedirect(req.getContextPath() + "/register");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public User getUserByEmail(Authentication authentication) {
        String email = AuthenticationUtils.checkmail(authentication.getPrincipal());
        return userRepository.findByEmail(email);
    }
    @Transactional
    public String changeProfile(Authentication authentication,
                                String username,
                                String password,
                                String repeatPassword) {

        if (!password.equals(repeatPassword)) {
            return "Hasła nie są zgodne";
        }

        User usr = userRepository.findByEmail(AuthenticationUtils.checkmail(authentication.getPrincipal()));
        usr.setPassword(passwordEncoder.encode(password));
        usr.setName(username);

        usr.setEnabled(true);

        userRepository.save(usr);
        return "Zmiany zostały zapisane pomyślnie";
    }

    @Transactional
    public void deleteUser(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            messageRepository.deleteBySenderId(id);

            List<Conversation> conversationsToUpdate = conversationRepository.findAll()
                    .stream()
                    .filter(conversation -> conversation.getClient() != null && id.equals(conversation.getClient().getId()))
                    .collect(Collectors.toList());

            conversationsToUpdate.forEach(conversation -> {
                conversation.setClient(null);
                conversationRepository.save(conversation);
                conversationRepository.delete(conversation);
            });

            List<Conversation> userConversations = conversationRepository.findByParticipants_Id(id);
            userConversations.forEach(conversation -> {
                conversation.getParticipants().remove(user);
                conversationRepository.save(conversation);
            });

            userRepository.delete(user);
        } else {
            throw new UserNotFoundException(id);
        }
    }
    public List<User> getEmployeesAndAdmins(Authentication authentication) {
        User usr = userRepository.findByEmail(AuthenticationUtils.checkmail(authentication.getPrincipal()));
        List<User> users = userRepository.findUsersByRoles_NameIn("ROLE_EMPLOYEE", "ROLE_ADMIN");
        users.removeIf(user -> user.getEmail().equalsIgnoreCase(usr.getEmail()));
        return users;
    }
    @Transactional
    public void updateUser(Long id, String newName, String newEmail, String newRole) {
        Optional<User> userOptional = userRepository.findById(id);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setName(newName);
            user.setEmail(newEmail);

            Role role = roleRepository.findByName(newRole);
            if (role == null) {
                throw new IllegalArgumentException("Role not found: " + newRole);
            }
            user.setRoles(Collections.singletonList(role));

            userRepository.save(user);
        } else {
            throw new UserNotFoundException(id);
        }
    }
    public List<Map<String, Object>> searchUsers(String category, String searchText) {
        List<Map<String, Object>> usersWithRoles = new ArrayList<>();

        List<User> users;
        if ("name".equals(category)) {
            users = userRepository.findByNameContainingIgnoreCase(searchText);
        } else if ("email".equals(category)) {
            users = userRepository.findByEmailContainingIgnoreCase(searchText);
        } else if ("role".equals(category)) {
            users = userRepository.findByRoles_NameContainingIgnoreCase(searchText);
        } else {
            users = new ArrayList<>();
        }

        for (User user : users) {
            Map<String, Object> userWithRole = new HashMap<>();
            userWithRole.put("id", user.getId());
            userWithRole.put("name", user.getName());
            userWithRole.put("email", user.getEmail());
            userWithRole.put("role", user.getRoles().iterator().next().getName());

            usersWithRoles.add(userWithRole);
        }

        return usersWithRoles;
    }
    public List<String> getEmployeeNames() {
        List<User> users = userRepository.findUsersByRoles_NameIn("ROLE_EMPLOYEE", "ROLE_ADMIN");
        Set<String> uniqueEmployeeNames = users.stream()
                .map(User::getName)
                .collect(Collectors.toSet());
        return new ArrayList<>(uniqueEmployeeNames);
    }
    public ResponseEntity<?> sendNewPassword(Map<String, String> payload) {
        String email = payload.get("email");
        User user = userRepository.findByEmail(email);
        if (user != null) {
            String newPassword = GenerateCode.generateActivationCode();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            userUtils.sendResetEmail(email, newPassword);

            return ResponseEntity.ok().body("Nowe hasło zostało wysłane.");
        } else {
            return ResponseEntity.badRequest().body("Nie znaleziono użytkownika z takim adresem e-mail.");
        }
    }



}

