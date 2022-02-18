package org.beynet.model;

import javafx.application.Platform;
import org.apache.log4j.Logger;
import org.beynet.exceptions.PasswordMismatchException;
import org.beynet.model.password.GoogleDrive;
import org.beynet.model.password.OneDrive;
import org.beynet.model.store.PasswordStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

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

    public  SecretKey getSecretKeyFromPasswordAndSalt(String password, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        //if (secret!=null) return secret;
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec)
            .getEncoded(), "AES");
        //return secret;
    }

    public  IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return generateIvFromBytes(iv);
    }
    public IvParameterSpec generateIvFromBytes(byte[] bytes) {
        return new IvParameterSpec(bytes);
    }

    public byte[] encrypt_old(byte[] from) throws RuntimeException {
        try {
            Key key = new SecretKeySpec(completeTo128Bits(password), "AES");
            Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(from);

            return encrypted;
        } catch(Exception e) {
            throw new RuntimeException("unable to encrypt",e);
        }
    }
    public byte[] encrypt(byte[] from) throws RuntimeException {
        try {
            Key key = getSecretKeyFromPasswordAndSalt(password,"this is the salt");
            Cipher cipher = Cipher.getInstance(ALGO);
            IvParameterSpec ivParameterSpec = generateIv();
            cipher.init(Cipher.ENCRYPT_MODE, key,ivParameterSpec);
            byte[] encrypted = cipher.doFinal(from);
            byte[] iv = ivParameterSpec.getIV();
            int size = Integer.BYTES+iv.length+encrypted.length;
            byte[] result = new byte[size];
            ByteBuffer buf = ByteBuffer.allocateDirect(size);
            buf.putInt(iv.length);
            buf.put(iv);
            buf.put(encrypted);
            buf.rewind();
            buf.get(result);
            return result;
        } catch(Exception e) {
            throw new RuntimeException("unable to encrypt",e);
        }
    }

    public byte[] decrypt_old(byte[] from) throws RuntimeException, MainPasswordError {
        final Cipher cipher;
        try {
            Key key = new SecretKeySpec(completeTo128Bits(password), "AES");
            cipher = Cipher.getInstance("AES");

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

    public byte[] decrypt(byte[] from) throws RuntimeException, MainPasswordError {
        final Cipher cipher;
        byte[] toDecrypt;
        try {
            ByteBuffer buffer = ByteBuffer.allocate(from.length);
            buffer.put(from);
            buffer.rewind();
            int l = buffer.getInt();
            byte[] ivBytes = new byte[l];
            toDecrypt = new byte[from.length-Integer.BYTES-l];
            buffer.get(ivBytes);
            buffer.get(toDecrypt);
            Key key = getSecretKeyFromPasswordAndSalt(password,"this is the salt");
            cipher = Cipher.getInstance(ALGO);

            cipher.init(Cipher.DECRYPT_MODE, key,generateIvFromBytes(ivBytes));
        }catch(Exception e) {
            return decrypt_old(from);
            //throw new RuntimeException("unable to decrypt - configuration error ?",e);
        }
        try {
            return cipher.doFinal(toDecrypt);
        } catch (Exception e) {
            return decrypt_old(from);
            //throw new MainPasswordError("error accessing database");
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
    private SecretKey secret=null;
    private IvParameterSpec IV=null;
    private final PasswordStore store;
    private Path savePath;
    private Path saveFile;
    private String password;
    private static final String ALGO  = "AES/CBC/PKCS5Padding";

    public static final String APPLICATION_DEFAULT_FILE_NAME = "jpasswd.dat";

    private final static Logger logger = Logger.getLogger(Config.class);
}
