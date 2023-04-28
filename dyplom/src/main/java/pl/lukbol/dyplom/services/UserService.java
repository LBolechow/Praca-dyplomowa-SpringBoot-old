package pl.lukbol.dyplom.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.exceptions.UserNotFoundException;
import pl.lukbol.dyplom.repositories.RoleRepository;
import pl.lukbol.dyplom.repositories.UserRepository;


import java.util.Arrays;
import java.util.List;

@Service
public class UserService {


    public final RoleRepository roleRepository;
    public final UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;


    public UserService(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    public List<User> displayAllUsers() {
        return userRepository.findAll();
    }

    public User saveUser(@RequestBody User newUser) {
        return userRepository.save(newUser);
    }

    public  User GetUserById(@PathVariable Long id) {

        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
        User user = GetUserById(id);
        user.setName(updatedUser.getName());
        user.setEmail(updatedUser.getEmail());
        user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        return saveUser(user);
    }

    public void DeleteUserById(@PathVariable Long id) {
        userRepository.deleteById(id);
    }

    public ResponseEntity<String> registerUser(@RequestBody User newUser) {
        Role clientRole = roleRepository.findByName("ROLE_CLIENT");
        User client = new User();
        client.setName(newUser.getName());
        client.setEmail(newUser.getEmail());
        client.setPassword(passwordEncoder.encode(newUser.getPassword()));
        client.setRoles(Arrays.asList(clientRole));
        saveUser(client);
        return ResponseEntity.ok("Pomyślnie zarejestrowano użytkownika.");
    }



}

