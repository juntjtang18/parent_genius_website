package ca.parentgeniusai.website.model;

public class Category {
    private Long id;
    private String name;
    private Integer order;
    private String iconName; // Original Strapi field
    private String iconClass; // Font Awesome class

    public Category(Long id, String name, Integer order, String iconName) {
        this.id = id;
        this.name = name;
        this.order = order;
        this.iconName = iconName;
        // Map iconName to Font Awesome class
        this.iconClass = mapIconNameToFontAwesome(iconName);
    }

    // Simple mapping method
    private String mapIconNameToFontAwesome(String iconName) {
        if (iconName == null) return "fa-solid fa-circle-question"; // Default icon
        return switch (iconName) {
            case "cat_expert_articles.jpg" -> "fa-solid fa-book";
            case "cat_faq.jpg" -> "fa-solid fa-question-circle";
            case "cat_experience_sharing.jpg" -> "fa-solid fa-users";
            case "cat_hot_post.jpg" -> "fa-solid fa-fire";
            default -> "fa-solid fa-circle-question"; // Fallback
        };
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }
    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }
    public String getIconClass() { return iconClass; }
    public void setIconClass(String iconClass) { this.iconClass = iconClass; }
}