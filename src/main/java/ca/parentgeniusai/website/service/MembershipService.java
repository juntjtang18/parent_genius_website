package ca.parentgeniusai.website.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class MembershipService {
    private static final Logger logger = LoggerFactory.getLogger(MembershipService.class);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
        new ParameterizedTypeReference<>() {};

    public static final List<Option> CHILD_AGES = List.of(
        new Option("0-2", "0 - 2", 1),
        new Option("3-5", "3 - 5", 4),
        new Option("6-8", "6 - 8", 7),
        new Option("9-12", "9 - 12", 10),
        new Option("13-18", "13 - 18", 15)
    );

    public static final List<Option> HOBBIES = List.of(
        new Option("reading", "Reading", null),
        new Option("sports", "Sports", null),
        new Option("music", "Music", null),
        new Option("art", "Arts & crafts", null),
        new Option("nature", "Nature", null),
        new Option("science", "Science", null),
        new Option("games", "Games", null),
        new Option("other", "Other", null)
    );

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${strapi.root.url}")
    private String strapiRootUrl;

    public String getSignupUrl() {
        return "/signup";
    }

    public String safeReturnPath(String fromCourse) {
        if (fromCourse == null || fromCourse.isBlank()) {
            return null;
        }
        String path = fromCourse.trim();
        if (!path.startsWith("/") || path.startsWith("//") || path.contains("://")) {
            logger.warn("Ignoring unsafe membership return path");
            return null;
        }
        return path;
    }

    public RegisterResult register(String email, String password) {
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            return RegisterResult.error("Please enter a valid email address.");
        }
        if (password == null || password.length() < 6) {
            return RegisterResult.error("Password must be at least 6 characters long.");
        }

        String cleanEmail = email.trim();
        String baseUsername = sanitizeUsername(cleanEmail.substring(0, cleanEmail.indexOf('@')));
        String username = baseUsername;
        Map<String, Object> body = null;

        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                Map<String, String> payload = new LinkedHashMap<>();
                payload.put("email", cleanEmail);
                payload.put("password", password);
                payload.put("username", username);
                body = exchange(root() + "api/auth/local/register", HttpMethod.POST, jsonEntity(payload, null), MAP_TYPE);
                break;
            } catch (HttpClientErrorException e) {
                String raw = e.getResponseBodyAsString();
                logger.warn("Membership register attempt {} failed: {}", attempt + 1, raw);
                if (raw.contains("Username are already taken") || raw.contains("username is already taken")) {
                    username = baseUsername + (attempt + 1);
                    continue;
                }
                return RegisterResult.error(mapStrapiError(raw));
            } catch (Exception e) {
                logger.error("Membership register failed: {}", e.getMessage(), e);
                return RegisterResult.error("Signup failed due to a server error. Please try again later.");
            }
        }

        if (body == null || !(body.get("jwt") instanceof String jwt) || jwt.isBlank()) {
            return RegisterResult.error("Signup succeeded but authentication failed. Please sign in manually.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> user = body.get("user") instanceof Map<?, ?> map
            ? (Map<String, Object>) map : Map.of();
        Number userId = user.get("id") instanceof Number n ? n : null;
        String registeredUsername = user.get("username") instanceof String s ? s : username;
        logger.info("Registered Strapi user {} (id={})", registeredUsername, userId);
        return RegisterResult.ok(jwt, registeredUsername, userId);
    }

    public void updatePersonalization(String jwt, String childAgeId, String hobbyId) {
        if (jwt == null || jwt.isBlank()) {
            return;
        }
        Integer age = CHILD_AGES.stream()
            .filter(option -> option.id().equals(childAgeId))
            .map(Option::age)
            .findFirst()
            .orElse(null);
        String hobby = HOBBIES.stream()
            .filter(option -> option.id().equals(hobbyId))
            .map(Option::label)
            .findFirst()
            .orElse(null);
        if (age == null && (hobby == null || hobby.isBlank())) {
            return;
        }

        Map<String, Object> child = new LinkedHashMap<>();
        if (age != null) {
            child.put("age", age);
        }
        if (hobby != null && !hobby.isBlank()) {
            child.put("name", hobby);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("children", List.of(child));
        Map<String, Object> payload = Map.of("data", data);

        try {
            exchange(root() + "api/user-profiles/mine", HttpMethod.PUT, jsonEntity(payload, jwt), MAP_TYPE);
            logger.info("Updated membership personalization for new user");
        } catch (Exception e) {
            logger.warn("Could not save optional personalization: {}", e.getMessage());
        }
    }

    private HttpEntity<Object> jsonEntity(Object payload, String jwt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (jwt != null && !jwt.isBlank()) {
            headers.setBearerAuth(jwt);
        }
        return new HttpEntity<>(payload, headers);
    }

    private Map<String, Object> exchange(String url, HttpMethod method, HttpEntity<?> entity,
                                         ParameterizedTypeReference<Map<String, Object>> type) {
        return restTemplate.exchange(url, method, entity, type).getBody();
    }

    private String root() {
        return strapiRootUrl.endsWith("/") ? strapiRootUrl : strapiRootUrl + "/";
    }

    private static String sanitizeUsername(String raw) {
        String cleaned = raw.replaceAll("[^A-Za-z0-9._-]", "");
        if (cleaned.length() < 3) {
            cleaned = (cleaned + "parent").substring(0, Math.max(3, cleaned.length()));
        }
        return cleaned.length() > 24 ? cleaned.substring(0, 24) : cleaned;
    }

    private static String mapStrapiError(String raw) {
        if (raw == null) {
            return "Signup failed. Please try again.";
        }
        String lower = raw.toLowerCase();
        if (lower.contains("already taken") || lower.contains("already exist")) {
            return "Email or username is already taken.";
        }
        if (lower.contains("password") && lower.contains("at least 6")) {
            return "Password must be at least 6 characters long.";
        }
        if (lower.contains("email") && lower.contains("valid")) {
            return "Please enter a valid email address.";
        }
        return "Signup failed. Please try again.";
    }

    public record Option(String id, String label, Integer age) {}

    public record RegisterResult(boolean ok, String jwt, String username, Number userId, String error) {
        static RegisterResult ok(String jwt, String username, Number userId) {
            return new RegisterResult(true, jwt, username, userId, null);
        }

        static RegisterResult error(String message) {
            return new RegisterResult(false, null, null, null, message);
        }
    }
}
