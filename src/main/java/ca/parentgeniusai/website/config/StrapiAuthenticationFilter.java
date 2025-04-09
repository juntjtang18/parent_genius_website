package ca.parentgeniusai.website.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Map;

public class StrapiAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private static final Logger logger = LoggerFactory.getLogger(StrapiAuthenticationFilter.class);
    private final RestTemplate restTemplate;

    @Value("${strapi.root.url:http://localhost:8080/}")
    private String strapiRootUrl;

    public StrapiAuthenticationFilter(RestTemplate restTemplate, AuthenticationManager authenticationManager) {
        super(new AntPathRequestMatcher("/do-login", "POST"));
        this.restTemplate = restTemplate;
        setAuthenticationManager(authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationException("Authentication method not supported: " + request.getMethod()) {};
        }

        String email = request.getParameter("username"); // Form uses "username" but it's email
        String password = request.getParameter("password");

        if (email == null || password == null) {
            throw new AuthenticationException("Email or password missing") {};
        }

        logger.debug("Attempting Strapi authentication for email: " + email);
        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(email, password);
        Authentication auth = getAuthenticationManager().authenticate(authRequest);

        // Fetch username from Strapi
        String jwt = (String) auth.getDetails();
        String meUrl = strapiRootUrl + "api/users/me";
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(jwt);
        HttpEntity<String> authEntity = new HttpEntity<>(authHeaders);
        Map<String, Object> meResponse;
        String username;
        try {
            meResponse = restTemplate.exchange(meUrl, HttpMethod.GET, authEntity, Map.class).getBody();
            username = (String) meResponse.get("username");
            logger.debug("Fetched username: " + username);
        } catch (Exception e) {
            logger.warn("Failed to fetch username, using email as fallback: " + e.getMessage());
            username = email; // Fallback to email if fetch fails
        }

        // Set username as principal
        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
            username, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        newAuth.setDetails(jwt);
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
        logger.debug("Authentication set with username " + username + " and session ID: " + session.getId());

        return newAuth;
    }

    public static Authentication authenticateWithStrapi(String username, String password, RestTemplate restTemplate, String strapiRootUrl) throws AuthenticationException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"identifier\":\"%s\",\"password\":\"%s\"}", username, password);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        Map<String, Object> strapiResponse;
        try {
            strapiResponse = restTemplate.exchange(
                strapiRootUrl + "api/auth/local", HttpMethod.POST, entity, Map.class
            ).getBody();
        } catch (Exception e) {
            throw new AuthenticationException("Strapi authentication failed: " + e.getMessage()) {};
        }

        if (strapiResponse == null || !strapiResponse.containsKey("jwt")) {
            throw new AuthenticationException("No JWT in Strapi response") {};
        }

        String jwt = (String) strapiResponse.get("jwt");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            username, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        auth.setDetails(jwt);
        return auth;
    }
}