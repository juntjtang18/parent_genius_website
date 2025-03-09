package ca.parentgeniusai.website.controller;

import ca.parentgeniusai.website.model.Category;
import ca.parentgeniusai.website.model.Function;
import ca.parentgeniusai.website.service.CategoryService;
import ca.parentgeniusai.website.service.FunctionService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
public class ArticleController {
    private final CategoryService categoryService;
    private final FunctionService functionService;

    @Value("${strapi.url:http://localhost:8081/api/}")
    private String strapiUrl;

    @Value("${strapi.auth-token}")
    private String strapiToken;
    
    public ArticleController(CategoryService categoryService, FunctionService functionService) {
        this.categoryService = categoryService;
        this.functionService = functionService;
    }

    @GetMapping("/function-article-list")
    public String showArticleList(
            @RequestParam(value = "function", required = false) Long functionId,
            @RequestParam(value = "category", required = false) Long categoryId,
            @RequestParam(value = "article", required = false) Long articleId,
            Model model) {
        System.out.println("/function-article-list called with function=" + functionId + ", category=" + categoryId + ", article=" + articleId);

        List<Function> functions = functionService.getFunctions();
        List<Category> categories = categoryService.getCategories();

        System.out.println("Fetched functions: " + (functions != null ? functions.size() : "null") + " items");
        System.out.println("Fetched categories: " + (categories != null ? categories.size() : "null") + " items");

        Long selectedFunctionId = (functionId != null) ? 
            functionId : (functions != null && !functions.isEmpty() ? functions.get(0).getId() : 1L);
        Long selectedCategoryId = (categoryId != null) ? 
            categoryId : (categories != null && !categories.isEmpty() ? categories.get(0).getId() : 7L);

        System.out.println("Selected Function ID: " + selectedFunctionId);
        System.out.println("Selected Category ID: " + selectedCategoryId);

        model.addAttribute("functions", functions != null ? functions : Collections.emptyList());
        model.addAttribute("categories", categories != null ? categories : Collections.emptyList());
        model.addAttribute("selectedFunctionId", selectedFunctionId);
        model.addAttribute("selectedCategoryId", selectedCategoryId);
        model.addAttribute("selectedArticleId", articleId);
        model.addAttribute("strapiUrl", strapiUrl);
        model.addAttribute("strapiToken", strapiToken);

        System.out.println("Model attributes - selectedFunctionId=" + model.getAttribute("selectedFunctionId"));
        System.out.println("Model attributes - selectedCategoryId=" + model.getAttribute("selectedCategoryId"));
        System.out.println("Model attributes - selectedArticleId=" + model.getAttribute("selectedArticleId"));
        
        return "function-article-list";
    }
}