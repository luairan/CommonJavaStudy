package sort;

import java.io.*;
import java.nio.file.*;
import java.util.Random;

public class UseCaseTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("Usage: UseCaseTest output-file [count]");
        int count = args.length == 2 ? Integer.parseInt(args[1]) : 500000;
        if (count < 0) throw new IllegalArgumentException("count must be nonnegative");
        Random random = new Random();
        try (BufferedWriter out = Files.newBufferedWriter(Paths.get(args[0]))) {
            for (int i = 0; i < count; i++) { out.write(Integer.toString(random.nextInt(500000))); out.newLine(); }
        }
    }
}
