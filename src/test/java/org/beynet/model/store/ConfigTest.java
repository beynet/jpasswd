package org.beynet.model.store;

import org.beynet.model.Config;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by beynet on 15/10/2014.
 */
public class ConfigTest extends RootTest{

    @Test
    public void t() throws Exception {
        String test ="this is a test to be encrypted";

        final byte[] encrypt = Config.getInstance().encrypt(test.getBytes("UTF-8"));
        final byte[] decrypted = Config.getInstance().decrypt(encrypt);
        assertThat(new String(decrypted,"UTF-8"),is(test));
    }
}
