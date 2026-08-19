package ca.parentgeniusai.website.service;

import ca.parentgeniusai.website.model.Course;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PillarService {
    private static final Logger logger = LoggerFactory.getLogger(PillarService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${strapi.root.url}")
    private String STRAPI_ROOTURL;

    @Value("${strapi.auth-token}")
    private String AUTH_TOKEN;

    private static class CourseApiResponse {
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
        private CourseCategoryAttributes attributes;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public CourseCategoryAttributes getAttributes() { return attributes; }
        public void setAttributes(CourseCategoryAttributes attributes) { this.attributes = attributes; }
    }

    private static class CourseCategoryAttributes {
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    /**
     * Returns courses whose Strapi {@code coursecategory} id equals {@code courseCategoryId},
     * sorted by {@code order} then {@code title}.
     */
    public List<Course> getCoursesByCategoryId(Long courseCategoryId) {
        if (courseCategoryId == null) {
            return Collections.emptyList();
        }

        String url = STRAPI_ROOTURL
            + "api/courses"
            + "?filters[coursecategory][id][$eq]=" + courseCategoryId
            + "&populate=icon_image,coursecategory"
            + "&sort[0]=order:asc"
            + "&sort[1]=title:asc"
            + "&pagination[pageSize]=100";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", AUTH_TOKEN);
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            logger.info("Fetching courses for coursecategory {}: {}", courseCategoryId, url);
            ResponseEntity<CourseApiResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, CourseApiResponse.class
            );

            if (response.getBody() == null || response.getBody().getData() == null) {
                logger.warn("No 'data' field found in the courses response for category {}", courseCategoryId);
                return Collections.emptyList();
            }

            return response.getBody().getData().stream()
                .map(this::toCourse)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching courses for coursecategory {}: {}", courseCategoryId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private Course toCourse(CourseResponse resp) {
        CourseAttributes attrs = resp.getAttributes();
        if (attrs == null) {
            return new Course(resp.getId(), null, null, null, null, null);
        }

        Long categoryId = null;
        String categoryName = null;
        if (attrs.getCoursecategory() != null && attrs.getCoursecategory().getData() != null) {
            RelationData category = attrs.getCoursecategory().getData();
            categoryId = category.getId();
            if (category.getAttributes() != null) {
                categoryName = category.getAttributes().getName();
            }
        }

        return new Course(
            resp.getId(),
            attrs.getTitle(),
            attrs.getOrder(),
            resolveIconImageUrl(attrs.getIcon_image()),
            categoryId,
            categoryName
        );
    }

    private String resolveIconImageUrl(MediaWrapper iconImage) {
        if (iconImage == null || iconImage.getData() == null || iconImage.getData().getAttributes() == null) {
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
