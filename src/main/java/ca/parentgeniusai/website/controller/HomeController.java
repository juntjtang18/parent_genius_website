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

    // Home page route
	@GetMapping("/")
	public String home(@RequestParam(name = "lang", required = false) String lang, Model model) {
	    return "index";
	}

    // Vision page route
    @GetMapping("/vision")
    public String vision() {
    	System.out.println("Vision endpoint hit!");
        return "vision";  // Return the view name for Vision page
    }

    // About Us page route (under Vision)
    @GetMapping("/vision/about")
    public String about() {
        return "about";  // Return the view name for About Us page
    }

    // Mission Statement page route (under Vision)
    @GetMapping("/vision/mission")
    public String mission() {
        return "mission";  // Return the view name for Mission Statement page
    }

    // Contact Us page route (under Vision)
    @GetMapping("/vision/contact")
    public String contact() {
        return "contact";  // Return the view name for Contact Us page
    }

    // Functions page route
    @GetMapping("/functions")
    public String functions() {
        return "functions";  // Return the view name for Functions page
    }

    // Submenu routes under Functions
    @GetMapping("/functions/emotional-development")
    public String emotionalDevelopment() {
        return "emotionalDevelopment";  // Return the view name for Emotional Development page
    }

    @GetMapping("/functions/school-social-life")
    public String schoolSocialLife() {
        return "schoolSocialLife";  // Return the view name for School & Social Life page
    }

    @GetMapping("/functions/growth-milestones")
    public String growthMilestones() {
        return "growthMilestones";  // Return the view name for Growth & Milestones page
    }

    @GetMapping("/functions/parenting-resources")
    public String parentingResources() {
        return "parentingResources";  // Return the view name for Parenting Resources page
    }

    @GetMapping("/functions/child-experts")
    public String childExperts() {
        return "childExperts";  // Return the view name for Child Experts page
    }

    @GetMapping("/functions/nutrition-for-kids")
    public String nutritionForKids() {
        return "nutritionForKids";  // Return the view name for Nutrition for Kids page
    }

    // FAQ page route
    @GetMapping("/faq")
    public String faq() {
        return "faq";  // Return the view name for FAQ page
    }

    // Submenu routes under FAQ
    @GetMapping("/faq/parents")
    public String faqParents() {
        return "faqParents";  // Return the view name for Parents FAQ page
    }

    @GetMapping("/faq/feedback")
    public String faqFeedback() {
        return "faqFeedback";  // Return the view name for Feedback FAQ page
    }

    // Support page route
    @GetMapping("/support")
    public String support() {
        return "support";  // Return the view name for Support page
    }

    // Submenu routes under Support
    @GetMapping("/support/funding")
    public String funding() {
        return "funding";  // Return the view name for Funding page
    }

    @GetMapping("/support/join-us")
    public String joinUs() {
        return "joinUs";  // Return the view name for Join Us page
    }

    // Language options (could be configured later for i18n)
    @GetMapping("/language")
    public String language() {
        return "language";  // Return the view name for Language page
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
