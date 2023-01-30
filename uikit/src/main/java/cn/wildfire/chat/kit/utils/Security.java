package cn.wildfire.chat.kit.utils;

import android.text.TextUtils;
import android.util.Base64;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Security {

    private final static String AES_IV = getAesIv();// AES密匙偏移量
    private final static Charset charSet = Charset.forName("UTF-8");
    //    public final static String LIVE_KEY = getLiveKey();
    private final static String CipherStr = getCipher();


    public static String decrypt(String text) {
        if (TextUtils.isEmpty(text)) return null;
        return decrypt(text, getKey());
    }

    // AES解密
    private static String decrypt(String sSrc, String sKey) {
        try {
            // 判断Key是否正确
            if (sKey == null) {
                return null;
            }
            // 判断Key是否为16位
            if (sKey.length() != 16) {
                return null;
            }
            byte[] raw = sKey.getBytes(charSet);
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance(CipherStr);
            IvParameterSpec iv = new IvParameterSpec(AES_IV.getBytes(charSet));
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] encrypted1 = Base64.decode(sSrc.getBytes(charSet), Base64.NO_WRAP);// 先用bAES64解密
            try {
                byte[] original = cipher.doFinal(encrypted1);
                String originalString = new String(original, charSet);
                return originalString;
            } catch (Exception e) {
                System.out.println(e.toString());
                return null;
            }
        } catch (Exception ex) {
            System.out.println(ex.toString());
            return null;
        }
    }

    public static String encrypt(String text) {
        return encrypt(text, getKey());
    }

    // AES加密
    private static String encrypt(String sSrc, String sKey) {
        if (sKey == null || sSrc == null) {
            return null;
        }
        // 判断Key是否为16位
        if (sKey.length() != 16) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(CipherStr);// "算法/模式/补码方式"
            byte[] raw = sKey.getBytes(charSet);
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            IvParameterSpec iv = new IvParameterSpec(AES_IV.getBytes(charSet));// 使用CBC模式，需要一个向量iv，可增加加密算法的强度
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            byte[] encrypted = cipher.doFinal(sSrc.getBytes(charSet));
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * AES密匙偏移量
     *
     * @return
     */
    private static String getAesIv() {
        String c = "77563114?;=:;=90";//"0102030405060708"
        return new String(encode(c));
    }

    private static String getLiveKey() {
        String c = "JAEcbod!q>=9?>89";//"MG@game!~0054411"
        return new String(encode(c));
    }

    /**
     * 算法/模式/补码方式
     *
     * @return
     */
    private static String getCipher() {
        String c = "FCV+@@B/_EN_>Zhls{s";//"AES/CBC/PKCS5Padding"
        return new String(encode(c));
    }

    private static String getKey() {
        String c = "630<5498=>45:?00";//1558668820991598
        return new String(encode(c));
    }

    private static char[] encode(String text) {
        char[] charArray = text.toCharArray();
        int count = charArray.length;
        for (int i = 0; i < count; i++) {
            charArray[i] = (char) (charArray[i] ^ i ^ 7);
        }
        return charArray;
    }

    /**
     * sha 256 hash
     *
     * @param text
     * @return
     */
    public static String getSha256(String text) {
        String result = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            digest.update(text.getBytes("utf8"));
            result = String.format("%064x", new BigInteger(1, digest.digest()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
