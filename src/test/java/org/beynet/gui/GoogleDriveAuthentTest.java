package org.beynet.gui;

import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by beynet on 23/10/14.
 */
public class GoogleDriveAuthentTest {

    @Test
    public void testCodeRetrieval() {
        String expected ="this/istheexpectedcode";
        String uri = "http://localhost?code="+expected;
        assertThat(GoogleDriveAuthent.getCodeFromURL(uri), is(expected));
        uri = "http://localhost?val1=val2&code="+expected+"&val3=val4&val5=val6";
        assertThat(GoogleDriveAuthent.getCodeFromURL(uri), is(expected));
    }



}
