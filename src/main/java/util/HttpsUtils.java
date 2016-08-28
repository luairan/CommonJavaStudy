package util;

import javax.net.ssl.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * http https 工具 code by canmo
 * <p/>
 * 不使用apache httpclient 来处理。
 * <p/>
 * <p/>
 * 不用担心连接复用的问题
 */

public class HttpsUtils {
    public static byte[] doUrl(String url, ByteArrayOutputStream param, String charSet,
                               Method method) throws KeyManagementException,
            NoSuchAlgorithmException, IOException {
        System.setProperty("http.maxRedirects", "100");
        boolean isHttps = url.startsWith("https");
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        if (method == Method.GET && param != null) {
            url = url + "?" + new String(param.toByteArray());
        }

        URL console = new URL(url);

        HttpURLConnection conn = (HttpURLConnection) console.openConnection();


        if (isHttps) {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[]{new TrustAnyTrustManager()},
                    new java.security.SecureRandom());
            ((HttpsURLConnection) conn).setSSLSocketFactory(sc
                    .getSocketFactory());
            ((HttpsURLConnection) conn)
                    .setHostnameVerifier(new TrustAnyHostnameVerifier());
        }

        if (method == Method.POST) {
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
        }
        conn.setRequestProperty("accept", "*/*");
        conn.setRequestProperty("connection", "Keep-Alive");
        conn.setRequestProperty("Accept-Language", "zh-CN");
        conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
        if (charSet != null && !charSet.equals(""))
            conn.setRequestProperty("Content-Type", "text/plain; charset=" + charSet);

        conn.connect();

        if (method == Method.POST) {
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            out.write(param.toByteArray());
            // 刷新、关闭
            out.flush();
            out.close();
        }

        InputStream is = conn.getInputStream();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        if (is != null) {

            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                outStream.write(buffer, 0, len);
            }
            is.close();
        }
        conn.disconnect();
        return outStream.toByteArray();
    }


    public static byte[] doUrl(String url, HttpsParam param,
                               Method method) throws KeyManagementException,
            NoSuchAlgorithmException, IOException {
        if (param == null) param = new HttpsParam();
        return doUrl(url, param.toByte(), null, method);
    }

    public static byte[] doUrl(String url, HttpsParam param,
                               String charSet, Method method) throws KeyManagementException,
            NoSuchAlgorithmException, IOException {
        if (param == null) param = new HttpsParam();
        return doUrl(url, param.toByte(charSet), charSet, method);
    }

    private static class TrustAnyTrustManager implements X509TrustManager {

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
    }

    private static class TrustAnyHostnameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    public enum Method {
        GET, POST;
    }

    public static void main(String[] args) throws KeyManagementException,
            NoSuchAlgorithmException, IOException {
        byte[] s = HttpsUtils.doUrl("https://www.baidu.com/", null, Method.GET);
        System.out.println(new String(s, "utf-8"));
    }
}
