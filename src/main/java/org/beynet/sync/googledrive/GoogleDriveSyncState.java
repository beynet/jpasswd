package org.beynet.sync.googledrive;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.beynet.exceptions.PasswordMismatchException;
import org.beynet.gui.GoogleDriveAuthent;
import org.beynet.model.Config;
import org.beynet.model.Observer;
import org.beynet.sync.AuthenticationException;
import org.beynet.sync.HttpHelper;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Optional;

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
            if (refreshToken!=null) credentials.put(HttpHelper.REFRESH_TOKEN,refreshToken);
            else credentials.remove(HttpHelper.REFRESH_TOKEN);
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
                if (credentials.get(HttpHelper.REFRESH_TOKEN) == null) {
                    logger.debug("no tokens first authent");
                    this.code = null;
                    callAuthent((Stage) credentials.get(APPLICATION_MAIN_STAGE));
                    try {
                        retrieveAccessTokenAndRefreshTokenFromCode(credentials);
                        break;
                    } catch (Exception e) {
                        credentials.remove(HttpHelper.REFRESH_TOKEN);
                    }
                } else if (credentials.get(HttpHelper.ACCESS_TOKEN) == null) {
                    logger.debug("using refresh token found");
                    try {
                        retrieveAccessTokenFromRefreshTokenCode(credentials);
                        break;
                    }
                    catch(AuthenticationException e) {
                        credentials.remove(HttpHelper.REFRESH_TOKEN);
                    }
                    catch (IOException e) {
                        logger.error("error - unable to obtain access token from refresh token", e);
                        credentials.remove(HttpHelper.REFRESH_TOKEN);
                    }
                }
            }
            if (credentials.get(HttpHelper.REFRESH_TOKEN)!=null) Config.getInstance().updateGoogleDriveRefreshToken((String) credentials.get(HttpHelper.REFRESH_TOKEN));
            return MERGE_WITH_REMOTE;
        }

        void retrieveAccessTokenFromRefreshTokenCode(Map<String,Object> credentials) throws IOException,AuthenticationException {
            String query="refresh_token=$refreshToken&client_id=$client_id&grant_type=refresh_token&client_secret=$client_secret";
            query=query.replace("$refreshToken", URLEncoder.encode((String)credentials.get(HttpHelper.REFRESH_TOKEN), "UTF-8"));
            query=query.replace("$client_id",URLEncoder.encode(CLIENT_ID,"UTF-8"));
            query=query.replace("$client_secret",URLEncoder.encode(CLIENT_SECRET,"UTF-8"));

            getAccessAndRefreshTokensFromJson(credentials,HttpHelper.postStringXWWWFormUrlEncodedAndReturnResponseString(query, "https://accounts.google.com/o/oauth2/token"));
        }

        void retrieveAccessTokenAndRefreshTokenFromCode(Map<String,Object> credentials) throws IOException,AuthenticationException {
            String query="code=$code&client_id=$client_id&grant_type=authorization_code&client_secret=$client_secret&redirect_uri=$redirect_uri";
            query=query.replace("$code",URLEncoder.encode(code,"UTF-8"));
            query=query.replace("$client_id",URLEncoder.encode(CLIENT_ID,"UTF-8"));
            query=query.replace("$client_secret",URLEncoder.encode(CLIENT_SECRET,"UTF-8"));
            query=query.replace("$redirect_uri",URLEncoder.encode(REDIRECT_URI,"UTF-8"));

            getAccessAndRefreshTokensFromJson(credentials,HttpHelper.postStringXWWWFormUrlEncodedAndReturnResponseString(query,"https://accounts.google.com/o/oauth2/token"));
            logger.info("logged in token="+credentials.get(HttpHelper.ACCESS_TOKEN)+" refresh token="+credentials.get(HttpHelper.REFRESH_TOKEN));
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
            credentials.put(HttpHelper.ACCESS_TOKEN,actualObj.get(HttpHelper.ACCESS_TOKEN)!=null?actualObj.get(HttpHelper.ACCESS_TOKEN).getTextValue():null);
            if (actualObj.get(HttpHelper.REFRESH_TOKEN)!=null) credentials.put(HttpHelper.REFRESH_TOKEN,actualObj.get(HttpHelper.REFRESH_TOKEN).getTextValue());
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
        Optional<JsonNode> searchApplicationFileId(Map<String,Object> credentials) throws IOException {
            logger.debug("search expected file on server");
            return DriveHelper.getApplicationFileJsonNode(credentials);
        }
        @Override
        public GoogleDriveSyncState _process(Map<String, Object> credentials) {
            logger.debug("MERGE");
            Optional<JsonNode> remoteFileNode;
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
            if (credentials.get(HttpHelper.REFRESH_TOKEN)==null) return AUTHENT;

            if (remoteFileNode.isPresent()) {
                credentials.put(REMOTE_FILE,remoteFileNode.get());
                try {
                    mergeWithRemote(credentials);
                } catch (IOException e) {
                    logger.error("unable to retrieve remote file",e);
                    return AUTHENT;
                }
                if (credentials.get(HttpHelper.REFRESH_TOKEN)==null) return AUTHENT;
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
            Optional<byte[]> optRemoteFileContent = DriveHelper.downloadFile(remoteFile.get("id").getTextValue(),credentials);
            byte[] remoteFileContent = optRemoteFileContent.get();
            try {
                Config.getInstance().merge(remoteFileContent);
                logger.debug("remote file merged");
            } catch (PasswordMismatchException e) {
                logger.error("fail to merge : password mismatch");
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
                        if (credentials.get(HttpHelper.ACCESS_TOKEN)==null) {
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
                    logger.error("ioexception ",e);
                    if (credentials.get(HttpHelper.ACCESS_TOKEN)!=null) {
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
            Config.getInstance().getPasswordStore().deleteObserver​(obs);
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
        logger.debug("enter in process "+toString());
        if (!this.equals(START) && !this.equals(AUTHENT) && credentials.get(HttpHelper.ACCESS_TOKEN) == null) {
            return AUTHENT;
        }
        else return _process(credentials);
    }



    private void writeString(OutputStream os,String toWrite) throws IOException {
        os.write(toWrite.getBytes("UTF-8"));
    }


    protected void createNewFile(Map<String,Object> credentials,byte[] file) throws IOException {
        String urlStr = new String("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart");
        uploadFile(credentials,file,urlStr,"POST");
    }


    protected void modifyUploadedFile(Map<String,Object> credentials,byte[] file) throws IOException {
        JsonNode remoteFile = (JsonNode) credentials.get(REMOTE_FILE);
        final String id = remoteFile.get("id").getTextValue();
        String url = "https://www.googleapis.com/upload/drive/v3/files/"+id+"?uploadType=media";
        logger.info("will update file id="+id+" url="+url);
        HttpURLConnection httpURLConnection = HttpHelper.buildURLConnection(url, credentials);
        httpURLConnection.setRequestProperty("Content-Type","application/dat");
        httpURLConnection.setRequestProperty("Content-Length",""+file.length);
        httpURLConnection.setRequestProperty("X-HTTP-Method-Override", "PATCH"); // force PATCH method
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setDoOutput(true);
        try (OutputStream os = httpURLConnection.getOutputStream()) {
            os.write(file);
        }
        Optional<byte[]> bytes = HttpHelper.readBytesResponse(httpURLConnection, credentials);
        if (bytes.isPresent()) {
            final String jsonString = new String(bytes.get(),"UTF-8");
            logger.debug("file uploaded - response ="+ jsonString);
            ObjectMapper mapper = new ObjectMapper();
            credentials.put(REMOTE_FILE, mapper.readTree(jsonString));
        }
    }


    protected void uploadFile(Map<String,Object> credentials,byte[] file,String urlStr,String httpMethod) throws IOException {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        final String json ="{\"name\":\""+Config.getInstance().getFileName()+"\"}";
        final String part ="jpasswd_part";

        writeString(response, "--" + part + "\r\n");
        writeString(response,"Content-Type: application/json; charset=UTF-8\r\n\r\n");
        response.write(json.getBytes());
        writeString(response, "\r\n--" + part + "\r\n");
        writeString(response,"Content-Type: application/dat\r\n\r\n");
        response.write(file);
        writeString(response,"\r\n--"+part+"--\r\n");

        final HttpURLConnection urlConnection = HttpHelper.buildURLConnection(urlStr,credentials);
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
                final String jsonString = HttpHelper.getJsonString(is);
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
                    final String error = HttpHelper.getJsonString(is2);
                    throw new IOException("Error received from serveur code=" + responseCode + " message=" + error);
                }
                else {
                    credentials.remove(HttpHelper.ACCESS_TOKEN);
                }
            }
        }
    }

    private final static Logger logger = Logger.getLogger(GoogleDriveSyncState.class);

    private final static String CLIENT_ID     = "852683704707-03hsoogpu5i2g5oua3ipd706cuj76dsc.apps.googleusercontent.com";
    private final static String CLIENT_SECRET = "PmBzxHcXpmTpyK3xMM5g_Xw_";
    private final static String REDIRECT_URI  = "http://localhost";



    public final static String APPLICATION_MAIN_STAGE = "mainStage";
    public final static String REMOTE_FILE = "remoteFile";

    
}
