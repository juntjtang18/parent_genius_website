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
            Model model) {
        System.out.println("/function-article-list called with function=" + functionId + ", category=" + categoryId);

        // Fetch all functions and categories
        List<Function> functions = functionService.getFunctions();
        List<Category> categories = categoryService.getCategories();

        System.out.println("Fetched functions: " + (functions != null ? functions.size() : "null") + " items");
        System.out.println("Fetched categories: " + (categories != null ? categories.size() : "null") + " items");

        // Default to first function/category if not provided
        String selectedFunctionId = (functionId != null && !functionId.isEmpty()) ? 
            functionId : (functions != null && !functions.isEmpty() ? functions.get(0).getId().toString() : "1"); // Fallback to "1" if empty
        String selectedCategoryId = (categoryId != null && !categoryId.isEmpty()) ? 
            categoryId : (categories != null && !categories.isEmpty() ? categories.get(0).getId().toString() : "7"); // Fallback to "7" if empty

        System.out.println("Selected Function ID: " + selectedFunctionId);
        System.out.println("Selected Category ID: " + selectedCategoryId);

        // Add to model
        model.addAttribute("functions", functions != null ? functions : Collections.emptyList());
        model.addAttribute("categories", categories != null ? categories : Collections.emptyList());
        model.addAttribute("selectedFunctionId", selectedFunctionId);
        model.addAttribute("selectedCategoryId", selectedCategoryId);
        System.out.println("selectedFunctionId=" + model.getAttribute("selectedCategoryId"));
        return "function-article-list";
    }
}