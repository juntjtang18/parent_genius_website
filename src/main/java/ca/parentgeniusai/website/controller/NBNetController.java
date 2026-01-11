package ca.parentgeniusai.website.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class NBNetController {
    private static final Logger logger = LoggerFactory.getLogger(NBNetController.class);

    @Value("${strapi.root.url:http://localhost:8081/}")
    private String strapiRootUrl;

    private String strapiApiBaseUrl;

    @PostConstruct
    public void init() {
        strapiApiBaseUrl = strapiRootUrl.endsWith("/") ? strapiRootUrl + "api" : strapiRootUrl + "/api";
        logger.info("NBNetController: Strapi API Base URL initialized to {}", strapiApiBaseUrl);
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

    @GetMapping("/nbnet/familynplaymates")
    public String familyNplaymatesHome(Model model, HttpServletRequest request, Authentication authentication) {

        String jwt = getJwtToken(request);
        if (jwt == null) {
            logger.warn("User not authenticated. Cannot access family playmates page.");
            return "redirect:/signin";
        }

        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("strapiToken", "Bearer " + jwt);
        model.addAttribute("currentUser", authentication != null ? authentication.getName() : "Anonymous");

        return "family-playmates";
    }
    
    @GetMapping("/nbnet/playplaces")
    public String playplacesHome(Model model, HttpServletRequest request, Authentication authentication) {
        String jwt = getJwtToken(request);
        // this page can be publicly accessed.
        // if (jwt == null) {
        //    logger.warn("User not authenticated. Cannot access family playmates page.");
        //    return "redirect:/signin";
        //}
        model.addAttribute("strapiApiUrl", strapiApiBaseUrl);
        model.addAttribute("strapiToken", "Bearer " + jwt);
        model.addAttribute("currentUser", authentication != null ? authentication.getName() : "Anonymous");

        return "playplace";        
    	
    }
    
}
