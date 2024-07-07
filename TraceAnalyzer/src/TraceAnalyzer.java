import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class TraceAnalyzer {

    public static void main(String[] args) {
        if (args.length != 6) {
            System.out.println(
                    "Usage: java TraceAnalyzer <interval_window_ms> <real_runtime_ms> <trace_runtime_ms> <trace_dir> <dram_percentage> <sub_interval_duration_ms>");
            return;
        }

        long intervalWindowMs = Long.parseLong(args[0]);
        long realRuntime = Long.parseLong(args[1]);
        long traceRuntime = Long.parseLong(args[2]);
        String traceDir = args[3];
        double dramPercentage = Double.parseDouble(args[4]);
        float ptsIntervalDurationMs = Float.parseFloat(args[5]);

        if (dramPercentage <= 0 || dramPercentage > 1) {
            System.out.println("DRAM percentage must be between 0 and 1 (exclusive)");
            return;
        }

        double slowdownFactor = (double) traceRuntime / realRuntime;
        long traceIntervalWindowMs = (long) (intervalWindowMs * slowdownFactor);
        long tracePTSWindowMs = (long) (ptsIntervalDurationMs * slowdownFactor);

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

            // Convert PT scan time to ticks
            long tracePTSWindowTicks = (globalEndTimestamp - globalStartTimestamp) / traceRuntime
                    * tracePTSWindowMs;

            long currentIntervalStart = globalStartTimestamp;
            long currentIntervalEnd = globalStartTimestamp + traceIntervalWindowTicks;

            // We only contemplate 'full' intervals, i.e., intervals that
            // start and end within the global trace timestamps
            IntervalAnalyzer intervalAnalyzer = new IntervalAnalyzer(traceFiles, tracePTSWindowTicks);
            while (currentIntervalEnd <= globalEndTimestamp) {
                intervalAnalyzer.analyzeInterval(currentIntervalStart, currentIntervalEnd);

                // Collect and compare hot pages
                List<PageStats> estimatedHotPages = intervalAnalyzer.getHotPagesByFirstAccess();
                List<PageStats> actualHotPages = intervalAnalyzer.getHotPagesByTotalAccess();
                List<PageStats> ptsHotPages = intervalAnalyzer.getHotPagesByPTSScore();

                // Compare rankings and calculate accuracy
                HitRatioStats hitRatios = calculateAccuracy(actualHotPages, estimatedHotPages, ptsHotPages,
                        dramPercentage);
                System.out.println(
                        "<Interval " + currentIntervalStart + " - " + currentIntervalEnd + ">");

                // Print number of pages accessed in interval
                System.out.println("Number of pages accessed: " + actualHotPages.size());

                // If number of pages accessed is bigger than 0, print hit ratios rounded to 3
                // decimal places
                if (actualHotPages.size() > 0) {
                    double actualHitRatioRounded = BigDecimal.valueOf(hitRatios.getActualHitRatio())
                            .setScale(3, RoundingMode.HALF_UP)
                            .doubleValue();

                    double estimatedHitRatioRounded = BigDecimal.valueOf(hitRatios.getEstimatedHitRatio())
                            .setScale(3, RoundingMode.HALF_UP)
                            .doubleValue();

                    double ptsHitRatioRounded = BigDecimal.valueOf(hitRatios.getPTSHitRatio())
                            .setScale(3, RoundingMode.HALF_UP)
                            .doubleValue();

                    System.out.println("Actual hit ratio: " + actualHitRatioRounded);
                    System.out.println("Estimated hit ratio: " + estimatedHitRatioRounded);
                    System.out.println("PTS hit ratio: " + ptsHitRatioRounded);
                } else {
                    // If no pages were accessed, don't print hit ratios and print a message
                    System.out.println("No pages accessed in this interval");
                }

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

            // Read the first line
            if ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                long timestamp = Long.parseLong(parts[0], 16);
                if (timestamp < globalStartTimestamp)
                    globalStartTimestamp = timestamp;
            }

            reader.close();

            // Read the last line
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            long fileLength = raf.length() - 1;
            raf.seek(fileLength);

            // Move to the beginning of the last line
            while (fileLength > 0) {
                fileLength--;
                raf.seek(fileLength);
                if (raf.readByte() == '\n') {
                    break;
                }
            }

            String lastLine = raf.readLine();
            String[] parts = lastLine.split(",");
            long timestamp = Long.parseLong(parts[0], 16);
            if (timestamp > globalEndTimestamp)
                globalEndTimestamp = timestamp;

            raf.close();
        }

        return new long[] { globalStartTimestamp, globalEndTimestamp };
    }

    private static HitRatioStats calculateAccuracy(List<PageStats> actual, List<PageStats> estimated,
            List<PageStats> pts,
            double dramPercentage) {

        // We look at the top DRAM percentage of pages
        int topN = (int) Math.ceil(Math.min(estimated.size(), actual.size()) * dramPercentage);

        List<PageStats> topEstimated = estimated.subList(0, topN);
        List<PageStats> topActual = actual.subList(0, topN);
        List<PageStats> topPTS = pts.subList(0, topN);

        // Calculate total accesses of all pages
        long totalAccesses = actual.stream().mapToLong(PageStats::getAccessCount).sum();

        // Calculate total accesses of top N pages
        long topActualAccesses = topActual.stream().mapToLong(PageStats::getAccessCount).sum();
        long topEstimatedAccesses = topEstimated.stream().mapToLong(PageStats::getAccessCount).sum();
        long topPTSAccesses = topPTS.stream().mapToLong(PageStats::getAccessCount).sum();

        // Calculate DRAM hit ratio of top N pages
        double hitRatioActual = (double) topActualAccesses / totalAccesses;
        double hitRatioEstimated = (double) topEstimatedAccesses / totalAccesses;
        double hitRatioPTS = (double) topPTSAccesses / totalAccesses;

        return new HitRatioStats(hitRatioActual, hitRatioEstimated, hitRatioPTS);
    }
}
