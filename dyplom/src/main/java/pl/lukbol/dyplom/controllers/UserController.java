package pl.lukbol.dyplom.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.services.UserService;

import java.util.List;

@RestController
public class UserController {

    private static final String PATH = "/users";
    private static final String PATH2 = "/users/{id}";
    @Autowired
    private UserService userService;
    private UserRepository userRepository;
    @GetMapping(PATH)
    List<User> displayAllUsers() {return userService.displayAllUsers();   }

    @PostMapping(PATH)
    User AddNewUser(@RequestBody User newUser) {return userService.saveUser(newUser);  }

    @GetMapping(PATH2)
    User GetUserById(@PathVariable Long id) {return userService.GetUserById(id);}

    @PutMapping(PATH2)
    User updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
       return userService.updateUser(id, updatedUser);
    }
    @DeleteMapping(PATH2)
    void DeleteUserById(@PathVariable Long id) { userService.DeleteUserById(id);}

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User newUser) {
        return  userService.registerUser(newUser);
    }

    @PostMapping("/login")
    public String processLogin(Authentication authentication, HttpServletRequest request) {
        if (authentication != null) {
            return "redirect:/users";
        } else {
            request.setAttribute("error", true);
            return "login";
        }
    }
    @GetMapping(value = "/login", produces = "text/html")
    public ModelAndView showLoginPage() {
        return new ModelAndView("login.html");
    }

}
