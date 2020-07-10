package org.beynet.sync.onedrive;

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

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

public enum OneDriveSyncState {
    START{
        @Override
        protected OneDriveSyncState _process(Map<String, Object> credentials) {
            String refreshToken = Config.getInstance().getOneDriveRefreshToken();
            if (refreshToken!=null) credentials.put(HttpHelper.REFRESH_TOKEN,refreshToken);
            else credentials.remove(HttpHelper.REFRESH_TOKEN);
            return AUTHENT;
        }
    },
    AUTHENT{
        @Override
        protected OneDriveSyncState _process(Map<String, Object> credentials) {
            logger.debug("AUTHENT");
            while (true) {
                // no refresh token - first authent we will connect
                // to google oauth systems using embedded web browser
                // --------------------------------------------------
                if (credentials.get(HttpHelper.REFRESH_TOKEN) == null) {
                    logger.debug("no tokens first authent");
                    this.code = null;
                    try {
                        callAuthent((Stage) credentials.get(APPLICATION_MAIN_STAGE));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return STOP;
                    }
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
            if (credentials.get(HttpHelper.REFRESH_TOKEN)!=null) Config.getInstance().updateOneDriveRefreshToken((String) credentials.get(HttpHelper.REFRESH_TOKEN));
            return MERGE_WITH_REMOTE;
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

        void retrieveAccessTokenFromRefreshTokenCode(Map<String,Object> credentials) throws IOException,AuthenticationException {
            String query = "client_id="+CLIENT_ID+
                    "&scope="+CLIENT_SCOPE+
                    "&refresh_token="+credentials.get(HttpHelper.REFRESH_TOKEN)+
                    "&redirect_uri="+REDIRECT_URI+
                    "&grant_type=refresh_token";

            getAccessAndRefreshTokensFromJson(credentials,HttpHelper.postStringXWWWFormUrlEncodedAndReturnResponseString(query,"https://login.microsoftonline.com/common/oauth2/v2.0/token"));
            logger.info("logged in token="+credentials.get(HttpHelper.ACCESS_TOKEN)+" refresh token="+credentials.get(HttpHelper.REFRESH_TOKEN));
        }

        private void retrieveAccessTokenAndRefreshTokenFromCode(Map<String,Object> credentials) throws IOException,AuthenticationException {
            String query = "client_id="+CLIENT_ID+
                    "&scope="+CLIENT_SCOPE+
                    "&code="+code+
                    "&redirect_uri="+REDIRECT_URI+
                    "&grant_type=authorization_code";

            getAccessAndRefreshTokensFromJson(credentials,HttpHelper.postStringXWWWFormUrlEncodedAndReturnResponseString(query,"https://login.microsoftonline.com/common/oauth2/v2.0/token"));
            logger.info("logged in token="+credentials.get(HttpHelper.ACCESS_TOKEN)+" refresh token="+credentials.get(HttpHelper.REFRESH_TOKEN));
        }
        private void callAuthent(Stage applicationMainStage) throws InterruptedException{
            String authentURI ="https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id="+CLIENT_ID+"&grant_type=authorization_code&code="+CLIENT_CODE+"&redirect_uri=http%3A%2F%2Flocalhost%3A9091%2Fjpasswd&response_type=code&response_mode=query&scope="+CLIENT_SCOPE+"&state=12345";
            Platform.runLater(() -> {
                final GoogleDriveAuthent googleDriveAuthent = new GoogleDriveAuthent(applicationMainStage, authentURI, this, c -> code = c);
                googleDriveAuthent.show();
            });
            logger.debug("wait until authent is done");
            synchronized (this) {
                this.wait();
            }
        }
        String code ;

    },
    MERGE_WITH_REMOTE{

        @Override
        public OneDriveSyncState _process(Map<String, Object> credentials) {
            logger.debug("MERGE");

            if (credentials.get(HttpHelper.REFRESH_TOKEN)==null) return AUTHENT;
            try {
                mergeWithRemote(credentials);
            } catch (IOException e) {
                logger.error("unable to retrieve remote file",e);
                return AUTHENT;
            }
            if (credentials.get(HttpHelper.REFRESH_TOKEN)==null) return AUTHENT;


            return SEND_FILE;
        }

        /**
         * downlaod backup file from drive and merge it with local file
         * @param credentials
         * @throws IOException
         */
        private void mergeWithRemote(Map<String,Object> credentials) throws IOException {
            Optional<byte[]> result = HttpHelper.readBytesFromRessourceWithGET(FILE_URI.toString(),credentials);
            try {
                if (result.isPresent()) Config.getInstance().merge(result.get());
            } catch (PasswordMismatchException e) {
                logger.error("fail to merge : password mismatch");
            }
        }
    },

    // SAVE database in google drive
    // -----------------------------
    SEND_FILE {
        @Override
        public OneDriveSyncState _process(Map<String, Object> credentials)  {
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
                    uploadFile(credentials, file);
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


    RETRY_SEND_FILE{
        @Override
        protected OneDriveSyncState _process(Map<String, Object> credentials) {
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
                        uploadFile(credentials,file);
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
    WAIT_FOR_CHANGE{
        @Override
        protected OneDriveSyncState _process(Map<String, Object> credentials) {
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
    STOP{
        @Override
        protected OneDriveSyncState _process(Map<String, Object> credentials) {
            return null;
        }
    }

    ;


    protected void uploadFile(Map<String,Object> credentials,byte[] file) throws IOException {
        logger.info("will update file  url="+FILE_URI.toString());
        HttpURLConnection httpURLConnection = HttpHelper.buildURLConnection(FILE_URI.toString(), credentials);
        httpURLConnection.setRequestProperty("Content-Type","application/octet-stream");
        httpURLConnection.setRequestProperty("Accept","application/json");
        httpURLConnection.setRequestMethod("PUT");
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setDoInput(true);
        try (OutputStream os = httpURLConnection.getOutputStream()) {
            os.write(file);
        }
        Optional<byte[]> bytes = HttpHelper.readBytesResponse(httpURLConnection, credentials);
        if (bytes.isPresent()) {
            final String jsonString = new String(bytes.get(),"UTF-8");
            logger.debug("file uploaded - response ="+ jsonString);
        }
    }

    public OneDriveSyncState process(Map<String, Object> credentials) {
        logger.debug("enter in process "+toString());
        if (!this.equals(START) && !this.equals(AUTHENT) && credentials.get(HttpHelper.ACCESS_TOKEN) == null ) {
            return AUTHENT;
        }
        else return _process(credentials);
    }

    protected abstract OneDriveSyncState _process(Map<String, Object> credentials);

    private final static Logger logger = Logger.getLogger(OneDriveSyncState.class);
    public final static String APPLICATION_MAIN_STAGE = "mainStage";
    private final static String CLIENT_ID="92f2b06c-cde4-47e1-87d0-731726fec933";
    private final static String CLIENT_CODE="A19iZqU_iyGtS~jxTQuGX1~J4~41Hw-Dom";
    private final static String CLIENT_SCOPE="offline_access%20files.readwrite.appfolder%20user.read";


    private final static String REDIRECT_URI  = "http%3A%2F%2Flocalhost%3A9091%2Fjpasswd";


    private final static URI FILE_URI = URI.create("https://graph.microsoft.com/v1.0/drive/special/approot:/jpasswd.dat:/content");

}
