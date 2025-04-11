package ca.parentgeniusai.website.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PostsController {

    @Value("${strapi.root.url:http://localhost:8080/}")
    private String strapiRootUrl;

    @GetMapping("/posts")
    public String getPosts(Model model, Authentication authentication) { // Removed @AuthenticationPrincipal
        String strapiToken;
        if (authentication != null && authentication.isAuthenticated()) {
            Object details = authentication.getDetails();
            if (details instanceof String) {
                strapiToken = "Bearer " + (String) details;
                System.out.println("Extracted JWT from details: " + strapiToken);
            } else {
                System.out.println("Details not a String: " + (details != null ? details.getClass().getName() : "null"));
                strapiToken = "Bearer default-token";
            }
        } else {
            System.out.println("Authentication is null or not authenticated");
            strapiToken = "Bearer default-token";
        }

        model.addAttribute("strapiUrl", strapiRootUrl + "api/");
        model.addAttribute("strapiToken", strapiToken);
        model.addAttribute("currentUser", authentication != null ? authentication.getName() : "Anonymous");

        return "posts";
    }
}