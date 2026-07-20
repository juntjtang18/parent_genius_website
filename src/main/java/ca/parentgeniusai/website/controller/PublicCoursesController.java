package ca.parentgeniusai.website.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicCoursesController {

    @Value("${strapi.root.url:http://localhost:8081/}")
    private String strapiRootUrl;

    @Value("${strapi.auth-token:}")
    private String strapiToken;

    private String strapiApiUrl;

    @PostConstruct
    public void init() {
        strapiApiUrl = strapiRootUrl.endsWith("/") ? strapiRootUrl + "api" : strapiRootUrl + "/api";
    }

    @GetMapping("/courses/foundation")
    public String foundationCourses(Model model, HttpServletRequest request) {
        return renderCategory(model, request, "foundation", "navbar_courses.foundation");
    }

    @GetMapping("/courses/membership-only")
    public String membershipCourses(Model model, HttpServletRequest request) {
        return renderCategory(model, request, "membership-only", "navbar_courses.membership");
    }

    @GetMapping("/courses/parenting-tools")
    public String parentingToolsCourses(Model model, HttpServletRequest request) {
        return renderCategory(model, request, "parenting-tools", "navbar_courses.parenting_tools");
    }

    @GetMapping("/courses/{courseId:\\d+}")
    public String publicCourseDetail(@org.springframework.web.bind.annotation.PathVariable Long courseId, Model model) {
        model.addAttribute("courseId", courseId);
        model.addAttribute("strapiApiUrl", strapiApiUrl);
        model.addAttribute("strapiRootUrl", strapiRootUrl);
        model.addAttribute("strapiToken", strapiToken);
        return "courses/detail";
    }

    private String renderCategory(Model model, HttpServletRequest request, String categorySlug, String titleMessageKey) {
        HttpSession session = request.getSession(false);
        String jwt = session != null ? (String) session.getAttribute("STRAPI_JWT") : null;
        model.addAttribute("categorySlug", categorySlug);
        model.addAttribute("titleMessageKey", titleMessageKey);
        model.addAttribute("strapiApiUrl", strapiApiUrl);
        model.addAttribute("strapiRootUrl", strapiRootUrl);
        model.addAttribute("strapiToken", strapiToken);
        model.addAttribute("userLoggedIn", jwt != null && !jwt.isBlank());
        return "courses/category";
    }
}
