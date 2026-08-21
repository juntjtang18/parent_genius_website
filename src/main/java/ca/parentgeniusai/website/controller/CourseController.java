package ca.parentgeniusai.website.controller;

import java.util.List;

import ca.parentgeniusai.website.model.Course;
import ca.parentgeniusai.website.model.Pillar;
import ca.parentgeniusai.website.service.CourseService;
import ca.parentgeniusai.website.service.PillarService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CourseController {

    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);
    static final String SESSION_ASK_QUESTION = "PILLAR_ASK_QUESTION";
    static final String SESSION_LOGIN_REDIRECT = "LOGIN_REDIRECT";

    private final CourseService courseService;
    private final PillarService pillarService;

    public CourseController(CourseService courseService, PillarService pillarService) {
        this.courseService = courseService;
        this.pillarService = pillarService;
    }

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
            return "redirect:/signin";
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
            return "redirect:/signin";
        }
        model.addAttribute("courseId", courseId);
        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        model.addAttribute("returnUrl", "/course-list");
        model.addAttribute("strapiRootUrl", strapiRootUrl);
        model.addAttribute("pillars", pillarService.getPillars());
        logger.info("Serving edit page for course ID: {}", courseId);
        return "edit-course-content";
    }
    
    @GetMapping("/course-list")
    public String courseListPage(Model model, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) {
            logger.error("User not authenticated. Cannot access course-list page.");
            return "redirect:/signin";
        }
        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        model.addAttribute("strapiRootUrl", strapiRootUrl);
        model.addAttribute("pillars", pillarService.getPillars());
        model.addAttribute("courseCategories", courseService.getCourseCategories());
        logger.info("Serving course-list page");
        return "course-list";
    }

    @GetMapping("/courses/pillar/{pillarId}")
    public String pillarCourses(@PathVariable Long pillarId,
            @RequestParam(name = "askError", required = false) String askError,
            Model model,
            HttpServletRequest request) {
        Pillar pillar = pillarService.getPillarById(pillarId);
        if (pillar == null) {
            logger.warn("Pillar {} not found; redirecting to /pillars", pillarId);
            return "redirect:/pillars";
        }
        List<Course> courses = courseService.getCoursesByPillarId(pillarId);
        int heroIndex = heroIndexFor(pillar);
        model.addAttribute("pillar", pillar);
        model.addAttribute("courses", courses);
        model.addAttribute("heroIndex", heroIndex);
        model.addAttribute("askError", askError != null);
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SESSION_ASK_QUESTION) instanceof String pending
                && !pending.isBlank()) {
            model.addAttribute("askQuestion", pending);
        }
        logger.info("Serving pillar courses page for pillar {} (hero {}) with {} courses",
            pillarId, heroIndex, courses.size());
        return "courses/pillar-courses";
    }

    @PostMapping("/courses/pillar/ask")
    public String askAi(
            @RequestParam(name = "question", required = false) String question,
            @RequestParam(name = "fromPillarId", required = false) Long fromPillarId,
            HttpServletRequest request) {
        String q = question == null ? "" : question.trim();
        Long fallback = fromPillarId != null ? fromPillarId : 1L;
        if (q.isEmpty()) {
            return "redirect:/courses/pillar/" + fallback;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_ASK_QUESTION, q);

        String jwtToken = getJwtToken(request);
        if (jwtToken == null) {
            session.setAttribute(SESSION_LOGIN_REDIRECT, "/courses/pillar/" + fallback);
            logger.info("Ask AI requires login; saving question and redirecting to signin");
            return "redirect:/signin";
        }

        Long matched = pillarService.classifyPillar(q, jwtToken);
        if (matched == null) {
            logger.warn("[AskAI] controller: no pillar for question='{}'; staying on {}", q, fallback);
            return "redirect:/courses/pillar/" + fallback + "?askError=1";
        }
        logger.info("[AskAI] controller: question='{}' -> pillar {}", q, matched);
        session.removeAttribute(SESSION_ASK_QUESTION);
        return "redirect:/courses/pillar/" + matched;
    }

    private int heroIndexFor(Pillar pillar) {
        String n = pillar.getName() == null ? "" : pillar.getName().toLowerCase();
        if (n.contains("foundation")) return 1;
        if (n.contains("emotion") || n.contains("wellbeing") || n.contains("mental")) return 2;
        if (n.contains("communicat") || n.contains("connection") || n.contains("relationship")) return 3;
        if (n.contains("learning") || n.contains("thinking")) return 4;
        if (n.contains("daily") || n.contains("modern")) return 5;
        if (n.contains("neuro") || n.contains("inclusion")) return 6;
        if (pillar.getOrder() != null && pillar.getOrder() >= 1 && pillar.getOrder() <= 6) {
            return pillar.getOrder();
        }
        return 1;
    }

    // REMOVED: All remaining proxy endpoints for /api/strapi/courses/{courseId}
    // (GET, PUT, DELETE) have been removed. The frontend now handles these
    // operations by calling the Strapi API directly.

}
