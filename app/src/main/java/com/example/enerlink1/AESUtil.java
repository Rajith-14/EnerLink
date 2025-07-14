package com.example.enerlink1;

import android.util.Base64;

import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESUtil {

    private static final String AES = "AES";
    private static final String AES_MODE = "AES/CBC/PKCS5Padding";
    private static final String IV = "0123456789abcdef"; // 16-byte IV (can be random too)

    // Replace this key with a shared secret key (can be exchanged securely)
    private static final String SECRET_KEY = "mySuperSecretKey"; // 16 bytes = 128-bit key

    private static SecretKeySpec getKeySpec() {
        return new SecretKeySpec(SECRET_KEY.getBytes(), AES);
    }

    public static String encrypt(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_MODE);
        IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, getKeySpec(), ivSpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        return Base64.encodeToString(encrypted, Base64.DEFAULT);
    }

    public static String decrypt(String cipherText) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_MODE);
        IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes());
        cipher.init(Cipher.DECRYPT_MODE, getKeySpec(), ivSpec);
        byte[] decoded = Base64.decode(cipherText, Base64.DEFAULT);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted);
    }
}

