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
    public List<Map<String, Object>> getExpertSectionsMenu() {

        List<Map<String, Object>> result = new ArrayList<>();

        try {
            String url = String.format("%s/expert-sections?pagination[pageSize]=100", strapiApiBaseUrl);

            // Public endpoint → no headers needed
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return result;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode dataNode = root.path("data");

            if (dataNode.isArray()) {
                for (JsonNode item : dataNode) {

                    long id = item.path("id").asLong();
                    String title = item.path("attributes").path("title").asText("");

                    Map<String, Object> section = new HashMap<>();
                    section.put("id", id);
                    section.put("title", title);

                    result.add(section);
                }
            }

        } catch (Exception e) {
            logger.error("Error fetching expert sections (public menu)", e);
        }

        return result;
    }


    @GetMapping("/expert/section/{id}")
    public String expertEssayList(@PathVariable("id") Long sectionId,
                                  Model model) {

        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("expertSectionId", sectionId);

        String sectionTitle = "Expert Section";
        String sectionDescription = "";

        try {
            String url = strapiApiBaseUrl + "/expert-sections/" + sectionId;

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response =
                    restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode attrs = root.path("data").path("attributes");
                sectionTitle = attrs.path("title").asText(sectionTitle);
                sectionDescription = attrs.path("description").asText("");
            }
        } catch (Exception e) {
            logger.error("Failed to fetch expert section " + sectionId + " from Strapi", e);
        }

        model.addAttribute("expertSectionTitle", sectionTitle);
        model.addAttribute("expertSectionDescription", sectionDescription);

        return "expertessay-list";
    }



}
