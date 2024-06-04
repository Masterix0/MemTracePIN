import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class IntervalAnalyzer {

    private List<File> traceFiles;
    private long intervalStart;
    private long intervalEnd;
    private Map<String, PageStats> pageStatsMap;

    public IntervalAnalyzer(List<File> traceFiles, long intervalStart, long intervalEnd) {
        this.traceFiles = traceFiles;
        this.intervalStart = intervalStart;
        this.intervalEnd = intervalEnd;
        this.pageStatsMap = new ConcurrentHashMap<>();
    }

    public void analyzeInterval() throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Void>> futures = new ArrayList<>();

        for (File file : traceFiles) {
            futures.add(executor.submit(() -> {
                analyzeFile(file);
                return null;
            }));
        }

        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
    }

    private void analyzeFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            long timestamp = Long.parseLong(parts[0], 16);
            if (timestamp < intervalStart)
                continue;
            if (timestamp > intervalEnd)
                break;

            String pageId = parts[2];

            pageStatsMap.compute(pageId, (k, v) -> {
                if (v == null) {
                    v = new PageStats(pageId);
                }
                v.incrementAccessCount();
                if (timestamp < v.getFirstAccessTime()) {
                    v.setFirstAccessTime(timestamp);
                }
                return v;
            });
        }

        reader.close();
    }

    public List<PageStats> getHotPagesByFirstAccess() {
        List<PageStats> hotPages = new ArrayList<>(pageStatsMap.values());
        hotPages.sort(Comparator.comparingLong(PageStats::getFirstAccessTime));
        return hotPages;
    }

    public List<PageStats> getHotPagesByTotalAccess() {
        List<PageStats> hotPages = new ArrayList<>(pageStatsMap.values());
        hotPages.sort(Comparator.comparingInt(PageStats::getAccessCount).reversed());
        return hotPages;
    }
}
