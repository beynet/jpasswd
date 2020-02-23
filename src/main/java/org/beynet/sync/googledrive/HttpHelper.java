package org.beynet.sync.googledrive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
    static String getJsonString(InputStream is) throws IOException {
        return new String(readAllByte(is),"UTF-8");
    }
}
