package org.beynet.utils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by beynet on 23/10/14.
 */
public class URIUtils {
    public static String encodeQueryParam(String param) throws URISyntaxException {
        URI uri = new URI(null,null,null,"p="+param,null);
        final String s = uri.toString();
        System.out.println(s);
        final String pattern = "?p=";
        return s.substring(s.indexOf(pattern)+pattern.length());
    }
}
