/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.hpel;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import com.ibm.wsspi.logging.TextFileOutputStreamFactory;

/**
 *
 */
public class CommonUtils {
    // Extension used for HPEL files.
    public final static String WBL_EXT = ".wbl";
    // Assumed name of the server.
    public final static String SERVER_NAME = "server1";
    // Assumed product info (don't have to match real version)
    public final static String PRODUCT_INFO = "Liberty in Unittest";
    // It should be usable in both this project and build.image project runs.
    public final static File LOG_DIR = new File("../com.ibm.ws.logging.hpel/build/logs");
    // Unittest logs should have different location than server logs
    public final static File UNITTEST_LOGS = new File("../com.ibm.ws.logging.hpel/build/unittest-logs");
    // File line of text files created by LogProvider
    public final static String STREAM_TAG = "This is text file generated with the help of TextFileOutputStreamFactory";

    // Delete directories recursively.
    public static void delDir(File dir) {
        if (dir != null && dir.exists()) {
            if (dir.isDirectory()) {
                for (File file : dir.listFiles()) {
                    delDir(file);
                }
            }
            assertTrue("Failed to delete file " + dir.getAbsolutePath(), dir.delete());
        }
    }

    public static int listWbls(File dir, List<? super File> files) {
        int count = 0;
        if (dir != null && dir.exists()) {
            if (dir.isDirectory()) {
                for (File file : dir.listFiles()) {
                    count += listWbls(file, files);
                }
            } else {
                if (dir.getName().endsWith(WBL_EXT)) {
                    count++;
                    if (files != null) {
                        files.add(dir);
                    }
                }
            }
        }
        return count;
    }

    public static boolean textFileContainsLine(File file, String match) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                if (line.contains(match)) {
                    return true;
                }
            }
        } finally {
            br.close();
        }
        return false;
    }

    public final static TextFileOutputStreamFactory TEXT_FACTORY = new TextFileOutputStreamFactory() {
        private FileOutputStream putTestTag(FileOutputStream stream) {
            new PrintStream(stream).println(STREAM_TAG);
            return stream;
        }

        @Override
        public FileOutputStream createOutputStream(String name, boolean append) throws IOException {
            return putTestTag(new FileOutputStream(name, append));
        }

        @Override
        public FileOutputStream createOutputStream(String name) throws IOException {
            return putTestTag(new FileOutputStream(name));
        }

        @Override
        public FileOutputStream createOutputStream(File file, boolean append) throws IOException {
            return putTestTag(new FileOutputStream(file, append));
        }

        @Override
        public FileOutputStream createOutputStream(File file) throws IOException {
            return putTestTag(new FileOutputStream(file));
        }
    };

}
