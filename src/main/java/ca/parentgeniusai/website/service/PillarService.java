package ca.parentgeniusai.website.service;

import ca.parentgeniusai.website.model.Course;
import ca.parentgeniusai.website.model.Pillar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PillarService {
    private static final Logger logger = LoggerFactory.getLogger(PillarService.class);
    private static final String COURSES_POPULATE =
        "populate[courses][populate]=icon_image,coursecategory"
            + "&populate[courses][sort][0]=order:asc"
            + "&populate[courses][sort][1]=title:asc";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${strapi.root.url}")
    private String STRAPI_ROOTURL;

    @Value("${strapi.auth-token}")
    private String AUTH_TOKEN;

    private static class PillarListResponse {
        private List<PillarResponse> data;

        public List<PillarResponse> getData() { return data; }
        public void setData(List<PillarResponse> data) { this.data = data; }
    }

    private static class PillarSingleResponse {
        private PillarResponse data;

        public PillarResponse getData() { return data; }
        public void setData(PillarResponse data) { this.data = data; }
    }

    private static class PillarResponse {
        private Long id;
        private PillarAttributes attributes;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public PillarAttributes getAttributes() { return attributes; }
        public void setAttributes(PillarAttributes attributes) { this.attributes = attributes; }
    }

    private static class PillarAttributes {
        private String name;
        private Integer order;
        private CourseListWrapper courses;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getOrder() { return order; }
        public void setOrder(Integer order) { this.order = order; }
        public CourseListWrapper getCourses() { return courses; }
        public void setCourses(CourseListWrapper courses) { this.courses = courses; }
    }

    private static class CourseListWrapper {
        private List<CourseResponse> data;

        public List<CourseResponse> getData() { return data; }
        public void setData(List<CourseResponse> data) { this.data = data; }
    }

    private static class CourseResponse {
        private Long id;
        private CourseAttributes attributes;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public CourseAttributes getAttributes() { return attributes; }
        public void setAttributes(CourseAttributes attributes) { this.attributes = attributes; }
    }

    private static class CourseAttributes {
        private String title;
        private Integer order;
        private MediaWrapper icon_image;
        private RelationWrapper coursecategory;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Integer getOrder() { return order; }
        public void setOrder(Integer order) { this.order = order; }
        public MediaWrapper getIcon_image() { return icon_image; }
        public void setIcon_image(MediaWrapper icon_image) { this.icon_image = icon_image; }
        public RelationWrapper getCoursecategory() { return coursecategory; }
        public void setCoursecategory(RelationWrapper coursecategory) { this.coursecategory = coursecategory; }
    }

    private static class MediaWrapper {
        private MediaData data;

        public MediaData getData() { return data; }
        public void setData(MediaData data) { this.data = data; }
    }

    private static class MediaData {
        private MediaAttributes attributes;

        public MediaAttributes getAttributes() { return attributes; }
        public void setAttributes(MediaAttributes attributes) { this.attributes = attributes; }
    }

    private static class MediaAttributes {
        private String url;
        private Map<String, Object> formats;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public Map<String, Object> getFormats() { return formats; }
        public void setFormats(Map<String, Object> formats) { this.formats = formats; }
    }

    private static class RelationWrapper {
        private RelationData data;

        public RelationData getData() { return data; }
        public void setData(RelationData data) { this.data = data; }
    }

    private static class RelationData {
        private Long id;
        private NamedAttributes attributes;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public NamedAttributes getAttributes() { return attributes; }
        public void setAttributes(NamedAttributes attributes) { this.attributes = attributes; }
    }

    private static class NamedAttributes {
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public List<Pillar> getPillars() {
        String url = STRAPI_ROOTURL + "api/pillars?sort[0]=order:asc&pagination[pageSize]=100";
        try {
            logger.info("Fetching pillars: {}", url);
            ResponseEntity<PillarListResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, authEntity(), PillarListResponse.class
            );
            if (response.getBody() == null || response.getBody().getData() == null) {
                logger.warn("No 'data' field found in the pillars response");
                return Collections.emptyList();
            }
            return response.getBody().getData().stream()
                .map(resp -> toPillar(resp, false))
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching pillars: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Ask Strapi POST /api/ai/chat (same path as the site chatbot) to pick one pillar.
     * {@code userJwt} must be the logged-in user's users-permissions JWT, not the CMS token.
     */
    public Long classifyPillar(String question, String userJwt) {
        if (question == null || question.isBlank() || userJwt == null || userJwt.isBlank()) {
            logger.warn("[AskAI] skip classify: blank question or jwt questionBlank={} jwtBlank={}",
                question == null || question.isBlank(), userJwt == null || userJwt.isBlank());
            return null;
        }
        List<Pillar> pillars = getPillars();
        logger.info("[AskAI] question='{}' jwtLen={} pillarCount={} pillars={}",
            question.trim(), userJwt.length(), pillars.size(), summarizePillars(pillars));
        if (pillars.isEmpty()) {
            logger.warn("[AskAI] cannot classify; pillar list is empty");
            return null;
        }

        String url = STRAPI_ROOTURL + "api/ai/classify-pillar";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userJwt);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        List<Map<String, Object>> pillarPayload = pillars.stream()
            .map(pillar -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", pillar.getId());
                item.put("name", pillar.getName());
                return item;
            })
            .collect(Collectors.toList());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("question", question.trim());
        body.put("pillars", pillarPayload);

        try {
            logger.info("[AskAI] POST {} question='{}' pillars={}", url, question.trim(), summarizePillars(pillars));
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class
            );
            logger.info("[AskAI] HTTP {} rawBody={}", response.getStatusCode(), response.getBody());
            String reply = extractClassifyReply(response.getBody());
            Long bodyPillarId = extractClassifyPillarId(response.getBody());
            logger.info("[AskAI] extracted pillarId={} reply='{}'", bodyPillarId, reply);
            Long pillarId = bodyPillarId != null ? bodyPillarId : parsePillarId(reply, question, pillars);
            logger.info("[AskAI] matched pillarId={}", pillarId);
            return pillarId;
        } catch (HttpClientErrorException e) {
            logger.error("[AskAI] Strapi rejected classify: status={} body={}",
                e.getStatusCode(), e.getResponseBodyAsString());
            return parsePillarId(null, question, pillars);
        } catch (Exception e) {
            logger.error("[AskAI] classify failed: {}", e.getMessage(), e);
            return parsePillarId(null, question, pillars);
        }
    }

    private String summarizePillars(List<Pillar> pillars) {
        return pillars.stream()
            .map(p -> p.getId() + ":" + p.getName())
            .collect(Collectors.joining(", "));
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> classifyData(Map<?, ?> body) {
        if (body == null) {
            return null;
        }
        Object data = body.get("data");
        return data instanceof Map<?, ?> dataMap ? dataMap : null;
    }

    private Long extractClassifyPillarId(Map<?, ?> body) {
        Map<?, ?> data = classifyData(body);
        if (data == null || data.get("pillarId") == null) {
            return null;
        }
        try {
            return Long.valueOf(data.get("pillarId").toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractClassifyReply(Map<?, ?> body) {
        Map<?, ?> data = classifyData(body);
        if (data == null) {
            logger.warn("[AskAI] extract reply: data is missing from {}", body);
            return null;
        }
        Object text = data.get("text");
        return text != null ? text.toString() : null;
    }

    private Long parsePillarId(String reply, String question, List<Pillar> pillars) {
        Set<Long> ids = pillars.stream()
            .map(Pillar::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Long fromTagged = extractTaggedId(reply, ids);
        if (fromTagged != null) {
            logger.info("[AskAI] parse: PILLAR_ID tag -> {}", fromTagged);
            return fromTagged;
        }

        Long fromNumber = extractFirstKnownId(reply, ids);
        if (fromNumber != null) {
            logger.info("[AskAI] parse: number in short reply -> {}", fromNumber);
            return fromNumber;
        }

        Long fromName = matchPillarName(reply, pillars);
        if (fromName != null) {
            logger.info("[AskAI] parse: pillar name in reply -> {}", fromName);
            return fromName;
        }

        Long fromReplyKeywords = matchKeywords(reply, pillars);
        if (fromReplyKeywords != null) {
            logger.info("[AskAI] parse: keywords in reply -> {}", fromReplyKeywords);
            return fromReplyKeywords;
        }

        Long fromQuestionKeywords = matchKeywords(question, pillars);
        if (fromQuestionKeywords != null) {
            logger.info("[AskAI] parse: keywords in question -> {}", fromQuestionKeywords);
            return fromQuestionKeywords;
        }

        logger.warn("[AskAI] parse: no match from reply='{}' question='{}'", reply, question);
        return null;
    }

    private Long extractTaggedId(String reply, Set<Long> ids) {
        if (reply == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("PILLAR_ID\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(reply);
        if (matcher.find()) {
            Long id = Long.valueOf(matcher.group(1));
            return ids.contains(id) ? id : null;
        }
        return null;
    }

    private Long extractFirstKnownId(String reply, Set<Long> ids) {
        if (reply == null || reply.isBlank()) {
            return null;
        }
        String trimmed = reply.trim();
        // Long chatbot essays often contain ages or step counts that are not pillar ids.
        if (trimmed.length() > 40) {
            return null;
        }
        try {
            Long exact = Long.valueOf(trimmed);
            if (ids.contains(exact)) {
                return exact;
            }
        } catch (NumberFormatException ignored) {
            // scan embedded numbers next
        }
        Matcher matcher = Pattern.compile("\\b(\\d+)\\b").matcher(trimmed);
        while (matcher.find()) {
            Long id = Long.valueOf(matcher.group(1));
            if (ids.contains(id)) {
                return id;
            }
        }
        return null;
    }

    private Long matchPillarName(String text, List<Pillar> pillars) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String lower = text.toLowerCase();
        for (Pillar pillar : pillars) {
            if (pillar.getName() != null && lower.contains(pillar.getName().toLowerCase())) {
                return pillar.getId();
            }
        }
        return null;
    }

    private Long matchKeywords(String text, List<Pillar> pillars) {
        if (text == null || text.isBlank() || pillars.isEmpty()) {
            return null;
        }
        String lower = text.toLowerCase();
        int order;
        if (containsAny(lower, "depress", "sad", "anxi", "emotion", "tantrum", "meltdown", "mental", "wellbeing", "self-esteem")) {
            order = 2;
        } else if (containsAny(lower, "sleep", "bedtime", "screen", "phone", "gaming", "cyber", "routine", "co-parent", "coparent")) {
            order = 5;
        } else if (containsAny(lower, "adhd", "autism", "neurodivers", "sensory", "inclusion")) {
            order = 6;
        } else if (containsAny(lower, "listen", "communicat", "talk", "social", "friend", "bond")) {
            order = 3;
        } else if (containsAny(lower, "homework", "school", "focus", "executive", "thinking", "learning")) {
            order = 4;
        } else if (containsAny(lower, "trust", "boundar", "discipline", "responsible", "toddler")) {
            order = 1;
        } else {
            return null;
        }
        for (Pillar pillar : pillars) {
            if (pillar.getOrder() != null && pillar.getOrder() == order) {
                return pillar.getId();
            }
        }
        if (order >= 1 && order <= pillars.size()) {
            return pillars.get(order - 1).getId();
        }
        return null;
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public Pillar getPillarById(Long pillarId) {
        if (pillarId == null) {
            return null;
        }
        String url = STRAPI_ROOTURL + "api/pillars/" + pillarId + "?" + COURSES_POPULATE;
        try {
            logger.info("Fetching pillar {}: {}", pillarId, url);
            ResponseEntity<PillarSingleResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, authEntity(), PillarSingleResponse.class
            );
            if (response.getBody() == null || response.getBody().getData() == null) {
                logger.warn("No pillar data returned for id {}", pillarId);
                return null;
            }
            return toPillar(response.getBody().getData(), true);
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Pillar {} not found", pillarId);
            return null;
        } catch (Exception e) {
            logger.error("Error fetching pillar {}: {}", pillarId, e.getMessage(), e);
            return null;
        }
    }

    private HttpEntity<String> authEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", AUTH_TOKEN);
        headers.set("Accept", "application/json");
        return new HttpEntity<>(headers);
    }

    private Pillar toPillar(PillarResponse resp, boolean includeCourses) {
        PillarAttributes attrs = resp.getAttributes();
        Pillar pillar = new Pillar(
            resp.getId(),
            attrs != null ? attrs.getName() : null,
            attrs != null ? attrs.getOrder() : null
        );
        if (includeCourses && attrs != null && attrs.getCourses() != null
            && attrs.getCourses().getData() != null) {
            pillar.setCourses(attrs.getCourses().getData().stream()
                .map(courseResp -> toCourse(courseResp, pillar))
                .sorted(Comparator
                    .comparing(Course::getOrder, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(Course::getTitle, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList()));
        }
        return pillar;
    }

    private Course toCourse(CourseResponse resp, Pillar pillar) {
        CourseAttributes attrs = resp.getAttributes();
        if (attrs == null) {
            return new Course(resp.getId(), null, null, null, null, null,
                pillar.getId(), pillar.getName());
        }

        RelationData category = attrs.getCoursecategory() != null
            ? attrs.getCoursecategory().getData() : null;

        return new Course(
            resp.getId(),
            attrs.getTitle(),
            attrs.getOrder(),
            resolveIconImageUrl(attrs.getIcon_image()),
            category != null ? category.getId() : null,
            category != null && category.getAttributes() != null
                ? category.getAttributes().getName() : null,
            pillar.getId(),
            pillar.getName()
        );
    }

    private String resolveIconImageUrl(MediaWrapper iconImage) {
        if (iconImage == null || iconImage.getData() == null
            || iconImage.getData().getAttributes() == null) {
            return null;
        }

        MediaAttributes media = iconImage.getData().getAttributes();
        String relativeUrl = formatUrl(media.getFormats(), "medium");
        if (relativeUrl == null) {
            relativeUrl = formatUrl(media.getFormats(), "small");
        }
        if (relativeUrl == null) {
            relativeUrl = media.getUrl();
        }
        if (relativeUrl == null || relativeUrl.isBlank()) {
            return null;
        }
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl;
        }
        return STRAPI_ROOTURL.endsWith("/")
            ? STRAPI_ROOTURL.substring(0, STRAPI_ROOTURL.length() - 1) + relativeUrl
            : STRAPI_ROOTURL + relativeUrl;
    }

    private String formatUrl(Map<String, Object> formats, String key) {
        if (formats == null || !formats.containsKey(key) || formats.get(key) == null) {
            return null;
        }
        Object format = formats.get(key);
        if (format instanceof Map<?, ?> formatMap) {
            Object url = formatMap.get("url");
            return url != null ? url.toString() : null;
        }
        return null;
    }
}
