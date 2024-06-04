public class PageStats {

    private String pageId;
    private int accessCount;
    private long firstAccessTime;

    public PageStats(String pageId) {
        this.pageId = pageId;
        this.accessCount = 0;
        this.firstAccessTime = Long.MAX_VALUE;
    }

    public String getPageId() {
        return pageId;
    }

    public int getAccessCount() {
        return accessCount;
    }

    public void incrementAccessCount() {
        this.accessCount++;
    }

    public long getFirstAccessTime() {
        return firstAccessTime;
    }

    public void setFirstAccessTime(long firstAccessTime) {
        this.firstAccessTime = firstAccessTime;
    }
}
