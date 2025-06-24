package ca.parentgeniusai.website.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Controller
public class TopicController {

    private static final Logger logger = LoggerFactory.getLogger(TopicController.class);

    @Value("${strapi.root.url:http://localhost:8081/}")
    private String strapiRootUrl;

    private String strapiApiBaseUrl;

    @PostConstruct
    public void init() {
        strapiApiBaseUrl = strapiRootUrl.endsWith("/") ? strapiRootUrl + "api" : strapiRootUrl + "/api";
        logger.info("TopicController: Strapi API Base URL initialized to {}", strapiApiBaseUrl);
    }

    private String getJwtToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String jwt = (session != null) ? (String) session.getAttribute("STRAPI_JWT") : null;
        if (jwt == null || jwt.isBlank()) {
            logger.warn("No JWT token found in session attribute 'STRAPI_JWT'");
            return null;
        }
        return jwt;
    }

    // Page serving endpoints

    @GetMapping("/topics")
    public String topicsListPage(Model model, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) return "redirect:/signin?error=unauthenticated";
        model.addAttribute("strapiApiUrl", "/api/strapi");
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        model.addAttribute("strapiRootUrl", strapiRootUrl);
        return "topics";
    }

    @GetMapping("/new-topic")
    public String newTopicPage(Model model, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) return "redirect:/signin?error=unauthenticated";
        model.addAttribute("strapiApiUrl", "/api/strapi");
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        return "new-topic";
    }

    @GetMapping("/topics/{topicId}/edit")
    public String editTopicPage(@PathVariable String topicId, Model model, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) return "redirect:/signin?error=unauthenticated";
        model.addAttribute("topicId", topicId);
        // FIX: Pass the REAL Strapi URL to the template, not the proxy.
        // This follows the exact pattern of the working CourseController.
        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        model.addAttribute("strapiRootUrl", strapiRootUrl);
        logger.info("Serving edit page for topic ID: {}", topicId);
        return "edit-topic";
    }

    // Proxy endpoints for LIST and CREATE operations, which are simpler.
    // The EDIT page will now call Strapi directly from the frontend.

    @GetMapping("/api/strapi/topics")
    @ResponseBody
    public ResponseEntity<String> getTopicsFromStrapi(HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\":\"User not authenticated\"}");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String fetchUrl = UriComponentsBuilder.fromHttpUrl(strapiApiBaseUrl + "/topics")
                .queryParam("populate", "icon_image")
                .queryParam("sort", "title:asc")
                .toUriString();
        try {
            return restTemplate.exchange(fetchUrl, HttpMethod.GET, entity, String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }
    
    @PostMapping(value = "/api/strapi/topics", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<String> createTopicInStrapi(
            @RequestParam("data") String jsonData,
            @RequestParam(value = "files.icon_image", required = false) MultipartFile iconImage,
            HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\":\"User not authenticated\"}");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(jwtToken);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("data", jsonData);
        if (iconImage != null && !iconImage.isEmpty()) {
            try {
                body.add("files.icon_image", new ByteArrayResource(iconImage.getBytes()) {
                    @Override public String getFilename() { return iconImage.getOriginalFilename(); }
                });
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Error processing file upload\"}");
            }
        }
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        try {
            return restTemplate.postForEntity(strapiApiBaseUrl + "/topics", requestEntity, String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }
}
