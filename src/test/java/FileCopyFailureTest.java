import nioSample.nio.sample.filecopy.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class FileCopyFailureTest {
    static Stream<BiFunction<String, String, CommonCopy>> implementations() {
        return Stream.of(StreamFileCopy::new, StreamFileCopyWithBuffer::new,
                NIOFileCopy::new, MappedFileCopy::new);
    }

    @ParameterizedTest @MethodSource("implementations")
    void invalidSourceIsReported(BiFunction<String, String, CommonCopy> factory,
                                 @TempDir Path directory) throws Exception {
        Path destination = directory.resolve("existing.txt");
        Files.writeString(destination, "keep destination");
        checkFailure(factory.apply("invalid\0source", destination.toString()));
        assertEquals("keep destination", Files.readString(destination));
    }

    @ParameterizedTest @MethodSource("implementations")
    void invalidDestinationIsReported(BiFunction<String, String, CommonCopy> factory,
                                      @TempDir Path directory) throws Exception {
        Path source = directory.resolve("source.txt");
        Files.writeString(source, "keep source");
        checkFailure(factory.apply(source.toString(), "invalid\0destination"));
        assertEquals("keep source", Files.readString(source));
    }

    @ParameterizedTest @MethodSource("implementations")
    void nullPathsFailBeforeTaskStarts(BiFunction<String, String, CommonCopy> factory) {
        assertThrows(NullPointerException.class, () -> factory.apply(null, "destination"));
        assertThrows(NullPointerException.class, () -> factory.apply("source", null));
    }

    private void checkFailure(CommonCopy copy) throws Exception {
        AtomicReference<Throwable> uncaught = new AtomicReference<>();
        Thread thread = new Thread(copy);
        thread.setUncaughtExceptionHandler((worker, error) -> uncaught.set(error));
        thread.start();
        thread.join(5000);
        assertFalse(thread.isAlive(), "copy did not finish");
        assertTrue(copy.flag, "completion must be published");
        assertNotNull(copy.getFailure(), "failed copy must never look successful");
        assertInstanceOf(InvalidPathException.class, copy.getFailure().getCause());
        assertNull(uncaught.get(), "failure should be available through getFailure()");
    }
}
