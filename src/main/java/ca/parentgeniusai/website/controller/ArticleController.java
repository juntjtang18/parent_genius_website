package ca.parentgeniusai.website.controller;

import ca.parentgeniusai.website.model.Function;
import ca.parentgeniusai.website.service.FunctionService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
public class ArticleController {
    private final FunctionService functionService;
    private static final Logger logger = LoggerFactory.getLogger(ArticleController.class);

    @Value("${strapi.root.url:http://localhost:8081/}")
    private String strapiRootUrl;
    private String strapiApiUrl;
    
    @Value("${strapi.auth-token}")
    private String strapiToken;
    
    public ArticleController(FunctionService functionService) {
        this.functionService = functionService;
    }

    @PostConstruct
    public void init() {
        strapiApiUrl = strapiRootUrl + "api/";
        logger.info("strapiApiUrl initialized to {}", strapiApiUrl);
    }

    @GetMapping("/article")
    public String showArticle(
            @RequestParam(value = "article", required = true) Long articleId,
            HttpSession session,
            Model model) {
        logger.info("/article called with articleId={}", articleId);
        
        String userJwt = (String) session.getAttribute("STRAPI_JWT");
        model.addAttribute("strapiUserJwt", userJwt);
        model.addAttribute("articleId", articleId);
        model.addAttribute("strapiUrl", strapiApiUrl);
        model.addAttribute("strapiToken", strapiToken);
        logger.info("strapiApiUrl={}", strapiApiUrl);
        return "article";
    }
    
    @GetMapping("/function-article-list")
    public String showArticleList(
            @RequestParam(value = "function", required = false) Long functionId,
            @RequestParam(value = "article", required = false) Long articleId,
            HttpSession session, // <-- By adding this, Spring provides the session
            Model model) {
        List<Function> functions = functionService.getFunctions();
        Long selectedFunctionId = (functionId != null) ? 
            functionId : (functions != null && !functions.isEmpty() ? functions.get(0).getId() : 1L);
        String userJwt = (String) session.getAttribute("STRAPI_JWT");
        model.addAttribute("strapiUserJwt", userJwt);

        model.addAttribute("functions", functions != null ? functions : Collections.emptyList());
        model.addAttribute("selectedFunctionId", selectedFunctionId);
        model.addAttribute("selectedArticleId", articleId);
        model.addAttribute("strapiUrl", strapiApiUrl);
        model.addAttribute("strapiToken", strapiToken);
        logger.info("strapiApiUrl={}", strapiApiUrl);
        return "function-article-list";
    }
    
    @GetMapping("/new-article")
    public String newArticle(
            @RequestParam("function") String functionId,
            Model model) {
        model.addAttribute("functionId", functionId);
        model.addAttribute("returnUrl", "/function-article-list?function=" + functionId);
        model.addAttribute("strapiUrl", strapiApiUrl);
        model.addAttribute("strapiToken", strapiToken);
        logger.info("strapiApiUrl={}", strapiApiUrl);
        return "new-article";
    }

    @GetMapping("/edit-article")
    public String editArticle(
            @RequestParam("article") Long articleId,
            HttpSession session,
            Model model) {
        logger.info("/edit-article called with articleId={}", articleId);
        String userJwt = (String) session.getAttribute("STRAPI_JWT");
        model.addAttribute("strapiUserJwt", userJwt);
        model.addAttribute("articleId", articleId);
        model.addAttribute("strapiUrl", strapiApiUrl);
        model.addAttribute("strapiToken", strapiToken);
        logger.info("strapiApiUrl={}", strapiApiUrl);
        return "edit-article";
    }
}