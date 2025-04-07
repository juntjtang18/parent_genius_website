package ca.parentgeniusai.website.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class LoginController {

    private final BCryptPasswordEncoder passwordEncoder;
    //private final RestTemplate restTemplate;
    @Value("${strapi.url}")
    private String strapiUrl;

    public LoginController(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        //this.restTemplate = restTemplate;
    }

    @GetMapping("/signin")
    public String signin(Model model, @RequestParam(value = "error", required = false) String error, HttpServletRequest request) {
        System.out.println("end point /signin triggered.");
        model.addAttribute("error", error != null);
        String referer = request.getHeader("Referer");
        model.addAttribute("referer", referer != null && !referer.contains("/signin") ? referer : "/");
        return "signin";
    }
    
    @GetMapping("/protected")
    public String protectedPage(Authentication auth, Model model) {
        model.addAttribute("username", auth != null ? auth.getName() : "Guest");
        return "protected";
    }

    @GetMapping("/generate-hash")
    @ResponseBody
    public String generatePasswordHash(@RequestParam String password) {
        return "Generated hash for '" + password + "': " + passwordEncoder.encode(password);
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

}