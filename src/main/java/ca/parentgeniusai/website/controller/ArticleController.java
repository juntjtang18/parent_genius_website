package ca.parentgeniusai.website.controller;

import ca.parentgeniusai.website.model.Category;
import ca.parentgeniusai.website.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ArticleController {
    private final CategoryService categoryService;

    public ArticleController(CategoryService categoryService) { // Ensure it is being injected
        this.categoryService = categoryService;
    }

    @GetMapping("/function-article-list")
    public String showArticleList(Model model) {
    	System.out.println("/function-article-list called.");
        List<Category> categories = categoryService.getCategories();
        model.addAttribute("categories", categories);
        return "function-article-list"; // Matches Thymeleaf template
    }
}
