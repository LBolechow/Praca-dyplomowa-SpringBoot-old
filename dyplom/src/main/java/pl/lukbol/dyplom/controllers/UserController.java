package pl.lukbol.dyplom.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.exceptions.UserNotFoundException;
import pl.lukbol.dyplom.repositories.ConversationRepository;
import pl.lukbol.dyplom.repositories.MessageRepository;
import pl.lukbol.dyplom.repositories.RoleRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(maxAge = 3600)
public class UserController {

    private static final String PATH = "/users";
    private static final String PATH2 = "/users/{id}";

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final ConversationRepository conversationRepository;

    private final MessageRepository messageRepository;


    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addUser(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("role") String roleName) {

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

    @PostMapping(value = "/register", consumes = {"*/*"})
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

    @GetMapping(value = "/user", consumes = {"*/*"})
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
