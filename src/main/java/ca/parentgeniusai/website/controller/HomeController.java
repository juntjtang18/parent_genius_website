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
                       HttpServletRequest request, HttpServletResponse response, 
                       Model model) {
        System.out.printf("passed in param lang=%s\n", lang);
        if (lang != null) {
            LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
            if (localeResolver != null) {
                Locale locale = new Locale(lang);
                localeResolver.setLocale(request, response, locale);
            }
        }
        // No need for model.addAttribute("auth", auth) - handled by GlobalControllerAdvice
        return "index";
    }

    @GetMapping("/vision")
    public String vision(HttpServletRequest request, HttpServletResponse response, 
                         @RequestParam(name = "lang", required = false) String lang, 
                         Model model) {
        System.out.printf("passed in param lang=%s\n", lang);
        if (lang != null) {
            LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
            if (localeResolver != null) {
                Locale locale = new Locale(lang);
                localeResolver.setLocale(request, response, locale);
            }
        }
        System.out.println("Vision endpoint hit!");
        // No need for model.addAttribute("auth", auth)
        return "vision";
    }
    
    /*
    @GetMapping("/posts")
    public String posts(Model model) {
        // Keep username logic, but auth is handled globally
        if (model.containsAttribute("auth") && model.getAttribute("auth") != null) {
            model.addAttribute("username", ((org.springframework.security.core.Authentication) model.getAttribute("auth")).getName());
        } else {
            model.addAttribute("username", "Guest");
        }
        return "posts";
    }
	*/
    @GetMapping("/faq")
    public String faq(Model model) {
        // No need for auth parameter or model attribute
        return "faq";
    }

    @GetMapping("/language")
    public String language(Model model) {
        // No need for auth parameter or model attribute
        return "language";
    }

    @GetMapping("/change-language")
    public String changeLanguage(@RequestParam("lang") String lang, 
                                 HttpServletRequest request, HttpServletResponse response) {
        LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
        if (localeResolver != null) {
            Locale locale = new Locale(lang);
            localeResolver.setLocale(request, response, locale);
        }
        return "redirect:/";
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicy(Model model) {
        // No need for auth parameter or model attribute
        return "privacy-policy";
    }
    
    @GetMapping("/term-of-service")
    public String termsOfService(Model model) {
        // No need for auth parameter or model attribute
        return "term-of-service";
    }

    @GetMapping("/join-us")
    public String joinUs(Model model) {
        // No need for auth parameter or model attribute
        return "join-us";    
    }
}