package org.beynet.model.store;

import org.beynet.model.password.Password;
import org.beynet.model.password.WebLoginAndPassword;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by beynet on 15/10/2014.
 */
public class PasswordStoreTest {

    private Password createTestPassword() {
        return new WebLoginAndPassword(URI.create("http://fake-uri.fake"),"login","password");
    }

    private PasswordStore writeAndReload(PasswordStore s) throws IOException, ClassNotFoundException {
        Path temporaryFile =null;
        try {
            temporaryFile = Files.createTempFile("tmp", ".dat");
            s.save(temporaryFile);
            return PasswordStore.fromFile(temporaryFile);
        }finally {
            if (temporaryFile!=null && Files.exists(temporaryFile)) Files.delete(temporaryFile);
        }
    }

    /**
     * test that password added on remote is added on local store
     */
    @Test
    public void mergePasswordAddedOnRemote() throws IOException, ClassNotFoundException {
        PasswordStore s1 = new PasswordStore();
        PasswordStore s2 = new PasswordStore();

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
    public void mergePasswordModifiedOnRemote() throws IOException, ClassNotFoundException {
        PasswordStore s1 = new PasswordStore();
        PasswordStore s2 = new PasswordStore();

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
    public void mergePasswordModifiedOnLocal() throws IOException, ClassNotFoundException {
        PasswordStore s1 = new PasswordStore();
        PasswordStore s2 = new PasswordStore();

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


}
