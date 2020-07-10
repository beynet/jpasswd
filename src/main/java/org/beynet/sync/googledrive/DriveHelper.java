package org.beynet.sync.googledrive;

import org.apache.log4j.Logger;
import org.beynet.model.Config;
import org.beynet.sync.HttpHelper;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

import java.io.IOException;
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
        Optional<byte[]> optBytes = HttpHelper.readBytesFromRessourceWithGET(urlStr,credentials);
        if (optBytes.isPresent()) {
            result = Optional.of(new String(optBytes.get(),"UTF-8"));
        }
        return result;
    }


    public static Optional<byte[]> downloadFile(String fileID,Map<String,Object> credentials) throws IOException{
        final String downloadUrlStr = "https://www.googleapis.com/drive/v3/files/"+fileID+"?alt=media";
        return HttpHelper.readBytesFromRessourceWithGET(downloadUrlStr,credentials);
    }








    private final static Logger logger = Logger.getLogger(DriveHelper.class);
}
