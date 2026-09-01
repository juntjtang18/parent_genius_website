package ca.parentgeniusai.website.model;

import java.util.Collections;
import java.util.List;

public class CommunityPostPage {
    private List<CommunityPost> posts = Collections.emptyList();
    private int page = 1;
    private int pageSize = 50;
    private int pageCount = 1;
    private long total = 0;

    public static CommunityPostPage empty(int page, int pageSize) {
        CommunityPostPage result = new CommunityPostPage();
        result.page = Math.max(1, page);
        result.pageSize = pageSize;
        result.pageCount = 1;
        result.total = 0;
        result.posts = Collections.emptyList();
        return result;
    }

    public List<CommunityPost> getPosts() { return posts; }
    public void setPosts(List<CommunityPost> posts) { this.posts = posts; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
}
