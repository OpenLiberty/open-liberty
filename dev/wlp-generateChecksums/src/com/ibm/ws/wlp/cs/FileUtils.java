package com.ibm.ws.wlp.cs;

import java.io.Closeable;
import java.io.IOException;

public class FileUtils {
    public static boolean tryToClose(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
                return true;
            } catch (IOException e) {
            }
        }
        return false;
    }
}
