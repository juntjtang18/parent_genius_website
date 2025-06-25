package ca.parentgeniusai.website.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

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
        model.addAttribute("strapiApiUrl", "/api/strapi");
        model.addAttribute("strapiToken", "Bearer " + jwtToken);
        model.addAttribute("strapiRootUrl", strapiRootUrl);
        return "tips";
    }

    @GetMapping("/new-tip")
    public String newTipPage(Model model, HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) return "redirect:/signin?error=unauthenticated";
        model.addAttribute("strapiApiUrl", "/api/strapi");
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

    // API proxy methods
    @GetMapping("/api/strapi/tips")
    @ResponseBody
    public ResponseEntity<String> getTips(HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        String url = UriComponentsBuilder.fromHttpUrl(strapiApiBaseUrl + "/tips")
            .queryParam("populate", "icon_image")
            .queryParam("sort", "text:asc")
            .toUriString();
        try {
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    @PostMapping(value = "/api/strapi/tips", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<String> createTip(
            @RequestParam("data") String jsonData,
            @RequestParam(value = "files.icon_image", required = false) MultipartFile iconImage,
            HttpServletRequest request) {
        String jwtToken = getJwtToken(request);
        if (jwtToken == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(jwtToken);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("data", jsonData);

        if (iconImage != null && !iconImage.isEmpty()) {
            try {
                body.add("files.icon_image", new ByteArrayResource(iconImage.getBytes()) {
                    @Override public String getFilename() { return iconImage.getOriginalFilename(); }
                });
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Error reading file\"}");
            }
        }
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        try {
            return restTemplate.postForEntity(strapiApiBaseUrl + "/tips", requestEntity, String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }
}
