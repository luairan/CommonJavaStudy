package luairan.http.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/** Uses the JDK default certificate and hostname verification for HTTPS. */
public class HttpsUtils {
    public static byte[] doUrl(String url, ByteArrayOutputStream param, String charSet,
                               Method method) throws KeyManagementException,
            NoSuchAlgorithmException, IOException {
        return doUrl(url,param,charSet,method,10000,10000);
    }

    public static byte[] doUrl(String url, ByteArrayOutputStream param, String charSet,
                               Method method, int connectTimeoutMillis, int readTimeoutMillis)
            throws KeyManagementException, NoSuchAlgorithmException, IOException {
        if (connectTimeoutMillis <= 0 || readTimeoutMillis <= 0) {
            throw new IllegalArgumentException("Timeouts must be positive");
        }
        Objects.requireNonNull(method, "method");
        Charset charset = charSet == null || charSet.isEmpty()
                ? StandardCharsets.UTF_8 : Charset.forName(charSet);
        byte[] body = param == null ? new byte[0] : param.toByteArray();
        if (method == Method.GET && body.length > 0) {
            int hash = url.indexOf('#');
            String fragment = hash < 0 ? "" : url.substring(hash);
            String base = hash < 0 ? url : url.substring(0, hash);
            String separator = base.contains("?")
                    ? (base.endsWith("?") || base.endsWith("&") ? "" : "&") : "?";
            url = base + separator + new String(body, charset) + fragment;
        }
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setConnectTimeout(connectTimeoutMillis);
            conn.setReadTimeout(readTimeoutMillis);
            conn.setRequestMethod(method.name());
            conn.setRequestProperty("Accept", "*/*");
            if (method == Method.POST) {
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded; charset=" + charset.name());
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(body);
                }
            }
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = in.read(buffer)) != -1) out.write(buffer, 0, length);
                return out.toByteArray();
            }
        } finally {
            conn.disconnect();
        }
    }

    public static byte[] doUrl(String url, HttpsParam param, Method method)
            throws KeyManagementException, NoSuchAlgorithmException, IOException {
        return doUrl(url, param, "UTF-8", method);
    }

    public static byte[] doUrl(String url, HttpsParam param, String charSet, Method method)
            throws KeyManagementException, NoSuchAlgorithmException, IOException {
        String encoding = charSet == null || charSet.isEmpty() ? "UTF-8" : charSet;
        return doUrl(url, param == null ? null : param.toByte(encoding), encoding, method);
    }

    public enum Method { GET, POST }

    public static void main(String[] args) throws Exception {
        System.out.println(new String(doUrl("https://www.baidu.com/", null, Method.GET),
                StandardCharsets.UTF_8));
    }
}
