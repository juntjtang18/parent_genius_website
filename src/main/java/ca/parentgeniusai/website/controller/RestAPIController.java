package ca.parentgeniusai.website.controller;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestAPIController {
	@Value("${strapi.url}")
	private String strapiRootUrl;
	
	@Value("${strapi.auth-token}")
	private String authToken;
	
    @GetMapping("/api/languages")
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
        languages.put("si", "Sinhala");
        languages.put("hi", "Hindi");
        languages.put("bn", "Bengali");
        return languages;
    }
    
    @GetMapping("/api/strapi_config")
    public Map<String, String> getStrapiConfig() {
    	return Map.of(
    			"STRAP_ROOTRUL", strapiRootUrl,
    			"AUTH_TOKEN", authToken
    			);
    			
    }
}