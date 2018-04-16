/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.artifact.zip.cache.internal;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.artifact.zip.cache.internal.ZipFileReaper.ZipFileData;

/**
 * Utility encapsulation of a pending closes collection used by the reaper.
 */
public class ZipFilePendingCloses {
    static final TraceComponent tc = Tr.register(ZipFilePendingCloses.class);

    private static boolean isDebugEnabled() {
    	return ( tc.isDebugEnabled() );
    }

    @Trivial
    private static void debug(String methodName, String name, String text) {
    	Tr.debug(tc, methodName + "(" + name + ") " + text);
    }

    //

    public ZipFilePendingCloses(ZipFileReaper reaper, String name) {
        this.reaper = reaper;

        this.name = name;

        this.storage = new LinkedHashMap<String, ZipFileData>() {
            private static final long serialVersionUID = 1L;

            @Override
            @Trivial
            protected boolean removeEldestEntry(Map.Entry<String, ZipFileData> eldestEntry) {
                String methodName = "removeEldestEntry(pendingCloses)";

                // Don't remove the eldest entry when on a shutdown reap:
                // Allow all of the open zip files to pend before doing any of
                // the closes.
                if ( !getIsActive() ) {
                    return false;
                }

                int useMaxCache = getMaxCache();
                if ( useMaxCache < 0 ) {
                    // A max cache of -1 makes the cache unbounded.
                    return false;
                } else if ( size() < useMaxCache ) {
                    return false;
                } else {
                    // Set the eldest: The caller of 'put' needs to check
                    // for this and close it.
                    //
                    // Alternatively, the close could be performed here.
                    // That is not currently done: The caller of 'put' is responsible
                    // for doing all zip data state updates.

                    if ( isDebugEnabled() ) {
                        debug(methodName, name, "Removed [ " + eldestEntry.getKey() + " ]");
                    }
                    setLeastRecent( eldestEntry.getValue() );

                    return true;
                }
            }
        };

        this.mostRecent = null;
        this.leastRecent = null;
    }

    //

    private final ZipFileReaper reaper;

    public ZipFileReaper getReaper() {
        return reaper;
    }

    public int getMaxCache() {
        return getReaper().getMaxCache();
    }

    public boolean getIsActive() {
        return getReaper().getIsActive();
    }

    //

    private final String name;

    public String getName() {
        return name;
    }

    //

    private final LinkedHashMap<String, ZipFileData> storage;
    private ZipFileData mostRecent;
    private ZipFileData leastRecent;

    //

    @Trivial
    public LinkedHashMap<String, ZipFileData> getStorage() {
        return storage;
    }

    @Trivial
    public boolean isEmpty() {
        return storage.isEmpty();
    }

    @Trivial
    public boolean hasOne() {
        return ( storage.size() == 1 );
    }

    @Trivial
    public boolean isFull() {
        // 'maxCache == -1' means never full
        // 'maxCache == 0' is not allowed
        return ( storage.size() == getMaxCache() );
    }

    @Trivial
    public ZipFileData put(String path, ZipFileData data) {
        mostRecent = data;

        return storage.put(path, data);
    }

    @Trivial
    public ZipFileData remove(String path) {
        return storage.remove(path);
    }

    @Trivial
    public ZipFileData getMostRecent() {
        return mostRecent;
    }

    @Trivial
    public void setLeastRecent(ZipFileData eldestPendingClose) {
        this.leastRecent = eldestPendingClose;
    }

    @Trivial
    public ZipFileData takeLeastRecent() {
         ZipFileData useLeastRecent = leastRecent;
         if ( useLeastRecent != null ) {
             leastRecent = null;
         }
         return useLeastRecent;
    }
}
