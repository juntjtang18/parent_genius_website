package ca.parentgeniusai.website.controller;

import ca.parentgeniusai.website.service.FunctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private FunctionService functionService;

    @Value("${strapi.root.url}")
    private String STRAPI_ROOT_URL;

    // This endpoint remains as-is
    @PostMapping("/cache/clear-functions")
    public ResponseEntity<String> clearFunctionsCache() {
        functionService.clearFunctionsWithArticlesCache();
        return ResponseEntity.ok("Successfully cleared the 'functionsWithArticles' cache.");
    }

    // START --- NEW SECURE DELETE ENDPOINT ---
    /**
     * Deletes an article using the logged-in user's JWT for authorization.
     *
     * @param id The ID of the article to delete.
     * @param userAuthHeader The "Authorization" header from the incoming request, containing the user's JWT.
     * @return A response entity indicating success or failure.
     */
    @DeleteMapping("/articles/{id}")
    public ResponseEntity<String> deleteArticle(
            @PathVariable Long id,
            @RequestHeader("Authorization") String userAuthHeader) { // Read the user's auth header

        logger.info("Attempting to delete article ID: {} on behalf of the logged-in user", id);
        String strapiApiUrl = STRAPI_ROOT_URL + "api/articles/" + id;

        try {
            HttpHeaders headers = new HttpHeaders();
            // Use the user's JWT to authorize the request with Strapi
            headers.set("Authorization", userAuthHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(strapiApiUrl, HttpMethod.DELETE, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Successfully deleted article ID: {} from Strapi.", id);
                // IMPORTANT: Clear the application cache after deletion
                functionService.clearFunctionsWithArticlesCache();
                return ResponseEntity.ok("Article deleted successfully.");
            } else {
                logger.error("Failed to delete article ID: {}. Strapi responded with status: {}", id, response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).body("Failed to delete article from Strapi.");
            }
        } catch (Exception e) {
            logger.error("An error occurred while trying to delete article ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
    // END --- NEW SECURE DELETE ENDPOINT ---
}