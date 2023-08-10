/*******************************************************************************
 * Copyright (c) 2019,2023 IBM Corporation and others.
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
package com.ibm.ws.artifact.fat;

import java.util.zip.ZipFile;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import componenttest.topology.impl.LibertyServer;

/**
 * Zip file extension which provides simple name based
 * entry lookups.
 */
public class LookupZipArchive implements Closeable {
    /**
     * Log informational text using {@link FATLogging}.
     * 
     * @param methodName The method performing logging.
     * 
     * @param text Text to log.
     */
    private static void logInfo(String methodName, String text) {
        FATLogging.info(LookupZipArchive.class, methodName, text);
    }

    //

    /**
     * Answer the common fragment of the dump files of a server.
     * 
     * The common fragment has the server name plus ".dump".
     * 
     * @param server The server for which to answer the dump file name fragment.
     * 
     * @return The common dump file name fragment for the server.
     */
    public static String getDumpName(LibertyServer server) {
        return server.getServerName() + ".dump";
    }
    
    /**
     * Answer the most recent dump file for a server.
     * 
     * Use the common dump file name fragment (@link {@link #getDumpName(LibertyServer)}
     * and the file last modified times to locate the most recent
     * dump.
     * 
     * @param server The server for which to locate a most recent dump file.
     *
     * @return The most recent dump file.  Null if the server is null, or
     *     does not have a directory, or has no dump files.
     */
    public static File getLatestDump(LibertyServer server) {
        String methodName = "getMostRecentDump";

        if ( server == null ) {
            logInfo(methodName, "Return [ null ]: Null server");
            return null;
        }

        String dumpName = getDumpName(server);
        logInfo(methodName, "Dump name [ " + dumpName + " ]");

        String serverRoot = server.getServerRoot();
        File serverDirectory = new File(serverRoot);
        if( !serverDirectory.isDirectory() ) {
            logInfo(methodName, "Return [ null ]: Server directory does not exist [ " + serverDirectory.getAbsolutePath() + " ]");
            return null;
        }

        File[] dumpArchives = serverDirectory.listFiles(
            (File dir, String fileName) -> fileName.contains(dumpName) );

        if ( dumpArchives.length == 0 ) {
            logInfo(methodName, "Return [ null ]: No dumps were found");            
            return null;
        }

        File mostRecentDump = dumpArchives[0];
        
        if ( dumpArchives.length > 1 ) {
            long mostRecentModified = mostRecentDump.lastModified(); 

            for ( int dumpNo = 1; dumpNo < dumpArchives.length; ++dumpNo ) {
                File nextDump = dumpArchives[dumpNo];
                long nextModified = nextDump.lastModified();
                if ( nextModified > mostRecentModified ) {
                    mostRecentDump = nextDump;
                    mostRecentModified = nextModified;
                }
            }
        }

        logInfo(methodName, "Return [ " + mostRecentDump.getAbsolutePath() + " ]");
        return mostRecentDump;
    }

    public static ZipEntry getEntry(ZipFile zipFile, String entryName){
        String methodName = "getEntry";
        String zipName = zipFile.getName();

        ZipEntry selectedEntry = null;
        Enumeration<? extends ZipEntry> e = zipFile.entries();
        while ( (selectedEntry == null) && e.hasMoreElements() ) {
            ZipEntry nextEntry = e.nextElement();

            if ( !nextEntry.isDirectory() ) {
                if ( nextEntry.getName().endsWith(entryName) ) {
                    selectedEntry = nextEntry;
                }
            }
        }

        logInfo(methodName, "Return [ " + selectedEntry + " ] for [ " + entryName + " ] in [ " + zipName + " ]");            
        return selectedEntry;
    }

    //

    /**
     * Simple reference type.  Used to store successes and failures
     * in the zip entry lookup table
     *
     * @param <T> The type of object references.
     */
    private static class Ref<T> {
        public final T referent;
        
        public Ref(T referent) {
            this.referent = referent;
        }
    }
    
    public LookupZipArchive(File archiveFile) throws IOException {
        this.archiveFile = archiveFile;
        this.zipArchive = new ZipFile(archiveFile);
        this.entryMap = new HashMap<>(1);
    }

    private final File archiveFile;

    public File getArchiveFile() {
        return archiveFile;
    }

    private final ZipFile zipArchive;

    public ZipFile getZipArchive() {
        return zipArchive;
    }

    public String getName() {
        return getZipArchive().getName();
    }

    public InputStream getInputStream(ZipEntry entry) throws IOException {
        return getZipArchive().getInputStream(entry);
    }

    /**
     * Close the underlying archive.
     * 
     * See {@link Closeable#close()} and {@link ZipFile#close()}.
     * 
     * @throws IOException Thrown if the zip file fails to close.
     */
    @Override
    public void close() throws IOException {
        getZipArchive().close();
    }

    private final Map<String, Ref<ZipEntry>> entryMap;  

    /**
     * Answer the first entry which matches the specified name.
     * 
     * Use {@link String #endsWith(String)} to match entry names
     * to the specified name.
     * 
     * @param entryName The name to search for.
     * 
     * @return The zip entry which has a name ending with the specified name.
     */
    public ZipEntry getEntry(String entryName) {
        Ref<ZipEntry> entryRef = entryMap.computeIfAbsent(
                entryName,
                useEntryName -> new Ref<>( getEntry( getZipArchive(), useEntryName) ) );
        return entryRef.referent;
    }
    
    /**
     * Tell if an entry exists which matches the specified name.
     *
     * See {@link #getEntry(String)}.
     *
     * @return True or false telling if a matching entry exists.
     */
    public boolean hasEntry(String entryName) {
        return ( getEntry(entryName) != null );
    }

    /**
     * Answer the stream for the specified entry.  Answer
     * null if the entry does not exist.
     *
     * See {@link #getEntry(String)}.
     *
     * @param entryName The name to use to find an entry.
     *
     * @return The input stream for the matching entry.  Null
     *     if no matching entry is found.
     *
     * @throws IOException Thrown if an input stream cannot be
     *     opened on the entry.
     */
    public InputStream getInputStream(String entryName) throws IOException {
        ZipEntry entry = getEntry(entryName);
        if ( entry == null ) {
            throw new IOException("Entry not found [ " + entryName + " ]");
        }
        return getInputStream(entry);
    }
}
