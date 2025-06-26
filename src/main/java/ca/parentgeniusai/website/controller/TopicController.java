package ca.parentgeniusai.website.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class TopicController {

    private static final Logger logger = LoggerFactory.getLogger(TopicController.class);

    @Value("${strapi.root.url:http://localhost:8081/}")
    private String strapiRootUrl;

    private String strapiApiBaseUrl;

    @PostConstruct
    public void init() {
        strapiApiBaseUrl = strapiRootUrl.endsWith("/") ? strapiRootUrl + "api" : strapiRootUrl + "/api";
        logger.info("TopicController: Strapi API Base URL initialized to {}", strapiApiBaseUrl);
    }

    private String getJwtToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String jwt = (session != null) ? (String) session.getAttribute("STRAPI_JWT") : null;
        if (jwt == null || jwt.isBlank()) {
            logger.warn("No JWT token found in session attribute 'STRAPI_JWT'");
            return null;
        }
        return jwt;
    }

    // Page serving endpoints

    @GetMapping("/topics")
    public String topicsListPage(Model model, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) return "redirect:/signin?error=unauthenticated";
        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        model.addAttribute("strapiRootUrl", strapiRootUrl);
        return "topics";
    }

    @GetMapping("/new-topic")
    public String newTopicPage(Model model, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) return "redirect:/signin?error=unauthenticated";
        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        return "new-topic";
    }

    @GetMapping("/topics/{topicId}/edit")
    public String editTopicPage(@PathVariable String topicId, Model model, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) return "redirect:/signin?error=unauthenticated";
        model.addAttribute("topicId", topicId);
        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        model.addAttribute("strapiRootUrl", strapiRootUrl);
        logger.info("Serving edit page for topic ID: {}", topicId);
        return "edit-topic";
    }

    // REMOVED: The redundant POST /api/strapi/topics proxy endpoint has been deleted.
    // The frontend now handles topic creation by calling the Strapi API directly.
}
