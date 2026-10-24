import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Maven entry points for the two existing assertion-based regression groups. */
class RegressionSuiteTest {
    @Test
    @DisplayName("Core regressions: sorting, queues, TLS and NIO")
    @Timeout(value = 120, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void coreRegressions(@TempDir Path directory) throws Exception {
        Path keystore = directory.resolve("test.p12");
        Path log = directory.resolve("keytool.log");
        Process keytool = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair", "-alias", "test", "-keyalg", "RSA", "-keysize", "2048",
                "-validity", "2", "-dname", "CN=wrong.example", "-ext", "SAN=dns:wrong.example",
                "-storetype", "PKCS12", "-keystore", keystore.toString(),
                "-storepass", "changeit", "-keypass", "changeit")
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(keytool.waitFor(30, TimeUnit.SECONDS), "keytool timed out");
            assertEquals(0, keytool.exitValue(), () -> readLog(log));
        } finally {
            if (keytool.isAlive()) keytool.destroyForcibly().waitFor();
        }
        RegressionTest.main(new String[]{keystore.toString()});
    }

    @Test
    @DisplayName("Extended regressions: file integrity, utilities and network lifecycle")
    @Timeout(value = 180, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void extendedRegressions() throws Exception {
        MoreRegressionTest.main(new String[0]);
    }

    private static String readLog(Path log) {
        try { return Files.readString(log); }
        catch (java.io.IOException e) { return "Cannot read keytool log: " + e; }
    }
}
