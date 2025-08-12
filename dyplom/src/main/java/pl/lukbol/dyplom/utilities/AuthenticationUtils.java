package pl.lukbol.dyplom.utilities;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

public class AuthenticationUtils {

    private static String OAUTH_ATTR = "email";

    public static String checkmail(Object authentication) {
        if (authentication instanceof DefaultOidcUser) {
            DefaultOidcUser oauth2User = (DefaultOidcUser) authentication;
            return oauth2User.getAttribute(OAUTH_ATTR);
        } else if (authentication instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication;
            return userDetails.getUsername();
        } else if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            String email = oauthToken.getPrincipal().getAttribute(OAUTH_ATTR);
            return email;
        } else if (authentication instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken oauthToken = (UsernamePasswordAuthenticationToken) authentication;
            String email = oauthToken.getName();
            return email;
        } else {
            return "notfound";
        }
    }
}
