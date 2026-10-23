package util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

/** Deduplicates exported bookmark links, retaining the first line for each URL. */
public class HTMLTagFilter {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) throw new IllegalArgumentException("Usage: HTMLTagFilter input-directory output-file");
        Pattern pattern = Pattern.compile("\\bHREF\\s*=\\s*[\"']([^\"']+)[\"']",Pattern.CASE_INSENSITIVE);
        Set<String> seen = new HashSet<>();
        List<String> result = new ArrayList<>();
        Path output = Paths.get(args[1]).toAbsolutePath().normalize();
        try (Stream<Path> files = Files.list(Paths.get(args[0]))) {
            Iterator<Path> paths = files.filter(Files::isRegularFile).sorted().iterator();
            while (paths.hasNext()) {
                Path path = paths.next();
                if (path.toAbsolutePath().normalize().equals(output)
                        || (Files.exists(output) && Files.isSameFile(path,output))) continue;
                try (BufferedReader reader = Files.newBufferedReader(path,StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Matcher match = pattern.matcher(line);
                        if (match.find() && seen.add(match.group(1))) result.add(line);
                    }
                }
            }
        }
        Files.write(output,result,StandardCharsets.UTF_8);
    }
}
