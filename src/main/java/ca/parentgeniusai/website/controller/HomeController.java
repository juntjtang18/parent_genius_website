package ca.parentgeniusai.website.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import ca.parentgeniusai.website.model.Pillar;
import ca.parentgeniusai.website.service.PillarService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class HomeController {

    private static final String[] PILLAR_ICONS = {
        "fas fa-home",
        "fas fa-heart",
        "fas fa-handshake",
        "fas fa-book-open",
        "fas fa-theater-masks",
        "fas fa-puzzle-piece"
    };

    private static final String[] PILLAR_DESCRIPTIONS = {
        "Build a strong foundation for confident parenting by understanding child development, positive parenting approaches, family routines, and the essential principles that support healthy growth from early childhood through adolescence.",
        "Support emotional regulation, resilience, confidence, self-awareness, mental wellbeing, and healthy coping strategies so children can navigate life's challenges with greater independence and emotional strength.",
        "Strengthen communication within families while helping children build meaningful relationships, social skills, empathy, collaboration, conflict resolution, and a strong sense of belonging at home, school, and in the community.",
        "Help children develop executive functioning, attention, memory, critical thinking, problem-solving, study skills, motivation, creativity, and lifelong learning habits for success both in and beyond school.",
        "Equip families with practical strategies for everyday life, including healthy routines, sleep, nutrition, technology and AI, screen time, online safety, sensory-friendly environments, independence, and other real-world challenges facing today's families.",
        "Develop a deeper understanding of neurodiversity, including autism, ADHD, dyslexia, sensory processing differences, executive functioning challenges, giftedness, & other diverse learning profiles. Learn practical, strengths-based strategies that promote inclusion, confidence, and success for every child."
    };

    private final PillarService pillarService;

    public HomeController(PillarService pillarService) {
        this.pillarService = pillarService;
    }

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
    
    @GetMapping("/index-inner/why-parenting")
    public String indexWhyParenting() {
    	return "index-inner/why-parenting";
    }
    
    @GetMapping("/index-inner/why-creativity")
    public String indexWhyCreativity() {
    	return "index-inner/why-creativity";
    }
    
    @GetMapping("/index-inner/why-community")
    public String indexWhyCommunity() {
    	return "index-inner/why-community";
    }
    
    @GetMapping("/membership")
    public String membership() {
    	return "membership";
    }
    
    @GetMapping("/pillars")
    public String pillars(HttpServletRequest request, HttpServletResponse response,
                          @RequestParam(name = "lang", required = false) String lang,
                          Model model) {
        if (lang != null) {
            LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
            if (localeResolver != null) {
                localeResolver.setLocale(request, response, new Locale(lang));
            }
        }
        model.addAttribute("pillarCards", toPillarCards(pillarService.getPillars()));
        return "pillars";
    }

    private List<PillarCard> toPillarCards(List<Pillar> pillars) {
        List<PillarCard> cards = new ArrayList<>();
        if (pillars == null) {
            return cards;
        }
        for (int i = 0; i < pillars.size(); i++) {
            Pillar pillar = pillars.get(i);
            cards.add(new PillarCard(
                pillar.getId(),
                pillar.getName(),
                descriptionFor(pillar.getName(), i),
                PILLAR_ICONS[i % PILLAR_ICONS.length]
            ));
        }
        return cards;
    }

    private String descriptionFor(String name, int index) {
        String n = name == null ? "" : name.toLowerCase();
        if (n.contains("foundation")) {
            return PILLAR_DESCRIPTIONS[0];
        }
        if (n.contains("emotion") || n.contains("wellbeing") || n.contains("mental")) {
            return PILLAR_DESCRIPTIONS[1];
        }
        if (n.contains("communicat") || n.contains("connection") || n.contains("relationship")) {
            return PILLAR_DESCRIPTIONS[2];
        }
        if (n.contains("learning") || n.contains("thinking")) {
            return PILLAR_DESCRIPTIONS[3];
        }
        if (n.contains("daily") || n.contains("modern")) {
            return PILLAR_DESCRIPTIONS[4];
        }
        if (n.contains("neuro") || n.contains("inclusion")) {
            return PILLAR_DESCRIPTIONS[5];
        }
        return PILLAR_DESCRIPTIONS[Math.min(index, PILLAR_DESCRIPTIONS.length - 1)];
    }

    public static class PillarCard {
        private final Long id;
        private final String name;
        private final String description;
        private final String iconClass;

        public PillarCard(Long id, String name, String description, String iconClass) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.iconClass = iconClass;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getIconClass() { return iconClass; }
    }

    @GetMapping({"/about-us", "/vision"})
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
        return "about-us";
    }
    
    @GetMapping("/feedback")
    public String posts(Model model) {
        // Keep username logic, but auth is handled globally
        if (model.containsAttribute("auth") && model.getAttribute("auth") != null) {
            model.addAttribute("username", ((org.springframework.security.core.Authentication) model.getAttribute("auth")).getName());
        } else {
            model.addAttribute("username", "Guest");
        }
        return "feedback";
    }
	
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

    @GetMapping("/community")
    public String community() {
        return "community";
    }

    @GetMapping("/community-guidelines")
    public String communityGuideLines(Model model) {
    	return "community-guidelines";
    }
    
    
    @GetMapping("/join-us")
    public String joinUs(Model model) {
        // No need for auth parameter or model attribute
        return "membership";    
    }
    
}