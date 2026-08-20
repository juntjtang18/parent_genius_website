package ca.parentgeniusai.website.model;

import java.util.ArrayList;
import java.util.List;

public class Pillar {
    private Long id;
    private String name;
    private Integer order;
    private List<Course> courses = new ArrayList<>();

    public Pillar() {}

    public Pillar(Long id, String name, Integer order) {
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
    public List<Course> getCourses() { return courses; }
    public void setCourses(List<Course> courses) {
        this.courses = courses != null ? courses : new ArrayList<>();
    }
}
