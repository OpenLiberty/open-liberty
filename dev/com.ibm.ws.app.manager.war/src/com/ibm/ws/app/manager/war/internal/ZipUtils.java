/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.war.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class ZipUtils {
    private static final TraceComponent tc = Tr.register(ZipUtils.class);
    private static final String WAR_EXTENSION = ".war";

    public void recursiveDelete(File f) throws IOException {
        File[] subFiles = f.listFiles();
        if (subFiles != null) {
            for (File c : subFiles) {
                recursiveDelete(c);
            }
        }
        if (!f.delete())
            throw new IOException("Failed to delete file " + f.getName());
    }

    /**
     * Unzip utility. Assumes that the source zip and target directory are valid.
     *
     * @param sourceZip
     * @param targetDir
     * @return
     * @throws IOException
     * @throws ZipException
     */
    public void unzip(File sourceZip, File targetDir) throws ZipException, IOException {
        if (!sourceZip.exists() || !targetDir.exists())
            return;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Unzipping file: " + sourceZip);
        }

        List<File> warFiles = new ArrayList<File>();
        ZipFile zf = new ZipFile(sourceZip);
        try {
            for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
                ZipEntry ze = e.nextElement();
                String fileName = ze.getName();
                if (fileName.endsWith(WAR_EXTENSION)) {
                    File warFile = new File(targetDir, fileName);
                    if (!validateFile(targetDir, warFile)) {
                        Tr.error(tc, "error.file.outside.archive", fileName, sourceZip.getName());
                        continue;
                    }
                    warFiles.add(warFile);
                    fileName = fileName + ".tmp";
                }
                File targetFile = new File(targetDir, fileName);
                if (!validateFile(targetDir, targetFile)) {
                    Tr.error(tc, "error.file.outside.archive", fileName, sourceZip.getName());
                    continue;
                }
                if (ze.isDirectory()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Creating directory: " + targetFile);
                    }
                    if (!targetFile.exists() && !targetFile.mkdirs()) {
                        throw new IOException("Could not create directory " + targetFile.getAbsolutePath());
                    }
                } else {
                    if (!targetFile.getParentFile().mkdirs() && !targetFile.getParentFile().exists()) {
                        //we can't find or create the required file. Log error message.
                        throw new IOException("Could not create directory " + targetFile.getParentFile().getAbsolutePath());
                    }

                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unzipping: " + ze.getName() + " (" + ze.getSize() + " bytes) into " + targetFile);
                    }

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
                        if (bis != null)
                            bis.close();

                        if (bos != null) {
                            bos.flush();
                            bos.close();
                        }

                    }

                    long modified = ze.getTime();
                    if (modified < 0) {
                        modified = sourceZip.lastModified();
                    }
                    targetFile.setLastModified(modified);
                }
            }
        } finally {
            if (zf != null)
                zf.close();
        }

        for (File warDirectory : warFiles) {
            // Create the directory for the expanded war
            if (!warDirectory.exists() && !warDirectory.mkdirs()) {
                throw new IOException("Could not create directory " + warDirectory.getAbsolutePath());
            }

            File warFile = new File(warDirectory.getParentFile(), warDirectory.getName() + ".tmp");
            // Expand the WAR
            unzip(warFile, warDirectory);

            // Delete the temporary file
            if (!warFile.delete()) {
                // warning? This is unlikely to cause any problems
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Could not delete temporary war file during expansion");
                }
            }
        }
    }

    /**
     * Validates that a WAR file does not point to files outside the archive (eg ../../../someFile.txt)
     * Return true if valid, false if not
     */
    private boolean validateFile(File targetDirectory, File targetFile) throws IOException, ZipException {
        return targetFile.getCanonicalPath().startsWith(targetDirectory.getCanonicalPath() + File.separator);
    }

}
