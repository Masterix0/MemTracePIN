public class PageStats {

    private String pageId;
    private long accessCount;
    private long firstAccessTime;
    private long PTSScore;

    public PageStats(String pageId) {
        this.pageId = pageId;
        this.accessCount = 0;
        this.firstAccessTime = Long.MAX_VALUE;
    }

    public String getPageId() {
        return pageId;
    }

    public long getAccessCount() {
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

    public long getPTSScore() {
        return PTSScore;
    }

    public void incrementPTSScore() {
        this.PTSScore++;
    }
}
