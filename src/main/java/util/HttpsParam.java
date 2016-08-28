package util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class HttpsParam {
    private Map<String, String> paramMap = new HashMap<String, String>();

    public void putParam(String key, String value) {
        paramMap.put(key, value);
    }

    @SuppressWarnings("deprecation")
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : paramMap.entrySet()) {
            sb.append(URLEncoder.encode(entry.getKey()));
            sb.append('=');
            sb.append(URLEncoder.encode(entry.getValue()));
            sb.append('&');
        }
        sb.delete(sb.length() - 1, sb.length());
        return sb.toString();
    }


    public String toString(String enc) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : paramMap.entrySet()) {
            sb.append(URLEncoder.encode(entry.getKey(), enc));
            sb.append('=');
            sb.append(URLEncoder.encode(entry.getValue(), enc));
            sb.append('&');
        }
        sb.delete(sb.length() - 1, sb.length());
        return sb.toString();
    }

    public ByteArrayOutputStream toByte(String enc) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (Entry<String, String> entry : paramMap.entrySet()) {
            if (bos.size() > 0) bos.write("&".getBytes(enc));
            bos.write(URLEncoder.encode(entry.getKey(), enc).getBytes());
            bos.write("=".getBytes(enc));
            bos.write(URLEncoder.encode(entry.getValue(), enc).getBytes());
        }
        return bos;
    }

    @SuppressWarnings("deprecation")
    public ByteArrayOutputStream toByte() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (Entry<String, String> entry : paramMap.entrySet()) {
            if (bos.size() > 0) bos.write("&".getBytes());
            bos.write(URLEncoder.encode(entry.getKey()).getBytes());
            bos.write("=".getBytes());
            bos.write(URLEncoder.encode(entry.getValue()).getBytes());
        }
        return bos;
    }


    public static void main(String[] args) throws IOException {
        HttpsParam p = new HttpsParam();
        p.putParam("id", "&luairan");
        p.putParam("user", "&l卢爱ran");

        System.out.println(p.toString("UTF-8"));
        System.out.println(p.toByte());
    }
}
