package org.beynet.sync;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.beynet.gui.GoogleDriveAuthent;
import org.beynet.model.Config;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by beynet on 25/10/14.
 */
public class GoogleDriveSync implements Runnable{

    public GoogleDriveSync(Stage applicationMainStage,Map<String,String> credentials) {
        this.applicationMainStage = applicationMainStage;
        this.credentials = credentials;
    }

    public GoogleDriveSync(Stage applicationMainStage) {
        this.applicationMainStage = applicationMainStage;
        this.credentials = new HashMap<>();
    }

    @Override
    public void run() {
        while(true) {
            try {
                saveFile(Config.getInstance().getPasswordStore().getFileContent());
            } catch (IOException e) {
                logger.error("error during process",e);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveFile(byte[] file) throws IOException {
        if (credentials.isEmpty()) {
            logger.debug("no tokens first authent");
            this.code = null;
            callAuthent();
            if (this.code==null) return;
            retrieveAccessTokenAndRefreshTokenFromCode(code);
        }
        else if (credentials.get(ACCESS_TOKEN)==null) {
            retrieveAccessTokenFromRefreshTokenCode(credentials.get(REFRESH_TOKEN));
        }
        searchApplicationFileId();
        postFile(file);
    }

    private void callAuthent() {
        String authentURI ="https://accounts.google.com/o/oauth2/auth?response_type=code&redirect_uri="+REDIRECT_URI+"&scope=https://www.googleapis.com/auth/drive.file&client_id="+CLIENT_ID;
        Platform.runLater(() -> {
            final GoogleDriveAuthent googleDriveAuthent = new GoogleDriveAuthent(applicationMainStage, authentURI,this, c -> code = c);
            googleDriveAuthent.show();
        });
        logger.debug("wait until authen is done");
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private void writeString(OutputStream os,String toWrite) throws IOException {
        os.write(toWrite.getBytes("UTF-8"));
    }


    private void createNewFile(byte[] file) throws IOException {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        final String json ="{\"title\":\"jpasswd.dat\"}";
        final String part ="jpasswd_part";
        URL r = new URL("https://www.googleapis.com/upload/drive/v2/files?uploadType=multipart&access_token="+credentials.get(ACCESS_TOKEN));


        writeString(response, "--" + part + "\r\n");
        writeString(response,"Content-Type: application/json; charset=UTF-8\r\n\r\n");
        response.write(json.getBytes());
        writeString(response, "\r\n--" + part + "\r\n");
        writeString(response,"Content-Type: application/dat\r\n\r\n");
        response.write(file);
        writeString(response,"\r\n\r\n--"+part+"--\r\n");

        final HttpURLConnection urlConnection = (HttpURLConnection) r.openConnection();
        urlConnection.setRequestProperty("Content-Type","multipart/related; boundary=\""+part+"\"");
        urlConnection.setRequestProperty("Content-Length",Integer.valueOf(response.size()).toString());
        urlConnection.setRequestMethod("POST");
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);

        try (OutputStream os = urlConnection.getOutputStream()) {
            os.write(response.toByteArray());
            Files.write(Paths.get("/tmp/response.dat"),response.toByteArray());
        }
        final int responseCode = urlConnection.getResponseCode();
        if (responseCode<=200) {
            try(InputStream is =urlConnection.getInputStream()){
                logger.debug("file created - response ="+getJsonString(is));
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
            try (InputStream is2 = is) {
                final String error = getJsonString(is2);
                throw new IOException("Error received from serveur code="+responseCode+" message="+error);
            }

        }

    }

    private void postFile(byte[] file) throws IOException {
        if (fileFound==null) {
            createNewFile(file);
        }
    }

    /**
     * read a string from steam
     * @param is
     * @return
     * @throws IOException
     */
    String getJsonString(InputStream is) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (true) {
            final int read = is.read(buffer);
            if (read==-1) break;
            result.write(buffer,0,read);
        }
        return new String(result.toByteArray(),"UTF-8");
    }

    /**
     * parse json object an expect to read access and refresh tokens
     * @param json
     * @return a map containing tokens read
     * @throws IOException
     */
    void getAccessAndRefreshTokensFromJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(json);
        credentials.put(ACCESS_TOKEN,actualObj.get(ACCESS_TOKEN)!=null?actualObj.get(ACCESS_TOKEN).getTextValue():null);
        credentials.put(REFRESH_TOKEN,actualObj.get(REFRESH_TOKEN)!=null?actualObj.get(REFRESH_TOKEN).getTextValue():null);
    }

    /**
     * post content of the string to url as a application/x-www-form-urlencoded"
     * @param toBePosted
     * @param url
     * @return
     * @throws java.io.IOException
     */
    private String postStringXWWWFormUrlEncodedAndReturnResponseString(String toBePosted,String url) throws IOException {
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
                throw new IOException("Error received from serveur code="+responseCode+" message="+json);
            }

        }
    }


    void retrieveAccessTokenFromRefreshTokenCode(String refreshToken) throws IOException {
        String query="refresh_token=$refreshToken&client_id=$client_id&grant_type=refresh_token&client_secret=$client_secret";
        query=query.replace("$refreshToken", URLEncoder.encode(refreshToken, "UTF-8"));
        query=query.replace("$client_id",URLEncoder.encode(CLIENT_ID,"UTF-8"));
        query=query.replace("$client_secret",URLEncoder.encode(CLIENT_SECRET,"UTF-8"));

        getAccessAndRefreshTokensFromJson(postStringXWWWFormUrlEncodedAndReturnResponseString(query, "https://accounts.google.com/o/oauth2/token"));
    }

    void retrieveAccessTokenAndRefreshTokenFromCode(String code) throws IOException {
        String query="code=$code&client_id=$client_id&grant_type=authorization_code&client_secret=$client_secret&redirect_uri=$redirect_uri";
        query=query.replace("$code",URLEncoder.encode(code,"UTF-8"));
        query=query.replace("$client_id",URLEncoder.encode(CLIENT_ID,"UTF-8"));
        query=query.replace("$client_secret",URLEncoder.encode(CLIENT_SECRET,"UTF-8"));
        query=query.replace("$redirect_uri",URLEncoder.encode(REDIRECT_URI,"UTF-8"));

        getAccessAndRefreshTokensFromJson(postStringXWWWFormUrlEncodedAndReturnResponseString(query,"https://accounts.google.com/o/oauth2/token"));
        logger.info("logged in token="+credentials.get(ACCESS_TOKEN)+" refresh token="+credentials.get(REFRESH_TOKEN));
    }

    void handleReconnect(Map<String,String> credential) {
//        tryRefresh(credential);
//        reconnect();
    }

    void searchApplicationFileId() throws IOException {
        logger.debug("search expected file on server");
        URL url ;
        try {
            url =new URL("https://www.googleapis.com/drive/v2/files?access_token="+credentials.get(ACCESS_TOKEN));
        } catch (MalformedURLException e) {
            throw new RuntimeException("url should be valid",e);
        }
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setDoOutput(false);
        final int responseCode = urlConnection.getResponseCode();
        if (responseCode==200) {
            try(InputStream is =urlConnection.getInputStream()){
                String listFiles = getJsonString(is);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode actualObj = mapper.readTree(listFiles);
                final ArrayNode items = (ArrayNode)actualObj.get("items");
                for (int i=0;i<items.size();i++) {
                    JsonNode fileNode = items.get(i);
                    final JsonNode title = fileNode.get("title");
                    final JsonNode id = fileNode.get("id");
                    if (Config.APPLICATION_FILE_NAME.equals(title)) {
                        logger.info("file found on server with id="+id.getTextValue());
                        this.fileFound = fileNode;
                    }
                }
                logger.info("file not found on server");
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
                if (responseCode!=401) {
                    final String json = getJsonString(response);
                    throw new IOException("Error received from serveur code=" + responseCode + " message=" + json);
                }
                else {
                    credentials.remove(ACCESS_TOKEN);
                }
            }
        }
    }

    private Map<String,String> credentials;
    private Stage              applicationMainStage ;
    private JsonNode           fileFound ;
    private String             code ;

    private final static String CLIENT_ID     = "630492774750-mdt07rqpebk8rjtpkc10a0m0308r5h9l.apps.googleusercontent.com";
    private final static String CLIENT_SECRET = "YKM8hMJUEuAaQxUEoWfQ9TSV";
    private final static String REDIRECT_URI  = "http://localhost";

    public final static String ACCESS_TOKEN   = "access_token";
    public final static String REFRESH_TOKEN  = "refresh_token";
    private final static Logger logger = Logger.getLogger(GoogleDriveSync.class);


}
