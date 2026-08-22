package ca.parentgeniusai.website.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Course {
    private Long id;
    private String title;
    private Integer order;
    private String iconImageUrl;
    private Long courseCategoryId;
    private String courseCategoryName;
    private Long pillarId;
    private String pillarName;
    private Boolean published;
    private String keywords;
    private List<Map<String, Object>> content = new ArrayList<>();

    public Course() {}

    public Course(Long id, String title, Integer order, String iconImageUrl,
                  Long courseCategoryId, String courseCategoryName) {
        this(id, title, order, iconImageUrl, courseCategoryId, courseCategoryName, null, null);
    }

    public Course(Long id, String title, Integer order, String iconImageUrl,
                  Long courseCategoryId, String courseCategoryName,
                  Long pillarId, String pillarName) {
        this.id = id;
        this.title = title;
        this.order = order;
        this.iconImageUrl = iconImageUrl;
        this.courseCategoryId = courseCategoryId;
        this.courseCategoryName = courseCategoryName;
        this.pillarId = pillarId;
        this.pillarName = pillarName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }
    public String getIconImageUrl() { return iconImageUrl; }
    public void setIconImageUrl(String iconImageUrl) { this.iconImageUrl = iconImageUrl; }
    public Long getCourseCategoryId() { return courseCategoryId; }
    public void setCourseCategoryId(Long courseCategoryId) { this.courseCategoryId = courseCategoryId; }
    public String getCourseCategoryName() { return courseCategoryName; }
    public void setCourseCategoryName(String courseCategoryName) { this.courseCategoryName = courseCategoryName; }
    public Long getPillarId() { return pillarId; }
    public void setPillarId(Long pillarId) { this.pillarId = pillarId; }
    public String getPillarName() { return pillarName; }
    public void setPillarName(String pillarName) { this.pillarName = pillarName; }
    public Boolean getPublished() { return published; }
    public void setPublished(Boolean published) { this.published = published; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    /** Keywords split on ',' or ', ', with blank entries removed. */
    public List<String> getKeywordList() {
        return parseKeywords(keywords);
    }

    public static List<String> parseKeywords(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    public List<Map<String, Object>> getContent() { return content; }
    public void setContent(List<Map<String, Object>> content) {
        this.content = content != null ? content : new ArrayList<>();
    }
}
