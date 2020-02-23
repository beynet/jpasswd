package org.beynet.sync.googledrive;

import org.apache.log4j.Logger;
import org.beynet.model.Config;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

public class DriveHelper {

    public static Optional<JsonNode> parseNode(final String node) throws IOException {
        Optional<JsonNode> result = Optional.empty();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(node);
        if (actualObj!=null) result = Optional.of(actualObj);

        return result;
    }

    public static Optional<JsonNode> getApplicationFileFromResultList(final String listFiles) throws IOException {
        Optional<JsonNode> result = Optional.empty();

        Optional<JsonNode> optJsonNodeList = parseNode(listFiles);
        if (optJsonNodeList.isPresent()) {
            JsonNode actualObj = optJsonNodeList.get();
            final ArrayNode items = (ArrayNode) actualObj.get("files");
            for (int i = 0; i < items.size(); i++) {
                JsonNode fileNode = items.get(i);
                final JsonNode name = fileNode.get("name");
                final JsonNode id = fileNode.get("id");
                final JsonNode explicitlyTrashed = fileNode.get("explicitlyTrashed"); // skipping file in trash

                if (name != null && id != null && Config.getInstance().getFileName().equals(name.getTextValue()) &&
                        (explicitlyTrashed == null || explicitlyTrashed.getBooleanValue() == false)
                ) {
                    logger.info("file found on server with id=" + id.getTextValue());
                    result = Optional.of(fileNode);
                }
            }
        }

        return result;
    }


    public static HttpURLConnection buildURLConnection(String urlStr, Map<String,Object> credentials) throws IOException {
        URL url ;
        try {
            url =new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException("url should be valid",e);
        }
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setRequestProperty("Authorization","Bearer "+credentials.get(GoogleDriveSyncState.ACCESS_TOKEN));
        return urlConnection;
    }

    public static Optional<JsonNode> getApplicationFileJsonNode(Map<String,Object> credentials) throws IOException {
        Optional<JsonNode> result = Optional.empty();
        String url =  "https://www.googleapis.com/drive/v3/files?fields=files(explicitlyTrashed,id,name)";
        Optional<String> optFileList = readUTF8StringFromResourceWithGET(url,credentials);
        if (optFileList.isPresent()) {
            String fileListStr = optFileList.get();
            logger.debug("file list found "+fileListStr);
            result = getApplicationFileFromResultList(fileListStr);
        }
        return result;
    }

    public static Optional<String> readUTF8StringFromResourceWithGET(String urlStr, Map<String,Object> credentials) throws IOException {
        Optional<String> result = Optional.empty();
        Optional<byte[]> optBytes = readBytesFromRessourceWithGET(urlStr,credentials);
        if (optBytes.isPresent()) {
            result = Optional.of(new String(optBytes.get(),"UTF-8"));
        }
        return result;
    }


    public static Optional<byte[]> downloadFile(String fileID,Map<String,Object> credentials) throws IOException{
        final String downloadUrlStr = "https://www.googleapis.com/drive/v3/files/"+fileID+"?alt=media";
        return DriveHelper.readBytesFromRessourceWithGET(downloadUrlStr,credentials);
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
                if (responseCode!=401) {
                    final String json = HttpHelper.getJsonString(response);
                    throw new IOException("Error received from serveur code=" + responseCode + " message=" + json);
                }
                else {
                    credentials.remove(GoogleDriveSyncState.ACCESS_TOKEN);
                }
            }
        }

        return result;
    }



    private final static Logger logger = Logger.getLogger(DriveHelper.class);
}
