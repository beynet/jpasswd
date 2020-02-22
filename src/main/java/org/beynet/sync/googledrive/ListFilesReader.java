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

public class ListFilesReader {
    Optional<JsonNode> getFileFromListV2(final String listFiles) throws IOException {
        Optional<JsonNode> result = Optional.empty();
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
                result = Optional.of(fileNode);
            }
        }

        return result;
    }

    public static Optional<JsonNode> parseNode(final String node) throws IOException {
        Optional<JsonNode> result = Optional.empty();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(node);
        if (actualObj!=null) result = Optional.of(actualObj);

        return result;
    }

    public static Optional<JsonNode> getFileFromListV3(final String listFiles) throws IOException {
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

    public static Optional<String> readUTF8String(String urlStr, Map<String,Object> credentials) throws IOException {
        Optional<String> result = Optional.empty();
        Optional<byte[]> optBytes = readBytes(urlStr,credentials);
        if (optBytes.isPresent()) {
            result = Optional.of(new String(optBytes.get(),"UTF-8"));
        }
        return result;
    }

    public static Optional<byte[]> readBytes(String urlStr, Map<String,Object> credentials) throws IOException {
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
                result = Optional.of(readAllByte(is));
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
                    credentials.remove(GoogleDriveSyncState.ACCESS_TOKEN);
                }
            }
        }

        return result;
    }


    /**
     * read a string from steam
     * @param is
     * @return
     * @throws IOException
     */
    static String getJsonString(InputStream is) throws IOException {
        return new String(readAllByte(is),"UTF-8");
    }


    static byte[] readAllByte(InputStream is) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (true) {
            final int read = is.read(buffer);
            if (read==-1) break;
            result.write(buffer,0,read);
        }
        return result.toByteArray();
    }

    private final static Logger logger = Logger.getLogger(ListFilesReader.class);
}
