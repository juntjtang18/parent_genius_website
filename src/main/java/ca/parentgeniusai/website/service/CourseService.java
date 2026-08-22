package ca.parentgeniusai.website.service;

import ca.parentgeniusai.website.model.Course;
import ca.parentgeniusai.website.model.CourseAttachmentFile;
import ca.parentgeniusai.website.model.CourseCategory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
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
public class CourseService {
    private static final Logger logger = LoggerFactory.getLogger(CourseService.class);
    private static final String ATTACHMENTS_COMPONENT = "coursecontent.attachments";
    private static final String LIST_POPULATE = "populate=icon_image,coursecategory,pillar";
    private static final String DETAIL_POPULATE =
        "populate[content][on][coursecontent.text]=true"
            + "&populate[content][on][coursecontent.quiz]=true"
            + "&populate[content][on][coursecontent.pagebreaker]=true"
            + "&populate[content][on][coursecontent.external-video]=true"
            + "&populate[content][on][coursecontent.image][populate]=image_file"
            + "&populate[content][on][coursecontent.video][populate][0]=video_file"
            + "&populate[content][on][coursecontent.video][populate][1]=thumbnail"
            + "&populate[content][on][coursecontent.attachments][populate]=files"
            + "&populate=icon_image,coursecategory,pillar";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${strapi.root.url}")
    private String STRAPI_ROOTURL;

    @Value("${strapi.auth-token}")
    private String AUTH_TOKEN;

    private static class CourseListResponse {
        private List<CourseResponse> data;

        public List<CourseResponse> getData() { return data; }
        public void setData(List<CourseResponse> data) { this.data = data; }
    }

    private static class CourseSingleResponse {
        private CourseResponse data;

        public CourseResponse getData() { return data; }
        public void setData(CourseResponse data) { this.data = data; }
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
        private RelationWrapper pillar;
        private Boolean published;
        private String keywords;
        private List<Map<String, Object>> content;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Integer getOrder() { return order; }
        public void setOrder(Integer order) { this.order = order; }
        public MediaWrapper getIcon_image() { return icon_image; }
        public void setIcon_image(MediaWrapper icon_image) { this.icon_image = icon_image; }
        public RelationWrapper getCoursecategory() { return coursecategory; }
        public void setCoursecategory(RelationWrapper coursecategory) { this.coursecategory = coursecategory; }
        public RelationWrapper getPillar() { return pillar; }
        public void setPillar(RelationWrapper pillar) { this.pillar = pillar; }
        public Boolean getPublished() { return published; }
        public void setPublished(Boolean published) { this.published = published; }
        public String getKeywords() { return keywords; }
        public void setKeywords(String keywords) { this.keywords = keywords; }
        public List<Map<String, Object>> getContent() { return content; }
        public void setContent(List<Map<String, Object>> content) { this.content = content; }
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
        private Integer order;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getOrder() { return order; }
        public void setOrder(Integer order) { this.order = order; }
    }

    private static class CourseCategoryListResponse {
        private List<RelationData> data;

        public List<RelationData> getData() { return data; }
        public void setData(List<RelationData> data) { this.data = data; }
    }

    public List<CourseCategory> getCourseCategories() {
        String url = STRAPI_ROOTURL + "api/coursecategories?sort[0]=order:asc&pagination[pageSize]=100";
        try {
            logger.info("Fetching course categories: {}", url);
            ResponseEntity<CourseCategoryListResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, authEntity(), CourseCategoryListResponse.class
            );
            if (response.getBody() == null || response.getBody().getData() == null) {
                return Collections.emptyList();
            }
            return response.getBody().getData().stream()
                .map(item -> new CourseCategory(
                    item.getId(),
                    item.getAttributes() != null ? item.getAttributes().getName() : null,
                    item.getAttributes() != null ? item.getAttributes().getOrder() : null
                ))
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching course categories: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<Course> getCoursesByPillarId(Long pillarId) {
        if (pillarId == null) {
            return Collections.emptyList();
        }
        List<Course> courses = fetchCourses(
            "filters[pillar][id][$eq]=" + pillarId,
            "pillar " + pillarId,
            1,
            100
        );
        courses.sort(Comparator
            .comparing((Course course) -> Boolean.TRUE.equals(course.getPublished()), Comparator.reverseOrder())
            .thenComparing(Course::getOrder, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(Course::getTitle, Comparator.nullsLast(String::compareToIgnoreCase)));
        return courses;
    }

    public List<Course> getPublishedCourses() {
        List<Course> courses = new ArrayList<>();
        int page = 1;
        int pageSize = 100;
        while (page <= 50) {
            List<Course> batch = fetchCourses(
                "filters[published][$eq]=true",
                "published page " + page,
                page,
                pageSize
            );
            courses.addAll(batch);
            if (batch.size() < pageSize) {
                break;
            }
            page++;
        }
        return courses;
    }

    /**
     * Ask Strapi POST /api/ai/classify-course to pick one published course.
     * {@code userJwt} must be the logged-in user's users-permissions JWT, not the CMS token.
     */
    public Long classifyCourse(String question, String userJwt) {
        if (question == null || question.isBlank() || userJwt == null || userJwt.isBlank()) {
            logger.warn("[AskAI] skip classify course: blank question or jwt questionBlank={} jwtBlank={}",
                question == null || question.isBlank(), userJwt == null || userJwt.isBlank());
            return null;
        }

        List<Course> courses = getPublishedCourses();
        logger.info("[AskAI] question='{}' jwtLen={} publishedCourseCount={}",
            question.trim(), userJwt.length(), courses.size());
        if (courses.isEmpty()) {
            logger.warn("[AskAI] cannot classify course; no published courses");
            return null;
        }

        String url = STRAPI_ROOTURL + "api/ai/classify-course";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userJwt);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        List<Map<String, Object>> coursePayload = courses.stream()
            .map(course -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", course.getId());
                item.put("title", course.getTitle());
                item.put("keywords", course.getKeywords() == null ? "" : course.getKeywords());
                return item;
            })
            .collect(Collectors.toList());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("question", question.trim());
        body.put("courses", coursePayload);

        try {
            logger.info("[AskAI] POST {} question='{}' courseCount={}", url, question.trim(), courses.size());
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class
            );
            logger.info("[AskAI] HTTP {} rawBody={}", response.getStatusCode(), response.getBody());
            Set<Long> knownIds = courseIds(courses);
            Long bodyCourseId = extractClassifyCourseId(response.getBody());
            if (bodyCourseId != null && knownIds.contains(bodyCourseId)) {
                logger.info("[AskAI] matched courseId={}", bodyCourseId);
                return bodyCourseId;
            }
            Long fromReply = extractTaggedCourseId(extractClassifyReply(response.getBody()), knownIds);
            if (fromReply != null) {
                logger.info("[AskAI] matched courseId from reply={}", fromReply);
                return fromReply;
            }
            Long fallback = pickBestCourse(question, courses);
            logger.info("[AskAI] fallback courseId={}", fallback);
            return fallback;
        } catch (HttpClientErrorException e) {
            logger.error("[AskAI] Strapi rejected classify course: status={} body={}",
                e.getStatusCode(), e.getResponseBodyAsString());
            return pickBestCourse(question, courses);
        } catch (Exception e) {
            logger.error("[AskAI] classify course failed: {}", e.getMessage(), e);
            return pickBestCourse(question, courses);
        }
    }

    private Map<?, ?> classifyData(Map<?, ?> body) {
        if (body == null) {
            return null;
        }
        Object data = body.get("data");
        return data instanceof Map<?, ?> dataMap ? dataMap : null;
    }

    private Long extractClassifyCourseId(Map<?, ?> body) {
        Map<?, ?> data = classifyData(body);
        if (data == null || data.get("courseId") == null) {
            return null;
        }
        try {
            return Long.valueOf(data.get("courseId").toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractClassifyReply(Map<?, ?> body) {
        Map<?, ?> data = classifyData(body);
        if (data == null || data.get("text") == null) {
            return null;
        }
        return data.get("text").toString();
    }

    private Set<Long> courseIds(List<Course> courses) {
        return courses.stream()
            .map(Course::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private Long extractTaggedCourseId(String reply, Set<Long> ids) {
        if (reply == null || reply.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile("COURSE_ID\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(reply);
        if (matcher.find()) {
            Long id = Long.valueOf(matcher.group(1));
            return ids.contains(id) ? id : null;
        }
        String trimmed = reply.trim();
        if (trimmed.length() <= 12) {
            try {
                Long exact = Long.valueOf(trimmed);
                return ids.contains(exact) ? exact : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Long pickBestCourse(String question, List<Course> courses) {
        if (courses == null || courses.isEmpty()) {
            return null;
        }
        if (question != null && !question.isBlank()) {
            String lower = question.toLowerCase();
            Long bestId = null;
            int bestScore = 0;
            for (Course course : courses) {
                int score = 0;
                for (String keyword : course.getKeywordList()) {
                    String needle = keyword.toLowerCase();
                    if (needle.length() >= 2 && lower.contains(needle)) {
                        score++;
                    }
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestId = course.getId();
                }
            }
            if (bestScore > 0) {
                return bestId;
            }
        }
        return courses.stream()
            .map(Course::getId)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    public List<Course> getCoursesByCategoryId(Long courseCategoryId) {
        if (courseCategoryId == null) {
            return Collections.emptyList();
        }
        return fetchCourses(
            "filters[coursecategory][id][$eq]=" + courseCategoryId,
            "coursecategory " + courseCategoryId,
            1,
            100
        );
    }

    public Course getCourseById(Long courseId) {
        if (courseId == null) {
            return null;
        }

        String url = STRAPI_ROOTURL + "api/courses/" + courseId + "?" + DETAIL_POPULATE;
        try {
            logger.info("Fetching course {}: {}", courseId, url);
            ResponseEntity<CourseSingleResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, authEntity(), CourseSingleResponse.class
            );
            if (response.getBody() == null || response.getBody().getData() == null) {
                logger.warn("No course data returned for id {}", courseId);
                return null;
            }
            return toCourse(response.getBody().getData(), true);
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Course {} not found", courseId);
            return null;
        } catch (Exception e) {
            logger.error("Error fetching course {}: {}", courseId, e.getMessage(), e);
            return null;
        }
    }

    private List<Course> fetchCourses(String filterQuery, String logLabel, int page, int pageSize) {
        String url = STRAPI_ROOTURL
            + "api/courses"
            + "?" + filterQuery
            + "&" + LIST_POPULATE
            + "&sort[0]=order:asc"
            + "&sort[1]=title:asc"
            + "&pagination[page]=" + page
            + "&pagination[pageSize]=" + pageSize;

        try {
            logger.info("Fetching courses for {}: {}", logLabel, url);
            ResponseEntity<CourseListResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, authEntity(), CourseListResponse.class
            );
            if (response.getBody() == null || response.getBody().getData() == null) {
                logger.warn("No 'data' field found in the courses response for {}", logLabel);
                return Collections.emptyList();
            }
            return response.getBody().getData().stream()
                .map(resp -> toCourse(resp, false))
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching courses for {}: {}", logLabel, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private HttpEntity<String> authEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", AUTH_TOKEN);
        headers.set("Accept", "application/json");
        return new HttpEntity<>(headers);
    }

    private Course toCourse(CourseResponse resp, boolean includeContent) {
        CourseAttributes attrs = resp.getAttributes();
        if (attrs == null) {
            return new Course(resp.getId(), null, null, null, null, null);
        }

        RelationData category = relationData(attrs.getCoursecategory());
        RelationData pillar = relationData(attrs.getPillar());
        Course course = new Course(
            resp.getId(),
            attrs.getTitle(),
            attrs.getOrder(),
            resolveIconImageUrl(attrs.getIcon_image()),
            category != null ? category.getId() : null,
            relationName(category),
            pillar != null ? pillar.getId() : null,
            relationName(pillar)
        );
        course.setPublished(attrs.getPublished());
        course.setKeywords(attrs.getKeywords());
        if (includeContent) {
            course.setContent(normalizeCourseContent(attrs.getContent()));
        }
        return course;
    }

    private RelationData relationData(RelationWrapper wrapper) {
        return wrapper != null ? wrapper.getData() : null;
    }

    private String relationName(RelationData data) {
        if (data == null || data.getAttributes() == null) {
            return null;
        }
        return data.getAttributes().getName();
    }

    List<Map<String, Object>> normalizeCourseContent(List<Map<String, Object>> content) {
        if (content == null) {
            return Collections.emptyList();
        }
        content.forEach(this::normalizeContentBlock);
        return content;
    }

    public List<CourseAttachmentFile> extractAttachmentFiles(Object filesField) {
        List<CourseAttachmentFile> files = new ArrayList<>();
        for (Object entry : asObjectList(unwrapStrapiData(filesField))) {
            CourseAttachmentFile file = toAttachmentFile(entry);
            if (file != null && file.getUrl() != null && !file.getUrl().isBlank()) {
                files.add(file);
            }
        }
        return files;
    }

    private void normalizeContentBlock(Map<String, Object> item) {
        if (item == null) {
            return;
        }
        absolutizeMediaUrls(item);
        if (!ATTACHMENTS_COMPONENT.equals(item.get("__component"))) {
            return;
        }
        Object title = item.get("title");
        item.put("title", title == null ? "" : title.toString());
        item.put("files", extractAttachmentFiles(item.get("files")).stream()
            .map(CourseAttachmentFile::toMap)
            .collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    private CourseAttachmentFile toAttachmentFile(Object entry) {
        if (!(entry instanceof Map<?, ?> raw)) {
            return null;
        }
        Map<String, Object> file = (Map<String, Object>) raw;
        Map<String, Object> attributes = asStringObjectMap(file.get("attributes"));
        Long id = toLong(file.get("id"));
        String name = firstNonBlank(
            stringValue(file.get("name")),
            attributes != null ? stringValue(attributes.get("name")) : null,
            id != null ? "File " + id : "Download"
        );
        String url = firstNonBlank(
            stringValue(file.get("url")),
            attributes != null ? stringValue(attributes.get("url")) : null
        );
        String mime = firstNonBlank(
            stringValue(file.get("mime")),
            attributes != null ? stringValue(attributes.get("mime")) : null
        );
        return new CourseAttachmentFile(id, name, toAbsoluteUrl(url), mime);
    }

    @SuppressWarnings("unchecked")
    private void absolutizeMediaUrls(Map<String, Object> node) {
        if (node == null) {
            return;
        }
        Object url = node.get("url");
        if (url instanceof String relativeUrl) {
            node.put("url", toAbsoluteUrl(relativeUrl));
        }
        for (Object value : node.values()) {
            if (value instanceof Map<?, ?> nested) {
                absolutizeMediaUrls((Map<String, Object>) nested);
            } else if (value instanceof List<?> list) {
                for (Object entry : list) {
                    if (entry instanceof Map<?, ?> nested) {
                        absolutizeMediaUrls((Map<String, Object>) nested);
                    }
                }
            }
        }
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
        return toAbsoluteUrl(relativeUrl);
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

    private String toAbsoluteUrl(String relativeUrl) {
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

    private Object unwrapStrapiData(Object value) {
        if (value instanceof Map<?, ?> map && map.containsKey("data")) {
            return map.get("data");
        }
        return value;
    }

    private List<Object> asObjectList(Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        if (value instanceof Map<?, ?>) {
            return List.of(value);
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asStringObjectMap(Object value) {
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
