package ca.parentgeniusai.website.config;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class StrapiAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final RestTemplate restTemplate;

    public StrapiAuthenticationFilter(RestTemplate restTemplate, AuthenticationManager authenticationManager) {
        super("/login");
        this.restTemplate = restTemplate;
        setAuthenticationManager(authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationException("Authentication method not supported: " + request.getMethod()) {};
        }

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || password == null) {
            throw new AuthenticationException("Username or password missing") {};
        }

        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);
        return getAuthenticationManager().authenticate(authRequest);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        response.sendRedirect("/login?error=true");
    }

    public static Authentication authenticateWithStrapi(String username, String password, RestTemplate restTemplate, String strapiUrl) throws AuthenticationException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"identifier\":\"%s\",\"password\":\"%s\"}", username, password);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            Map<String, Object> strapiResponse = restTemplate.exchange(
                strapiUrl + "auth/local",
                HttpMethod.POST,
                entity,
                Map.class
            ).getBody();

            if (strapiResponse != null && strapiResponse.containsKey("jwt")) {
                String jwt = (String) strapiResponse.get("jwt");
                Map<String, Object> user = (Map<String, Object>) strapiResponse.get("user");
                String email = (String) user.get("email");
                String role = (String) ((Map<String, Object>) user.get("role")).get("type");

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                );
                auth.setDetails(jwt);
                return auth;
            }
        } catch (Exception e) {
            throw new AuthenticationException("Strapi authentication failed: " + e.getMessage()) {};
        }

        throw new AuthenticationException("Invalid credentials from Strapi") {};
    }
}