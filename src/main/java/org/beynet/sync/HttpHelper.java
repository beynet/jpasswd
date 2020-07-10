package org.beynet.sync;

import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

public class HttpHelper {

    public static byte[] readAllByte(InputStream is) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (true) {
            final int read = is.read(buffer);
            if (read==-1) break;
            result.write(buffer,0,read);
        }
        return result.toByteArray();
    }

    /**
     * read a string from steam
     * @param is
     * @return
     * @throws IOException
     */
    public static String getJsonString(InputStream is) throws IOException {
        return new String(readAllByte(is),"UTF-8");
    }

    /**
     * post content of the string to url as a application/x-www-form-urlencoded"
     * @param toBePosted
     * @param url
     * @return
     * @throws java.io.IOException
     */
    public static String postStringXWWWFormUrlEncodedAndReturnResponseString(String toBePosted,String url) throws IOException, AuthenticationException {
        final URL r;
        try {
            r = new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException("URL should be valid ", e);
        }
        final HttpURLConnection urlConnection = (HttpURLConnection) r.openConnection();
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        urlConnection.setRequestMethod("POST");
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);


        try (OutputStream os = urlConnection.getOutputStream()) {
            os.write(toBePosted.getBytes("UTF-8"));
        }
        final int responseCode = urlConnection.getResponseCode();
        if (responseCode==200) {
            try(InputStream is =urlConnection.getInputStream()){
                return getJsonString(is);
            }
        }
        else {
            InputStream is ;
            if (responseCode>=400) {
                is = urlConnection.getErrorStream();
            }
            else {
                is = urlConnection.getInputStream();
            }
            try (InputStream response = is) {
                final String json = getJsonString(response);
                if (responseCode>=400 && responseCode<500) {
                    throw new AuthenticationException("Error received from serveur code="+responseCode+" message="+json);
                }
                else throw new IOException("Error received from serveur code="+responseCode+" message="+json);
            }

        }
    }

    public static HttpURLConnection buildURLConnection(String urlStr, Map<String,Object> credentials) throws IOException {
        URL url ;
        try {
            url =new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException("url should be valid",e);
        }
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setRequestProperty("Authorization","Bearer "+credentials.get(ACCESS_TOKEN));
        return urlConnection;
    }

    public static Optional<byte[]> readBytesFromRessourceWithGET(String urlStr, Map<String,Object> credentials) throws IOException {
        logger.info("calling url "+urlStr);

        final HttpURLConnection urlConnection = buildURLConnection(urlStr,credentials);
        urlConnection.setRequestMethod("GET");
        urlConnection.setDoOutput(false);
        return readBytesResponse(urlConnection,credentials);
    }

    public static Optional<byte[]> readBytesResponse(final HttpURLConnection urlConnection,Map<String,Object> credentials) throws IOException {
        Optional<byte[]> result = Optional.empty();
        final int responseCode = urlConnection.getResponseCode();
        if (responseCode==200) {
            try(InputStream is =urlConnection.getInputStream()){
                result = Optional.of(HttpHelper.readAllByte(is));
            }
        }
        else {
            InputStream is ;
            if (responseCode>=400) {
                is = urlConnection.getErrorStream();
            }
            else {
                is = urlConnection.getInputStream();
            }
            try (InputStream response = is) {
                if (responseCode==404) {

                }
                else if (responseCode!=401) {
                    final String json = HttpHelper.getJsonString(response);
                    throw new IOException("Error received from serveur code=" + responseCode + " message=" + json);
                }
                else {
                    credentials.remove(ACCESS_TOKEN);
                }
            }
        }

        return result;
    }

    private final static Logger logger = Logger.getLogger(HttpHelper.class);

    public final static String ACCESS_TOKEN   = "access_token";
    public final static String REFRESH_TOKEN  = "refresh_token";
}
