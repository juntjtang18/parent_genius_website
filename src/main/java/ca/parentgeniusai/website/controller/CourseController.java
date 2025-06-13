package ca.parentgeniusai.website.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class CourseController {

    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);

    @Value("${strapi.root.url:http://localhost:8080/}")
    private String strapiRootUrl;

    private String strapiApiBaseUrl;

    @PostConstruct
    public void init() {
        strapiApiBaseUrl = strapiRootUrl.endsWith("/") ? strapiRootUrl + "api" : strapiRootUrl + "/api";
        logger.info("Strapi API Base URL initialized to: {}", strapiApiBaseUrl);
    }
    
    /**
     * REVERTED: This method now looks for the JWT in the HttpSession, where the
     * StrapiAuthenticationFilter will place it.
     */
    private String getJwtToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false); // false = don't create if it doesn't exist
        String jwt = (session != null) ? (String) session.getAttribute("STRAPI_JWT") : null;
        if (jwt == null || jwt.isBlank()) {
            logger.warn("No JWT token found in session attribute 'STRAPI_JWT'");
            return null;
        }
        return jwt;
    }

    // ALL OTHER METHODS in this controller remain UNCHANGED from your original file.
    // They correctly use the getJwtToken() method above.
    @GetMapping("/new-course")
    public String newCoursePage(Model model, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) {
            logger.error("User not authenticated. Cannot access new-course page.");
            return "redirect:/signin?error=unauthenticated";
        }
        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        model.addAttribute("returnUrl", "/course-list");
        logger.info("Serving new-course page");
        return "new-course";
    }

    @GetMapping("/courses/{courseId}/edit")
    public String editCourseContentPage(@PathVariable String courseId, Model model, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) {
            logger.error("User not authenticated. Cannot access edit-course page.");
            return "redirect:/signin?error=unauthenticated";
        }
        model.addAttribute("courseId", courseId);
        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        model.addAttribute("returnUrl", "/course-list");
        
        // ADD THIS LINE
        model.addAttribute("strapiRootUrl", strapiRootUrl); 

        logger.info("Serving edit page for course ID: {}", courseId);
        return "edit-course-content";
    }
    
    @GetMapping("/course-list")
    public String courseListPage(Model model, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) {
            logger.error("User not authenticated. Cannot access course-list page.");
            return "redirect:/signin?error=unauthenticated";
        }
        model.addAttribute("strapiApiUrl", "/api/strapi");
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        model.addAttribute("strapiRootUrl", strapiRootUrl);
        logger.info("Serving course-list page");
        return "course-list";
    }

    @GetMapping("/api/strapi/courses")
    public ResponseEntity<String> getCourseListFromStrapi(@RequestParam(name = "categoryId", required = false) String categoryId,
                                                          HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) {
            logger.error("User not authenticated. Cannot fetch course list.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\":\"User not authenticated\"}");
        }

        logger.info("Fetching course list from Strapi. Category ID: {}", categoryId);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(strapiApiBaseUrl + "/courses")
                .queryParam("populate", "icon_image,coursecategory");

        if (categoryId != null && !categoryId.isEmpty()) {
            uriBuilder.queryParam("filters[coursecategory][id][$eq]", categoryId);
        }

        String fetchUrl = uriBuilder.toUriString();
        logger.info("Fetching from URL: {}", fetchUrl);

        try {
            return restTemplate.exchange(fetchUrl, HttpMethod.GET, entity, String.class);
        } catch (HttpClientErrorException e) {
            logger.error("Error fetching course list from Strapi: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error fetching course list from Strapi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Unexpected error: " + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/api/strapi/courses")
    public ResponseEntity<String> createCourseInStrapi(@RequestBody String payload, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) {
            logger.error("User not authenticated. Cannot create course.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\":\"User not authenticated\"}");
        }

        logger.info("Attempting to create new course in Strapi. Payload: {}", payload);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);

        try {
            return restTemplate.postForEntity(strapiApiBaseUrl + "/courses", new HttpEntity<>(payload, headers), String.class);
        } catch (HttpClientErrorException e) {
            logger.error("Error creating course in Strapi: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error creating course in Strapi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Unexpected error: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/api/strapi/courses/{courseId}")
    public ResponseEntity<String> getCourseFromStrapi(@PathVariable String courseId, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) {
            logger.error("User not authenticated. Cannot fetch course.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\":\"User not authenticated\"}");
        }

        logger.info("Fetching course data from Strapi for ID: {}", courseId);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);

        String fetchUrl = strapiApiBaseUrl + "/courses/" + courseId + "?populate[content][populate]=*";

        try {
            return restTemplate.exchange(fetchUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        } catch (HttpClientErrorException e) {
            logger.error("Error fetching course {} from Strapi: {} - {}", courseId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error fetching course {} from Strapi", courseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Unexpected error: " + e.getMessage() + "\"}");
        }
    }

    @PutMapping("/api/strapi/courses/{courseId}")
    public ResponseEntity<String> updateCourseInStrapi(@PathVariable String courseId,
                                                       @RequestBody String payload,
                                                       HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) {
            logger.error("User not authenticated. Cannot update course.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\":\"User not authenticated\"}");
        }

        logger.info("Updating course ID {}. Payload: {}", courseId, payload);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);

        try {
            return restTemplate.exchange(strapiApiBaseUrl + "/courses/" + courseId, HttpMethod.PUT, new HttpEntity<>(payload, headers), String.class);
        } catch (HttpClientErrorException e) {
            logger.error("Error updating course {} in Strapi: {} - {}", courseId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error updating course {} in Strapi", courseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Unexpected error: " + e.getMessage() + "\"}");
        }
    }

    @DeleteMapping("/api/strapi/courses/{courseId}")
    public ResponseEntity<String> deleteCourseFromStrapi(@PathVariable String courseId, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) {
            logger.error("User not authenticated. Cannot delete course.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\":\"User not authenticated\"}");
        }

        logger.info("Deleting course ID {} from Strapi", courseId);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);

        try {
            return restTemplate.exchange(strapiApiBaseUrl + "/courses/" + courseId, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
        } catch (HttpClientErrorException e) {
            logger.error("Error deleting course {} from Strapi: {} - {}", courseId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error deleting course {} from Strapi", courseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Unexpected error: " + e.getMessage() + "\"}");
        }
    }
}