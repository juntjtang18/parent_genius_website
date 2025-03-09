package ca.parentgeniusai.website.model;

import java.util.ArrayList;
import java.util.List;

public class Function {
    private Long id;
    private String name;
    private Integer order;
    private String iconName;
    private List<Article> articles = new ArrayList<>();

    public Function() {}

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
    public List<Article> getArticles() { return articles; }
    public void setArticles(List<Article> articles) { this.articles = articles; }

    public static class Article {
        private Long id;
        private String title;
        private Long categoryId; // Not used here, but kept for consistency
        private String iconImageUrl; // Changed from categoryIcon to iconImageUrl

        public Article(Long id, String title, Long categoryId, String iconImageUrl) {
            this.id = id;
            this.title = title;
            this.categoryId = categoryId;
            this.iconImageUrl = iconImageUrl;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public Long getCategoryId() { return categoryId; }
        public String getIconImageUrl() { return iconImageUrl; }
    }
}