package ca.parentgeniusai.website.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class ExpertController {
    private static final Logger logger = LoggerFactory.getLogger(ExpertController.class);

    @Value("${strapi.root.url:http://localhost:8081/}")
    private String strapiRootUrl;

    private String strapiApiBaseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private String getJwtToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String jwt = (session != null) ? (String) session.getAttribute("STRAPI_JWT") : null;
        if (jwt == null || jwt.isBlank()) {
            logger.warn("No JWT token found in session attribute 'STRAPI_JWT'");
            return null;
        }
        return jwt;
    }
    
    @PostConstruct
    public void init() {
        strapiApiBaseUrl = strapiRootUrl.endsWith("/") ? strapiRootUrl + "api" : strapiRootUrl + "/api";
        logger.info("TipController: Strapi API Base URL initialized to {}", strapiApiBaseUrl);
    }
    
    
    @GetMapping("/expert")
    public String miniCourseSegment() {
        return "expert";
    }
    
    @GetMapping("/expertessay/{expertEssayId}/edit")
    public String editTipPage(@PathVariable String expertEssayId, 
            @RequestParam(value = "sectionId", required = false) Long sectionId,
    		Model model, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) return "redirect:/signin?error=unauthenticated";
        model.addAttribute("expertEssayId", expertEssayId);
        model.addAttribute("strapiApiUrl", strapiApiBaseUrl); // Direct API call
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        model.addAttribute("strapiRootUrl", strapiRootUrl);
        model.addAttribute("selectedSectionId", sectionId);  // <= key line
        return "edit-expertessay";
    }

    @GetMapping("/new-expertessay")
    public String newExpertEssayPage(
            @RequestParam(value = "sectionId", required = false) Long sectionId,
            Model model,
            HttpServletRequest request) {

        String jwtToken = getJwtToken(request);
        if (jwtToken == null) {
            return "redirect:/signin?error=unauthenticated";
        }

        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        model.addAttribute("strapiRootUrl", strapiRootUrl);

        // this will be 5 if you came from /expert/section/5, or null if opened generically
        model.addAttribute("selectedSectionId", sectionId);

        // must match templates/new-expertessay.html
        return "new-expertessay";
    }

    
    /**
     * Endpoint used by the navbar to build the Expert dropdown menu.
     * Returns a simple list like:
     * [
     *   { "id": 1, "title": "Sleep & Routines" },
     *   { "id": 2, "title": "Emotional Health" }
     * ]
     */
    @GetMapping("/api/expert-sections/menu")
    @ResponseBody
    public List<Map<String, Object>> getExpertSectionsMenu(
            @RequestParam(value = "locale", required = false, defaultValue = "en") String locale,
            HttpServletRequest request) {

        List<Map<String, Object>> result = new ArrayList<>();
        logger.info("getExpertSectionsMenu() called.");
        
        try {
            String jwtToken = getJwtToken(request);
            if (jwtToken == null || jwtToken.isEmpty()) {
                logger.warn("No JWT token found in session.");
                return result;  // return empty list → frontend will show 'failed to load'
            }

        	String encodedLocale = URLEncoder.encode(locale, StandardCharsets.UTF_8);
            String url = String.format(
                    "%s/expert-sections?pagination[pageSize]=100&locale=%s",
                    strapiApiBaseUrl,
                    encodedLocale
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            org.springframework.web.client.RestTemplate restTemplate =
                    new org.springframework.web.client.RestTemplate();

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.warn("Failed to fetch expert sections from Strapi: status={}",
                        response.getStatusCode());
                return result;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode dataNode = root.path("data");

            if (dataNode.isArray()) {
                for (JsonNode item : dataNode) {
                    JsonNode idNode = item.path("id");
                    JsonNode attrs = item.path("attributes");
                    JsonNode titleNode = attrs.path("title");

                    Map<String, Object> section = new HashMap<>();
                    section.put("id", idNode.asLong());
                    section.put("title", titleNode.isMissingNode() ? "" : titleNode.asText(""));
                    result.add(section);
                }
            }

        } catch (Exception e) {
            logger.error("Error fetching expert sections from Strapi", e);
        }

        return result;
    }

    @GetMapping("/expert/section/{id}")
    public String showExpertSection(
            @PathVariable("id") Long id,
            Model model,
            HttpServletRequest request) {

        String jwtToken = getJwtToken(request);
        if (jwtToken == null) {
            return "redirect:/signin?error=unauthenticated";
        }

        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        model.addAttribute("expertSectionId", id);

        String title = "Expert Section";
        String description = "";

        try {
            String url = strapiApiBaseUrl + "/expert-sections/" + id;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwtToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.getBody());
                JsonNode attrs = root.path("data").path("attributes");
                title = attrs.path("title").asText(title);
                description = attrs.path("description").asText("");
            }
        } catch (Exception e) {
            // log if you want, but keep defaults if something fails
            e.printStackTrace();
        }

        model.addAttribute("expertSectionTitle", title);
        model.addAttribute("expertSectionDescription", description);

        return "expertessay-list";
    }



}
