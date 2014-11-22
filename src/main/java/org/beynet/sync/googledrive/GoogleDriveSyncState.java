package org.beynet.sync.googledrive;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.beynet.exceptions.PasswordMismatchException;
import org.beynet.gui.GoogleDriveAuthent;
import org.beynet.model.Config;
import org.beynet.sync.AuthenticationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Observer;

/**
 * Created by beynet on 03/11/14.
 */
public enum GoogleDriveSyncState {
    // initial state
    // -------------
    START {
        @Override
        public GoogleDriveSyncState _process(Map<String, Object> credentials) {
            String refreshToken = Config.getInstance().getGoogleDriveRefreshToken();
            if (refreshToken!=null) credentials.put(REFRESH_TOKEN,refreshToken);
            else credentials.remove(REFRESH_TOKEN);
            return AUTHENT;
        }
    },

    // this state is reponsible to verify
    // that authentication with google drive servers is done
    // -----------------------------------------------------
    AUTHENT {
        @Override
        public GoogleDriveSyncState _process(Map<String, Object> credentials) {
            logger.debug("AUTHENT");
            while (true) {
                // no refresh token - first authent we will connect
                // to google oauth systems using embedded web browser
                // --------------------------------------------------
                if (credentials.get(REFRESH_TOKEN) == null) {
                    logger.debug("no tokens first authent");
                    this.code = null;
                    callAuthent((Stage) credentials.get(APPLICATION_MAIN_STAGE));
                    try {
                        retrieveAccessTokenAndRefreshTokenFromCode(credentials);
                        break;
                    } catch (Exception e) {
                        credentials.remove(REFRESH_TOKEN);
                    }
                } else if (credentials.get(ACCESS_TOKEN) == null) {
                    logger.debug("using refresh token found");
                    try {
                        retrieveAccessTokenFromRefreshTokenCode(credentials);
                        break;
                    }
                    catch(AuthenticationException e) {
                        credentials.remove(REFRESH_TOKEN);
                    }
                    catch (IOException e) {
                        logger.error("error - unable to obtain access token from refresh token", e);
                        credentials.remove(REFRESH_TOKEN);
                    }
                }
            }
            if (credentials.get(REFRESH_TOKEN)!=null) Config.getInstance().updateGoogleDriveRefreshToken((String) credentials.get(REFRESH_TOKEN));
            return MERGE_WITH_REMOTE;
        }

        void retrieveAccessTokenFromRefreshTokenCode(Map<String,Object> credentials) throws IOException,AuthenticationException {
            String query="refresh_token=$refreshToken&client_id=$client_id&grant_type=refresh_token&client_secret=$client_secret";
            query=query.replace("$refreshToken", URLEncoder.encode((String)credentials.get(REFRESH_TOKEN), "UTF-8"));
            query=query.replace("$client_id",URLEncoder.encode(CLIENT_ID,"UTF-8"));
            query=query.replace("$client_secret",URLEncoder.encode(CLIENT_SECRET,"UTF-8"));

            getAccessAndRefreshTokensFromJson(credentials,postStringXWWWFormUrlEncodedAndReturnResponseString(query, "https://accounts.google.com/o/oauth2/token"));
        }

        void retrieveAccessTokenAndRefreshTokenFromCode(Map<String,Object> credentials) throws IOException,AuthenticationException {
            String query="code=$code&client_id=$client_id&grant_type=authorization_code&client_secret=$client_secret&redirect_uri=$redirect_uri";
            query=query.replace("$code",URLEncoder.encode(code,"UTF-8"));
            query=query.replace("$client_id",URLEncoder.encode(CLIENT_ID,"UTF-8"));
            query=query.replace("$client_secret",URLEncoder.encode(CLIENT_SECRET,"UTF-8"));
            query=query.replace("$redirect_uri",URLEncoder.encode(REDIRECT_URI,"UTF-8"));

            getAccessAndRefreshTokensFromJson(credentials,postStringXWWWFormUrlEncodedAndReturnResponseString(query,"https://accounts.google.com/o/oauth2/token"));
            logger.info("logged in token="+credentials.get(ACCESS_TOKEN)+" refresh token="+credentials.get(REFRESH_TOKEN));
        }
        /**
         * parse json object an expect to read access and refresh tokens
         * @param json
         * @return a map containing tokens read
         * @throws IOException
         */
        void getAccessAndRefreshTokensFromJson(Map<String,Object> credentials,String json) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualObj = mapper.readTree(json);
            credentials.put(ACCESS_TOKEN,actualObj.get(ACCESS_TOKEN)!=null?actualObj.get(ACCESS_TOKEN).getTextValue():null);
            if (actualObj.get(REFRESH_TOKEN)!=null) credentials.put(REFRESH_TOKEN,actualObj.get(REFRESH_TOKEN).getTextValue());
        }

        private void callAuthent(Stage applicationMainStage) {
            String authentURI ="https://accounts.google.com/o/oauth2/auth?response_type=code&redirect_uri="+REDIRECT_URI+"&scope=https://www.googleapis.com/auth/drive.file&client_id="+CLIENT_ID;
            Platform.runLater(() -> {
                final GoogleDriveAuthent googleDriveAuthent = new GoogleDriveAuthent(applicationMainStage, authentURI, this, c -> code = c);
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
        String code ;
    },

    // in this state google drive backup is downloaded
    // and then merged with current database
    // ------------------------------------------------
    MERGE_WITH_REMOTE {
        // search for expected file on gdrive
        // -----------------------------------
        JsonNode searchApplicationFileId(Map<String,Object> credentials) throws IOException {
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
                    logger.debug("files found=\n"+listFiles);
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode actualObj = mapper.readTree(listFiles);
                    final ArrayNode items = (ArrayNode)actualObj.get("items");
                    for (int i=0;i<items.size();i++) {
                        JsonNode fileNode = items.get(i);
                        final JsonNode title = fileNode.get("title");
                        final JsonNode id = fileNode.get("id");
                        final JsonNode explicitlyTrashed = fileNode.get("explicitlyTrashed"); // skipping file in trash

                        if (title!=null && id!=null && Config.getInstance().getFileName().equals(title.getTextValue()) &&
                            (explicitlyTrashed==null|| explicitlyTrashed.getBooleanValue()==false)
                            ) {
                            logger.info("file found on server with id="+id.getTextValue());
                            return fileNode;
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
            return null;
        }
        @Override
        public GoogleDriveSyncState _process(Map<String, Object> credentials) {
            logger.debug("MERGE");
            JsonNode remoteFileNode;
            while(true) {
                try {
                    remoteFileNode = searchApplicationFileId(credentials);
                    break;
                } catch (IOException e) {
                    logger.error("error during search file id",e);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    return STOP;
                }
            }
            if (credentials.get(REFRESH_TOKEN)==null) return AUTHENT;
            if (remoteFileNode != null) {
                credentials.put(REMOTE_FILE,remoteFileNode);
                try {
                    mergeWithRemote(credentials);
                } catch (IOException e) {
                    logger.error("unable to retrieve remote file",e);
                    return AUTHENT;
                }
                if (credentials.get(REFRESH_TOKEN)==null) return AUTHENT;
            }

            return SEND_FILE;
        }

        /**
         * downlaod backup file from drive and merge it with local file
         * @param credentials
         * @throws IOException
         */
        private void mergeWithRemote(Map<String,Object> credentials) throws IOException {
            JsonNode remoteFile = (JsonNode) credentials.get(REMOTE_FILE);
            final String downloadUrlStr = remoteFile.get("downloadUrl").getTextValue();
            final URL downloadUrl = new URL(downloadUrlStr +"&access_token="+credentials.get(ACCESS_TOKEN));

            final HttpURLConnection urlConnection = (HttpURLConnection) downloadUrl.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(false);
            final int responseCode = urlConnection.getResponseCode();
            if (responseCode==200) {
                try(InputStream is =urlConnection.getInputStream()){
                    byte[] remoteFileContent = readAllByte(is);
                    try {
                        Config.getInstance().merge(remoteFileContent);
                        logger.debug("remote file merged");
                    } catch (PasswordMismatchException e) {
                        logger.error("fail to merge : password mismatch");
                    }
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
    },


    // state reached when last SEND_FILE has failed
    RETRY_SEND_FILE {
        @Override
        protected GoogleDriveSyncState _process(Map<String, Object> credentials) {
            while(true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return STOP;
                }
                logger.debug("RE SEND FILE");
                byte[] file ;
                try {
                    file = Config.getInstance().getPasswordStore().getFileContent();
                } catch (IOException e) {
                    logger.error("unable to retrieve store file",e);
                    return MERGE_WITH_REMOTE;
                }
                if (file!=null) {
                    try {
                        if (credentials.get(REMOTE_FILE) == null) {
                            createNewFile(credentials, file);
                        } else {
                            modifyUploadedFile(credentials, file);
                        }
                        break;
                    } catch (IOException e) {
                        //authent lost
                        if (credentials.get(ACCESS_TOKEN)==null) {
                            return AUTHENT;
                        }
                    }
                }
            }
            return WAIT_FOR_CHANGE;
        }
    },

    // SAVE database in google drive
    // -----------------------------
    SEND_FILE {
        @Override
        public GoogleDriveSyncState _process(Map<String, Object> credentials)  {
            logger.debug("SEND FILE");
            byte[] file ;
            try {
                file = Config.getInstance().getPasswordStore().getFileContent();
            } catch (IOException e) {
                logger.error("unable to retrieve store file",e);
                return MERGE_WITH_REMOTE;
            }
            if (file!=null) {
                try {
                    if (credentials.get(REMOTE_FILE) == null) {
                        createNewFile(credentials, file);
                    } else {
                        modifyUploadedFile(credentials, file);
                    }
                } catch (IOException e) {
                    if (credentials.get(ACCESS_TOKEN)!=null) {
                        return RETRY_SEND_FILE;
                    }
                    else {
                        return AUTHENT;
                    }
                }
            }
            return WAIT_FOR_CHANGE;
        }

    },

    // wait for a change on local database
    // -----------------------------------
    WAIT_FOR_CHANGE {
        @Override
        protected GoogleDriveSyncState _process(Map<String, Object> credentials) {
            final Observer obs = (o, arg) -> {
                synchronized (this) {
                    this.notify();
                }
            };
            Config.getInstance().getPasswordStore().addObserver(obs);

            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return STOP;
                }
            }
            Config.getInstance().getPasswordStore().deleteObserver(obs);
            return SEND_FILE;
        }
    },

    // final state
    // -----------
    STOP {
        @Override
        public GoogleDriveSyncState _process(Map<String, Object> credentials)  {
            return null;
        }
    };

    protected abstract GoogleDriveSyncState _process(Map<String, Object> credentials);

    public GoogleDriveSyncState process(Map<String, Object> credentials) {
        logger.debug("inter in process "+toString());
        if (!this.equals(START) && !this.equals(AUTHENT) && credentials.get(ACCESS_TOKEN) == null) {
            return AUTHENT;
        }
        else return _process(credentials);
    }

    /**
     * post content of the string to url as a application/x-www-form-urlencoded"
     * @param toBePosted
     * @param url
     * @return
     * @throws java.io.IOException
     */
    protected String postStringXWWWFormUrlEncodedAndReturnResponseString(String toBePosted,String url) throws IOException, AuthenticationException {
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
                    throw new AuthenticationException();
                }
                else throw new IOException("Error received from serveur code="+responseCode+" message="+json);
            }

        }
    }

    byte[] readAllByte(InputStream is) throws IOException {
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
    String getJsonString(InputStream is) throws IOException {
        return new String(readAllByte(is),"UTF-8");
    }

    private void writeString(OutputStream os,String toWrite) throws IOException {
        os.write(toWrite.getBytes("UTF-8"));
    }
    /**
     * method called to upload the file when this file does not already exist
     * @param file
     * @throws IOException
     */
    protected void createNewFile(Map<String,Object> credentials,byte[] file) throws IOException {
        URL r = new URL("https://www.googleapis.com/upload/drive/v2/files?uploadType=multipart&access_token="+credentials.get(ACCESS_TOKEN));
        uploadFile(credentials,file,r,"POST");
    }

    protected void modifyUploadedFile(Map<String,Object> credentials,byte[] file) throws IOException {
        JsonNode remoteFile = (JsonNode) credentials.get(REMOTE_FILE);
        final String id = remoteFile.get("id").getTextValue();
        URL r = new URL("https://www.googleapis.com/upload/drive/v2/files/"+id+"?uploadType=multipart&access_token="+credentials.get(ACCESS_TOKEN));
        logger.info("will update file id="+id);
        uploadFile(credentials,file, r, "PUT");
    }

    protected void uploadFile(Map<String,Object> credentials,byte[] file,URL r,String httpMethod) throws IOException {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        final String json ="{\"title\":\""+Config.getInstance().getFileName()+"\"}";
        final String part ="jpasswd_part";

        writeString(response, "--" + part + "\r\n");
        writeString(response,"Content-Type: application/json; charset=UTF-8\r\n\r\n");
        response.write(json.getBytes());
        writeString(response, "\r\n--" + part + "\r\n");
        writeString(response,"Content-Type: application/dat\r\n\r\n");
        response.write(file);
        writeString(response,"\r\n--"+part+"--\r\n");

        final HttpURLConnection urlConnection = (HttpURLConnection) r.openConnection();
        urlConnection.setRequestProperty("Content-Type","multipart/related; boundary=\""+part+"\"");
        urlConnection.setRequestProperty("Content-Length",Integer.valueOf(response.size()).toString());
        urlConnection.setRequestMethod(httpMethod);
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);

        try (OutputStream os = urlConnection.getOutputStream()) {
            os.write(response.toByteArray());
        }
        final int responseCode = urlConnection.getResponseCode();
        if (responseCode<300) {
            try(InputStream is =urlConnection.getInputStream()){
                final String jsonString = getJsonString(is);
                logger.debug("file uploaded - response ="+ jsonString);
                ObjectMapper mapper = new ObjectMapper();
                credentials.put(REMOTE_FILE, mapper.readTree(jsonString));
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
                if (responseCode!=401) {
                    final String error = getJsonString(is2);
                    throw new IOException("Error received from serveur code=" + responseCode + " message=" + error);
                }
                else {
                    credentials.remove(ACCESS_TOKEN);
                }
            }
        }
    }

    private final static Logger logger = Logger.getLogger(GoogleDriveSyncState.class);

    private final static String CLIENT_ID     = "73790136390-12qcthfps4lr641tdclq3irroi2pf9dh.apps.googleusercontent.com";
    private final static String CLIENT_SECRET = "2cn_GFJrnHDNyNQHn69o-zHt";
    private final static String REDIRECT_URI  = "http://localhost";

    public final static String ACCESS_TOKEN   = "access_token";
    public final static String REFRESH_TOKEN  = "refresh_token";

    public final static String APPLICATION_MAIN_STAGE = "mainStage";
    public final static String REMOTE_FILE = "remoteFile";

    
}
