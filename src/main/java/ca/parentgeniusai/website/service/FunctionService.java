package ca.parentgeniusai.website.service;

import ca.parentgeniusai.website.model.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FunctionService {
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${strapi.url}")
    private String STRAPI_ROOTURL;
    @Value("${strapi.auth-token}")
    private String AUTH_TOKEN;
    
    // Nested static class for the full API response
    private static class ApiResponse {
        private List<FunctionResponse> data;
        private Meta meta;

        public List<FunctionResponse> getData() { return data; }
        public void setData(List<FunctionResponse> data) { this.data = data; }
        public Meta getMeta() { return meta; }
        public void setMeta(Meta meta) { this.meta = meta; }
    }

    // Nested static class for each function item in "data"
    private static class FunctionResponse {
        private Long id;
        private FunctionAttributes attributes;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public FunctionAttributes getAttributes() { return attributes; }
        public void setAttributes(FunctionAttributes attributes) { this.attributes = attributes; }
    }

    // Nested static class for the "attributes" object
    private static class FunctionAttributes {
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

    // Nested static class for "meta"
    private static class Meta {
        private Pagination pagination;

        public Pagination getPagination() { return pagination; }
        public void setPagination(Pagination pagination) { this.pagination = pagination; }
    }

    // Nested static class for "pagination"
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

    // Method to fetch and map functions
    public List<Function> getFunctions() {
        System.out.println("getFunctions() called.");
        System.out.println("Auth_toke=" + AUTH_TOKEN);
        System.out.println("Strapi_rooturl=" + STRAPI_ROOTURL);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", AUTH_TOKEN);
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ApiResponse> response = restTemplate.exchange(
            		STRAPI_ROOTURL + "functions",
                HttpMethod.GET,
                entity,
                ApiResponse.class
            );

            System.out.println("Raw response body: " + response.getBody());

            if (response.getBody() != null && response.getBody().getData() != null) {
                List<FunctionResponse> responses = response.getBody().getData();
                System.out.println("Functions data: " + responses);
                // Map FunctionResponse to Function
                return responses.stream()
                    .map(resp -> new Function(
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