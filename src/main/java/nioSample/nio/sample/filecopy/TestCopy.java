package nioSample.nio.sample.filecopy;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class TestCopy {
    public static void main(String[] args) throws IOException, InterruptedException {
        InputStream config = args.length == 0
                ? TestCopy.class.getResourceAsStream("/nioSample/fileConfig.properties")
                : Files.newInputStream(Paths.get(args[0]));
        if (config == null) throw new FileNotFoundException("Missing copy configuration");
        List<CommonCopy> copies = new ArrayList<>();
        List<Thread> workers = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(config, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
                String[] paths = line.split(",", -1);
                if (paths.length != 2) throw new IOException("Expected source,destination: " + line);
                copies.add(new StreamFileCopyWithBuffer(paths[0].trim(), paths[1].trim()));
            }
        }
        long start = System.nanoTime();
        for (CommonCopy copy : copies) {
            Thread worker = new Thread(copy);
            workers.add(worker);
            worker.start();
        }
        for (Thread worker : workers) worker.join();
        for (CommonCopy copy : copies) if (copy.getFailure() != null) throw copy.getFailure();
        System.out.println("time used (ms): " + (System.nanoTime() - start) / 1000000);
    }
}
