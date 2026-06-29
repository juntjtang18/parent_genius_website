package ca.parentgeniusai.website.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicCoursesController {

    @GetMapping("/courses/foundation")
    public String foundationCourses(Model model) {
        return renderCategory(model, "foundation", "navbar_courses.foundation");
    }

    @GetMapping("/courses/membership-only")
    public String membershipCourses(Model model) {
        return renderCategory(model, "membership-only", "navbar_courses.membership");
    }

    @GetMapping("/courses/parenting-tools")
    public String parentingToolsCourses(Model model) {
        return renderCategory(model, "parenting-tools", "navbar_courses.parenting_tools");
    }

    private String renderCategory(Model model, String categorySlug, String titleMessageKey) {
        model.addAttribute("categorySlug", categorySlug);
        model.addAttribute("titleMessageKey", titleMessageKey);
        return "courses/category";
    }
}
