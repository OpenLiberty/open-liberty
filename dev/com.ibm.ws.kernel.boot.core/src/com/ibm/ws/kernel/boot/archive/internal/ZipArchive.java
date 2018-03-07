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
package com.ibm.ws.kernel.boot.archive.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import com.ibm.ws.kernel.boot.archive.UnixModeHelper;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.FileUtils;

public class ZipArchive extends AbstractArchive {

    private final File archiveFile;

    private final ZipArchiveOutputStream zipOutputStream;

    private static UnixModeHelper helper;

    static {
        try {
            Class.forName("java.nio.file.attribute.PosixFilePermission");
            helper = new UnixModeHelperImpl();
        } catch (ClassNotFoundException e) {
            // Expected on Java 6, in which case we don't use the helper and cope.
        } catch (NoClassDefFoundError e) {
            // Expected in unit tests, in which case we don't use the helper and cope.
        }
    }

    /**
     * Create an archive
     * 
     * @param archiveFile the target zip file.
     */
    public ZipArchive(File archiveFile) throws IOException {
        this.archiveFile = archiveFile;
        this.zipOutputStream = new ZipArchiveOutputStream(archiveFile);
    }

    @Override
    public void addFileEntry(String entryPath, File source) throws IOException {
        processEntry(entryPath, source);
    }

    @Override
    public void addDirEntry(String entryPath, File source, List<String> dirContent) throws IOException {
        //create the dir from the entry prefix
        if (!entryPath.isEmpty()) {
            processEntry(entryPath, source);
        }

        //add the contents
        for (String relativePath : dirContent) {
            String targetPath = entryPath + relativePath;
            File sourceFile = new File(source, relativePath);

            processEntry(targetPath, sourceFile);
        }
    }

    private void processEntry(String targetPath, File sourceFile) throws IOException {
        if (sourceFile.isDirectory()) {
            targetPath = FileUtils.normalizeDirPath(targetPath);
        }

        if (!entryPaths.add(targetPath)) {
            //already added the entry path
            return;
        }

        // Ignore the file if it happens to be the zipfile's path we are writing to. 
        // Use Canonical instead of equals method - this compares paths in a system dependent way 
        if (archiveFile.getCanonicalFile().equals(sourceFile.getCanonicalFile())) {
            return;
        }

        // add the entry
        ZipArchiveEntry entry = (ZipArchiveEntry) zipOutputStream.createArchiveEntry(sourceFile, targetPath);

        if (helper != null) {
            entry.setUnixMode(helper.getUnixMode(sourceFile));
        }
        zipOutputStream.putArchiveEntry(entry);
        if (sourceFile.isFile()) {
            // Add the file data
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(sourceFile);
                int bytesIn = 0;
                byte[] readBuffer = new byte[8192];
                bytesIn = fileInputStream.read(readBuffer);
                while (bytesIn != -1) {
                    zipOutputStream.write(readBuffer, 0, bytesIn);
                    bytesIn = fileInputStream.read(readBuffer);
                }
            } catch (IOException e) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.unableZipFile"), sourceFile.getAbsolutePath(), e));
            } finally {
                Utils.tryToClose(fileInputStream);
            }
        }
        zipOutputStream.closeArchiveEntry();
    }

    @Override
    public void close() throws IOException {
        zipOutputStream.close();
    }
}