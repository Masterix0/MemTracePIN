import java.io.*;
import java.util.*;

public class TraceAnalyzer {

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println(
                    "Usage: java TraceAnalyzer <real_runtime_ms> <trace_runtime_ms> <trace_dir> <interval_window_ms>");
            return;
        }

        long realRuntime = Long.parseLong(args[0]);
        long traceRuntime = Long.parseLong(args[1]);
        String traceDir = args[2];
        long intervalWindowMs = Long.parseLong(args[3]);

        double slowdownFactor = (double) traceRuntime / realRuntime;
        long traceIntervalWindowMs = (long) (intervalWindowMs * slowdownFactor);

        // Print 'slowdownFactor' and 'traceIntervalWindowMs'
        System.out.println("slowdownFactor: " + slowdownFactor);
        System.out.println("traceIntervalWindowMs: " + traceIntervalWindowMs);

        // Print 'GOT HERE #'
        System.out.println("GOT HERE #1");

        try {
            List<File> traceFiles = getTraceFiles(traceDir);

            // Print file names
            for (File file : traceFiles) {
                System.out.println(file.getName());
            }

            long[] globalTimestamps = getGlobalTimestamps(traceFiles);
            long globalStartTimestamp = globalTimestamps[0];
            long globalEndTimestamp = globalTimestamps[1];

            // Print global timestamps
            System.out.println("Global start timestamp: " + globalStartTimestamp);
            System.out.println("Global end timestamp: " + globalEndTimestamp);

            // Find out how traceIntervalWindow in cpu cycles
            long traceIntervalWindowTimeStamps = (globalEndTimestamp - globalStartTimestamp) / traceRuntime
                    * traceIntervalWindowMs;

            // Print 'traceIntervalWindowTimeStamps'
            System.out.println("traceIntervalWindowTimeStamps: " + traceIntervalWindowTimeStamps);

            long currentIntervalStart = globalStartTimestamp;
            long currentIntervalEnd = globalStartTimestamp + traceIntervalWindowTimeStamps;

            System.out.println("GOT HERE #2");

            while (currentIntervalEnd <= globalEndTimestamp) {
                IntervalAnalyzer intervalAnalyzer = new IntervalAnalyzer(traceFiles, currentIntervalStart,
                        currentIntervalEnd);
                intervalAnalyzer.analyzeInterval();

                // Collect and compare hot pages
                List<PageStats> estimatedHotPages = intervalAnalyzer.getHotPagesByFirstAccess();
                List<PageStats> actualHotPages = intervalAnalyzer.getHotPagesByTotalAccess();

                // Compare rankings and calculate accuracy
                double accuracy = calculateAccuracy(estimatedHotPages, actualHotPages);
                System.out.println(
                        "Interval " + currentIntervalStart + " - " + currentIntervalEnd + ": Accuracy = " + accuracy);

                currentIntervalStart = currentIntervalEnd;
                currentIntervalEnd = currentIntervalStart + traceIntervalWindowTimeStamps;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<File> getTraceFiles(String dirPath) {
        // Print dirPath
        System.out.println("dirPath: " + dirPath);

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

            while ((line = reader.readLine()) != null && count < 4000) {
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

            for (int i = 0; i < 4000 && fileLength > 0; i++) {
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

    private static double calculateAccuracy(List<PageStats> estimated, List<PageStats> actual) {
        int matches = 0;
        for (PageStats estPage : estimated) {
            for (PageStats actPage : actual) {
                if (estPage.getPageId().equals(actPage.getPageId())) {
                    matches++;
                    break;
                }
            }
        }

        // Print matches and estimated size
        System.out.println("Matches: " + matches);
        System.out.println("Estimated size: " + estimated.size());

        return (double) matches / estimated.size();
    }
}
