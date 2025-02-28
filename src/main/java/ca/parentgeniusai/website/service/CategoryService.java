package ca.parentgeniusai.website.service;

import ca.parentgeniusai.website.model.Category;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service // Ensure this is present
public class CategoryService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String STRAPI_URL = "http://strapi.geniusParentingAI.ca:8080/api/categories";
    private final String AUTH_TOKEN = "Bearer 7fc113003fb842937914c7ed3cbe98084667e09537cf0103e05d01c72d4c571011523d001e5aab45bab26da12fc27a59c44d680a0164c801c2273540cf75dabf5279a643c1ed902b98f10b64a7df37d40e6ee3ffc2474303f5e4e9d8865b127177603a829748bfc8bf8a203bcfc97bb371333a9e29373adeab8b29caa89e68fb";

    public List<Category> getCategories() {
    	System.out.println("getCategories() called.");
    	
        String url = "http://strapi.geniusParentingAI.ca:8080/api/categories";
        
        // Set up headers with Authorization
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer 7fc113003fb842937914c7ed3cbe98084667e09537cf0103e05d01c72d4c571011523d001e5aab45bab26da12fc27a59c44d680a0164c801c2273540cf75dabf5279a643c1ed902b98f10b64a7df37d40e6ee3ffc2474303f5e4e9d8865b127177603a829748bfc8bf8a203bcfc97bb371333a9e29373adeab8b29caa89e68fb");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            // Make the REST call and capture the response
            ResponseEntity<Map<String, List<Category>>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, List<Category>>>() {});
            
            // Debug print: Log the raw JSON response
            System.out.println("Raw response body: " + response.getBody());
            
            // Check if the response contains the expected structure
            if (response.getBody() != null && response.getBody().containsKey("data")) {
                System.out.println("Categories data: " + response.getBody().get("data"));
            } else {
                System.out.println("No 'data' field found in the response.");
            }

            // Return the categories if available
            return response.getBody() != null ? response.getBody().get("data") : Collections.emptyList();
        } catch (Exception e) {
            // Print any exception that occurs
            e.printStackTrace();
            return Collections.emptyList();  // Return empty list in case of error
        }
    }


}
