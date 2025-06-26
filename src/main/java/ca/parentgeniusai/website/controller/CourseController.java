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
    
    private String getJwtToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String jwt = (session != null) ? (String) session.getAttribute("STRAPI_JWT") : null;
        if (jwt == null || jwt.isBlank()) {
            logger.warn("No JWT token found in session attribute 'STRAPI_JWT'");
            return null;
        }
        return jwt;
    }

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
        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        model.addAttribute("strapiRootUrl", strapiRootUrl);
        logger.info("Serving course-list page");
        return "course-list";
    }

    // REMOVED: All remaining proxy endpoints for /api/strapi/courses/{courseId}
    // (GET, PUT, DELETE) have been removed. The frontend now handles these
    // operations by calling the Strapi API directly.

}
