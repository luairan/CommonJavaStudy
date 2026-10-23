package nioSample.nio.sample.filecopy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;

public class MappedFileCopy extends CommonCopy {
    public MappedFileCopy(String fromFile, String toFile) { super(fromFile, toFile); }

    @Override
    protected void copy(Path from, Path to) throws IOException {
        try (FileChannel in = FileChannel.open(from, StandardOpenOption.READ);
             FileChannel out = FileChannel.open(to, StandardOpenOption.WRITE,
                     StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            long size = in.size();
            for (long offset = 0; offset < size;) {
                long length = Math.min(16L * 1024 * 1024, size - offset);
                ByteBuffer mapped = in.map(FileChannel.MapMode.READ_ONLY, offset, length);
                while (mapped.hasRemaining()) out.write(mapped);
                offset += length;
            }
        }
    }
}
