package util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/** Compatibility facade sharing HttpsUtils TLS, timeout and cleanup behavior. */
public class HttpUtils {
    public enum Method { GET, POST }

    public static byte[] doUrl(String url, String param, String charset, Method method)
            throws KeyManagementException, NoSuchAlgorithmException, IOException {
        String encoding = charset == null || charset.isEmpty() ? "UTF-8" : charset;
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        if (param != null) body.write(param.getBytes(encoding));
        return HttpsUtils.doUrl(url, body, encoding, HttpsUtils.Method.valueOf(method.name()));
    }
    public static byte[] doUrl(String url, String param, Method method)
            throws KeyManagementException, NoSuchAlgorithmException, IOException {
        return doUrl(url, param, "UTF-8", method);
    }
    public static byte[] doUrl(String url, Map<String, String> params, Method method)
            throws KeyManagementException, NoSuchAlgorithmException, IOException {
        return doUrl(url, params, "UTF-8", method);
    }
    public static byte[] doUrl(String url, Map<String, String> params, String charset, Method method)
            throws KeyManagementException, NoSuchAlgorithmException, IOException {
        HttpsParam body = new HttpsParam();
        if (params != null) params.forEach(body::putParam);
        return HttpsUtils.doUrl(url, body, charset, HttpsUtils.Method.valueOf(method.name()));
    }
}
