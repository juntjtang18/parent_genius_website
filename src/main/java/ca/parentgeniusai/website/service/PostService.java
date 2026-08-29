package ca.parentgeniusai.website.service;

import ca.parentgeniusai.website.model.CommunityPost;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PostService {
    private static final Logger logger = LoggerFactory.getLogger(PostService.class);
    private static final Pattern LEAD_PATTERN = Pattern.compile("^([^:\\n]{1,80}):\\s*([\\s\\S]+)$");

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${strapi.root.url}")
    private String strapiRootUrl;

    @Value("${strapi.auth-token}")
    private String authToken;

    public List<CommunityPost> getPostsByPillarId(Long pillarId) {
        if (pillarId == null) {
            return Collections.emptyList();
        }

        String root = strapiRootUrl.endsWith("/") ? strapiRootUrl : strapiRootUrl + "/";
        String url = root + "api/getposts?sort=createdAt:desc"
            + "&pagination[page]=1"
            + "&pagination[pageSize]=50"
            + "&filters[pillar][id][$eq]=" + pillarId;

        try {
            logger.info("Fetching community posts for pillar {}: {}", pillarId, url);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.GET, authEntity(), JsonNode.class
            );
            JsonNode data = response.getBody() == null ? null : response.getBody().path("data");
            if (data == null || !data.isArray()) {
                return Collections.emptyList();
            }

            List<CommunityPost> posts = new ArrayList<>();
            for (JsonNode item : data) {
                CommunityPost post = toPost(item);
                if (post != null) {
                    posts.add(post);
                }
            }
            return posts;
        } catch (Exception e) {
            logger.error("Error fetching community posts for pillar {}: {}", pillarId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private CommunityPost toPost(JsonNode item) {
        if (item == null || item.isMissingNode()) {
            return null;
        }
        JsonNode attrs = item.has("attributes") ? item.get("attributes") : item;
        String content = text(attrs, "content");
        if (content.isBlank()) {
            return null;
        }

        String username = text(
            attrs.path("users_permissions_user").path("data").path("attributes"),
            "username"
        );
        if (username.isBlank()) {
            username = text(attrs.path("users_permissions_user"), "username");
        }

        Matcher matcher = LEAD_PATTERN.matcher(content.trim());
        String lead = null;
        String body = content.trim();
        if (matcher.matches()) {
            lead = matcher.group(1).trim();
            body = matcher.group(2).trim();
        }

        return new CommunityPost(
            item.path("id").isNumber() ? item.path("id").asLong() : null,
            displayName(username),
            lead,
            body
        );
    }

    static String displayName(String username) {
        if (username == null || username.isBlank()) {
            return "Anonymous";
        }
        String[] parts = username.trim().split("[@\\s._-]");
        return parts.length > 0 && !parts[0].isBlank() ? parts[0] : username;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.path(field).isMissingNode()) {
            return "";
        }
        return node.path(field).asText("");
    }

    private HttpEntity<String> authEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        headers.set("Accept", "application/json");
        return new HttpEntity<>(headers);
    }
}
