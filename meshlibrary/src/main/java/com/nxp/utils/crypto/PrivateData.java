package com.nxp.utils.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class PrivateData {
    private static final int ITERATE_CNT = 10;
    private static final String CHARACTER_ENCODING = "UTF-8";
    private static final String ALGORITHM = "MD5";
    private static final String SOLT = "aaaabbbbcccc";
    public static final String MESHLIB_DATA = "MESHLIB_DATA";
    public static final String ENCRIPT_PROVISION_KEY = "ENCRIPT_PROVISION_KEY";

    public static String createProvisionKey(String input) {
        String hashValue = input;
        try {
            hashValue = getHashValue(input + SOLT, ITERATE_CNT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hashValue;
    }

    public static void saveProvisionKey(Context context, String rawProvisonKey) throws Exception {
        SharedPreferences sharedPreferences = context.getSharedPreferences(MESHLIB_DATA, Context.MODE_PRIVATE);
        String encryptProvisionKey = CryptoUtils.encrypt(CryptoUtils.getAppSeed(context), rawProvisonKey);
        sharedPreferences.edit().putString(ENCRIPT_PROVISION_KEY, encryptProvisionKey).commit();
    }

    public static void removeProvisionKey(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(MESHLIB_DATA, Context.MODE_PRIVATE);
        sharedPreferences.edit().remove(ENCRIPT_PROVISION_KEY).commit();
    }

    public static String getProvisionKey(Context context) throws Exception {
        SharedPreferences sharedPreferences = context.getSharedPreferences(MESHLIB_DATA, Context.MODE_PRIVATE);
        String encryptProvisionKey = sharedPreferences.getString(ENCRIPT_PROVISION_KEY, null);
        if (TextUtils.isEmpty(encryptProvisionKey)) {
            throw new RuntimeException("PROVISION_KEY doesn't exist!!");
        }
        return CryptoUtils.decrypt(CryptoUtils.getAppSeed(context), encryptProvisionKey);
    }

    public static boolean isProvisionKeyExist(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(MESHLIB_DATA, Context.MODE_PRIVATE);
        String encryptProvisionKey = sharedPreferences.getString(ENCRIPT_PROVISION_KEY, null);
        return !TextUtils.isEmpty(encryptProvisionKey);
    }

    public static String createUUID(String provisionKey) {
        int length = provisionKey.length();
        int value = 0;
        for (int i = 0; i < length; i += 4) {
            String str = provisionKey.substring(i, i + 4 < length ? i + 4 : length);
            int strIntc = Integer.valueOf(str, 16).intValue();
            value += strIntc;
        }
        return String.format("%04X", new Object[]{Integer.valueOf(value & 0xFFFF)});
    }

    private static String getHashValue(String input, int iterateCnt) throws EncryptException {
        if (input == null)
            return null;
        String hashValue = null;
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            byte[] res = input.getBytes(CHARACTER_ENCODING);
            for (int i = 0; i < iterateCnt; i++) {
                md.update(res);
                res = md.digest();
            }
            hashValue = new String(CryptoUtils.toHex(res));
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptException(e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            throw new EncryptException(e.getMessage(), e);
        }

        return hashValue;
    }

    public static class EncryptException extends Exception {
        private static final long serialVersionUID = 3211462413768830411L;

        public EncryptException(String detailMessage, Throwable throwable) {
            super(throwable);
        }
    }
}

