package sort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

final class SortInput {
    private SortInput() { }
    static int[] read(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Usage: <sort class> input-file");
        try (Stream<String> lines = Files.lines(Paths.get(args[0]))) {
            return lines.map(String::trim).filter(line -> !line.isEmpty())
                    .mapToInt(Integer::parseInt).toArray();
        }
    }
}
