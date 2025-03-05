package ca.parentgeniusai.website.service;

import ca.parentgeniusai.website.model.Category;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${strapi.url}")
    private String STRAPI_ROOTURL;
    @Value("${strapi.auth-token}")
    private String AUTH_TOKEN;
    
    private static class ApiResponse {
        private List<CategoryResponse> data;
        private Meta meta;

        public List<CategoryResponse> getData() { return data; }
        public void setData(List<CategoryResponse> data) { this.data = data; }
        public Meta getMeta() { return meta; }
        public void setMeta(Meta meta) { this.meta = meta; }
    }

    private static class CategoryResponse {
        private Long id;
        private CategoryAttributes attributes;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public CategoryAttributes getAttributes() { return attributes; }
        public void setAttributes(CategoryAttributes attributes) { this.attributes = attributes; }
    }

    private static class CategoryAttributes {
        private String name;
        private Integer order;
        private String icon_name;
        private String createdAt;
        private String updatedAt;
        private String publishedAt;
        private String locale;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getOrder() { return order; }
        public void setOrder(Integer order) { this.order = order; }
        public String getIcon_name() { return icon_name; }
        public void setIcon_name(String icon_name) { this.icon_name = icon_name; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
        public String getPublishedAt() { return publishedAt; }
        public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
        public String getLocale() { return locale; }
        public void setLocale(String locale) { this.locale = locale; }
    }

    private static class Meta {
        private Pagination pagination;

        public Pagination getPagination() { return pagination; }
        public void setPagination(Pagination pagination) { this.pagination = pagination; }
    }

    private static class Pagination {
        private Integer page;
        private Integer pageSize;
        private Integer pageCount;
        private Integer total;

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
        public Integer getPageCount() { return pageCount; }
        public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }
    }

    public List<Category> getCategories() {
        System.out.println("getCategories() called.");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", AUTH_TOKEN);
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ApiResponse> response = restTemplate.exchange(
                STRAPI_ROOTURL + "categories?sort[0]=order:asc", // Sort by order ascending
                HttpMethod.GET,
                entity,
                ApiResponse.class
            );

            System.out.println("Raw response body: " + response.getBody());

            if (response.getBody() != null && response.getBody().getData() != null) {
                List<CategoryResponse> responses = response.getBody().getData();
                System.out.println("Categories data: " + responses);
                return responses.stream()
                    .map(resp -> new Category(
                        resp.getId(),
                        resp.getAttributes().getName(),
                        resp.getAttributes().getOrder(),
                        resp.getAttributes().getIcon_name()
                    ))
                    .collect(Collectors.toList());
            } else {
                System.out.println("No 'data' field found in the response.");
                return Collections.emptyList();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}