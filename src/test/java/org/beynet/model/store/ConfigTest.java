package org.beynet.model.store;

import org.beynet.model.Config;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by beynet on 15/10/2014.
 */
public class ConfigTest extends RootTest{

    @Test
    public void checkEncrypt() throws Exception {
        String test ="this is a test to be encrypted";

        final byte[] encrypt = Config.getInstance().encrypt(test.getBytes("UTF-8"));
        final byte[] decrypted = Config.getInstance().decrypt(encrypt);
        assertThat(new String(decrypted,"UTF-8"),is(test));
    }

    @Test
    public void checkEncryptOldReadNew() throws Exception {
        String test ="this is a test to be encrypted";

        final byte[] encrypt = Config.getInstance().encrypt_old(test.getBytes("UTF-8"));
        final byte[] decrypted = Config.getInstance().decrypt(encrypt);
        assertThat(new String(decrypted,"UTF-8"),is(test));
    }

}
