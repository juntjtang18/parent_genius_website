package ca.parentgeniusai.website.service;

import ca.parentgeniusai.website.model.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FunctionService {
	private static final Logger logger = LoggerFactory.getLogger(FunctionService.class);
	private final RestTemplate restTemplate = new RestTemplate();

    @Value("${strapi.root.url}")
    private String STRAPI_ROOTURL;

    @Value("${strapi.auth-token}")
    private String AUTH_TOKEN;

    // Nested static class for Functions API response
    private static class FunctionApiResponse {
        private List<FunctionResponse> data;
        private Meta meta;

        public List<FunctionResponse> getData() { return data; }
        public void setData(List<FunctionResponse> data) { this.data = data; }
        public Meta getMeta() { return meta; }
        public void setMeta(Meta meta) { this.meta = meta; }
    }

    private static class FunctionResponse {
        private Long id;
        private FunctionAttributes attributes;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public FunctionAttributes getAttributes() { return attributes; }
        public void setAttributes(FunctionAttributes attributes) { this.attributes = attributes; }
    }

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

    // Nested static class for Articles API response
    private static class ArticleApiResponse {
        private List<ArticleResponse> data;

        public List<ArticleResponse> getData() { return data; }
        public void setData(List<ArticleResponse> data) { this.data = data; }
    }

    private static class ArticleResponse {
        private Long id;
        private ArticleAttributes attributes;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public ArticleAttributes getAttributes() { return attributes; }
        public void setAttributes(ArticleAttributes attributes) { this.attributes = attributes; }
    }

    private static class ArticleAttributes {
        private String title;
        private String content;
        private Boolean published;
        private String createdAt;
        private String updatedAt;
        private String publishedAt;
        private String locale;
        private String create_time;
        private Boolean always_on_top;
        private Integer like_count;
        private Integer visit_count;
        private Integer sortScore;
        private Map<String, Object> icon_image;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Boolean getPublished() { return published; }
        public void setPublished(Boolean published) { this.published = published; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
        public String getPublishedAt() { return publishedAt; }
        public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
        public String getLocale() { return locale; }
        public void setLocale(String locale) { this.locale = locale; }
        public String getCreate_time() { return create_time; }
        public void setCreate_time(String create_time) { this.create_time = create_time; }
        public Boolean getAlways_on_top() { return always_on_top; }
        public void setAlways_on_top(Boolean always_on_top) { this.always_on_top = always_on_top; }
        public Integer getLike_count() { return like_count; }
        public void setLike_count(Integer like_count) { this.like_count = like_count; }
        public Integer getVisit_count() { return visit_count; }
        public void setVisit_count(Integer visit_count) { this.visit_count = visit_count; }
        public Integer getSortScore() { return sortScore; }
        public void setSortScore(Integer sortScore) { this.sortScore = sortScore; }
        public Map<String, Object> getIcon_image() { return icon_image; }
        public void setIcon_image(Map<String, Object> icon_image) { this.icon_image = icon_image; }
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

    // Fetch only functions (for /function-article-list)
    @Cacheable("functions")
    public List<Function> getFunctions() {
        String functionsUrl = STRAPI_ROOTURL + "api/functions";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", AUTH_TOKEN);
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<FunctionApiResponse> funcResponse = restTemplate.exchange(
                functionsUrl, HttpMethod.GET, entity, FunctionApiResponse.class
            );

            if (funcResponse.getBody() == null || funcResponse.getBody().getData() == null) {
                System.out.println("No 'data' field found in the functions response.");
                return Collections.emptyList();
            }

            return funcResponse.getBody().getData().stream()
                .map(funcResp -> new Function(
                    funcResp.getId(),
                    funcResp.getAttributes().getName(),
                    funcResp.getAttributes().getOrder(),
                    funcResp.getAttributes().getIcon_name()
                ))
                .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // Fetch functions with articles (for /functions)
    @Cacheable("functionsWithArticles")
    public List<Function> getFunctionsWithArticles() {
        String functionsUrl = STRAPI_ROOTURL + "api/functions";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", AUTH_TOKEN);
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            logger.info("Fetching functions from {}", functionsUrl);
            ResponseEntity<FunctionApiResponse> funcResponse = restTemplate.exchange(
                functionsUrl, HttpMethod.GET, entity, FunctionApiResponse.class
            );

            if (funcResponse.getStatusCode() != HttpStatus.OK) {
                logger.error("Failed to fetch functions, status: {}", funcResponse.getStatusCode());
                return Collections.emptyList();
            }

            if (funcResponse.getBody() == null || funcResponse.getBody().getData() == null) {
                logger.warn("No 'data' field found in the functions response: {}", funcResponse.getBody());
                return Collections.emptyList();
            }

            List<FunctionResponse> functionResponses = funcResponse.getBody().getData();
            logger.info("Fetched {} functions", functionResponses.size());

            return functionResponses.stream().map(funcResp -> {
                Function func = new Function(
                    funcResp.getId(),
                    funcResp.getAttributes().getName(),
                    funcResp.getAttributes().getOrder(),
                    funcResp.getAttributes().getIcon_name()
                );

                String articlesUrl = STRAPI_ROOTURL + "api/articles?filters[functions][id][$in]=" + func.getId() +
                    "&pagination[pageSize]=5&sort[0]=sortScore:desc&populate=icon_image";
                try {
                    logger.info("Fetching articles for function {}: {}", func.getId(), articlesUrl);
                    ResponseEntity<ArticleApiResponse> articlesResponse = restTemplate.exchange(
                        articlesUrl, HttpMethod.GET, entity, ArticleApiResponse.class
                    );
                    if (articlesResponse.getBody() != null && articlesResponse.getBody().getData() != null) {
                        List<Function.Article> articles = articlesResponse.getBody().getData().stream()
                            .map(artResp -> {
                                String title = artResp.getAttributes().getTitle();
                                Long articleId = artResp.getId();
                                String iconImageUrl = null;
                                Map<String, Object> iconImage = artResp.getAttributes().getIcon_image();
                                if (iconImage != null && iconImage.containsKey("data") && iconImage.get("data") != null) {
                                    Map<String, Object> imageData = (Map<String, Object>) ((Map<String, Object>) iconImage.get("data")).get("attributes");
                                    String url = imageData.get("url").toString();
                                    iconImageUrl = (url.startsWith("http://") || url.startsWith("https://")) ? url : STRAPI_ROOTURL + url;
                                    logger.info("Set iconImageUrl for article {} to {}", articleId, iconImageUrl);
                                } else {
                                    logger.warn("No icon_image data for article {}", articleId);
                                }
                                return new Function.Article(articleId, title, null, iconImageUrl);
                            })
                            .collect(Collectors.toList());
                        func.setArticles(articles);
                        logger.info("Fetched {} articles for function {}", articles.size(), func.getId());
                    }
                } catch (Exception e) {
                    logger.error("Error fetching articles for function {}: {}", func.getId(), e.getMessage());
                }
                return func;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching functions: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @CacheEvict(value = "functionsWithArticles", allEntries = true)
    public void clearFunctionsWithArticlesCache() {
        logger.info("Cache 'functionsWithArticles' has been cleared.");
        // The method body can be empty; the annotation handles the cache eviction.
    }
}