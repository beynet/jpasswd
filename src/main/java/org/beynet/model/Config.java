package org.beynet.model;

import org.beynet.model.store.PasswordStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.Key;

/**
 * Created by beynet on 15/10/2014.
 */
public class Config {

    private Config(String password) {
        this.store = new PasswordStore();
        this.password = password;
    }

    public static void initConfig(String password) {
        synchronized (Config.class) {
            if (_instance==null) {
                _instance = new Config(password);
            }
            else {
                throw new RuntimeException("config already initialized");
            }
        }
    }


    public static Config getInstance() {
        if (_instance==null) {
            throw new RuntimeException("config not initialized");
        }
        return _instance;
    }

    public byte[] completeTo128Bits(String password) throws UnsupportedEncodingException {
        byte[] b = password.getBytes("UTF-8");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int i=0;i<16;i++) {
            bos.write(b[i%b.length]);
        }
        return bos.toByteArray();
    }

    public byte[] encrypt(byte[] from) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGO);
        Key key = new SecretKeySpec(completeTo128Bits(password),ALGO);
        Cipher cipher = Cipher.getInstance(ALGO);

        cipher.init(Cipher.ENCRYPT_MODE,key);
        byte[] encrypted = cipher.doFinal(from);

        return encrypted;
    }

    public byte[] decrypt(byte[] from) throws Exception {
        Key key = new SecretKeySpec(completeTo128Bits(password),ALGO);
        Cipher cipher = Cipher.getInstance(ALGO);

        cipher.init(Cipher.DECRYPT_MODE,key);
        return cipher.doFinal(from);
    }

    public final PasswordStore getPasswordStore() {
        return store;
    }

    private  static  Config _instance  = null ;
    private final PasswordStore store;
    private String password;
    private static final String ALGO  = "AES";
}
