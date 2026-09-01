package ca.parentgeniusai.website.service;

import ca.parentgeniusai.website.model.CommunityPost;
import ca.parentgeniusai.website.model.CommunityPostPage;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public static final int PAGE_SIZE = 50;

    public CommunityPostPage getPostsByPillarId(Long pillarId, int page) {
        int safePage = Math.max(1, page);
        if (pillarId == null) {
            return CommunityPostPage.empty(safePage, PAGE_SIZE);
        }

        String root = strapiRootUrl.endsWith("/") ? strapiRootUrl : strapiRootUrl + "/";
        String url = root + "api/getposts?sort=createdAt:desc"
            + "&pagination[page]=" + safePage
            + "&pagination[pageSize]=" + PAGE_SIZE
            + "&filters[pillar][id][$eq]=" + pillarId;

        try {
            logger.info("Fetching community posts for pillar {} page {}: {}", pillarId, safePage, url);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.GET, authEntity(), JsonNode.class
            );
            JsonNode body = response.getBody();
            CommunityPostPage result = new CommunityPostPage();
            result.setPage(safePage);
            result.setPageSize(PAGE_SIZE);

            JsonNode pagination = body == null ? null : body.path("meta").path("pagination");
            if (pagination != null && !pagination.isMissingNode()) {
                result.setPage(pagination.path("page").asInt(safePage));
                result.setPageSize(pagination.path("pageSize").asInt(PAGE_SIZE));
                result.setPageCount(Math.max(1, pagination.path("pageCount").asInt(1)));
                result.setTotal(pagination.path("total").asLong(0));
            }

            JsonNode data = body == null ? null : body.path("data");
            if (data == null || !data.isArray()) {
                result.setPosts(Collections.emptyList());
                return result;
            }

            List<CommunityPost> posts = new ArrayList<>();
            for (JsonNode item : data) {
                CommunityPost post = toPost(item);
                if (post != null) {
                    posts.add(post);
                }
            }
            result.setPosts(posts);
            if (result.getTotal() == 0) {
                result.setTotal(posts.size());
            }
            return result;
        } catch (Exception e) {
            logger.error("Error fetching community posts for pillar {} page {}: {}",
                pillarId, safePage, e.getMessage(), e);
            return CommunityPostPage.empty(safePage, PAGE_SIZE);
        }
    }

    public Long createPost(String content, Long pillarId, String userJwt, List<MultipartFile> mediaFiles) {
        String text = content == null ? "" : content.trim();
        if (pillarId == null || userJwt == null || userJwt.isBlank()) {
            return null;
        }

        Long userId = currentUserId(userJwt);
        if (userId == null) {
            logger.warn("Cannot create community post: could not resolve current user");
            return null;
        }

        List<Long> mediaIds = uploadMedia(mediaFiles, userJwt);
        if (text.isBlank() && mediaIds.isEmpty()) {
            return null;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", text);
        data.put("pillar", pillarId);
        data.put("users_permissions_user", userId);
        if (!mediaIds.isEmpty()) {
            data.put("media", mediaIds);
        }
        Map<String, Object> payload = Map.of("data", data);

        String url = root() + "api/posts";
        try {
            logger.info("Creating community post for pillar {} by user {}", pillarId, userId);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.POST, jsonEntity(payload, userJwt), JsonNode.class
            );
            JsonNode body = response.getBody();
            long id = body == null ? 0 : body.path("data").path("id").asLong(0);
            if (id <= 0) {
                logger.warn("Strapi created post but returned no id: {}", body);
                return null;
            }
            return id;
        } catch (HttpClientErrorException e) {
            logger.error("Strapi rejected community post create: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            logger.error("Error creating community post for pillar {}: {}", pillarId, e.getMessage(), e);
            return null;
        }
    }

    private List<Long> uploadMedia(List<MultipartFile> mediaFiles, String userJwt) {
        if (mediaFiles == null || mediaFiles.isEmpty()) {
            return Collections.emptyList();
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            for (MultipartFile file : mediaFiles) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String filename = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
                body.add("files", new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return filename;
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Failed to read media files for community post: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
        if (body.isEmpty()) {
            return Collections.emptyList();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(userJwt);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                root() + "api/upload", HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class
            );
            JsonNode uploaded = response.getBody();
            if (uploaded == null || !uploaded.isArray()) {
                return Collections.emptyList();
            }
            List<Long> ids = new ArrayList<>();
            for (JsonNode item : uploaded) {
                if (item.path("id").isNumber()) {
                    ids.add(item.path("id").asLong());
                }
            }
            return ids;
        } catch (Exception e) {
            logger.error("Failed to upload community post media: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private Long currentUserId(String userJwt) {
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                root() + "api/users/me", HttpMethod.GET, jsonEntity(null, userJwt), JsonNode.class
            );
            JsonNode body = response.getBody();
            if (body == null || !body.path("id").isNumber()) {
                return null;
            }
            return body.path("id").asLong();
        } catch (Exception e) {
            logger.warn("Failed to load current user from Strapi: {}", e.getMessage());
            return null;
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

    private String root() {
        return strapiRootUrl.endsWith("/") ? strapiRootUrl : strapiRootUrl + "/";
    }

    private HttpEntity<String> authEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        headers.set("Accept", "application/json");
        return new HttpEntity<>(headers);
    }

    private HttpEntity<Object> jsonEntity(Object payload, String userJwt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(userJwt);
        return new HttpEntity<>(payload, headers);
    }
}
