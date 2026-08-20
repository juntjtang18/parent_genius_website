package ca.parentgeniusai.website.service;

import ca.parentgeniusai.website.model.Course;
import ca.parentgeniusai.website.model.CourseCategory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseService {
    private static final Logger logger = LoggerFactory.getLogger(CourseService.class);
    private static final String LIST_POPULATE = "populate=icon_image,coursecategory,pillar";
    private static final String DETAIL_POPULATE =
        "populate[content][populate]=image_file,video_file,thumbnail"
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
            "pillar " + pillarId
        );
        courses.sort(Comparator
            .comparing((Course course) -> Boolean.TRUE.equals(course.getPublished()), Comparator.reverseOrder())
            .thenComparing(Course::getOrder, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(Course::getTitle, Comparator.nullsLast(String::compareToIgnoreCase)));
        return courses;
    }

    public List<Course> getCoursesByCategoryId(Long courseCategoryId) {
        if (courseCategoryId == null) {
            return Collections.emptyList();
        }
        return fetchCourses(
            "filters[coursecategory][id][$eq]=" + courseCategoryId,
            "coursecategory " + courseCategoryId
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

    private List<Course> fetchCourses(String filterQuery, String logLabel) {
        String url = STRAPI_ROOTURL
            + "api/courses"
            + "?" + filterQuery
            + "&" + LIST_POPULATE
            + "&sort[0]=order:asc"
            + "&sort[1]=title:asc"
            + "&pagination[pageSize]=100";

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
        if (includeContent) {
            course.setContent(resolveContentMediaUrls(attrs.getContent()));
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

    private List<Map<String, Object>> resolveContentMediaUrls(List<Map<String, Object>> content) {
        if (content == null) {
            return Collections.emptyList();
        }
        content.forEach(this::absolutizeMediaUrls);
        return content;
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
}
