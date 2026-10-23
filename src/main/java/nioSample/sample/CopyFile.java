package nioSample.sample;

import nioSample.nio.sample.filecopy.NIOFileCopy;

public class CopyFile {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: CopyFile source destination");
        NIOFileCopy copy = new NIOFileCopy(args[0], args[1]);
        copy.run();
        if (copy.getFailure() != null) throw copy.getFailure();
    }
}
