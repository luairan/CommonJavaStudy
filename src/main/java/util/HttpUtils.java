package util;

import javax.net.ssl.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * 
 * 
 * 
 * 
 * http https 工具 code by canmo
 * 
 * 不适合大量调用
 * 
 */

public class HttpUtils {
	public static byte[] doUrl(String url, String param, String charSet,
			Method method) throws KeyManagementException,
			NoSuchAlgorithmException, IOException {
		boolean isHttps = url.startsWith("https");

		if (method == Method.GET && param != null) {
			url = url + "?" + param;
		}

		URL console = new URL(url);
		URLConnection conn = console.openConnection();
		if (isHttps) {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, new TrustManager[] { new TrustAnyTrustManager() },
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
			((HttpURLConnection) conn).setRequestMethod("POST");
		}
		conn.setRequestProperty("accept", "*/*");
		conn.setRequestProperty("connection", "Keep-Alive");
		if (charSet != null && !charSet.equals(""))
			conn.setRequestProperty("Charset", charSet);
		conn.connect();
		if (method == Method.POST) {
			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
			if (charSet != null && !charSet.equals(""))
				out.write(param.getBytes(charSet));
			else
				out.write(param.getBytes());
			// 刷新、关闭
			out.flush();
			out.close();
		}

		InputStream is = conn.getInputStream();
		if (is != null) {
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int len = 0;
			while ((len = is.read(buffer)) != -1) {
				outStream.write(buffer, 0, len);
			}
			is.close();
			return outStream.toByteArray();
		}
		return null;
	}

	public static byte[] doUrl(String url, String param, Method method)
			throws KeyManagementException, NoSuchAlgorithmException,
			IOException {
		return doUrl(url, param, null, method);
	}

	public static byte[] doUrl(String url, Map<String, String> para,
			Method method) throws KeyManagementException,
			NoSuchAlgorithmException, IOException {

		return doUrl(url, para, null, method);
	}

	public static byte[] doUrl(String url, Map<String, String> para,
			String charSet, Method method) throws KeyManagementException,
			NoSuchAlgorithmException, IOException {

		StringBuilder sb = new StringBuilder();
		String param = null;
		if (para != null && para.size() > 0) {
			for (Entry<String, String> en : para.entrySet()) {
				sb.append(URLEncoder.encode(en.getKey(),charSet));
				sb.append('=');
				sb.append(URLEncoder.encode(en.getValue(),charSet));
				sb.append('&');
			}
			sb.delete(sb.length() - 1, sb.length());
			param = sb.toString();
		}

		return doUrl(url, param, null, method);
	}

	private static class TrustAnyTrustManager implements X509TrustManager {

		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
		}

		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[] {};
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
//		 System.out.println(new
//		 String(HttpUtils.doUrl("https://www.google.com.hk", "dsasa" ,
//		 Method.GET)));
	}
}
