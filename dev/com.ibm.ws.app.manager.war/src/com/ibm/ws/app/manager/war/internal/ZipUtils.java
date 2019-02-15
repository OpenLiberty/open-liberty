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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 * File and zip file utilities.
 */
public class ZipUtils {
    private static final TraceComponent tc = Tr.register(ZipUtils.class);

    /**
     * Attempt to recursively delete a target file.
     * 
     * Answer null if the delete was successful.  Answer the first
     * file which could not be deleted if the delete failed.
     * 
     * If the delete fails, wait 400 MS then try again.  Do this
     * for the entire delete operation, not for each file deletion.
     *
     * A test must be done to verify that the file exists before
     * invoking this method: If the file does not exist, the
     * delete operation will fail.
     *
     * @param file The file to delete recursively.
     *
     * @return Null if the file was deleted.  The first file which
     *     could not be deleted if the file could not be deleted.
     */
    @Trivial
    public File deleteWithRetry(File file) {
        String methodName = "deleteWithRetry";

        String filePath;
        if ( tc.isDebugEnabled() ) {
            filePath = file.getAbsolutePath();
            Tr.debug(tc, methodName + ": Recursively delete [ " + filePath + " ]");
        } else {
            filePath = null;
        }

        File firstFailure = delete(file);
        if ( firstFailure == null ) {
            if ( filePath != null ) {
                Tr.debug(tc, methodName + ": Successful first delete [ " + filePath + " ]");
            }
            return null;
        }

        if ( filePath != null ) {
            Tr.debug(tc, methodName + ": Failed first delete [ " + filePath + " ]: Sleep 400 ms and retry");
        }
        try {
            Thread.sleep(400); // Twice zip.reaper.slow.pend.max
        } catch ( InterruptedException e ) {
            // FFDC
        }

        File secondFailure = delete(file);
        
        if ( secondFailure == null ) {
            if ( filePath != null ) {
                Tr.debug(tc, methodName + ": Successful first delete [ " + filePath + " ]");
            }
            return null;
        } else {
            if ( filePath != null ) {
                Tr.debug(tc, methodName + ": Failed second delete [ " + filePath + " ]");
            }
            return secondFailure;
        }
    }

    /**
     * Attempt to recursively delete a file.
     *
     * Do not retry in case of a failure.
     *
     * A test must be done to verify that the file exists before
     * invoking this method: If the file does not exist, the
     * delete operation will fail.
     *
     * @param file The file to recursively delete.
     *
     * @return Null if the file was deleted.  The first file which
     *     could not be deleted if the file could not be deleted.
     */
    @Trivial
    public File delete(File file) {
        String methodName = "delete";

        String filePath;
        if ( tc.isDebugEnabled() ) {
            filePath = file.getAbsolutePath();
        } else {
            filePath = null;
        }

        if ( file.isDirectory() ) {
            if ( filePath != null ) {
                Tr.debug(tc, methodName + "Delete directory [ " + filePath + " ]");
            }

            File firstFailure = null;

            File[] subFiles = file.listFiles();
            if ( subFiles != null ) {
                for ( File subFile : subFiles ) {
                    File nextFailure = delete(subFile);
                    if ( (nextFailure != null) && (firstFailure == null) ) {
                        firstFailure = nextFailure;
                    }
                }
            }

            if ( firstFailure != null ) {
                if ( filePath != null ) {
                    Tr.debug(tc, methodName +
                        ": Cannot delete [ " + filePath + " ]" +
                        " Child [ " + firstFailure.getAbsolutePath() + " ] could not be deleted.");
                }
                return firstFailure;
            }
        } else {
            if ( filePath != null ) {
                Tr.debug(tc, methodName + "Delete simple file [ " + filePath + " ]");
            }
        }

        if ( !file.delete() ) {
            return file;
        } else {
            return null;
        }
    }

    //

    public static final boolean IS_EAR = true;
    public static final boolean IS_NOT_EAR = false;

    private static final String WAR_EXTENSION = ".war";

    /**
     * Unpack a source archive into a target directory.
     *
     * If the source archive is a WAR, package web module archives as well.
     *
     * This operation is not smart enough to avoid unpacking WAR files
     * in an application library folder.  However, that is very unlikely to
     * happen.
     *
     * @param source The source archive which is to be unpacked.
     * @param target The directory into which to unpack the source archive.
     * @param isEar Control parameter: Is the source archive an EAR file.
     *     When the source archive is an EAR, unpack nested WAR files.
     * @param lastModified The last modified value to use for the expanded
     *     archive.
     *
     * @throws IOException Thrown in case of a failure.
     */
    public void unzip(File source, File target, boolean isEar, long lastModified) throws IOException {
        String methodName = "unzip";

        String sourcePath = source.getAbsolutePath();
        String targetPath = target.getAbsolutePath();

        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName + ": Source [ " + sourcePath + " ] Size [ " + source.length() + " ]");
            Tr.debug(tc, methodName + ": Target [ " + targetPath + " ]");
        }

        if ( !source.exists() ) {
            throw new IOException("Source [ " + sourcePath + " ] does not exist");
        } else if ( !target.exists() ) {
            throw new IOException("Target [ " + targetPath + " ] does not exist");
        }

        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName + ": Set last modified [ " + lastModified + " ]");
        }
        target.setLastModified(lastModified); 

        List<Object[]> warData = ( isEar ? new ArrayList<Object[]>() : null );

        ZipFile sourceZip = new ZipFile(source);
        try {
            Enumeration<? extends ZipEntry> sourceEntries = sourceZip.entries();
            while ( sourceEntries.hasMoreElements() ) {
                ZipEntry sourceEntry = sourceEntries.nextElement();

                String sourceEntryName = sourceEntry.getName();
                if ( reachesOut(sourceEntryName) ) {
                    Tr.error(tc, "error.file.outside.archive", sourceEntryName, sourcePath);
                    continue;
                }

                String targetFileName;
                Object[] nextWarData;
                if ( isEar && !sourceEntry.isDirectory() && sourceEntryName.endsWith(WAR_EXTENSION) ) {
                    for ( int tmpNo = 0;
                          sourceZip.getEntry( targetFileName = sourceEntryName + ".tmp" + tmpNo ) != null;
                          tmpNo++ ) {
                        // Empty
                    }
                    nextWarData = new String[] { sourceEntryName, targetFileName, null };
                    warData.add(nextWarData);
                } else {
                    targetFileName = sourceEntryName;
                    nextWarData = null;
                }

                File targetFile = new File(target, targetFileName);

                if ( sourceEntry.isDirectory() ) {
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + ": Directory [ " + sourceEntryName + " ]");
                    }

                    if ( !targetFile.exists() && !targetFile.mkdirs() ) {
                        throw new IOException("Failed to create directory [ + " + targetFile.getAbsolutePath() + " ]");
                    }

                } else {
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + ": Simple file [ " + sourceEntryName + " ] [ " + sourceEntry.getSize() + " ]");
                    }

                    File targetParent = targetFile.getParentFile();
                    if ( !targetParent.mkdirs() && !targetParent.exists() ) {
                        throw new IOException("Failed to create directory [ " + targetParent.getAbsolutePath() + " ]");
                    }

                    transfer(sourceZip, sourceEntry, targetFile); // throws IOException
                }

                // If the entry doesn't provide a meaningful last modified time,
                // use the parent file's last modified time.

                long entryModified = sourceEntry.getTime();
                if ( entryModified == -1 ) {
                    entryModified = lastModified;
                }
                targetFile.setLastModified(entryModified);
                
                if ( nextWarData != null ) {
                	nextWarData[2] = Long.valueOf(entryModified);
                }
            }

        } finally {
            if ( sourceZip != null ) {
                sourceZip.close();
            }
        }

        if ( isEar ) {
            for ( Object[] nextWarData : warData ) {
                String unpackedWarName = (String) nextWarData[0];
                String packedWarName = (String) nextWarData[1];
                long warLastModified = ((Long) nextWarData[2]).longValue();

                File unpackedWarFile = new File(target, unpackedWarName);
                if ( !unpackedWarFile.exists() && !unpackedWarFile.mkdirs() ) {
                    throw new IOException("Failed to create [ " + unpackedWarFile.getAbsolutePath() + " ]");
                }

                File packedWarFile = new File(target, packedWarName);

                unzip(packedWarFile, unpackedWarFile, IS_NOT_EAR, warLastModified);

                if ( !packedWarFile.delete() ) {
                    if ( tc.isDebugEnabled() ) {
                        Tr.debug(tc, methodName + ": Failed to delete temporary WAR [ " + packedWarFile.getAbsolutePath() + " ]");
                    }
                }
            }
        }
    }

    private byte[] transferBuffer = new byte[16 * 1024];

    private void transfer(ZipFile sourceZip, ZipEntry sourceEntry, File targetFile) throws IOException {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = sourceZip.getInputStream(sourceEntry);
            outputStream = new FileOutputStream(targetFile);

            int lastRead;
            while ( (lastRead = inputStream.read(transferBuffer)) != -1) {
                outputStream.write(transferBuffer, 0, lastRead);
            }

        } finally {
            if ( inputStream != null ) {
                inputStream.close();
            }
            if ( outputStream != null ) {
                outputStream.close();
            }
        }
    }

    private boolean reachesOut(String entryPath) {
        if ( !entryPath.contains("..") ) {
            return false;
        }

        String normalizedPath = PathUtils.normalizeUnixStylePath(entryPath);
        return PathUtils.isNormalizedPathAbsolute(normalizedPath); // Leading ".." or "/.."

        // The following is very inefficient ... and doesn't work when there
        // are symbolic links.
        // return targetFile.getCanonicalPath().startsWith(targetDirectory.getCanonicalPath() + File.separator);
    }
}
