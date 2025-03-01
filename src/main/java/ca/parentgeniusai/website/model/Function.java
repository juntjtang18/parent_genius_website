package ca.parentgeniusai.website.model;

public class Function {
    private Long id;
    private String name;
    private Integer order;
    private String iconName;

    public Function(Long id, String name, Integer order, String iconName) {
        this.id = id;
        this.name = name;
        this.order = order;
        this.iconName = iconName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }
    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }
}