/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 */
public class TestUtils {
    private static final String CLASS_NAME = TestUtils.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
    /**
     * Test data directory: note the space! always test paths with spaces. Dratted
     * windows.
     */
    public static final String TEST_DATA_DIR = "../com.ibm.ws.kernel.filemonitor_test/unittest/test data";
    public static final File TEST_DATA = new File(TEST_DATA_DIR);

    public static void unzip(File sourceZip, File targetDir) throws Exception {
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            //we need to do an exists check again to close timing window
            if (!targetDir.mkdirs() && !targetDir.exists()) {
                throw new IllegalArgumentException("could not create target dir: " + targetDir);
            }
        }
        if (sourceZip == null || !sourceZip.isFile())
            throw new IllegalArgumentException("sourceZip must exist: " + sourceZip);
        if (targetDir == null || !targetDir.isDirectory())
            throw new IllegalArgumentException("targetDir must exist: " + targetDir);

        LOG.logp(Level.INFO, CLASS_NAME, "unzip", "Unzipping file: " + sourceZip + ", size: " + sourceZip.length());
        ZipFile zf = new ZipFile(sourceZip);
        try {
            for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
                ZipEntry ze = e.nextElement();
                File targetFile = new File(targetDir, ze.getName());
                if (ze.isDirectory()) {
                    LOG.logp(Level.INFO, CLASS_NAME, "unzip", "Creating directory: " + targetFile);
                    targetFile.mkdirs();
                } else {
                    if (!targetFile.getParentFile().mkdirs() && !targetFile.getParentFile().exists()) {
                        //we can't find or create the required file. Log error message.
                        LOG.logp(Level.SEVERE, CLASS_NAME, "unzip", "Test utils unable to create the location " + targetFile.getParentFile().getAbsolutePath());
                    }
                    LOG.logp(Level.INFO, CLASS_NAME, "unzip", "Unzipping: " + ze.getName() + " (" + ze.getSize() + " bytes) into " + targetFile);

                    byte[] buffer = new byte[2048];
                    BufferedInputStream bis = null;
                    BufferedOutputStream bos = null;

                    try {
                        bis = new BufferedInputStream(zf.getInputStream(ze));
                        bos = new BufferedOutputStream(new FileOutputStream(targetFile), buffer.length);
                        int size;
                        while ((size = bis.read(buffer, 0, buffer.length)) != -1) {
                            bos.write(buffer, 0, size);
                        }
                    } finally {
                        TestUtils.tryToClose(bis);
                        TestUtils.tryToFlush(bos);
                        TestUtils.tryToClose(bos);
                    }
                }
            }
        } finally {
            tryToClose(zf);
        }
    }

    public static final void tryToFlush(Flushable stream) {
        if (stream == null)
            return;
        try {
            stream.flush();
        } catch (IOException ioe) {
        }
    }

    public static final void tryToClose(Closeable stream) {
        if (stream == null)
            return;
        try {
            stream.close();
        } catch (IOException ioe) {
        }
    }

    public static final void tryToClose(ZipFile zip) {
        if (zip == null)
            return;
        try {
            zip.close();
        } catch (IOException ioe) {
        }
    }
}
