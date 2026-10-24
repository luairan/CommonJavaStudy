package nioSample.nio.sample.filecopy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class CommonCopy implements Runnable {
    protected final String fromFile;
    protected final String toFile;
    /** Completion flag; inspect getFailure() to distinguish success from failure. */
    public volatile boolean flag;
    private volatile IOException failure;

    public CommonCopy(String fromFile, String toFile) {
        this.fromFile = java.util.Objects.requireNonNull(fromFile, "fromFile");
        this.toFile = java.util.Objects.requireNonNull(toFile, "toFile");
    }

    public IOException getFailure() { return failure; }

    @Override
    public final void run() {
        flag = false;
        failure = null;
        try {
            Path from = Paths.get(fromFile), to = Paths.get(toFile);
            if (from.toAbsolutePath().normalize().equals(to.toAbsolutePath().normalize())
                    || (Files.exists(to) && Files.isSameFile(from, to))) {
                throw new IOException("Source and destination are the same file");
            }
            copy(from, to);
        } catch (InvalidPathException e) {
            failure = new IOException("Invalid source or destination path", e);
        } catch (IOException e) {
            failure = e;
        } finally {
            flag = true;
        }
    }

    protected abstract void copy(Path from, Path to) throws IOException;
}
