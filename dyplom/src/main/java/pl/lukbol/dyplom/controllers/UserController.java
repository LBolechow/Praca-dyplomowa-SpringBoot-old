package pl.lukbol.dyplom.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import pl.lukbol.dyplom.classes.*;
import pl.lukbol.dyplom.configs.MailConfig;
import pl.lukbol.dyplom.exceptions.UserNotFoundException;
import pl.lukbol.dyplom.repositories.ConversationRepository;
import pl.lukbol.dyplom.repositories.MessageRepository;
import pl.lukbol.dyplom.repositories.RoleRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.UserService;
import pl.lukbol.dyplom.utilities.AuthenticationUtils;
import pl.lukbol.dyplom.utilities.GenerateCode;


import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(maxAge = 3600)
public class UserController {

    private static final String PATH = "/users";
    private static final String PATH2 = "/users/{id}";

    private UserService userService;
    @Autowired
    PasswordEncoder passwordEncoder;

    private final JavaMailSender mailSender;
    private UserRepository userRepository;

    private RoleRepository roleRepository;

    private ConversationRepository conversationRepository;

    private MessageRepository messageRepository;
    public UserController(UserRepository userRepository, RoleRepository roleRepository, JavaMailSender mailSender, ConversationRepository conversationRepository, MessageRepository messageRepository, UserService userService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.mailSender = mailSender;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
    }


    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addUser(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("role") String roleName) {

        // Wywołujemy metodę addUser z UserService
        return userService.addUser(name, email, password, roleName);
    }

    @GetMapping("/users/findByRole")
    @ResponseBody
    public List<User> getUsersByRoles() {
        return userService.getUsersByRoles();
    }
    @GetMapping("/panel_administratora")
    public ModelAndView displayAllUsers(Authentication authentication,
                                        @RequestParam(name = "page", defaultValue = "0") int page,
                                        @RequestParam(name = "size", defaultValue = "10") int size) {
        return userService.getAllUsers(page, size);
    }
    @PostMapping(value ="/register", consumes = {"*/*"})
    public void registerUser(@RequestParam("name") String name,
                             @RequestParam("email") String email,
                             @RequestParam("password") String password,
                             HttpServletRequest req,
                             HttpServletResponse resp) {
        userService.registerUser(name, email, password, req, resp);
    }
    @GetMapping("/get_message")
    @ResponseBody
    public String getMessageFromSession(HttpServletRequest request) {
        return (String) request.getSession().getAttribute("message");
    }

    @GetMapping(value="/user", consumes = {"*/*"})
    public User user(Authentication authentication) {
        return userService.getUserByEmail(authentication);
    }

    @PostMapping(value = "/profile/apply", consumes = {"*/*"})
    public String changeProfile(Authentication authentication,
                                @RequestParam("username") String username,
                                @RequestParam("password") String password,
                                @RequestParam("repeatPassword") String repeatPassword) {

       return userService.changeProfile(authentication, username, password, repeatPassword);
    }
    @GetMapping("/users/employees-and-admins")
    public List<User> getEmployeesAndAdmins(Authentication authentication) {
        return userService.getEmployeesAndAdmins(authentication);
    }
    @DeleteMapping("/users/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
    @PutMapping("/users/update/{id}")
    public ResponseEntity<String> updateUser(@PathVariable Long id,
                                             @RequestParam("name") String newName,
                                             @RequestParam("email") String newEmail,
                                             @RequestParam("role") String newRole) {
        try {
            userService.updateUser(id, newName, newEmail, newRole);
            return ResponseEntity.ok("User updated successfully.");
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/search-users")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(@RequestParam("category") String category,
                                                                 @RequestParam("searchText") String searchText) {
        List<Map<String, Object>> usersWithRoles = userService.searchUsers(category, searchText);
        return ResponseEntity.ok(usersWithRoles);
    }
    @GetMapping("/user/employees-and-admins")
    public List<String> getEmployeeNames() {
        return userService.getEmployeeNames();
    }
    @PostMapping("/send-new-password")
    public ResponseEntity<?> sendNewPassword(@RequestBody Map<String, String> payload) {
        return userService.sendNewPassword(payload);
    }

}
