package util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpsParam {
    private final Map<String, String> paramMap = new LinkedHashMap<>();

    public void putParam(String key, String value) { paramMap.put(key, value); }

    @Override
    public String toString() {
        try {
            return toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    public String toString(String enc) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            if (result.length() > 0) result.append('&');
            result.append(URLEncoder.encode(entry.getKey(), enc)).append('=')
                    .append(URLEncoder.encode(entry.getValue(), enc));
        }
        return result.toString();
    }

    public ByteArrayOutputStream toByte(String enc) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(toString(enc).getBytes(enc));
        return out;
    }

    public ByteArrayOutputStream toByte() throws IOException { return toByte("UTF-8"); }
}
