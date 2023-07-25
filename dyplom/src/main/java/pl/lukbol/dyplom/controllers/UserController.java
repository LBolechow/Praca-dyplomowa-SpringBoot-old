package pl.lukbol.dyplom.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.repositories.RoleRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.UserService;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

    @GetMapping(value ="/user/add", consumes = {"*/*"})
    public void registerUser(@RequestParam("name") String name, @RequestParam("email") String email, @RequestParam("password") String password,  HttpServletRequest req, HttpServletResponse resp) {
        User newUsr = new User(name,email, passwordEncoder.encode(password));
        newUsr.setRoles(Arrays.asList(roleRepository.findByName("ROLE_CLIENT")));
        if (emailExists(newUsr.getEmail())) {
            req.getSession().setAttribute("message", "Użytkownik o takim adresie email już instnieje.");
        } else {
            userRepository.save(newUsr);
            req.getSession().setAttribute("message", "Poprawnie utworzono użytkownika.");
        }
        try {
            resp.sendRedirect(req.getContextPath() + "/");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }



}
