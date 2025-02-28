package ca.parentgeniusai.website.controller;

import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@RequestParam(name = "lang", required = false) String lang, 
                       HttpServletRequest request, HttpServletResponse response) {
    	System.out.printf("passed in param lang=%s\n", lang);
        if (lang != null) {
            LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
            if (localeResolver != null) {
                Locale locale = new Locale(lang);
                localeResolver.setLocale(request, response, locale);
            }
        }
        return "index";
    }

    @GetMapping("/vision")
    public String vision(HttpServletRequest request, HttpServletResponse response, 
                         @RequestParam(name = "lang", required = false) String lang) {
    	System.out.printf("passed in param lang=%s\n", lang);
        if (lang != null) {
            LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
            if (localeResolver != null) {
                Locale locale = new Locale(lang);
                localeResolver.setLocale(request, response, locale);
            }
        }
        System.out.println("Vision endpoint hit!");
        return "vision";
    }
    
    @GetMapping("/functions")
    public String functions() {
        return "functions";
    }

    @GetMapping("/functions/emotional-development")
    public String emotionalDevelopment() {
        return "emotionalDevelopment";
    }

    @GetMapping("/functions/school-social-life")
    public String schoolSocialLife() {
        return "schoolSocialLife";
    }

    @GetMapping("/functions/growth-milestones")
    public String growthMilestones() {
        return "growthMilestones";
    }

    @GetMapping("/functions/parenting-resources")
    public String parentingResources() {
        return "parentingResources";
    }

    @GetMapping("/functions/child-experts")
    public String childExperts() {
        return "childExperts";
    }

    @GetMapping("/functions/nutrition-for-kids")
    public String nutritionForKids() {
        return "nutritionForKids";
    }
    
    @GetMapping("/new-article")
    public String newArticle() {
    	return "new-article";
    }

    @GetMapping("/faq")
    public String faq() {
        return "faq";
    }

    @GetMapping("/language")
    public String language() {
        return "language";
    }

    @GetMapping("/change-language")
    public String changeLanguage(@RequestParam("lang") String lang, HttpServletRequest request, HttpServletResponse response) {
        LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
        if (localeResolver != null) {
            Locale locale = new Locale(lang);
            localeResolver.setLocale(request, response, locale);
        }
        return "redirect:/";
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicy() {
        return "privacy-policy";
    }
}