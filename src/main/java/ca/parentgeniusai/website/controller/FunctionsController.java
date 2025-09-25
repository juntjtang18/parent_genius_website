package ca.parentgeniusai.website.controller;

import ca.parentgeniusai.website.model.Function;
import ca.parentgeniusai.website.service.FunctionService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class FunctionsController {

    private static final Logger logger = LoggerFactory.getLogger(FunctionsController.class);

    private final FunctionService functionService;

    public FunctionsController(FunctionService functionService) {
        this.functionService = functionService;
    }

    // ---- Strapi config (same pattern as ArticleController) ----
    @Value("${strapi.root.url:http://localhost:8081/}")
    private String strapiRootUrl;                // e.g. http://localhost:8081/

    private String strapiApiUrl;                  // e.g. http://localhost:8081/api/

    @Value("${strapi.auth-token:}")
    private String strapiToken;

    @PostConstruct
    public void init() {
        final boolean endsWithSlash = strapiRootUrl.endsWith("/");
        strapiApiUrl = strapiRootUrl + (endsWithSlash ? "api/" : "/api/");
        logger.info("functions: strapiApiUrl initialized to {}", strapiApiUrl);
    }
    // -----------------------------------------------------------

    @GetMapping("/functions")
    public String functions(@RequestParam(required = false) Long function, Model model) {
    	
        logger.info("/functions endpoint called with function={}", function);

        List<Function> functions = functionService.getFunctionsWithArticles();
        logger.info("Fetched {} functions", functions != null ? functions.size() : 0);

        // Choose the selected function id (trusting DB ids)
        Long selectedFunctionId =
                (function != null) ? function :
                (functions != null && !functions.isEmpty() ? functions.get(0).getId() : 1L);

        model.addAttribute("functions", functions != null ? functions : List.of());
        model.addAttribute("selectedFunctionId", selectedFunctionId);

        // Provide Strapi config to the page JS
        model.addAttribute("strapiUrl", strapiApiUrl);
        model.addAttribute("strapiToken", strapiToken);

        // Map function id -> fragment base
        String[] bases = { "seg-emotion", "seg-school", "seg-growth", "seg-parenting", "seg-experts" };
        int idx = selectedFunctionId.intValue() - 1; // protocol with DB: ids are 1..N
        String base = bases[idx];

        model.addAttribute("segTopTemplate",    "fragments/" + base + "-top");
        model.addAttribute("segTopName",        base + "-top");
        model.addAttribute("segBottomTemplate", "fragments/" + base + "-bottom");
        model.addAttribute("segBottomName",     base + "-bottom");

        logger.info("functions view: selected={}, base={}, api={}", selectedFunctionId, base, strapiApiUrl);
        return "functions";
    }
}
