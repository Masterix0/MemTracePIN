import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class TraceAnalyzer {

    public static void main(String[] args) {
        if (args.length != 7) {
            System.out.println(
                    "Usage: java TraceAnalyzer <workload_name> <interval_window_ms> <real_runtime_ms> <trace_runtime_ms> <trace_dir> <dram_percentage> <sub_interval_duration_ms>");
            return;
        }

        String workloadName = args[0];
        long intervalWindowMs = Long.parseLong(args[1]);
        long realRuntime = Long.parseLong(args[2]);
        long traceRuntime = Long.parseLong(args[3]);
        String traceDir = args[4];
        double dramPercentage = Double.parseDouble(args[5]);
        float ptsIntervalDurationMs = Float.parseFloat(args[6]);

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

            // Create the output directory if it doesn't exist
            File outputDir = new File("output");
            if (!outputDir.exists()) {
                outputDir.mkdir();
            }

            // Create the output filename using the workload name and other parameters
            String outputFilename = String.format("output/%s-%d-%.2f-%.2f.csv",
                    workloadName, intervalWindowMs, ptsIntervalDurationMs, dramPercentage);

            // Initialize CSV writer
            BufferedWriter csvWriter = new BufferedWriter(new FileWriter(outputFilename));
            csvWriter.write(
                    "interval_start_timestamp,interval_end_timestamp,number_of_pages_accessed,total_access_count,actual_accesses_dram_hit_ratio,estimated_dram_hit_ratio,pts_dram_hit_ratio\n");

            // We only contemplate 'full' intervals, i.e., intervals that
            // start and end within the global trace timestamps
            IntervalAnalyzer intervalAnalyzer = new IntervalAnalyzer(traceFiles, tracePTSWindowTicks);
            while (currentIntervalEnd <= globalEndTimestamp) {
                intervalAnalyzer.analyzeInterval(currentIntervalStart, currentIntervalEnd);

                // Compare rankings and calculate accuracy
                HitRatioStats hitRatios = calculateAccuracy(intervalAnalyzer, dramPercentage);
                System.out.println(
                        "<Interval " + currentIntervalStart + " - " + currentIntervalEnd + ">");

                // Print number of pages accessed in interval
                long numberOfPagesAccessed = hitRatios.getNumPagesAccessed();
                System.out.println("Number of pages accessed: " + numberOfPagesAccessed);

                // If number of pages accessed is bigger than 0, print hit ratios rounded to 3
                // decimal places
                if (numberOfPagesAccessed > 0) {
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

                    // Write to CSV
                    long totalAccessCount = hitRatios.getNumAccesses();
                    csvWriter.write(currentIntervalStart + "," + currentIntervalEnd + "," + numberOfPagesAccessed + ","
                            + totalAccessCount + "," + hitRatios.getActualHitRatio() + ","
                            + hitRatios.getEstimatedHitRatio() + ","
                            + hitRatios.getPTSHitRatio() + "\n");
                } else {
                    // If no pages were accessed, don't print hit ratios and print a message
                    System.out.println("No pages accessed in this interval");
                }

                currentIntervalStart = currentIntervalEnd;
                currentIntervalEnd = currentIntervalStart + traceIntervalWindowTicks;
            }

            csvWriter.close();

            calculateOverallDRAMHitRatiosAndVariance(outputFilename);
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

    private static HitRatioStats calculateAccuracy(IntervalAnalyzer intervalAnalyzer, double dramPercentage) {

        // We look at the top DRAM percentage of pages
        int topN = (int) Math.ceil(intervalAnalyzer.getTotalPageCount() * dramPercentage);

        // Calculate total accesses of all pages
        long totalAccesses = intervalAnalyzer.getTotalAccessCount();

        // Collect and compare hot pages, then calculate total accesses of top N pages
        List<PageStats> topActual = intervalAnalyzer.getHotPagesByTotalAccess().subList(0, topN);
        long topActualAccesses = topActual.stream().mapToLong(PageStats::getAccessCount).sum();

        List<PageStats> topEstimated = intervalAnalyzer.getHotPagesByFirstAccess().subList(0, topN);
        long topEstimatedAccesses = topEstimated.stream().mapToLong(PageStats::getAccessCount).sum();

        List<PageStats> topPTS = intervalAnalyzer.getHotPagesByPTSScore().subList(0, topN);
        long topPTSAccesses = topPTS.stream().mapToLong(PageStats::getAccessCount).sum();

        // Calculate DRAM hit ratio of top N pages
        double hitRatioActual = (double) topActualAccesses / totalAccesses;
        double hitRatioEstimated = (double) topEstimatedAccesses / totalAccesses;
        double hitRatioPTS = (double) topPTSAccesses / totalAccesses;

        return new HitRatioStats(hitRatioActual, hitRatioEstimated, hitRatioPTS, intervalAnalyzer.getTotalPageCount(),
                totalAccesses);
    }

    private static void calculateOverallDRAMHitRatiosAndVariance(String csvFilePath) {
        long totalAccessCount = 0;
        double totalActualHits = 0;
        double totalEstimatedHits = 0;
        double totalPTSHits = 0;

        List<Double> actualHitRatios = new ArrayList<>();
        List<Double> estimatedHitRatios = new ArrayList<>();
        List<Double> ptsHitRatios = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            reader.readLine(); // Skip header

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                long intervalAccessCount = Long.parseLong(parts[3]);
                double actualHitRatio = Double.parseDouble(parts[4]);
                double estimatedHitRatio = Double.parseDouble(parts[5]);
                double ptsHitRatio = Double.parseDouble(parts[6]);

                totalAccessCount += intervalAccessCount;
                totalActualHits += intervalAccessCount * actualHitRatio;
                totalEstimatedHits += intervalAccessCount * estimatedHitRatio;
                totalPTSHits += intervalAccessCount * ptsHitRatio;

                actualHitRatios.add(actualHitRatio);
                estimatedHitRatios.add(estimatedHitRatio);
                ptsHitRatios.add(ptsHitRatio);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        double overallActualHitRatio = totalActualHits / totalAccessCount;
        double overallEstimatedHitRatio = totalEstimatedHits / totalAccessCount;
        double overallPTSHitRatio = totalPTSHits / totalAccessCount;

        double overallActualHitRatioRounded = BigDecimal.valueOf(overallActualHitRatio)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
        double overallEstimatedHitRatioRounded = BigDecimal.valueOf(overallEstimatedHitRatio)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
        double overallPTSHitRatioRounded = BigDecimal.valueOf(overallPTSHitRatio)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();

        double varianceActual = calculateVariance(actualHitRatios, overallActualHitRatio);
        double varianceEstimated = calculateVariance(estimatedHitRatios, overallEstimatedHitRatio);
        double variancePTS = calculateVariance(ptsHitRatios, overallPTSHitRatio);

        System.out.println("\n---------------------------------------------\n");
        System.out.println("Overall DRAM Hit Ratios:");
        System.out.println("Actual: " + overallActualHitRatioRounded + " (Variance: " + varianceActual + ")");
        System.out.println("Estimated: " + overallEstimatedHitRatioRounded + " (Variance: " + varianceEstimated + ")");
        System.out.println("PTS: " + overallPTSHitRatioRounded + " (Variance: " + variancePTS + ")");
    }

    private static double calculateVariance(List<Double> hitRatios, double mean) {
        double variance = 0;
        for (double hitRatio : hitRatios) {
            variance += Math.pow(hitRatio - mean, 2);
        }
        variance /= hitRatios.size();
        return BigDecimal.valueOf(variance)
                .setScale(6, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
