package pl.lukbol.dyplom.configs;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.repositories.RoleRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import pl.lukbol.dyplom.utilities.SecurityPaths;

import java.security.SecureRandom;
import java.util.*;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ROLE_NAME = "ROLE_CLIENT";
    private final CustomUserDetailsService customerUserDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final SecurityPaths securityPaths;
    @Autowired
    private final RoleRepository roleRepository;
    String generatedPassword = generateRandomPassword();
    @Value("/profile")
    private String successUrl;
    @Value("/login")
    private String failureUrl;
    @Autowired
    private UserRepository userRepository;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    protected SecurityFilterChain chains(HttpSecurity http) throws Exception {


        http
                .csrf().disable()
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(securityPaths.CLIENT_EMPLOYEE_ADMIN_PATHS).hasAnyRole("CLIENT", "EMPLOYEE", "ADMIN")
                        .requestMatchers(securityPaths.ADMIN_PATHS).hasRole("ADMIN")
                        .requestMatchers(securityPaths.ADMIN_EMPLOYEE_PATHS).hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers(securityPaths.PERMIT_ALL_PATHS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/*").permitAll()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .logout(l -> l
                        .logoutSuccessUrl("/").permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(this.oidcUserService())
                        )
                        .successHandler(successHandler())
                        .failureHandler(failureHandler())
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/profile")
                        .defaultSuccessUrl("/profile")
                        .permitAll()
                )
                .httpBasic()
                .and()
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                )
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(new HttpSessionSecurityContextRepository())
                );

        return http.build();
    }

    @Transactional
    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);
            String email = oidcUser.getEmail();
            List<User> users = userRepository.findAll();
            OidcUser finalOidcUser = oidcUser;
            User user = users.stream()
                    .filter(u -> u.getEmail().equals(email))
                    .findFirst()
                    .orElseGet(() -> {
                        User newUser = new User(finalOidcUser.getFullName(), email, passwordEncoder().encode(generatedPassword), false);
                        newUser.setRoles(Arrays.asList(roleRepository.findByName(ROLE_NAME)));
                        userRepository.save(newUser);
                        return newUser;
                    });

            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
            Collection<Role> rolesCollection = user.getRoles();
            for (Role element : rolesCollection) {
                mappedAuthorities.add(new SimpleGrantedAuthority(element.getName()));
            }

            oidcUser = new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
            return oidcUser;
        };
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    private boolean emailExists(String email) {
        return userRepository.findByEmail(email) != null;
    }

    @Bean
    public AuthenticationManager authenticationManagerBean(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(customerUserDetailsService);
        return authenticationManagerBuilder.build();
    }

    @Bean
    SimpleUrlAuthenticationSuccessHandler successHandler() {
        return new SimpleUrlAuthenticationSuccessHandler(successUrl);
    }

    @Bean
    SimpleUrlAuthenticationFailureHandler failureHandler() {
        return new SimpleUrlAuthenticationFailureHandler(failureUrl);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    private String generateRandomPassword() {
        int passwordLength = 12;
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[passwordLength];
        random.nextBytes(bytes);
        String password = Base64.getEncoder().encodeToString(bytes);

        return password;
    }

}