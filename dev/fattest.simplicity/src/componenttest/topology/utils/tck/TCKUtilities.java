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
package componenttest.topology.utils.tck;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.simplicity.log.Log;

public class TCKUtilities {
    private static final Class<TCKUtilities> c = TCKUtilities.class;

    public static String generateSHA256(File file) {
        String sha256 = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            sha256 = getFileChecksum(md, file);
        } catch (IOException e) {
            Log.error(c, "generateSHA256", e, "Could not read file: " + file);
            sha256 = "UNKNOWN";
        } catch (NoSuchAlgorithmException e) {
            Log.error(c, "generateSHA256", e, "Could not generate SHA-256 for: " + file);
            sha256 = "UNKNOWN";
        }

        return sha256;
    }

    public static String generateSHA1(File file) {
        String sha256 = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            sha256 = getFileChecksum(md, file);
        } catch (IOException e) {
            Log.error(c, "generateSHA1", e, "Could not read file: " + file);
            sha256 = "UNKNOWN";
        } catch (NoSuchAlgorithmException e) {
            Log.error(c, "generateSHA1", e, "Could not generate SHA-1 for: " + file);
            sha256 = "UNKNOWN";
        }

        return sha256;
    }

    public static String getFileChecksum(MessageDigest digest, File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[1024];
            int bytesCount = 0;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            } ;
        }

        byte[] bytes = digest.digest();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    /**
     * This method will print a String reliably to the 'standard' Standard.out
     * (i.e. the developers screen when running locally)
     *
     * @param msg
     */
    public static void printStdOutAndScreenIfLocal(String msg) {
        // If running locally print to screen and stdout if different else print to 'stdout' only
        if (Boolean.valueOf(System.getProperty("fat.test.localrun"))) {
            // Developers laptop FAT
            PrintStream screen = new PrintStream(new FileOutputStream(FileDescriptor.out));
            screen.println(msg);
            if (!System.out.equals(screen)) {
                System.out.println(msg);
            }
        } else {
            // Build engine FAT
            System.out.println(msg);
        }
    }

    public static boolean waitForProcess(Process process, long timeoutMS) throws InterruptedException {
        boolean timeout = false;
        if (timeoutMS > -1) {
            timeout = !process.waitFor(timeoutMS, TimeUnit.MILLISECONDS); // Requires Java 8+
            if (timeout) { //timeout!
                process.destroyForcibly(); //kill the process
                process.waitFor();
            }
        }
        return timeout;
    }

    public static void writeStringsToFile(String[] strings, File outputFile) {
        try (Writer writer = new FileWriter(outputFile)) {
            for (String line : strings) {
                writer.write(line);
                writer.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.error(c, "writeStringsToFile", e);
        }
    }
}
