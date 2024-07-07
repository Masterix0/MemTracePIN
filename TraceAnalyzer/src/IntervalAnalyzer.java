import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class IntervalAnalyzer {

    private List<File> traceFiles;
    private long intervalStart;
    private long intervalEnd;
    private Map<String, PageStats> pageStatsMap;
    private Map<File, Long> filePositions;

    public IntervalAnalyzer(List<File> traceFiles) {
        this.traceFiles = traceFiles;
        this.pageStatsMap = new ConcurrentHashMap<>();
        this.filePositions = new HashMap<>();

        // Initialize file positions to the start of each file
        for (File file : traceFiles) {
            filePositions.put(file, 0L);
        }
    }

    public void analyzeInterval(long intervalStart, long intervalEnd) throws IOException {
        this.intervalStart = intervalStart;
        this.intervalEnd = intervalEnd;
        this.pageStatsMap.clear();

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
        long filePosition = filePositions.get(file);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // Skip lines until reaching the last position
            for (long i = 0; i < filePosition; i++) {
                reader.readLine();
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                long timestamp = Long.parseLong(parts[0], 16);

                if (timestamp > intervalEnd) {
                    break;
                }

                if (timestamp >= intervalStart) {
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

                filePosition++;
            }

            filePositions.put(file, filePosition);
        }
    }

    public List<PageStats> getHotPagesByFirstAccess() {
        List<PageStats> hotPages = new ArrayList<>(pageStatsMap.values());
        hotPages.sort(Comparator.comparingLong(PageStats::getFirstAccessTime));
        return hotPages;
    }

    public List<PageStats> getHotPagesByTotalAccess() {
        List<PageStats> hotPages = new ArrayList<>(pageStatsMap.values());
        hotPages.sort(Comparator.comparingLong(PageStats::getAccessCount).reversed());
        return hotPages;
    }
}
