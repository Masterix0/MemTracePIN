import java.util.HashSet;
import java.util.Set;

public class PageStats {

    private String pageId;
    private long accessCount;
    private long firstAccessTime;
    private long PTSScore;

    // Set that contains indexes of subintervals in which the page was accessed
    private Set<Integer> subintervalIndexes;

    public PageStats(String pageId) {
        this.pageId = pageId;
        this.accessCount = 0;
        this.firstAccessTime = Long.MAX_VALUE;
        this.PTSScore = 0;
        this.subintervalIndexes = new HashSet<>();
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

    public void incrementPTSScore(int subintervalIndex) {
        // If subinterval access not already counted, increment PTSScore
        if (!subintervalIndexes.contains(subintervalIndex)) {
            this.PTSScore++;
            subintervalIndexes.add(subintervalIndex);
        }
    }

    public Set<Integer> getSubintervalIndexes() {
        return subintervalIndexes;
    }

    public void addSubintervalIndex(int subintervalIndex) {
        this.subintervalIndexes.add(subintervalIndex);
    }

    public void clearSubintervalIndexes() {
        this.subintervalIndexes.clear();
    }

    public boolean subintervalIndexesContain(int subintervalIndex) {
        return this.subintervalIndexes.contains(subintervalIndex);
    }
}
