package com.nxp.utils.crypto;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class CryptoUtils {
    private static final String HEX = "0123456789ABCDEF";
    private static final String TRANSFORMATION = "AES/ECB/NoPadding";

    public static String encrypt(String seed, String cleartext)
            throws Exception {
        byte[] rawKey = getRawKey(seed.getBytes());
        byte[] result = encrypt(rawKey, cleartext.getBytes());
        return toHex(result);
    }

    public static String encryptHexString(String hexKey, String hexText) throws Exception {
        byte[] rawKey = toByte(hexKey);
        byte[] result = encrypt(rawKey, toByte(hexText));
        return toHex(result);
    }

    public static byte[] encryptByte(byte[] key, byte[] text) throws Exception {
        byte[] result = encrypt(key, text);
        return result;
    }

    public static String decrypt(String seed, String encrypted) throws Exception {
        byte[] rawKey = getRawKey(seed.getBytes());
        byte[] enc = toByte(encrypted);
        byte[] result = decrypt(rawKey, enc);
        return new String(result);
    }

    public static String decryptHexString(String hexKey, String hexText) throws Exception {
        byte[] rawKey = toByte(hexKey);
        byte[] result = decrypt(rawKey, toByte(hexText));
        return toHex(result);
    }

    public static byte[] decryptByte(byte[] key, byte[] text) throws Exception {
        byte[] result = decrypt(key, text);
        return result;
    }

    public static final String getAppSeed(Context context) {
        PackageInfo packageInfo = null;
        String appSeed = context.getPackageName();
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    64);
        } catch (NameNotFoundException localNameNotFoundException) {
        }

        if (packageInfo != null) {
            Signature[] arrayOfSignature;
            int j = (arrayOfSignature = packageInfo.signatures).length;
            for (int i = 0; i < j; i++) {
                Signature signature = arrayOfSignature[i];
                appSeed = appSeed + signature.toCharsString();
            }
        }
        return appSeed;
    }

    private static byte[] getRawKey(byte[] seed) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG", "Crypto");
        sr.setSeed(seed);
        kgen.init(128, sr);
        SecretKey skey = kgen.generateKey();
        byte[] raw = skey.getEncoded();
        return raw;
    }

    private static byte[] encrypt(byte[] raw, byte[] clear) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(1, skeySpec);
        byte[] encrypted = cipher.doFinal(clear);
        return encrypted;
    }

    private static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(2, skeySpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }

    public static String toHex(String txt) {
        return toHex(txt.getBytes());
    }

    public static String fromHex(String hex) {
        return new String(toByte(hex));
    }

    public static byte[] toByte(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2), 16).byteValue();
        }
        return result;
    }

    public static String toHex(byte[] buf) {
        if (buf == null)
            return "";
        StringBuffer result = new StringBuffer(2 * buf.length);
        for (int i = 0; i < buf.length; i++) {
            appendHex(result, buf[i]);
        }
        return result.toString();
    }

    private static void appendHex(StringBuffer sb, byte b) {
        sb.append("0123456789ABCDEF".charAt(b >> 4 & 0xF)).append("0123456789ABCDEF".charAt(b & 0xF));
    }
}
