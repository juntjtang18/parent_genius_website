package ca.parentgeniusai.website.controller;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/languages")
public class RestAPIController {

    @GetMapping
    public Map<String, String> getSupportedLanguages() {
    	System.out.print("/api/languages called\n");
    	Map<String, String> languages = new LinkedHashMap<>();
        languages.put("en", "English");
        languages.put("fr", "French");
        languages.put("de", "German");
        languages.put("es", "Spanish");
        languages.put("zh_CN", "Chinese (Simplified)"); // Use zh_CN instead of zh
        languages.put("ja", "Japanese");
        languages.put("ko", "Korean");
        return languages;
    }
}