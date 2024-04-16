package com.psl.pallettracking.helper;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
//import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
//import org.apache.commons.codec.binary.Base64;

public class Md5Encryption {
    //https://stackoverflow.com/questions/29224892/c-sharp-to-java-encryption-and-decryption
    private static final String ALGORITHM = "md5";
    //private static final String DIGEST_STRING = "HG58YZ3CR9";
    private static final String DIGEST_STRING = "OTS";
    private static final String CHARSET_UTF_8 = "utf-8";
    private static final String SECRET_KEY_ALGORITHM = "DESede";
    private static final String TRANSFORMATION_PADDING = "DESede/CBC/PKCS5Padding";

    public static String hexToNumber(String hex){
        int num = Integer.parseInt(hex,16);
        return String.valueOf(num);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String encrypt(String inputString, String key) throws Exception {

        //----  Use specified 3DES key and IV from other source --------------

        final MessageDigest md = MessageDigest.getInstance("MD5");
        final byte[] digestOfPassword = md.digest(key.getBytes("UTF-8"));

        final byte[] digestOfPassword2 = md.digest(GetKeyAsBytes("OTS"));
        final byte[] digestOfPassword3 = GetKeyAsBytes("OTS");
        final byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
        for (int j = 0, k = 16; j < 8;) {
            keyBytes[k++] = keyBytes[j++];
        }

        final SecretKey sk = new SecretKeySpec(keyBytes, "DESede");
        final IvParameterSpec iv = new IvParameterSpec(new byte[8]);
        final Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, sk, iv);

        final byte[] plainTextBytes = inputString.getBytes("UTF-8");
        final byte[] cipherText = cipher.doFinal(plainTextBytes);
        // final String encodedCipherText = new sun.misc.BASE64Encoder()
        // .encode(cipherText);



       // String base64 = Base64.encodeToString(hash, Base64.DEFAULT);
        //return Base64.encodeBase64String(cipherText);
        return Base64.getEncoder().encodeToString(cipherText);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String decrypt(String input) throws Exception {

        //byte[] message = Base64.decodeBase64(input);
        byte[] message = Base64.getDecoder().decode(input);

        final MessageDigest md = MessageDigest.getInstance("MD5");
        final byte[] digestOfPassword = md.digest("OTS"
                .getBytes("UTF-8"));

        final byte[] digestOfPassword2 = md.digest(GetKeyAsBytes("OTS"));
        final byte[] digestOfPassword3 = GetKeyAsBytes("OTS");
        final byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
        for (int j = 0, k = 16; j < 8;) {
            keyBytes[k++] = keyBytes[j++];
        }

        final SecretKey key = new SecretKeySpec(keyBytes, "DESede");
        final IvParameterSpec iv = new IvParameterSpec(new byte[8]);
        final Cipher decipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        decipher.init(Cipher.DECRYPT_MODE, key, iv);

        // final byte[] encData = new
        // sun.misc.BASE64Decoder().decodeBuffer(message);
        final byte[] plainText = decipher.doFinal(message);

        return new String(plainText, "UTF-8");
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String decryptORIGINAL(String input) throws Exception {

        //byte[] message = Base64.decodeBase64(input);
        byte[] message = Base64.getDecoder().decode(input);

        final MessageDigest md = MessageDigest.getInstance("MD5");
        final byte[] digestOfPassword = md.digest("OTS"
                .getBytes("UTF-8"));

        final byte[] digestOfPassword2 = md.digest(GetKeyAsBytes("OTS"));
        final byte[] digestOfPassword3 = GetKeyAsBytes("OTS");
        final byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
        for (int j = 0, k = 16; j < 8;) {
            keyBytes[k++] = keyBytes[j++];
        }

        final SecretKey key = new SecretKeySpec(keyBytes, "DESede");
        final IvParameterSpec iv = new IvParameterSpec(new byte[8]);
        final Cipher decipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        decipher.init(Cipher.DECRYPT_MODE, key, iv);

        // final byte[] encData = new
        // sun.misc.BASE64Decoder().decodeBuffer(message);
        final byte[] plainText = decipher.doFinal(message);

        return new String(plainText, "UTF-8");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void main(String[] args) throws Exception {
        String text = "psladmin";
        //psladmin
        //5Lqz/KETt/kmlhCp5Fzk7Q==
        String encrt = encrypt("psladmin","OTS");
        System.out.println("ECR : "+encrt);

        System.out.println("ENCRYPTION : "+encrypt("psladmin","OTS")); // this is a byte array, you'll just see a reference to an array

        String decry = decrypt(encrt);
        System.out.println("DECRNEW : "+decry);
        String decryold1 = decryptORIGINAL("5Lqz/KETt/kmlhCp5Fzk7Q==");
        System.out.println("DECRORIGINAL1 : "+decryold1);
        String decryold = decryptORIGINAL("5Lqz/KETt/nlV6uFHiQtDw==");
        System.out.println("DECRORIGINAL : "+decryold);

    }


    public static byte[] GetKeyAsBytes(String key) {
        byte[] keyBytes = new byte[24]; // a Triple DES key is a byte[24] array

        for (int i = 0; i < key.length() && i < keyBytes.length; i++)
            keyBytes[i] = (byte) key.charAt(i);

        return keyBytes;
    }
}
