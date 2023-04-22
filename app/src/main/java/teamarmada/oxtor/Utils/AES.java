package teamarmada.oxtor.Utils;


import android.util.Base64;

import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import teamarmada.oxtor.Main.MainActivity;
import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Model.ProfileItem;
import teamarmada.oxtor.Repository.FirestoreRepository;

public class AES {

    public static final String TAG= AES.class.getSimpleName();
    public static final String ALGORITHM="PBKDF2WithHmacSHA1";
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256;
    public static byte[] SALT=new byte[16];

    private static SecretKeySpec getSecretKey(ProfileItem profileItem,FileItem item) throws Exception {
        FirestoreRepository.getInstance().fetchSALT(profileItem)
                .addOnSuccessListener(bytes -> SALT=bytes)
                .addOnFailureListener(Throwable::printStackTrace);
        item.setEncryptionPassword(MainActivity.getEncryptionPassword());
        KeySpec spec=new PBEKeySpec(item.getEncryptionPassword().toCharArray(), SALT, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        SecretKey tempKey = factory.generateSecret(spec);
        return new SecretKeySpec(tempKey.getEncoded(), "AES");
    }

    public static byte[] encrypt(byte[] encrypt,FileItem fileItem,ProfileItem profileItem) throws Exception {
        byte[] IV=new byte[16];
        (new SecureRandom()).nextBytes(IV);
        fileItem.setEncrypted(true).setIv(Base64.encodeToString(IV,Base64.DEFAULT));
        Cipher encryptCipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        encryptCipher.init(Cipher.ENCRYPT_MODE,getSecretKey(profileItem,fileItem),new IvParameterSpec(IV));
        return encryptCipher.doFinal(encrypt);
    }

    public static byte[] decrypt(byte[] encrypt,FileItem fileItem,ProfileItem profileItem) throws Exception {
        byte[] IV=Base64.decode(fileItem.getIv(),Base64.DEFAULT);
        fileItem.setEncrypted(false).setIv(null);
        Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        decryptCipher.init(Cipher.DECRYPT_MODE,getSecretKey(profileItem,fileItem),new IvParameterSpec(IV));
        return decryptCipher.doFinal(encrypt);
    }

    public static Cipher getEncryptCipher(FileItem fileItem,ProfileItem profileItem) throws Exception{
        byte[] IV=new byte[16];
        (new SecureRandom()).nextBytes(IV);
        Cipher encryptCipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        encryptCipher.init(Cipher.ENCRYPT_MODE,getSecretKey(profileItem,fileItem),new IvParameterSpec(IV));
        fileItem.setEncrypted(true).setIv(Base64.encodeToString(IV,Base64.DEFAULT));
        return encryptCipher;
    }

    public static Cipher getDecryptionCipher(FileItem fileItem,ProfileItem profileItem) throws Exception{
        byte[] IV=Base64.decode(fileItem.getIv(),Base64.DEFAULT);
        Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        decryptCipher.init(Cipher.DECRYPT_MODE,getSecretKey(profileItem,fileItem),new IvParameterSpec(IV));
        fileItem.setEncrypted(false).setIv(null);
        return decryptCipher;
    }

}
