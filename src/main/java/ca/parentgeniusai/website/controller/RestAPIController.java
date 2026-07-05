package ca.parentgeniusai.website.controller;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
public class RestAPIController {
	@Value("${strapi.root.url}")
	private String strapiRootUrl;
	
	@Value("${strapi.auth-token}")
	private String authToken;

    private final RestTemplate restTemplate;

    public RestAPIController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
	
    @GetMapping("/api/languages")
    public Map<String, String> getSupportedLanguages() {
    	System.out.print("/api/languages called\n");
    	Map<String, String> languages = new LinkedHashMap<>();
        languages.put("en", "English");
        languages.put("fr", "French");
        languages.put("de", "German");
        languages.put("es", "Spanish");
        languages.put("zh", "Chinese (Simplified)"); // Use zh_CN instead of zh
        languages.put("ja", "Japanese");
        languages.put("ko", "Korean");
        languages.put("si", "Sinhala");
        languages.put("hi", "Hindi");
        languages.put("bn", "Bengali");
        return languages;
    }
    
    @GetMapping("/api/strapi_config")
    public Map<String, String> getStrapiConfig() {
    	return Map.of(
    			"STRAP_ROOTRUL", strapiRootUrl,
    			"AUTH_TOKEN", authToken
    			);
    			
    }

    @GetMapping("/api/subscription/entitlements")
    public ResponseEntity<Map<String, Object>> getSubscriptionEntitlements(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String jwt = (session != null) ? (String) session.getAttribute("STRAPI_JWT") : null;
        if (jwt == null || jwt.isBlank()) {
            return ResponseEntity.ok(Map.of("entitlements", List.of()));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        String endpoint = strapiRootUrl.endsWith("/")
            ? strapiRootUrl + "api/v1.1/subscription/me"
            : strapiRootUrl + "/api/v1.1/subscription/me";

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                endpoint,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            if (response.getBody() == null) {
                return ResponseEntity.ok(Map.of("entitlements", List.of()));
            }
            return ResponseEntity.ok(response.getBody());
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            return ResponseEntity.ok(Map.of("entitlements", List.of()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("entitlements", List.of()));
        }
    }
}
