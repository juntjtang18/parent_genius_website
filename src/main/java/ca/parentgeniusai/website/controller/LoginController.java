package ca.parentgeniusai.website.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Controller
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private final BCryptPasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    @Value("${strapi.root.url:http://localhost:8080/}")
    private String strapiRootUrl;

    private static final Map<String, String> ERROR_MESSAGES = new HashMap<>();

    static {
        ERROR_MESSAGES.put("ValidationError:password must be at least 6 characters", "Password must be at least 6 characters long");
        ERROR_MESSAGES.put("ValidationError:email must be a valid email", "Please enter a valid email address");
        ERROR_MESSAGES.put("ValidationError:username must be at least 3 characters", "Username must be at least 3 characters long");
        ERROR_MESSAGES.put("ValidationError:username is invalid", "Username is invalid or already taken");
        ERROR_MESSAGES.put("ApplicationError:Email or Username are already taken", "Email or username is already taken");
        ERROR_MESSAGES.put("ApplicationError:An error occurred during account creation", "An error occurred during account creation");
        ERROR_MESSAGES.put("ValidationError:", "Please check your input and try again");
        ERROR_MESSAGES.put("ApplicationError:", "Signup failed. Please try again");
        ERROR_MESSAGES.put("", "Signup failed due to an unexpected error");
    }

    public LoginController(BCryptPasswordEncoder passwordEncoder, RestTemplate restTemplate) {
        this.passwordEncoder = passwordEncoder;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/signin")
    public String signin(Model model, @RequestParam(value = "error", required = false) String error, HttpServletRequest request) {
        logger.info("Endpoint /signin triggered.");
        model.addAttribute("error", error != null);
        String referer = request.getHeader("Referer");
        model.addAttribute("referer", referer != null && !referer.contains("/signin") ? referer : "/");
        // No need for model.addAttribute("auth", ...) - handled by GlobalControllerAdvice
        return "signin";
    }

    @GetMapping("/protected")
    public String protectedPage(Model model) {
        logger.info("Accessing /protected");
        // Auth is available via GlobalControllerAdvice; add username if needed
        if (model.containsAttribute("auth") && model.getAttribute("auth") != null) {
            model.addAttribute("username", ((Authentication) model.getAttribute("auth")).getName());
        } else {
            model.addAttribute("username", "Guest");
        }
        return "protected";
    }

    @GetMapping("/generate-hash")
    @ResponseBody
    public String generatePasswordHash(@RequestParam String password) {
        return "Generated hash for '" + password + "': " + passwordEncoder.encode(password);
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        logger.info("Showing signup form");
        // No need for model.addAttribute("auth", ...) - handled by GlobalControllerAdvice
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(
            @RequestParam String email,
            @RequestParam String password,
            Model model,
            HttpServletRequest request) {
        logger.info("Processing signup for email: " + email);
        model.addAttribute("email", email);
        model.addAttribute("password", password);

        String baseUsername = email.substring(0, email.indexOf("@"));
        logger.debug("Generated base username: " + baseUsername);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String username = baseUsername;
            Map<String, Object> signupResponse = null;
            int attempt = 0;
            final int maxAttempts = 10;

            // Try registering with unique username
            while (attempt < maxAttempts) {
                String body = String.format("{\"email\":\"%s\",\"password\":\"%s\",\"username\":\"%s\"}", email, password, username);
                HttpEntity<String> entity = new HttpEntity<>(body, headers);

                try {
                    String signupUrl = strapiRootUrl + "api/auth/local/register";
                    logger.info("Attempting signup with username: " + username);
                    signupResponse = restTemplate.exchange(
                        signupUrl, HttpMethod.POST, entity, Map.class
                    ).getBody();
                    logger.info("Signup response: " + signupResponse);
                    break; // Success, exit loop
                } catch (HttpClientErrorException e) {
                    String rawResponse = e.getResponseBodyAsString();
                    logger.debug("Signup attempt failed: " + rawResponse);
                    if (rawResponse.contains("Username are already taken")) {
                        attempt++;
                        username = baseUsername + attempt; // e.g., test1, test2
                        logger.debug("Username taken, retrying with: " + username);
                    } else {
                        throw e; // Other errors (e.g., email taken) bubble up
                    }
                }
            }

            if (signupResponse == null) {
                logger.warn("Signup failed: exceeded max attempts for username variations");
                model.addAttribute("signupError", "Too many users with similar usernames. Please try a different email.");
                return "signup";
            }

            if (!signupResponse.containsKey("jwt")) {
                logger.warn("Signup succeeded but no JWT in response: " + signupResponse);
                model.addAttribute("signupError", "Signup succeeded but authentication failed. Please sign in manually.");
                return "signup";
            }

            String jwt = (String) signupResponse.get("jwt");
            Map<String, Object> user = (Map<String, Object>) signupResponse.get("user");
            Integer userId = (Integer) user.get("id");
            String registeredUsername = (String) user.get("username");

            // Confirm the user (optional)
            String updateUserUrl = strapiRootUrl + "api/users/" + userId;
            String updateBody = "{\"confirmed\":true}";
            HttpEntity<String> updateEntity = new HttpEntity<>(updateBody, headers);
            logger.info("Confirming user " + userId + " at: " + updateUserUrl);
            try {
                restTemplate.exchange(updateUserUrl, HttpMethod.PUT, updateEntity, Map.class);
                logger.debug("User " + userId + " confirmed successfully");
            } catch (Exception e) {
                logger.warn("Failed to confirm user " + userId + ": " + e.getMessage());
            }

            // Authenticate with username
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                registeredUsername, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
            auth.setDetails(jwt);
            SecurityContextHolder.getContext().setAuthentication(auth);

            String sessionId = request.getSession(true).getId();
            request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            logger.info("User " + registeredUsername + " signed up and authenticated successfully with session ID: " + sessionId);

            logger.debug("Returning redirect:/");
            return "redirect:/";

        } catch (HttpClientErrorException e) {
            String rawResponse = e.getResponseBodyAsString();
            logger.error("Signup failed with status " + e.getStatusCode() + ": " + rawResponse);

            try {
                Map<String, Object> errorResponse = e.getResponseBodyAs(Map.class);
                String errorMessage = ERROR_MESSAGES.get("");

                if (errorResponse != null && errorResponse.containsKey("error")) {
                    Map<String, Object> error = (Map<String, Object>) errorResponse.get("error");
                    String errorName = (String) error.get("name");
                    String message = (String) error.get("message");

                    String key = errorName != null && message != null ? errorName + ":" + message : errorName + ":";
                    errorMessage = ERROR_MESSAGES.getOrDefault(key, ERROR_MESSAGES.get(errorName + ":"));
                }

                model.addAttribute("signupError", errorMessage);
            } catch (Exception parseEx) {
                logger.error("Error parsing Strapi response: " + parseEx.getMessage());
                model.addAttribute("signupError", "Signup failed. Please try again");
            }
            logger.debug("Returning signup due to HttpClientErrorException");
            return "signup";
        } catch (Exception e) {
            logger.error("Unexpected signup exception: " + e.getMessage(), e);
            model.addAttribute("signupError", "Signup failed due to a server error. Please try again later");
            logger.debug("Returning signup due to unexpected exception");
            return "signup";
        }
    }
}