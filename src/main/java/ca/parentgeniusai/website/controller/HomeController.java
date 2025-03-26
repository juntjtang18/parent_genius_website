package ca.parentgeniusai.website.controller;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import ca.parentgeniusai.website.service.FunctionService;
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
    
    @GetMapping("/term-of-service")
    public String termsOfService() {
        return "term-of-service";
    }

    @GetMapping("/join-us")
    public String joinUs() {
    	return "join-us";    
    }
    
}