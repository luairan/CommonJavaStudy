package nioSample.nio.sample.filecopy;

import java.io.*;
import java.nio.file.*;

public class StreamFileCopy extends CommonCopy {
    public StreamFileCopy(String fromFile, String toFile) { super(fromFile, toFile); }

    @Override
    protected void copy(Path from, Path to) throws IOException {
        try (InputStream in = Files.newInputStream(from);
             OutputStream out = Files.newOutputStream(to)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
        }
    }
}
