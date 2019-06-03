package ru.snek;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

public class Generator {
    private final static String CHARS_L = "abcdefghijklmnopqrstuvwxyz";
    private final static String CHARS_U = CHARS_L.toUpperCase();
    private final static String NUMBERS = "0123456789";
    private final static String CHAR_LIST = CHARS_L+CHARS_U+NUMBERS;
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String randomString(int length) {
        if(length < 1) return null;
        StringBuilder strb = new StringBuilder();
        for(int i = 0; i < length; ++i) {
            strb.append(CHAR_LIST.charAt(new Random().nextInt(CHAR_LIST.length())));
        }
        return strb.toString();
    }

    public static String hashPassword(String password, String salt) {
        char[] passwordArr = password.toCharArray();
        byte[] saltArr = salt.getBytes();
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance( "PBKDF2WithHmacSHA512" );
            PBEKeySpec spec = new PBEKeySpec(passwordArr, saltArr, 100, 512);
            SecretKey key = skf.generateSecret(spec);
            byte[] res = key.getEncoded();
            return bytesToHex(res);
        } catch ( NoSuchAlgorithmException | InvalidKeySpecException e ) {

        } catch (RuntimeException e) {e.printStackTrace();}
        return null;
    }
}
