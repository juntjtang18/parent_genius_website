package ca.parentgeniusai.website.controller;

import ca.parentgeniusai.website.model.Category;
import ca.parentgeniusai.website.model.Function;
import ca.parentgeniusai.website.service.CategoryService;
import ca.parentgeniusai.website.service.FunctionService;
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

    public ArticleController(CategoryService categoryService, FunctionService functionService) {
        this.categoryService = categoryService;
        this.functionService = functionService;
    }

    @GetMapping("/function-article-list")
    public String showArticleList(
            @RequestParam(value = "function", required = false) String functionId,
            @RequestParam(value = "category", required = false) String categoryId,
            @RequestParam(value = "article", required = false) String articleId, // New parameter
            Model model) {
        System.out.println("/function-article-list called with function=" + functionId + ", category=" + categoryId + ", article=" + articleId);

        List<Function> functions = functionService.getFunctions();
        List<Category> categories = categoryService.getCategories();

        System.out.println("Fetched functions: " + (functions != null ? functions.size() : "null") + " items");
        System.out.println("Fetched categories: " + (categories != null ? categories.size() : "null") + " items");

        String selectedFunctionId = (functionId != null && !functionId.isEmpty()) ? 
            functionId : (functions != null && !functions.isEmpty() ? functions.get(0).getId().toString() : "1");
        String selectedCategoryId = (categoryId != null && !categoryId.isEmpty()) ? 
            categoryId : (categories != null && !functions.isEmpty() ? categories.get(0).getId().toString() : "7");

        System.out.println("Selected Function ID: " + selectedFunctionId);
        System.out.println("Selected Category ID: " + selectedCategoryId);

        model.addAttribute("functions", functions != null ? functions : Collections.emptyList());
        model.addAttribute("categories", categories != null ? categories : Collections.emptyList());
        model.addAttribute("selectedFunctionId", selectedFunctionId);
        model.addAttribute("selectedCategoryId", selectedCategoryId);
        model.addAttribute("selectedArticleId", articleId); // Pass articleId to view
        System.out.println("selectedFunctionId=" + model.getAttribute("selectedFunctionId"));
        System.out.println("selectedCategoryId=" + model.getAttribute("selectedCategoryId"));
        System.out.println("selectedArticleId=" + model.getAttribute("selectedArticleId"));
        return "function-article-list";
    }
}