package ca.parentgeniusai.website.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PostsController {

    private static final Logger logger = LoggerFactory.getLogger(PostsController.class);

    @Value("${strapi.root.url:http://localhost:8080/}")
    private String strapiRootUrl;

    /**
     * Retrieves the JWT from the HttpSession, mirroring the logic in CourseController.
     * @param request The incoming HttpServletRequest.
     * @return The JWT string, or null if not found.
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

    @GetMapping("/posts")
    public String getPosts(Model model, HttpServletRequest request, Authentication authentication) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) {
            logger.error("User not authenticated. Cannot access posts page.");
            // Redirect to a sign-in page if not authenticated
            return "redirect:/signin?error=unauthenticated";
        }

        model.addAttribute("strapiUrl", strapiRootUrl + "api/");
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        model.addAttribute("currentUser", authentication != null ? authentication.getName() : "Anonymous");

        logger.info("Serving posts page for user: {}", (authentication != null ? authentication.getName() : "Anonymous"));
        return "posts";
    }
}