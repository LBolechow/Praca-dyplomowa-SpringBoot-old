package pl.lukbol.dyplom.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.exceptions.UserNotFoundException;
import pl.lukbol.dyplom.repositories.RoleRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.UserService;


import java.io.IOException;
import java.util.*;

@RestController
@CrossOrigin(maxAge = 3600)
public class UserController {

    private static final String PATH = "/users";
    private static final String PATH2 = "/users/{id}";
    @Autowired
    private UserService userService;
    @Autowired
    PasswordEncoder passwordEncoder;
    private UserRepository userRepository;

    private RoleRepository roleRepository;

    public UserController(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }
    private boolean emailExists(String email) {
        return userRepository.findByEmail(email) != null;
    }

    @PostMapping("/users/add")
    public ResponseEntity<Map<String, Object>> addUser(@RequestParam("name") String name,
                                                       @RequestParam("email") String email,
                                                       @RequestParam("password") String password,
                                                       @RequestParam("role") String roleName) {
        User newUser = new User(name, email, passwordEncoder.encode(password));

        Role role = roleRepository.findByName(roleName);

        if (emailExists(newUser.getEmail())) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Użytkownik o takim adresie email już istnieje.");
            return ResponseEntity.badRequest().body(response);
        }

        newUser.setRoles(Arrays.asList(role));
        userRepository.save(newUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Poprawnie utworzono użytkownika.");
        return ResponseEntity.ok(response);
    }



    @GetMapping("/panel_administratora")
    public ModelAndView displayAllUsers(Authentication authentication, @RequestParam(name = "page", defaultValue = "0") int page,
                                        @RequestParam(name = "size", defaultValue = "10") int size) {
        ModelAndView modelAndView = new ModelAndView("admin"); // Your HTML file name without the extension
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAll(pageable);

        modelAndView.addObject("users", userPage.getContent());
        modelAndView.addObject("currentPage", userPage.getNumber());
        modelAndView.addObject("totalPages", userPage.getTotalPages());

        return modelAndView;
    }
    @PostMapping(value ="/register", consumes = {"*/*"})
    public void registerUser(@RequestParam("name") String name, @RequestParam("email") String email, @RequestParam("password") String password,  HttpServletRequest req, HttpServletResponse resp) {
        User newUsr = new User(name,email, passwordEncoder.encode(password));
        newUsr.setRoles(Arrays.asList(roleRepository.findByName("ROLE_CLIENT")));
        if (emailExists(newUsr.getEmail())) {
            req.getSession().setAttribute("message", "Użytkownik o takim adresie email już istnieje.");
        } else {
            userRepository.save(newUsr);
            req.getSession().setAttribute("message", "Poprawnie utworzono użytkownika.");
        }

        try {
            resp.sendRedirect(req.getContextPath() + "/register");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    @GetMapping("/get_message")
    @ResponseBody
    public String getMessageFromSession(HttpServletRequest request) {
        return (String) request.getSession().getAttribute("message");
    }

    @GetMapping(value="/user", consumes = {"*/*"})
    public User user(Authentication authentication) {
        User usr = userRepository.findByEmail(checkmail(authentication.getPrincipal()));


        return usr;
    }
    public String checkmail(Object authentication){
        if (authentication instanceof DefaultOidcUser) {       //klasa która powstaje przy social loginie
            DefaultOidcUser oauth2User = (DefaultOidcUser) authentication;
            return oauth2User.getAttribute("email");
        } else if (authentication instanceof UserDetails) {    //zwykla klasa posiadająca dane z bazy
            UserDetails userDetails = (UserDetails) authentication;
            return userDetails.getUsername();
        }
        else if (authentication instanceof OAuth2AuthenticationToken) {    //zwykla klasa posiadająca dane z bazy
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            String email = oauthToken.getPrincipal().getAttribute("email");
            return email;
        }
        else if (authentication instanceof UsernamePasswordAuthenticationToken) {    //zwykla klasa posiadająca dane z bazy
            UsernamePasswordAuthenticationToken oauthToken = (UsernamePasswordAuthenticationToken) authentication;
            String email = oauthToken.getName();
            return email;
        }
        else {
            return "notfound";
        }
    }
    @PostMapping(value="/profile/apply", consumes = {"*/*"})
    public String changeProfile(Authentication authentication,
                                @RequestParam("username") String username,
                                @RequestParam("password") String password,
                                @RequestParam("repeatPassword") String repeatPassword) {

        if(!password.equals(repeatPassword)){
            return "Hasła nie są zgodne";
        }
        User usr = userRepository.findByEmail(checkmail(authentication.getPrincipal()));
        usr.setPassword(passwordEncoder.encode(password));
        usr.setName(username);
        userRepository.save(usr);
        return "Zmiany zostały zapisane pomyślnie";
    }
    @DeleteMapping("/users/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Ustaw status odpowiedzi na "204 No Content"
    public void deleteUser(@PathVariable Long id) {
        // Sprawdź, czy użytkownik istnieje
        Optional<User> userOptional = userRepository.findById(id);

        if (userOptional.isPresent()) {
            // Jeśli użytkownik istnieje, usuń go
            userRepository.delete(userOptional.get());
        } else {
            // Jeśli użytkownik o danym id nie istnieje, można obsłużyć to odpowiednio
            throw new UserNotFoundException(id);
        }
    }

}
