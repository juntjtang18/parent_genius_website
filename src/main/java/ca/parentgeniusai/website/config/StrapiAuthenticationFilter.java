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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

        // Step 1: Authenticate with Strapi /auth/local
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"identifier\":\"%s\",\"password\":\"%s\"}", email, password);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        Map<String, Object> strapiResponse;
        try {
            strapiResponse = restTemplate.exchange(
                strapiRootUrl + "api/auth/local", HttpMethod.POST, entity, Map.class
            ).getBody();
            logger.debug("Strapi /auth/local response: " + strapiResponse);
        } catch (Exception e) {
            logger.error("Strapi authentication failed: " + e.getMessage());
            throw new AuthenticationException("Authentication failed: " + e.getMessage()) {};
        }

        if (strapiResponse == null || !strapiResponse.containsKey("jwt")) {
            throw new AuthenticationException("No JWT in Strapi response") {};
        }

        String jwt = (String) strapiResponse.get("jwt");
        Map<String, Object> user = (Map<String, Object>) strapiResponse.get("user");
        String username = user != null && user.containsKey("username") ? (String) user.get("username") : email;

        // Step 2: Fetch role from /users/me
        String roleName = "AUTHENTICATED"; // Default to Strapi's "Authenticated" role
        String meUrl = strapiRootUrl + "api/users/me?populate=role";
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(jwt);
        HttpEntity<String> authEntity = new HttpEntity<>(authHeaders);
        try {
            Map<String, Object> meResponse = restTemplate.exchange(meUrl, HttpMethod.GET, authEntity, Map.class).getBody();
            logger.debug("Strapi /users/me response: " + meResponse);
            if (meResponse != null && meResponse.containsKey("role")) {
                Map<String, Object> role = (Map<String, Object>) meResponse.get("role");
                if (role != null && role.containsKey("name")) {
                    roleName = (String) role.get("name"); // e.g., "editor"
                } else {
                    logger.warn("Role object from /users/me found but no 'name' field: " + role);
                }
            } else {
                logger.warn("No 'role' field in /users/me response: " + meResponse);
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch role from /users/me, defaulting to 'AUTHENTICATED': " + e.getMessage());
        }

        // Step 3: Map Strapi role to Spring Security authority
        String authority = "ROLE_" + roleName.toUpperCase().replace(" ", "_"); // e.g., "ROLE_EDITOR"
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(authority));

        // Step 4: Create and set authentication with correct role
        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
            username, null, authorities
        );
        newAuth.setDetails(jwt);
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
        logger.debug("Authentication set with username " + username + ", role: " + roleName + ", and session ID: " + session.getId());

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
        Map<String, Object> user = (Map<String, Object>) strapiResponse.get("user");
        String authenticatedUsername = user != null && user.containsKey("username") ? (String) user.get("username") : username;

        // Fetch role from /users/me
        String roleName = "AUTHENTICATED"; // Default to Strapi's "Authenticated" role
        String meUrl = strapiRootUrl + "api/users/me?populate=role";
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(jwt);
        HttpEntity<String> authEntity = new HttpEntity<>(authHeaders);
        try {
            Map<String, Object> meResponse = restTemplate.exchange(meUrl, HttpMethod.GET, authEntity, Map.class).getBody();
            if (meResponse != null && meResponse.containsKey("role")) {
                Map<String, Object> role = (Map<String, Object>) meResponse.get("role");
                if (role != null && role.containsKey("name")) {
                    roleName = (String) role.get("name");
                }
            }
        } catch (Exception e) {
            // Log warning but proceed with default role
        }

        String authority = "ROLE_" + roleName.toUpperCase().replace(" ", "_");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            authenticatedUsername, null, Collections.singletonList(new SimpleGrantedAuthority(authority))
        );
        auth.setDetails(jwt);
        return auth;
    }
}