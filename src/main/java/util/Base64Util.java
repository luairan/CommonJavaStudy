package util;

import org.apache.commons.codec.binary.Base64;

/**
 * @author airan.lar
 */
public class Base64Util {


    /**
     * @param bytes
     * @return
     */
    public static byte[] decode(String data) {
        return Base64.decodeBase64(data);
    }

    /**
     * 二进制数据编码为BASE64字符串
     *
     * @param bytes
     * @return
     * @throws Exception
     */
    public static String encode(final byte[] bytes) {
        return new String(Base64.encodeBase64(bytes));
    }

}
