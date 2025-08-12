package pl.lukbol.dyplom.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import pl.lukbol.dyplom.repositories.RoleRepository;
import pl.lukbol.dyplom.repositories.UserRepository;

@RestController
@RequiredArgsConstructor
public class LoginController {
    private final UserRepository iUserRepository;
    private final RoleRepository iRoleRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
    @Autowired
    private AuthenticationManager authenticationManager;
    private SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @PostMapping("/login")
    public ModelAndView login(
            @RequestParam String email,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response) {

        final String VIEW_PROFILE = "profile";
        final String VIEW_LOGIN = "login";
        final String ERROR_INVALID_CREDENTIALS = "Podano nieprawidłowe dane logowania";

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(email, password);

        try {
            Authentication authentication = authenticationManager.authenticate(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            SecurityContext context = securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(authentication);
            securityContextHolderStrategy.setContext(context);

            securityContextRepository.saveContext(context, request, response);

            return new ModelAndView(VIEW_PROFILE);

        } catch (AuthenticationException e) {
            ModelAndView modelAndView = new ModelAndView(VIEW_LOGIN);
            if (e instanceof BadCredentialsException) {
                modelAndView.addObject("error", ERROR_INVALID_CREDENTIALS);
            }
            return modelAndView;
        }
    }


}