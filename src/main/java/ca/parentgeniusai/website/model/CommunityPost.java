package ca.parentgeniusai.website.model;

public class CommunityPost {
    private Long id;
    private String authorName;
    private String lead;
    private String body;

    public CommunityPost() {}

    public CommunityPost(Long id, String authorName, String lead, String body) {
        this.id = id;
        this.authorName = authorName;
        this.lead = lead;
        this.body = body;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getLead() { return lead; }
    public void setLead(String lead) { this.lead = lead; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
