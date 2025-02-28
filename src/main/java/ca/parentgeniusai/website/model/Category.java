package ca.parentgeniusai.website.model;

import java.util.Objects;

public class Category {
    private String name;
    private int order;
    private String iconName;

    // Constructor
    public Category(String name, int order, String iconName) {
        this.name = name;
        this.order = order;
        this.iconName = iconName;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    // toString Method
    @Override
    public String toString() {
        return "Category{" +
                "name='" + name + '\'' +
                ", order=" + order +
                ", iconName='" + iconName + '\'' +
                '}';
    }

    // equals Method
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return order == category.order &&
                name.equals(category.name) &&
                iconName.equals(category.iconName);
    }

    // hashCode Method
    @Override
    public int hashCode() {
        return Objects.hash(name, order, iconName);
    }
}
