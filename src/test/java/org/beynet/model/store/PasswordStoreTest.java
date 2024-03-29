package org.beynet.model.store;

import org.beynet.model.MainPasswordError;
import org.beynet.model.password.Password;
import org.beynet.model.password.WebLoginAndPassword;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created by beynet on 15/10/2014.
 */
public class PasswordStoreTest extends RootTest {

    private Password createTestPassword() {
        return new WebLoginAndPassword(URI.create("http://fake-uri.fake"),"login","password","more info");
    }

    private PasswordStore writeAndReload(PasswordStore s) throws IOException, ClassNotFoundException, MainPasswordError {
        Path temporaryFile =null;
        try {
            s.save();
            s.close();
            return new PasswordStore(s.storePath);
        }finally {
            if (temporaryFile!=null && Files.exists(temporaryFile)) Files.delete(temporaryFile);
        }
    }



    /**
     * test that password added on remote is added on local store
     */
    @Test
    public void mergePasswordAddedOnRemote() throws IOException, ClassNotFoundException, MainPasswordError {
        PasswordStore s1 = new PasswordStore(Files.createTempFile("tmp", ".dat"));
        PasswordStore s2 = new PasswordStore(Files.createTempFile("tmp", ".dat"));

        Password p1,p2,p3;

        p1=createTestPassword();
        p2=createTestPassword();
        p3=createTestPassword();

        s1.savePassword(p1);
        s2.savePassword(p1);

        s1.savePassword(p2);
        s2.savePassword(p2);

        s2.savePassword(p3);


        s1.merge(writeAndReload(s2));

        assertNotNull(s1.passwords.get(p3.getId()));
    }

    @Test
    public void mergePasswordModifiedOnRemote() throws IOException, ClassNotFoundException, MainPasswordError {
        PasswordStore s1 = new PasswordStore(Files.createTempFile("tmp", ".dat"));
        PasswordStore s2 = new PasswordStore(Files.createTempFile("tmp", ".dat"));

        WebLoginAndPassword p1,p2;

        p1= (WebLoginAndPassword) createTestPassword();
        p2= (WebLoginAndPassword) createTestPassword();

        s1.savePassword(p1);
        s2.savePassword(p1);

        s1.savePassword(p2);
        s2.savePassword(p2);


        p2 = (WebLoginAndPassword) p2.refresh(new WebLoginAndPassword(p2.getUri(),p2.getLogin(),"newpassword","new additional info"));
        s2.savePassword(p2);


        s1.merge(writeAndReload(s2));

        final Password actual = s1.passwords.get(p2.getId());
        assertNotNull(actual);
        assertEquals(p2,actual);
    }

    @Test
    public void mergePasswordModifiedOnLocal() throws IOException, ClassNotFoundException, MainPasswordError {
        PasswordStore s1 = new PasswordStore(Files.createTempFile("tmp", ".dat"));
        PasswordStore s2 = new PasswordStore(Files.createTempFile("tmp", ".dat"));

        WebLoginAndPassword p1,p2;

        p1= (WebLoginAndPassword) createTestPassword();
        
        p2= (WebLoginAndPassword) createTestPassword();

        s1.savePassword(p1);
        s2.savePassword(p1);

        s1.savePassword(p2);
        
        s2.savePassword(p2);


        p2 = (WebLoginAndPassword) p2.refresh(new WebLoginAndPassword(p2.getUri(),p2.getLogin(),"newpassword","new additional after modification"));
        s1.savePassword(p2);


        s1.merge(writeAndReload(s2));

        final Password actual = s1.passwords.get(p2.getId());
        assertNotNull(actual);
        assertEquals(p2,actual);
    }


    @Test
    public void search() throws IOException, ClassNotFoundException, MainPasswordError {
        PasswordStore s1 = new PasswordStore(Files.createTempFile("tmp", ".dat"));

        WebLoginAndPassword p1 = new WebLoginAndPassword(URI.create("http://www.google.fr"),"testeur","password","add on seach xziup");
        WebLoginAndPassword p2 = new WebLoginAndPassword(URI.create("http://www.fresnes.info"),"testeur","password2",null);

        s1.savePassword(p1);
        s1.savePassword(p2);

        Map<String,Password> results = s1.search("google");
        assertEquals(Integer.valueOf(1),Integer.valueOf(results.size()));
        assertEquals(p1,results.get(p1.getId()));

        results = s1.search("fresnes");
        assertEquals(Integer.valueOf(1),Integer.valueOf(results.size()));
        assertEquals(p2,results.get(p2.getId()));

        results = s1.search("xziup");
        assertEquals(Integer.valueOf(1),Integer.valueOf(results.size()));
        assertEquals(p1,results.get(p1.getId()));


        results = s1.search("testeur");
        assertEquals(Integer.valueOf(2),Integer.valueOf(results.size()));
        assertTrue(results.containsKey(p1.getId()));
        assertTrue(results.containsKey(p2.getId()));

    }

    @Test
    public void backupfile() throws IOException, ClassNotFoundException, MainPasswordError {

        Path storePath = Files.createTempFile("tmp", ".dat");
        PasswordStore s1 = new PasswordStore(storePath);
        Path expectedBackupFile = s1.getExpectedBackupPath();
        assertThat(expectedBackupFile,not(equalTo(s1.storePath)));

        WebLoginAndPassword p1 = new WebLoginAndPassword(URI.create("http://www.google.fr"),"testeur","password",null);
        WebLoginAndPassword p2 = new WebLoginAndPassword(URI.create("http://www.fresnes.info"),"testeur","password2","add on");

        s1.savePassword(p1);
        s1.save();
        s1.savePassword(p2);
        s1.save();

        assertThat(Files.exists(expectedBackupFile), is(Boolean.TRUE));

    }


}
