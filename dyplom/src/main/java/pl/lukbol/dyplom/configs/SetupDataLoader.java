package pl.lukbol.dyplom.configs;

import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pl.lukbol.dyplom.classes.Privilege;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.repositories.PrivilegeRepository;
import pl.lukbol.dyplom.repositories.RoleRepository;
import pl.lukbol.dyplom.repositories.UserRepository;

import java.util.Collection;
import java.util.List;

@Component
public class SetupDataLoader implements ApplicationListener<ContextRefreshedEvent> {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_EMPLOYEE = "ROLE_EMPLOYEE";
    private static final String ROLE_CLIENT = "ROLE_CLIENT";

    private static final String PRIVILEGE_READ = "READ_PRIVILEGE";
    private static final String PRIVILEGE_WRITE = "WRITE_PRIVILEGE";

    private static final String ADMIN_EMAIL = "admin@testowy.com";
    private static final String ADMIN_NAME = "Admin";
    private static final String ADMIN_PASSWORD = "admin1234";
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private boolean alreadySetup = false;

    public SetupDataLoader(PasswordEncoder passwordEncoder,
                           UserRepository userRepository,
                           RoleRepository roleRepository,
                           PrivilegeRepository privilegeRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.privilegeRepository = privilegeRepository;
    }

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (alreadySetup) return;

        Privilege readPrivilege = createPrivilegeIfNotFound(PRIVILEGE_READ);
        Privilege writePrivilege = createPrivilegeIfNotFound(PRIVILEGE_WRITE);

        createRoleIfNotFound(ROLE_ADMIN, List.of(readPrivilege, writePrivilege));
        createRoleIfNotFound(ROLE_EMPLOYEE, List.of(readPrivilege));
        createRoleIfNotFound(ROLE_CLIENT, List.of(readPrivilege));

        if (userRepository.findByEmail(ADMIN_EMAIL) == null) {
            Role adminRole = roleRepository.findByName(ROLE_ADMIN);
            User adminUser = new User();
            adminUser.setName(ADMIN_NAME);
            adminUser.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
            adminUser.setEmail(ADMIN_EMAIL);
            adminUser.setRoles(List.of(adminRole));
            userRepository.save(adminUser);
        }

        alreadySetup = true;
    }

    @Transactional
    Privilege createPrivilegeIfNotFound(String name) {
        Privilege privilege = privilegeRepository.findByName(name);
        if (privilege == null) {
            privilege = new Privilege(name);
            privilegeRepository.save(privilege);
        }
        return privilege;
    }

    @Transactional
    Role createRoleIfNotFound(String name, Collection<Privilege> privileges) {
        Role role = roleRepository.findByName(name);
        if (role == null) {
            role = new Role(name);
            role.setPrivileges(privileges);
            roleRepository.save(role);
        }
        return role;
    }
}

