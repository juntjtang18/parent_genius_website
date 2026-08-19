package ca.parentgeniusai.website.model;

public class Course {
    private Long id;
    private String title;
    private Integer order;
    private String iconImageUrl;
    private Long courseCategoryId;
    private String courseCategoryName;

    public Course() {}

    public Course(Long id, String title, Integer order, String iconImageUrl,
                  Long courseCategoryId, String courseCategoryName) {
        this.id = id;
        this.title = title;
        this.order = order;
        this.iconImageUrl = iconImageUrl;
        this.courseCategoryId = courseCategoryId;
        this.courseCategoryName = courseCategoryName;
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
}
