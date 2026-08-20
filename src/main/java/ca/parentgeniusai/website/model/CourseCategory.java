package ca.parentgeniusai.website.model;

public class CourseCategory {
    private Long id;
    private String name;
    private Integer order;

    public CourseCategory() {}

    public CourseCategory(Long id, String name, Integer order) {
        this.id = id;
        this.name = name;
        this.order = order;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }
}
