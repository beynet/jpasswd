package org.beynet.model;

import javafx.application.Platform;
import org.beynet.model.store.PasswordStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by beynet on 15/10/2014.
 */
public class Config implements Observer {

    private Config(String password,Path savePath) {
        this.password = password;
        this.savePath=savePath;
        this.saveFile = this.savePath.resolve(Paths.get("database.dat"));
        try {
            Files.createDirectories(savePath);
        } catch (IOException e) {
            throw new RuntimeException("unable to create path :"+savePath,e);
        }

            try {
                this.store = new PasswordStore(saveFile,this);
            } catch (Exception e) {
               throw new RuntimeException("unable to read database",e);
            }
        this.store.addObserver(this);
    }

    public static void initConfig(String password,Path savePath) {
        synchronized (Config.class) {
            if (_instance==null) {
                _instance = new Config(password,savePath);
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

    public byte[] encrypt(byte[] from) throws RuntimeException {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGO);
            Key key = new SecretKeySpec(completeTo128Bits(password), ALGO);
            Cipher cipher = Cipher.getInstance(ALGO);

            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(from);

            return encrypted;
        } catch(Exception e) {
            throw new RuntimeException("unable to encrypt",e);
        }
    }

    public byte[] decrypt(byte[] from) throws RuntimeException {
        try {
            Key key = new SecretKeySpec(completeTo128Bits(password), ALGO);
            Cipher cipher = Cipher.getInstance(ALGO);

            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(from);
        }catch(Exception e) {
            throw new RuntimeException("unable to decrypt",e);
        }
    }

    public void changeMainPassword(String password) {
        this.password=password;
    }

    public final PasswordStore getPasswordStore() {
        return store;
    }

    private  static  Config _instance  = null ;
    private final PasswordStore store;
    private Path savePath;
    private Path saveFile;
    private String password;
    private static final String ALGO  = "AES";

    @Override
    public void update(Observable o, Object arg) {
        Platform.runLater(() -> {
            try {
                this.store.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }


}
