package ca.parentgeniusai.website.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.client.RestTemplate;

public class StrapiAuthenticationManager implements AuthenticationManager {

    private final RestTemplate restTemplate;

    @Value("${strapi.url}")
    private String strapiUrl;

    public StrapiAuthenticationManager(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();

        if (username == null || password == null) {
            throw new AuthenticationException("Username or password is null") {};
        }

        return StrapiAuthenticationFilter.authenticateWithStrapi(username, password, restTemplate, strapiUrl);
    }
}