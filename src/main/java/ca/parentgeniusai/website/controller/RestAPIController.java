package ca.parentgeniusai.website.controller;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/api/course-files/download")
    public ResponseEntity<byte[]> downloadCourseFile(
            @RequestParam String url,
            @RequestParam(required = false) String name) {
        URI requested;
        try {
            requested = URI.create(url).normalize();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        if (requested.getScheme() == null || requested.getHost() == null) {
            return ResponseEntity.badRequest().build();
        }

        String root = strapiRootUrl.endsWith("/") ? strapiRootUrl : strapiRootUrl + "/";
        URI allowed = URI.create(root);
        if (!isAllowedDownloadUrl(allowed, requested)) {
            return ResponseEntity.badRequest().build();
        }

        try {
            ResponseEntity<byte[]> remote = restTemplate.exchange(
                    requested,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    byte[].class
            );
            if (!remote.getStatusCode().is2xxSuccessful() || remote.getBody() == null) {
                return ResponseEntity.notFound().build();
            }

            String path = requested.getPath();
            String filename = (name == null || name.isBlank())
                    ? path.substring(path.lastIndexOf('/') + 1)
                    : name.replaceAll("[\\\\/]", "");

            HttpHeaders headers = new HttpHeaders();
            MediaType type = remote.getHeaders().getContentType();
            headers.setContentType(type != null ? type : MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
            return ResponseEntity.ok().headers(headers).body(remote.getBody());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private static boolean sameHost(URI allowed, URI requested) {
        if (!allowed.getScheme().equalsIgnoreCase(requested.getScheme())) {
            return false;
        }
        if (!allowed.getHost().equalsIgnoreCase(requested.getHost())) {
            return false;
        }
        return normalizePort(allowed) == normalizePort(requested);
    }

    private static int normalizePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static boolean isAllowedDownloadUrl(URI allowedRoot, URI requested) {
        if (!"http".equalsIgnoreCase(requested.getScheme())
                && !"https".equalsIgnoreCase(requested.getScheme())) {
            return false;
        }
        if (sameHost(allowedRoot, requested)) {
            return true;
        }
        String host = requested.getHost();
        if (host == null) {
            return false;
        }
        return host.equalsIgnoreCase("storage.googleapis.com")
                || host.equalsIgnoreCase("storage.cloud.google.com");
    }
}
