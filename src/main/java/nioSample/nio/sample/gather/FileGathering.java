package nioSample.nio.sample.gather;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;

/** Demonstrates gathering writes without truncating either input to its first block. */
public class FileGathering {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) throw new IllegalArgumentException("Usage: FileGathering first second output");
        Path first = Paths.get(args[0]), second = Paths.get(args[1]), output = Paths.get(args[2]);
        if (Files.exists(output) && (Files.isSameFile(first, output) || Files.isSameFile(second, output))) {
            throw new IOException("Output must differ from both input files");
        }
        try (FileChannel a = FileChannel.open(first);
             FileChannel b = FileChannel.open(second);
             FileChannel out = FileChannel.open(output, StandardOpenOption.CREATE,
                     StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            // Complete each file before the next, preserving concatenation order.
            for (FileChannel in : new FileChannel[]{a, b}) {
                ByteBuffer[] buffers = {ByteBuffer.allocate(8192), ByteBuffer.allocate(8192)};
                long count;
                while ((count = in.read(buffers)) != -1) {
                    if (count == 0) continue;
                    for (ByteBuffer buffer : buffers) buffer.flip();
                    while (buffers[0].hasRemaining() || buffers[1].hasRemaining()) out.write(buffers);
                    for (ByteBuffer buffer : buffers) buffer.clear();
                }
            }
        }
    }
}
