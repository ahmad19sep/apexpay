package com.apexpay;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class BiometricHelper {

    private static final String KEY_ALIAS = "apexpay_biometric_key";
    private static final String KEYSTORE   = "AndroidKeyStore";

    public static Cipher getCipherForEncrypt() throws Exception {
        generateKeyIfAbsent();
        KeyStore ks = KeyStore.getInstance(KEYSTORE);
        ks.load(null);
        SecretKey key = (SecretKey) ks.getKey(KEY_ALIAS, null);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher;
    }

    public static Cipher getCipherForDecrypt(byte[] iv) throws Exception {
        KeyStore ks = KeyStore.getInstance(KEYSTORE);
        ks.load(null);
        SecretKey key = (SecretKey) ks.getKey(KEY_ALIAS, null);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher;
    }

    public static String encryptToBase64(Cipher cipher, String plaintext) throws Exception {
        byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));
        return Base64.encodeToString(encrypted, Base64.NO_WRAP);
    }

    public static String decryptFromBase64(Cipher cipher, String base64) throws Exception {
        byte[] decoded = Base64.decode(base64, Base64.NO_WRAP);
        return new String(cipher.doFinal(decoded), "UTF-8");
    }

    public static String ivToBase64(byte[] iv) {
        return Base64.encodeToString(iv, Base64.NO_WRAP);
    }

    public static byte[] ivFromBase64(String base64) {
        return Base64.decode(base64, Base64.NO_WRAP);
    }

    public static void deleteKey() {
        try {
            KeyStore ks = KeyStore.getInstance(KEYSTORE);
            ks.load(null);
            if (ks.containsAlias(KEY_ALIAS)) {
                ks.deleteEntry(KEY_ALIAS);
            }
        } catch (Exception ignored) {}
    }

    private static void generateKeyIfAbsent() throws Exception {
        KeyStore ks = KeyStore.getInstance(KEYSTORE);
        ks.load(null);
        if (ks.containsAlias(KEY_ALIAS)) return;

        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        kg.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build());
        kg.generateKey();
    }
}
