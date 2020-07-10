package org.beynet.model;

import javafx.application.Platform;
import org.apache.log4j.Logger;
import org.beynet.exceptions.PasswordMismatchException;
import org.beynet.model.password.GoogleDrive;
import org.beynet.model.password.OneDrive;
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

/**
 * Created by beynet on 15/10/2014.
 */
public class Config implements Observer {

    private Config(String password,Path savePath,String fileName) throws MainPasswordError {
        this.password = password;
        this.savePath=savePath;
        if (fileName==null) {
            this.saveFile = this.savePath.resolve(Paths.get(APPLICATION_DEFAULT_FILE_NAME));
        }
        else {
            this.saveFile = this.savePath.resolve(Paths.get(fileName));
        }
        try {
            Files.createDirectories(savePath);
        } catch (IOException e) {
            throw new RuntimeException("unable to create path :"+savePath,e);
        }

        try {
            this.store = new PasswordStore(saveFile,this);
        } catch (IOException e) {
            logger.error("error reading password file",e);
            throw new RuntimeException("error reading password file",e);
        }
        this.store.addObserver(this);
    }


    public static void initConfig(String password,Path savePath,String fileName) throws MainPasswordError {
        synchronized (Config.class) {
            if (_instance==null) {
                _instance = new Config(password,savePath,fileName);
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

    public String getFileName() {
        return saveFile.getFileName().toString();
    }

    public void merge(byte[] content) throws IOException, PasswordMismatchException {
        final byte[] decrypt ;
        try {
            decrypt = decrypt(content);
        } catch(MainPasswordError e) {
            throw new PasswordMismatchException();
        }
        final PasswordStore toMergeWith= new PasswordStore(decrypt, this);
        getPasswordStore().merge(toMergeWith);
    }

    /**
     * save google drive refresh token into database
     * @param refreshToken
     */
    public void updateGoogleDriveRefreshToken(String refreshToken) {
        getPasswordStore().savePassword(new GoogleDrive(refreshToken));
    }

    public void updateOneDriveRefreshToken(String refreshToken) {
        getPasswordStore().savePassword(new OneDrive(refreshToken));
    }

    public void removeGoogleDrivePassword() {
        getPasswordStore().removePassword(new GoogleDrive("t").getId());
    }

    public void removeOneDrivePassword() {
        getPasswordStore().removePassword(new OneDrive("t").getId());
    }

    public String getGoogleDriveRefreshToken() {
        return getPasswordStore().getGoogleDriveRefreshToken();
    }

    public String getOneDriveRefreshToken() {
        return getPasswordStore().getOneDriveRefreshToken();
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

    public byte[] decrypt(byte[] from) throws RuntimeException, MainPasswordError {
        final Cipher cipher;
        try {
            Key key = new SecretKeySpec(completeTo128Bits(password), ALGO);
            cipher = Cipher.getInstance(ALGO);

            cipher.init(Cipher.DECRYPT_MODE, key);
        }catch(Exception e) {
            throw new RuntimeException("unable to decrypt - configuration error ?",e);
        }
        try {
            return cipher.doFinal(from);
        } catch (Exception e) {
            throw new MainPasswordError("error accessing database");
        }
    }

    public void changeMainPassword(String password) {
        this.password=password;
    }

    public final PasswordStore getPasswordStore() {
        return store;
    }


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


    private  static  Config _instance  = null ;
    private final PasswordStore store;
    private Path savePath;
    private Path saveFile;
    private String password;
    private static final String ALGO  = "AES";

    public static final String APPLICATION_DEFAULT_FILE_NAME = "jpasswd.dat";

    private final static Logger logger = Logger.getLogger(Config.class);
}
