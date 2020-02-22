package org.beynet.gui;

import org.beynet.model.store.RootTest;
import org.beynet.sync.googledrive.ListFilesReader;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by beynet on 23/10/14.
 */
public class GoogleDriveAuthentTest extends RootTest {

    @Test
    public void testCodeRetrieval() {
        String expected ="4/wwHZ1YxHxR0T5fvyXjPxPKWn7pQRpSOXreI_ukOzTXY8yzecJ3IEs7DxrW66GDt-_VpyTAVN90Jbhj9bVIfXCYg";
        String uri = "http://localhost/?code=4%2FwwHZ1YxHxR0T5fvyXjPxPKWn7pQRpSOXreI_ukOzTXY8yzecJ3IEs7DxrW66GDt-_VpyTAVN90Jbhj9bVIfXCYg&scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fdrive.file";
        assertThat(GoogleDriveAuthent.getCodeFromURL(uri), is(expected));
    }

    String listFilesResultV3 = "{\n" +
            " \"kind\": \"drive#fileList\",\n" +
            " \"incompleteSearch\": false,\n" +
            " \"files\": [\n" +
            "  {\n" +
            "   \"kind\": \"drive#file\",\n" +
            "   \"id\": \"1gxxEdQ9uHgvc3UKxKJoD8tMmAPhxhhuj\",\n" +
            "   \"name\": \"jptest.dat\",\n" +
            "   \"mimeType\": \"application/dat\"\n" +
            "  }\n" +
            " ]\n" +
            "}\n";
    @Test
    public void readFileId() throws Exception {
        Optional<JsonNode> fileFromListV3 = ListFilesReader.getFileFromListV3(listFilesResultV3);
        assertThat(fileFromListV3.isPresent(),is(true));
        JsonNode node = fileFromListV3.get();
        assertThat(node.get("id").getTextValue(),is("1gxxEdQ9uHgvc3UKxKJoD8tMmAPhxhhuj"));
    }


}
