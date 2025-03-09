package ca.parentgeniusai.website.controller;

import ca.parentgeniusai.website.model.Function;
import ca.parentgeniusai.website.service.FunctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class FunctionsController {

    private static final Logger logger = LoggerFactory.getLogger(FunctionsController.class);

    @Autowired
    private FunctionService functionService;

    @GetMapping("/functions")
    public String functions(@RequestParam(required = false) Long function, Model model) {
        logger.info("/functions endpoint called with function={}", function);

        // Fetch functions with articles
        List<Function> functions = functionService.getFunctionsWithArticles();
        logger.info("Fetched {} functions", functions != null ? functions.size() : "null");

        // Determine selectedFunctionId without reassigning in lambda scope
        Long selectedFunctionId;
        if (function == null && functions != null && !functions.isEmpty()) {
            selectedFunctionId = functions.get(0).getId();
            logger.info("No function parameter provided; defaulting to first function ID: {}", selectedFunctionId);
        } else if (function != null && functions != null && !functions.stream().anyMatch(f -> f.getId().equals(function))) {
            logger.warn("Provided function ID {} not found; defaulting to first function ID", function);
            selectedFunctionId = functions.isEmpty() ? null : functions.get(0).getId();
        } else {
            selectedFunctionId = function; // Use provided value if valid
        }

        // Add attributes to model
        model.addAttribute("functions", functions != null ? functions : List.of());
        model.addAttribute("selectedFunctionId", selectedFunctionId);
        logger.info("Model attributes set: selectedFunctionId={}", selectedFunctionId);

        return "functions";
    }
}