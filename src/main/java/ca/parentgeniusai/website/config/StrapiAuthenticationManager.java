package ca.parentgeniusai.website.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.client.RestTemplate;

public class StrapiAuthenticationManager implements AuthenticationManager {

    private static final Logger log = LoggerFactory.getLogger(StrapiAuthenticationManager.class);
    private final RestTemplate restTemplate;
    private final String strapiRootUrl;

    public StrapiAuthenticationManager(RestTemplate restTemplate, String strapiRootUrl) {
        this.restTemplate = restTemplate;
        this.strapiRootUrl = strapiRootUrl;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getPrincipal() != null ? authentication.getPrincipal().toString() : null;
        String password = authentication.getCredentials() != null ? authentication.getCredentials().toString() : null;

        if (username == null || password == null) {
            log.error("Username or password is null");
            throw new AuthenticationException("Username or password is missing") {};
        }

        log.debug("Authenticating user: {}", username);

        UsernamePasswordAuthenticationToken authToken = StrapiAuthenticationFilter.authenticateWithStrapi(
            username, password, restTemplate, strapiRootUrl
        );

        if (authToken == null) {
            log.error("Strapi authentication failed for user: {}", username);
            throw new AuthenticationException("Strapi authentication failed") {};
        }

        return authToken;
    }
}