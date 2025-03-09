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
        // No manual assignment needed; @Autowired handles it
        System.out.println("Test Strapi URL: " + strapiUrl);
        System.out.println("Test Auth Token: " + authToken);
    }

    @Test
    void testGetFunctions_RealAPI() {
        List<Function> functions = functionService.getFunctions();

        assertNotNull(functions);
        assertFalse(functions.isEmpty(), "No functions found! Ensure Strapi is running and has data.");

        Function firstFunction = functions.get(0);
        assertNotNull(firstFunction.getId());
        assertNotNull(firstFunction.getName());
        assertNotNull(firstFunction.getOrder());
        assertNotNull(firstFunction.getIconName());

        System.out.println("Retrieved functions: " + functions);
    }
}