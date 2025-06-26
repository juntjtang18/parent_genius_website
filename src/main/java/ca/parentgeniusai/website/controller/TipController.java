package ca.parentgeniusai.website.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class TipController {

    private static final Logger logger = LoggerFactory.getLogger(TipController.class);

    @Value("${strapi.root.url:http://localhost:8081/}")
    private String strapiRootUrl;

    private String strapiApiBaseUrl;

    @PostConstruct
    public void init() {
        strapiApiBaseUrl = strapiRootUrl.endsWith("/") ? strapiRootUrl + "api" : strapiRootUrl + "/api";
        logger.info("TipController: Strapi API Base URL initialized to {}", strapiApiBaseUrl);
    }

    private String getJwtToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String jwt = (session != null) ? (String) session.getAttribute("STRAPI_JWT") : null;
        if (jwt == null || jwt.isBlank()) {
            logger.warn("No JWT token found in session attribute 'STRAPI_JWT'");
            return null;
        }
        return jwt;
    }

    // Page serving methods
    @GetMapping("/tips")
    public String tipsListPage(Model model, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) return "redirect:/signin?error=unauthenticated";
        // MODIFIED: Pass the actual Strapi API base URL to the view
        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        model.addAttribute("strapiRootUrl", strapiRootUrl);
        return "tips";
    }

    @GetMapping("/new-tip")
    public String newTipPage(Model model, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) return "redirect:/signin?error=unauthenticated";
        // MODIFIED: Pass the actual Strapi API base URL to the view
        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        return "new-tip";
    }

    @GetMapping("/tips/{tipId}/edit")
    public String editTipPage(@PathVariable String tipId, Model model, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) return "redirect:/signin?error=unauthenticated";
        model.addAttribute("tipId", tipId);
        model.addAttribute("strapiApiUrl", strapiApiBaseUrl); // Direct API call
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        model.addAttribute("strapiRootUrl", strapiRootUrl);
        return "edit-tip";
    }

    // REMOVED: The redundant proxy endpoints for listing and creating tips
    // have been deleted. The frontend will now call the Strapi API directly.
}
