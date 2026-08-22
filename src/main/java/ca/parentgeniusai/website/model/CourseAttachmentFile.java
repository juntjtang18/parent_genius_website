package ca.parentgeniusai.website.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class CourseAttachmentFile {
    private Long id;
    private String name;
    private String url;
    private String mime;

    public CourseAttachmentFile() {}

    public CourseAttachmentFile(Long id, String name, String url, String mime) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.mime = mime;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getMime() { return mime; }
    public void setMime(String mime) { this.mime = mime; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("url", url);
        map.put("mime", mime);
        return map;
    }
}
