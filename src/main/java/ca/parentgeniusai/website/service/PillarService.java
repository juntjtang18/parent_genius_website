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
import java.util.List;
import java.util.Map;
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
