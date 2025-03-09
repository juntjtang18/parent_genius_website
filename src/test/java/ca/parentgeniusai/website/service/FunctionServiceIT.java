package ca.parentgeniusai.website.service;

import ca.parentgeniusai.website.model.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class FunctionServiceIT {

    @Autowired
    private FunctionService functionService;

    @Value("${strapi.url}")
    private String strapiUrl;

    @Value("${strapi.auth-token}")
    private String authToken;

    @BeforeEach
    void setUp() {
        System.out.println("Test Strapi URL: " + strapiUrl);
        System.out.println("Test Auth Token: " + authToken);
    }

    @Test
    void testGetFunctions_RealAPI() {
        List<Function> functions = functionService.getFunctions();

        // Basic assertions
        assertNotNull(functions, "Functions list should not be null");
        assertFalse(functions.isEmpty(), "No functions found! Ensure Strapi is running and has data.");
        assertEquals(6, functions.size(), "Expected 6 functions from Strapi");

        // Verify specific function data (based on your Strapi output)
        Function emotionalDev = functions.stream()
            .filter(f -> f.getId().equals(1L))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Function with ID 1 not found"));
        assertEquals("Emotional Development", emotionalDev.getName(), "Name mismatch for ID 1");
        assertEquals(1, emotionalDev.getOrder(), "Order mismatch for ID 1");
        assertEquals("func_emotion_dev.jpg", emotionalDev.getIconName(), "Icon name mismatch for ID 1");
        assertTrue(emotionalDev.getArticles().isEmpty(), "Articles should be empty for getFunctions()");

        Function childExperts = functions.stream()
            .filter(f -> f.getId().equals(5L))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Function with ID 5 not found"));
        assertEquals("Child Experts", childExperts.getName(), "Name mismatch for ID 5");
        assertEquals(5, childExperts.getOrder(), "Order mismatch for ID 5");
        assertEquals("func_child_experts.jpg", childExperts.getIconName(), "Icon name mismatch for ID 5");

        System.out.println("Retrieved functions: " + functions);
    }

    @Test
    void testGetFunctionsWithArticles_RealAPI() {
        List<Function> functions = functionService.getFunctionsWithArticles();

        // Basic assertions
        assertNotNull(functions, "Functions list should not be null");
        assertFalse(functions.isEmpty(), "No functions found! Ensure Strapi is running and has data.");
        assertEquals(6, functions.size(), "Expected 6 functions from Strapi");

        // Verify function data and articles
        Function emotionalDev = functions.stream()
            .filter(f -> f.getId().equals(1L))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Function with ID 1 not found"));
        assertEquals("Emotional Development", emotionalDev.getName(), "Name mismatch for ID 1");
        assertEquals(1, emotionalDev.getOrder(), "Order mismatch for ID 1");
        assertEquals("func_emotion_dev.jpg", emotionalDev.getIconName(), "Icon name mismatch for ID 1");

        // Verify articles for function ID 1 (assuming at least one article exists in Strapi)
        List<Function.Article> articles = emotionalDev.getArticles();
        assertNotNull(articles, "Articles list should not be null for function ID 1");
        if (!articles.isEmpty()) {
            Function.Article firstArticle = articles.get(0);
            assertNotNull(firstArticle.getId(), "Article ID should not be null");
            assertNotNull(firstArticle.getTitle(), "Article title should not be null");
            // Note: iconImageUrl may be null if icon_image is not set in Strapi
            System.out.println("First article for Emotional Development: ID=" + firstArticle.getId() + 
                              ", Title=" + firstArticle.getTitle() + 
                              ", Icon=" + firstArticle.getIconImageUrl());
        } else {
            System.out.println("No articles found for Emotional Development in test data.");
        }

        // Verify another function (e.g., Child Experts)
        Function childExperts = functions.stream()
            .filter(f -> f.getId().equals(5L))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Function with ID 5 not found"));
        assertEquals("Child Experts", childExperts.getName(), "Name mismatch for ID 5");
        assertEquals(5, childExperts.getOrder(), "Order mismatch for ID 5");
        assertEquals("func_child_experts.jpg", childExperts.getIconName(), "Icon name mismatch for ID 5");

        System.out.println("Retrieved functions with articles: " + functions);
    }
}