package ca.parentgeniusai.website.controller;

import jakarta.annotation.PostConstruct;
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
    public String foundationCourses(Model model, org.springframework.security.core.Authentication authentication) {
        return renderCategory(model, "foundation", "navbar_courses.foundation", authentication);
    }

    @GetMapping("/courses/membership-only")
    public String membershipCourses(Model model, org.springframework.security.core.Authentication authentication) {
        return renderCategory(model, "membership-only", "navbar_courses.membership", authentication);
    }

    @GetMapping("/courses/parenting-tools")
    public String parentingToolsCourses(Model model, org.springframework.security.core.Authentication authentication) {
        return renderCategory(model, "parenting-tools", "navbar_courses.parenting_tools", authentication);
    }

    @GetMapping("/courses/{courseId:\\d+}")
    public String publicCourseDetail(
            @org.springframework.web.bind.annotation.PathVariable Long courseId,
            Model model,
            org.springframework.security.core.Authentication authentication) {
        boolean isStaff = false;
        if (authentication != null && authentication.getAuthorities() != null) {
            isStaff = authentication.getAuthorities().stream().anyMatch(a ->
                    "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_EDITOR".equals(a.getAuthority()));
        }
        model.addAttribute("courseId", courseId);
        model.addAttribute("isStaff", isStaff);
        model.addAttribute("strapiApiUrl", strapiApiUrl);
        model.addAttribute("strapiRootUrl", strapiRootUrl);
        model.addAttribute("strapiToken", strapiToken);
        return "courses/detail";
    }

    private String renderCategory(
            Model model,
            String categorySlug,
            String titleMessageKey,
            org.springframework.security.core.Authentication authentication) {
        boolean isStaff = false;
        if (authentication != null && authentication.getAuthorities() != null) {
            isStaff = authentication.getAuthorities().stream().anyMatch(a ->
                    "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_EDITOR".equals(a.getAuthority()));
        }
        model.addAttribute("categorySlug", categorySlug);
        model.addAttribute("titleMessageKey", titleMessageKey);
        model.addAttribute("isStaff", isStaff);
        model.addAttribute("strapiApiUrl", strapiApiUrl);
        model.addAttribute("strapiRootUrl", strapiRootUrl);
        model.addAttribute("strapiToken", strapiToken);
        return "courses/category";
    }
}
