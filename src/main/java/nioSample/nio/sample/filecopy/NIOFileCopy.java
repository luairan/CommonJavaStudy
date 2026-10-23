package nioSample.nio.sample.filecopy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;

public class NIOFileCopy extends CommonCopy {
    public NIOFileCopy(String fromFile, String toFile) { super(fromFile, toFile); }

    @Override
    protected void copy(Path from, Path to) throws IOException {
        try (FileChannel in = FileChannel.open(from, StandardOpenOption.READ);
             FileChannel out = FileChannel.open(to, StandardOpenOption.WRITE,
                     StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            while (in.read(buffer) != -1) {
                buffer.flip();
                while (buffer.hasRemaining()) out.write(buffer);
                buffer.clear();
            }
        }
    }
}
