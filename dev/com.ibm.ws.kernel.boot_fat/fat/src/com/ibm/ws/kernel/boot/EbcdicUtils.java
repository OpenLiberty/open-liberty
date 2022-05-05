/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public class EbcdicUtils {

    private static final Class<?> c = EbcdicUtils.class;
    public static String CLASS_NAME = "EbcdicUtils";

    /**
     * The given fileName is copied from the files/ dir to ${server.config.dir}
     * and converted to EBCDIC.
     *
     * @returns the fileName (absolute path) of the ebcdic version of the file.
     */
    public static String convertToEbcdic(LibertyServer server, String fileName) throws Exception {

        final String METHOD_NAME = "convertToEbcdic";
        // Copy to server root so that the jbatch utility can find it
        //server.copyFileToLibertyServerRoot(fileName);

        String fileNameEbcdic = fileName + ".ebcdic";

        Log.info(c, METHOD_NAME, "orignal: " + fileName);
        Log.info(c, METHOD_NAME, "ebcdic: " + fileNameEbcdic);

        File originalFile = new File(fileName);
        File ebcdicFile = new File(fileNameEbcdic);

        // Convert to ebcdic.
        File newFile = IOUtils.convertFile(fileName,
                                           Charset.forName("UTF-8"),
                                           fileNameEbcdic,
                                           Charset.forName("IBM-1047"));
        originalFile.delete();
        ebcdicFile.renameTo(originalFile);
        if (!originalFile.setExecutable(true)) {
            Log.info(c, METHOD_NAME, "Failed to mark file as executable: ");
        }

        Log.info(c, METHOD_NAME, "ebcdic name: " + newFile.getAbsolutePath());
        return fileNameEbcdic;
    }

    public static void copyFileUsingChannel(File source, File dest) throws IOException {
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;
        try {
            sourceChannel = new FileInputStream(source).getChannel();
            destChannel = new FileOutputStream(dest).getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } finally {
            sourceChannel.close();
            destChannel.close();
        }
    }

}

/**
 *
 * Could have imported apache.commons.lang3, but only needed a few methods.
 */
class StringUtils {

    /**
     * @return the given strs joined on the given delim.
     */
    public static String join(Collection<String> strs, String delim) {
        StringBuffer retMe = new StringBuffer();
        String d = "";
        for (String str : ((strs != null) ? strs : new ArrayList<String>())) {
            retMe.append(d).append(str);
            d = delim;
        }
        return retMe.toString();
    }

    /**
     * @return true if the string is null or "" or nothing but whitespace.
     */
    public static boolean isEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }
}

/**
 * IO utilities.
 */
class IOUtils {

    /**
     * Copy the given InputStream to the given OutputStream.
     *
     * Note: the InputStream is closed when the copy is complete. The OutputStream
     * is left open.
     */
    public static void copyStream(InputStream from, OutputStream to) throws IOException {
        byte buffer[] = new byte[2048];
        int bytesRead;
        while ((bytesRead = from.read(buffer)) != -1) {
            to.write(buffer, 0, bytesRead);
        }
        from.close();
    }

    /**
     * Copy the given Reader to the given Writer.
     *
     * This method is basically the same as copyStream; however Reader and Writer
     * objects are cognizant of character encoding, whereas InputStream and OutputStreams
     * objects deal only with bytes.
     *
     * Note: the Reader is closed when the copy is complete. The Writer
     * is left open. The Write is flushed when the copy is complete.
     */
    public static void copyReader(Reader from, Writer to) throws IOException {

        char buffer[] = new char[2048];
        int charsRead;
        while ((charsRead = from.read(buffer)) != -1) {
            to.write(buffer, 0, charsRead);
        }
        from.close();
        to.flush();
    }

    /**
     * Copy and convert from the given file and charset to the given file and charset.
     *
     * @return File(toFileName)
     */
    public static File convertFile(String fromFileName,
                                   Charset fromCharset,
                                   String toFileName,
                                   Charset toCharset) throws IOException {

        Reader reader = new InputStreamReader(new FileInputStream(fromFileName), fromCharset);
        Writer writer = new OutputStreamWriter(new FileOutputStream(toFileName), toCharset);

        copyReader(reader, writer);
        writer.close();

        return new File(toFileName);
    }

}
