import java.io.*;
import java.util.*;

public class TraceAnalyzer {

    // PIN tool uses per-thread buffer to store traces before dumping to file
    // We use this constant to make sure we minimize missing traces if dumping
    // wasn't sequential. Buffer is 8KB by default and each line is 27 bytes.
    // So 8K / 27 ~= 300 lines.
    public static final int BUF_LINES = 700;

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println(
                    "Usage: java TraceAnalyzer <interval_window_ms> <real_runtime_ms> <trace_runtime_ms> <trace_dir>");
            return;
        }

        long intervalWindowMs = Long.parseLong(args[0]);
        long realRuntime = Long.parseLong(args[1]);
        long traceRuntime = Long.parseLong(args[2]);
        String traceDir = args[3];

        double slowdownFactor = (double) traceRuntime / realRuntime;
        long traceIntervalWindowMs = (long) (intervalWindowMs * slowdownFactor);

        // Print slowdown factor
        System.out.println("Slowdown factor: " + slowdownFactor);

        try {
            List<File> traceFiles = getTraceFiles(traceDir);
            long[] globalTimestamps = getGlobalTimestamps(traceFiles);
            long globalStartTimestamp = globalTimestamps[0];
            long globalEndTimestamp = globalTimestamps[1];

            // Print global timestamps
            System.out.println("Global start timestamp: " + globalStartTimestamp);
            System.out.println("Global end timestamp: " + globalEndTimestamp);

            // Convert interval in ms to interval in ticks used in timestamps
            long traceIntervalWindowTicks = (globalEndTimestamp - globalStartTimestamp) / traceRuntime
                    * traceIntervalWindowMs;

            long currentIntervalStart = globalStartTimestamp;
            long currentIntervalEnd = globalStartTimestamp + traceIntervalWindowTicks;

            // We only contemplate 'full' intervals, i.e., intervals that
            // start and end within the global trace timestamps
            while (currentIntervalEnd <= globalEndTimestamp) {
                IntervalAnalyzer intervalAnalyzer = new IntervalAnalyzer(traceFiles, currentIntervalStart,
                        currentIntervalEnd);
                intervalAnalyzer.analyzeInterval();

                // Collect and compare hot pages
                List<PageStats> estimatedHotPages = intervalAnalyzer.getHotPagesByFirstAccess();
                List<PageStats> actualHotPages = intervalAnalyzer.getHotPagesByTotalAccess();

                // Compare rankings and calculate accuracy
                HitRatioStats hitRatios = calculateAccuracy(estimatedHotPages, actualHotPages);
                System.out.println(
                        "<Interval " + currentIntervalStart + " - " + currentIntervalEnd + ">");
                System.out.println("Actual hit ratio: " + hitRatios.getActualHitRatio());
                System.out.println("Estimated hit ratio: " + hitRatios.getEstimatedHitRatio());

                currentIntervalStart = currentIntervalEnd;
                currentIntervalEnd = currentIntervalStart + traceIntervalWindowTicks;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<File> getTraceFiles(String dirPath) {

        File dir = new File(dirPath);
        File[] files = dir.listFiles();

        return Arrays.asList(files);
    }

    private static long[] getGlobalTimestamps(List<File> traceFiles) throws IOException {
        long globalStartTimestamp = Long.MAX_VALUE;
        long globalEndTimestamp = Long.MIN_VALUE;

        for (File file : traceFiles) {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            int count = 0;

            while ((line = reader.readLine()) != null && count < BUF_LINES) {
                String[] parts = line.split(",");
                long timestamp = Long.parseLong(parts[0], 16);
                if (timestamp < globalStartTimestamp)
                    globalStartTimestamp = timestamp;
                if (timestamp > globalEndTimestamp)
                    globalEndTimestamp = timestamp;
                count++;
            }

            reader.close();

            RandomAccessFile raf = new RandomAccessFile(file, "r");
            long fileLength = raf.length() - 1;
            raf.seek(fileLength);

            for (int i = 0; i < BUF_LINES && fileLength > 0; i++) {
                raf.seek(--fileLength);
                if (raf.readByte() == '\n') {
                    String lastLine = raf.readLine();
                    String[] parts = lastLine.split(",");
                    long timestamp = Long.parseLong(parts[0], 16);
                    if (timestamp < globalStartTimestamp)
                        globalStartTimestamp = timestamp;
                    if (timestamp > globalEndTimestamp)
                        globalEndTimestamp = timestamp;
                }
            }

            raf.close();
        }

        return new long[] { globalStartTimestamp, globalEndTimestamp };
    }

    private static HitRatioStats calculateAccuracy(List<PageStats> estimated, List<PageStats> actual) {

        // We look at top half of pages
        // TODO: double check one third is good
        int topN = Math.min(estimated.size(), actual.size()) / 2;

        List<PageStats> topEstimated = estimated.subList(0, topN);
        List<PageStats> topActual = actual.subList(0, topN);

        // Calculate total accesses of all pages
        long totalAccesses = actual.stream().mapToLong(PageStats::getAccessCount).sum();

        // Calculate total accesses of top N pages
        long topActualAccesses = topActual.stream().mapToLong(PageStats::getAccessCount).sum();
        long topEstimatedAccesses = topEstimated.stream().mapToLong(PageStats::getAccessCount).sum();

        // Calculate DRAM hit ratio of top N pages
        double hitRatioActual = (double) topActualAccesses / totalAccesses;
        double hitRatioEstimated = (double) topEstimatedAccesses / totalAccesses;

        return new HitRatioStats(hitRatioActual, hitRatioEstimated);
    }
}
