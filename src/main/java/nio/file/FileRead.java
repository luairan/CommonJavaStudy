package nio.file;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class FileRead {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Usage: FileRead input-file");
        // A streaming reader keeps partial UTF-8 characters between channel reads.
        try (FileChannel channel = FileChannel.open(Paths.get(args[0]));
             Reader reader = new InputStreamReader(Channels.newInputStream(channel),StandardCharsets.UTF_8)) {
            char[] buffer = new char[48];
            int count;
            while ((count = reader.read(buffer)) != -1) System.out.print(new String(buffer,0,count));
        }
    }
}
