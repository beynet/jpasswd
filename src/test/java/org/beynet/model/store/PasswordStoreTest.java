package org.beynet.model.store;

import org.beynet.model.MainPasswordError;
import org.beynet.model.password.Password;
import org.beynet.model.password.WebLoginAndPassword;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by beynet on 15/10/2014.
 */
public class PasswordStoreTest extends RootTest {

    private Password createTestPassword() {
        return new WebLoginAndPassword(URI.create("http://fake-uri.fake"),"login","password");
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

        assertThat(s1.passwords.get(p3.getId()),notNullValue());
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


        p2 = new WebLoginAndPassword(p2.getUri(),p2.getLogin(),"newpassword");
        s2.savePassword(p2);


        s1.merge(writeAndReload(s2));

        final Password actual = s1.passwords.get(p2.getId());
        assertThat(actual,notNullValue());
        assertThat(actual,is(p2));
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


        p2 = new WebLoginAndPassword(p2.getUri(),p2.getLogin(),"newpassword");
        s1.savePassword(p2);


        s1.merge(writeAndReload(s2));

        final Password actual = s1.passwords.get(p2.getId());
        assertThat(actual,notNullValue());
        assertThat(actual,is(p2));
    }


    @Test
    public void search() throws IOException, ClassNotFoundException, MainPasswordError {
        PasswordStore s1 = new PasswordStore(Files.createTempFile("tmp", ".dat"));

        WebLoginAndPassword p1 = new WebLoginAndPassword(URI.create("http://www.google.fr"),"testeur","password");
        WebLoginAndPassword p2 = new WebLoginAndPassword(URI.create("http://www.fresnes.info"),"testeur","password2");

        s1.savePassword(p1);
        s1.savePassword(p2);

        Map<String,Password> results = s1.search("google");
        assertThat(new Integer(results.size()),is(Integer.valueOf(1)));
        assertThat(results.get(p1.getId()),is(p1));

        results = s1.search("fresnes");
        assertThat(new Integer(results.size()),is(Integer.valueOf(1)));
        assertThat(results.get(p2.getId()),is(p2));


        results = s1.search("testeur");
        assertThat(new Integer(results.size()),is(Integer.valueOf(2)));
        assertThat(results.containsKey(p1.getId()),is(Boolean.TRUE));
        assertThat(results.containsKey(p2.getId()),is(Boolean.TRUE));

    }

}
