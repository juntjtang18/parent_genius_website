package ca.parentgeniusai.website.controller;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate; // Or use WebClient for non-blocking

@Controller
public class CourseController {

    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);

    @Value("${strapi.root.url:http://localhost:1337/}") // Assuming Strapi runs on 1337 by default
    private String strapiRootUrl;

    private String strapiApiBaseUrl; // Will be strapiRootUrl + "api/"

    @Value("${strapi.auth-token}")
    private String strapiToken;

    @PostConstruct
    public void init() {
        strapiApiBaseUrl = strapiRootUrl.endsWith("/") ? strapiRootUrl + "api" : strapiRootUrl + "/api";
        logger.info("Strapi API Base URL initialized to: {}", strapiApiBaseUrl);
        if (strapiToken == null || strapiToken.isBlank()) {
            logger.warn("Strapi auth token is not configured. API calls to Strapi may fail.");
        }
    }

    // Page to create a new course (initially just title)
    @GetMapping("/new-course")
    public String newCoursePage(Model model) {
        model.addAttribute("strapiApiUrl", strapiApiBaseUrl); // For JS to make API call
        model.addAttribute("strapiToken", "Bearer " + strapiToken); // Pass token for JS
        model.addAttribute("returnUrl", "/course-list"); // Or wherever you list courses
        logger.info("Serving new-course page");
        return "new-course"; // Thymeleaf template name: new-course.html
    }

    // Page to edit course content (title and dynamic zone components)
    @GetMapping("/courses/{courseId}/edit")
    public String editCourseContentPage(@PathVariable String courseId, Model model) {
        model.addAttribute("courseId", courseId);
        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("strapiToken", "Bearer " + strapiToken);
        model.addAttribute("returnUrl", "/course-list"); // Or course detail page
        logger.info("Serving edit page for course ID: {}", courseId);
        return "edit-course-content"; // Thymeleaf template name: edit-course-content.html
    }

    // --- API Endpoints for Frontend to Interact with Strapi ---

    /**
     * Creates a new course in Strapi.
     * Expects a JSON body like: {"data": {"title": "My New Course Title"}}
     * Responds with the created course data from Strapi.
     */
    @PostMapping("/api/strapi/courses")
    public ResponseEntity<String> createCourseInStrapi(@RequestBody String payload) {
        logger.info("Attempting to create new course in Strapi. Payload: {}", payload);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(strapiToken);

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        String createUrl = strapiApiBaseUrl + "/courses";

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(createUrl, entity, String.class);
            logger.info("Course created successfully in Strapi. Status: {}, Response: {}", response.getStatusCode(), response.getBody());
            return response;
        } catch (HttpClientErrorException e) {
            logger.error("Error creating course in Strapi: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error creating course in Strapi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Unexpected error creating course: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Fetches a specific course's data from Strapi, populating its content.
     */
    @GetMapping("/api/strapi/courses/{courseId}")
    public ResponseEntity<String> getCourseFromStrapi(@PathVariable String courseId) {
        logger.info("Fetching course data from Strapi for ID: {}", courseId);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(strapiToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Populate the dynamic zone 'content' and its components deeply
        // Strapi v4 syntax for deep population:
        String fetchUrl = strapiApiBaseUrl + "/courses/" + courseId + "?populate[content][populate]=*";
        // For older Strapi versions or more specific needs, adjust 'populate' query param.
        // e.g., ?populate=content or ?populate[content][on][content.image][populate]=image_file

        try {
            ResponseEntity<String> response = restTemplate.exchange(fetchUrl, HttpMethod.GET, entity, String.class);
            logger.info("Successfully fetched course {} from Strapi.", courseId);
            return response;
        } catch (HttpClientErrorException e) {
            logger.error("Error fetching course {} from Strapi: {} - {}", courseId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error fetching course {} from Strapi", courseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Unexpected error fetching course: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Updates an existing course in Strapi.
     * Expects a JSON body with the course data, including the 'content' dynamic zone.
     * Example: {"data": {"title": "Updated Title", "content": [ ...components... ]}}
     */
    @PutMapping("/api/strapi/courses/{courseId}")
    public ResponseEntity<String> updateCourseInStrapi(@PathVariable String courseId, @RequestBody String payload) {
        logger.info("Attempting to update course ID {} in Strapi. Payload: {}", courseId, payload);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(strapiToken);

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        String updateUrl = strapiApiBaseUrl + "/courses/" + courseId;

        try {
            ResponseEntity<String> response = restTemplate.exchange(updateUrl, HttpMethod.PUT, entity, String.class);
            logger.info("Course ID {} updated successfully in Strapi. Status: {}", courseId, response.getStatusCode());
            return response;
        } catch (HttpClientErrorException e) {
            logger.error("Error updating course {} in Strapi: {} - {}", courseId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error updating course {} in Strapi", courseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Unexpected error updating course: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Deletes a course from Strapi.
     */
    @DeleteMapping("/api/strapi/courses/{courseId}")
    public ResponseEntity<String> deleteCourseFromStrapi(@PathVariable String courseId) {
        logger.info("Attempting to delete course ID {} from Strapi.", courseId);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(strapiToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String deleteUrl = strapiApiBaseUrl + "/courses/" + courseId;

        try {
            ResponseEntity<String> response = restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, String.class);
            logger.info("Course ID {} deleted successfully from Strapi. Status: {}", courseId, response.getStatusCode());
            return response;
        } catch (HttpClientErrorException e) {
            logger.error("Error deleting course {} from Strapi: {} - {}", courseId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error deleting course {} from Strapi", courseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Unexpected error deleting course: " + e.getMessage() + "\"}");
        }
    }
     // You would also add a @GetMapping for "/course-list" if you want a page to list courses
    @GetMapping("/course-list")
    public String courseListPage(Model model) {
        // This endpoint would typically fetch all courses from Strapi and pass them to a Thymeleaf template
        // For simplicity, I'm just setting the necessary JS variables for now.
        // You'd make a call to GET strapiApiBaseUrl + "/courses" here.
        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("strapiToken", "Bearer " + strapiToken);
        logger.info("Serving course-list page");
        return "course-list"; // Thymeleaf template name: course-list.html
    }
}
