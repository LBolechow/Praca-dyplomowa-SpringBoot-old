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
import pl.lukbol.dyplom.configs.CustomOAuth2UserService;
import pl.lukbol.dyplom.classes.Role;
import pl.lukbol.dyplom.classes.User;
import pl.lukbol.dyplom.configs.CustomUserDetailsService;
import pl.lukbol.dyplom.repositories.RoleRepository;
import pl.lukbol.dyplom.repositories.UserRepository;
import java.security.SecureRandom;
import java.util.Base64;



import java.util.*;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig  {

    private final CustomUserDetailsService customerUserDetailsService ;
    private final CustomOAuth2UserService customOAuth2UserService ;

    String generatedPassword = generateRandomPassword();
    @Value("/profile")
    private String successUrl;
    @Value("/login")
    private String failureUrl;
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    protected SecurityFilterChain chains(HttpSecurity http) throws Exception {

        http
                //.cors()
                //.and()
                .csrf().disable()
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/user", "/profile/**",  "/currentDate", "/clientChat", "/ws-chat/**", "/ws-chat", "/api/conversation", "/sendToEmployees", "/topic/employees", "/app", "/topic/**", "/employeeChat", "/api/get_conversations", "/conversation/**", "/sendToConversation/**", "/api/conversation/**/latest-message", "/api/markConversationAsRead/**", "/api/markAllConversationsAsUnread/**", "/index", "/user/activateMail", "/user/checkCode", "/user/orders").hasAnyRole("CLIENT", "EMPLOYEE", "ADMIN")
                        .requestMatchers("/admin/**", "/search-users", "/panel_administratora","/users/delete/**", "/users/update/**", "/users/add"  ).hasRole("ADMIN")
                        .requestMatchers("/order/add",  "/users", "/caldendar", "/daily", "/daily/**", "/order/getDailyOrders", "/users/findByRole", "/order/checkAvailability", "/order/getOrderDetails/{id}", "/order/edit/{id}", "/order/checkAvailabilityNextDay", "/order/delete/{id}", "/materials", "/order/search", "/material/{id}", "/order/otherEmployee/{orderId}", "/user/employees-and-admins").hasAnyRole("ADMIN", "EMPLOYEE")

                        .requestMatchers( "/register", "/error", "/webjars/**", "/githubprivacyerror.html","/css/**", "/static/**", "/images/**",
                                "/fonts/**", "/scripts/**", "/error", "/login", "/", "/user2", "/favicon", "/usersonline", "/user/profile/{id}", "/get_message", "/favicon.ico", "/price_list", "/locked", "/api/conversation", "/ordersList", "/order", "/order/**", "/order/checkOrder/{idCode}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/*").permitAll()
                        // .anyRequest().authenticated()

                        .and()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .logout(l -> l
                        .logoutSuccessUrl("/").permitAll()
                )
                //.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                //and()
                .oauth2Login().userInfoEndpoint().oidcUserService(this.oidcUserService()).and()
                .successHandler(successHandler())
                .failureHandler(failureHandler())
                .and()
                .formLogin((form) -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/profile")
                        .defaultSuccessUrl("/profile")
                        .permitAll()
                ).
                httpBasic().and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                .and()
                .securityContext()
                .securityContextRepository(new HttpSessionSecurityContextRepository())
                .and();
        // .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);


        return http.build();


    }
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private final RoleRepository roleRepository;

    @Transactional
    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);
            String email = oidcUser.getEmail();

            // Fetch the user entity along with roles eagerly using findAll()
            List<User> users = userRepository.findAll();
            OidcUser finalOidcUser = oidcUser;
            User user = users.stream()
                    .filter(u -> u.getEmail().equals(email))
                    .findFirst()
                    .orElseGet(() -> {
                        User newUser = new User(finalOidcUser.getFullName(), email, passwordEncoder().encode(generatedPassword), null, false, false);
                        newUser.setRoles(Arrays.asList(roleRepository.findByName("ROLE_CLIENT")));
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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception
    { return authenticationConfiguration.getAuthenticationManager();}

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    private String generateRandomPassword() {
        int passwordLength = 12; // Długość hasła
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[passwordLength];
        random.nextBytes(bytes);

        // Zamiana losowych bajtów na ciąg znaków w formie Base64
        String password = Base64.getEncoder().encodeToString(bytes);

        // Możesz też dopasować wygenerowane hasło do swoich wymagań, na przykład usuwając znaki specjalne, itp.

        return password;
    }

}