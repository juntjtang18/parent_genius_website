package ca.parentgeniusai.website.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StrapiAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final Logger log = LoggerFactory.getLogger(StrapiAuthenticationFilter.class);
    private final RestTemplate restTemplate;
    private final String strapiRootUrl;
    
    // This repository is injected from SecurityConfig to save the SecurityContext
    private SecurityContextRepository securityContextRepository;

    public void setSecurityContextRepository(SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    public StrapiAuthenticationFilter(RestTemplate restTemplate, String strapiRootUrl) {
        super(); // Call to super constructor
        this.restTemplate = restTemplate;
        this.strapiRootUrl = strapiRootUrl;
        setFilterProcessesUrl("/do-login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        // This method correctly passes the auth request to the AuthenticationManager
        // The manager will call the authenticateWithStrapi static method
        String username = obtainUsername(request);
        String password = obtainPassword(request);
        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);
        setDetails(request, authRequest);
        return this.getAuthenticationManager().authenticate(authRequest);
    }
    
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                          Authentication authResult) throws IOException, ServletException {

        // --- START OF FIX ---
        // Before Spring Security erases the credentials, get the JWT and save it to the session.
        if (authResult.getCredentials() instanceof String) {
            String jwt = (String) authResult.getCredentials();
            HttpSession session = request.getSession(true); // true = create if doesn't exist
            session.setAttribute("STRAPI_JWT", jwt);
            
            log.info("JWT successfully stored in HttpSession for user: {}", authResult.getName());
            log.info("JWT stored is: {}", session.getAttribute("STRAPI_JWT"));
            
            ResponseCookie cookie = ResponseCookie.from("STRAPI_JWT", jwt)
                    .path("/")            // visible to all pages
                    .httpOnly(false)      // MUST be false so JS can read it
                    .secure(true)         // true in HTTPS prod; set false ONLY for local http
                    .sameSite("Lax")      // if Spring site != Strapi site & you do cross-site, use "None"
                    .maxAge(Duration.ofDays(7))
                    .build();
                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        } else {
            log.warn("Could not store JWT in session because it was not found in the Authentication object's credentials.");
        }
        // --- END OF FIX ---
        
        // This part saves the main Spring Security context to the session. It's still needed.
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        this.securityContextRepository.saveContext(context, request, response);

        // This calls the original success handler to redirect the user (e.g., to "/")
        super.successfulAuthentication(request, response, chain, authResult);
    }


    // This static method is called by StrapiAuthenticationManager
    public static UsernamePasswordAuthenticationToken authenticateWithStrapi(
            String username, String password, RestTemplate restTemplate, String strapiRootUrl) {
        
        // Code for authenticating with Strapi... (This part is correct)
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Map<String, String> authBody = Map.of("identifier", username, "password", password);
        HttpEntity<Map<String, String>> authRequest = new HttpEntity<>(authBody, authHeaders);

        try {
            Map<String, Object> authResponse = restTemplate.postForObject(strapiRootUrl + "/api/auth/local", authRequest, Map.class);
            if (authResponse == null || !authResponse.containsKey("jwt")) {
                log.error("Authentication failed: No JWT in response from /api/auth/local");
                return null;
            }
            String jwt = (String) authResponse.get("jwt");
            log.debug("Successfully received JWT from Strapi.");

            HttpHeaders userDetailsHeaders = new HttpHeaders();
            userDetailsHeaders.setBearerAuth(jwt);
            HttpEntity<String> userDetailsEntity = new HttpEntity<>(userDetailsHeaders);

            String userDetailsUrl = strapiRootUrl + "/api/users/me?populate=role";

            Map<String, Object> userDetails = restTemplate.exchange(
                userDetailsUrl,
                HttpMethod.GET,
                userDetailsEntity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            ).getBody();

            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_AUTHENTICATED"));

            if (userDetails != null && userDetails.get("role") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> roleMap = (Map<String, Object>) userDetails.get("role");
                String roleName = (String) roleMap.get("name");
                if (roleName != null && !roleName.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()));
                    log.info("Success! User '{}' has been assigned role: {}", username, roleName.toUpperCase());
                }
            }
            
            // The JWT is placed in the 'credentials' field here.
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                username,
                jwt, // <-- The JWT
                authorities
            );

            authToken.setDetails(userDetails);
            return authToken;

        } catch (Exception e) {
            log.error("Failed to authenticate with Strapi", e);
            return null;
        }
    }
}