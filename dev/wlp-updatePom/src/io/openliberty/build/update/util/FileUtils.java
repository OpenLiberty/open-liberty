/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.build.update.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Basic file utilities.
 */
public class FileUtils {
    public static File ensure(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                return file;
            } else {
                throw new IOException("Target [ " + file.getAbsolutePath() + " ] exists as a simple file");
            }
        }

        file.mkdirs();

        if (!file.exists()) {
            throw new IOException("Target [ " + file.getAbsolutePath() + " ] could not be created as a directory");
        } else if (!file.isDirectory()) {
            throw new IOException("Target [ " + file.getAbsolutePath() + " ] re-appeared as a simple file");
        } else {
            return file;
        }
    }

    public static void write(InputStream inputStream, File outputFile, byte[] transferBuffer) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            transfer(inputStream, outputStream, transferBuffer);
        }
    }

    public static void transfer(File inputFile, File outputFile, byte[] transferBuffer) throws IOException {
        try (InputStream inputStream = new FileInputStream(inputFile);
                        OutputStream outputStream = new FileOutputStream(outputFile)) {
            transfer(inputStream, outputStream, transferBuffer);
        }
    }

    public static void transfer(InputStream inputStream, OutputStream outputStream, byte[] transferBuffer) throws IOException {
        int bytesRead;
        while ((bytesRead = inputStream.read(transferBuffer)) != -1) {
            outputStream.write(transferBuffer, 0, bytesRead);
        }
    }

    public static void copyFile(String inputPath, String outputPath, byte[] transferBuffer) throws IOException {
        try (InputStream inputStream = new FileInputStream(new File(inputPath));
                        OutputStream outputStream = new FileOutputStream(new File(outputPath))) {
            transfer(inputStream, outputStream, transferBuffer);
        }
    }

    public static void copyReplacing(File inputFile, File outputFile,
                                     String entryName, InputStream altEntryStream, byte[] transferBuffer) throws IOException {

        try (InputStream inputStream = new FileInputStream(inputFile);
                        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
                        OutputStream outputStream = new FileOutputStream(outputFile);
                        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {

            ZipEntry inputEntry;
            while ((inputEntry = zipInputStream.getNextEntry()) != null) {
                ZipEntry outputEntry;
                InputStream outputEntryStream;

                boolean isDir = inputEntry.isDirectory();

                if (isDir || !inputEntry.getName().equals(entryName)) {
                    // Don't change the entry.
                    outputEntry = inputEntry;
                    outputEntryStream = zipInputStream;
                } else {
                    // The new entry has the default compression method and new time settings.
                    outputEntry = new ZipEntry(inputEntry.getName());
                    outputEntry.setMethod(inputEntry.getMethod());
                    outputEntryStream = altEntryStream;
                }

                zipOutputStream.putNextEntry(outputEntry);
                if (!isDir) {
                    transfer(outputEntryStream, zipOutputStream, transferBuffer);
                }
                zipOutputStream.closeEntry();
            }
        }
    }
}
