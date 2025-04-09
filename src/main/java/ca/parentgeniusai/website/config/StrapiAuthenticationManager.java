package ca.parentgeniusai.website.config;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.client.RestTemplate;

public class StrapiAuthenticationManager implements AuthenticationManager {

    private final RestTemplate restTemplate;
    private final String strapiRootUrl;

    public StrapiAuthenticationManager(RestTemplate restTemplate, String strapiRootUrl) {
        this.restTemplate = restTemplate;
        this.strapiRootUrl = strapiRootUrl;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        return StrapiAuthenticationFilter.authenticateWithStrapi(username, password, restTemplate, strapiRootUrl);
    }
}