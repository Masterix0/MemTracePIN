import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class IntervalAnalyzer {

    private List<File> traceFiles;
    private long intervalStart;
    private long intervalEnd;
    private Map<Long, PageStats> pageStatsMap;
    private Map<File, Long> filePositions;
    private int lineLength = -1;
    private long subIntervalDuration; // Duration of each sub-interval for PTS scoring

    public IntervalAnalyzer(List<File> traceFiles, long subIntervalDuration) {
        this.traceFiles = traceFiles;
        this.subIntervalDuration = subIntervalDuration;
        this.pageStatsMap = new ConcurrentHashMap<>();
        this.filePositions = new HashMap<>();

        // Initialize file positions to the start of each file
        for (File file : traceFiles) {
            filePositions.put(file, 0L);
        }

        // Get the length of the first line in the first file
        if (!traceFiles.isEmpty()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(traceFiles.get(0)))) {
                lineLength = reader.readLine().length() + 1; // +1 for the newline character
            } catch (IOException e) {
                e.printStackTrace();
            }
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
            long bytesToSkip = filePosition * lineLength;

            // To avoid bad math or lines with different lengths, we skip a bit less than
            // the calculated bytes
            // This is a trade-off between performance and accuracy
            bytesToSkip -= (bytesToSkip / 100);

            reader.skip(bytesToSkip);

            // Skip to the next line
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");

                long timestamp = Long.parseLong(parts[0], 16);

                if (timestamp > intervalEnd) {
                    break;
                }

                if (timestamp >= intervalStart) {
                    long address = Long.parseLong(parts[2], 16);
                    // We are considering 4KB pages, so we mask the lower 12 bits to get the page ID
                    long pageId = address >>> 12;

                    pageStatsMap.compute(pageId, (k, v) -> {
                        if (v == null) {
                            v = new PageStats(pageId);
                        }
                        v.incrementAccessCount();
                        if (timestamp < v.getFirstAccessTime()) {
                            v.setFirstAccessTime(timestamp);
                        }

                        // Calculate sub-interval index
                        int subIntervalIndex = (int) ((timestamp - intervalStart) / subIntervalDuration);
                        v.incrementPTSScore(subIntervalIndex);

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

    public List<PageStats> getHotPagesByPTSScore() {
        List<PageStats> hotPages = new ArrayList<>(pageStatsMap.values());
        hotPages.sort(Comparator.comparingLong(PageStats::getPTSScore).reversed());
        return hotPages;
    }

    // Get total number of accesses in the interval
    public long getTotalAccessCount() {
        return pageStatsMap.values().stream().mapToLong(PageStats::getAccessCount).sum();
    }

    // Get total pages accessed in the interval
    public long getTotalPageCount() {
        return pageStatsMap.size();
    }
}
