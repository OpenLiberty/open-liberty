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
package test.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 */
public class TestFile {

    private static final File tempRoot;

    static {
        File buildDir = null;
        String buildDirProperty = System.getProperty("test.buildDir");
        if (buildDirProperty != null) {
            try {
                buildDir = new File(buildDirProperty);
                if (!buildDir.exists()) {
                    buildDir = null;
                }
            } catch (SecurityException e) {
                // Ignore it and try next location
            }
        }
        if (buildDir == null) {
            // Eclipse and ant both set the project root to user.dir
            String projectRoot = System.getProperty("user.dir");
            buildDir = new File(projectRoot + File.separator + "build");
        }

        if (!buildDir.exists()) {
            throw new IllegalStateException("Unable to find build dir " + buildDir.getAbsolutePath());
        }

        tempRoot = new File(buildDir, "tmp");
    }

    private static void ensureTempRoot() throws IOException {
        if (!tempRoot.mkdirs() && !tempRoot.exists()) {
            throw new IOException("Unable to create testRoot: " + tempRoot);
        }
    }

    public static File createTempDirectory(String name) throws IOException {
        ensureTempRoot();
        File f = new File(tempRoot, name);
        if (f.isFile()) {
            throw new IOException("Location already exists as a file: " + f);
        } else if (!f.mkdirs() && !f.exists()) {
            throw new IOException("Unable to create directory: " + f);
        }
        return f;
    }

    public static File createTempFile(String name, String suffix) throws IOException {
        ensureTempRoot();
        return File.createTempFile(name, suffix, tempRoot);
    }

    public static File createTempFile(String name, String suffix, File dir) throws IOException {
        return File.createTempFile(name, suffix, dir);
    }

    public static void unzip(File sourceZip, File targetDir) throws Exception {
        if (sourceZip == null || !sourceZip.isFile())
            throw new IllegalArgumentException("sourceZip must exist: " + sourceZip);
        if (targetDir == null || !targetDir.isDirectory())
            throw new IllegalArgumentException("targetDir must exist: " + targetDir);

        ZipFile zf = new ZipFile(sourceZip);
        for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
            ZipEntry ze = e.nextElement();
            File targetFile = new File(targetDir, ze.getName());
            if (ze.isDirectory()) {
                System.out.println("Creating directory: " + targetFile);
                targetFile.mkdir();
            } else {
                System.out.println("Unzipping: " + ze.getName() + " into " + targetFile);

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
                    tryToClose(bis);
                    tryToFlush(bos);
                    tryToClose(bos);
                }
            }
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

    public static void recursiveClean(final File fileToRemove) {
        if (fileToRemove == null)
            return;

        if (!fileToRemove.exists())
            return;

        if (fileToRemove.isDirectory()) {
            File[] files = fileToRemove.listFiles();
            for (File file : files) {
                if (file.isDirectory())
                    recursiveClean(file);
                else
                    file.delete();
            }
        }

        fileToRemove.delete();
    }
}
