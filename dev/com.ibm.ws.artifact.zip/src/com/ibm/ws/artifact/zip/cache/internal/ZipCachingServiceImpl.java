/*******************************************************************************
 * Copyright (c) 2012,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.zip.cache.internal;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.artifact.zip.cache.ZipCachingProperties;
import com.ibm.ws.artifact.zip.cache.ZipCachingService;
import com.ibm.ws.artifact.zip.cache.ZipFileHandle;

/**
 * Implementation of a zip file caching service.
 *
 * Three layers of caching are provided:
 *
 * A cache of zip file handles ({@link ZipFileHandle}) tracks the number
 * of opens and closes have been performed on each zip file, and holds the
 * reference to the zip file when at least one open is active.
 *
 * A cache of data for the manifest and small class files for each
 * zip file handle.
 *
 * A cache of zip files (held by a singleton {@link ZipFileReaper})
 * independently tracks open and closes, holds the actual zip file, and
 * provides a delay on zip file closes.
 *
 * Zip file handles are currently used only by zip type containers
 * ({@link ZipFileContainer}), and are only accessed in three ways:
 *
 * First, population of the entries of the zip container causes the
 * a zip file handle to be created, opened, and closed.
 *
 * Second enabling and disabling fast mode on a zip container cause,
 * respectively, a zip file handle to be opened and closed.  The zip file
 * handle is created if needed before it is opened.
 *
 * Third, obtaining an input stream on an entry of a zip container causes
 * the zip handle to be opened.  The close of the input stream of causes
 * the the zip handle to be closed.  The zip file handle is created if
 * needed before it is opened.
 *
 * In all three cases, the same zip file handle is used for the open
 * and close, guaranteeing that the open and close pair reach the same
 * zip file handle.
 *
 * A single zip file container will assign its zip file handle at most once,
 * meaning, all three types of access always use the same zip file handle
 * instance.  However, that does not guarantee unique access to the zip file
 * through the same handle, because the artifact file system does not
 * prevent the creation of multiple instances of equivalent zip file
 * containers.
 *
 * Often, the same zip file handle instance will be used across the multiple
 * instances.  A different zip file handle instance is used when there are
 * more than the zip cache count of unique zip files accessed, since the
 * cache overflow will discard the initial instance.
 */
public class ZipCachingServiceImpl implements ZipCachingService {
    /**
     * Public API: Obtain a zip file handle for a specified path.
     *
     * Callers should agree on the form to use for the path.  Current callers
     * obtain a canonical path, or an absolute path when the canonical path
     * cannot be obtained.
     *
     * The handle which is returned is not guaranteed to be unique, since
     * a cache overflow will result in create of new handles to the same
     * file.  Callers should be careful, then, when calling multiple
     * times for the same path.  If the same handle must be obtained, the
     * caller must store a reference to the initial zip file which was obtained.
     *
     * The handle which is obtained is not initially opened.
     *
     * @param path The path to the handle which is to be retrieved.
     *
     * @return The zip file handle for the specified path.
     *
     * @throws IOException Thrown in case of a problem obtaining the
     *     zip file handle.  Not currently thrown.
     */
    @Trivial
    public ZipFileHandle openZipFile(String path) throws IOException {
        return ZipCachingServiceImpl.getZipFileHandle(path);
    }

    //

    // TODO: With java8, we can replace this using 'computeIfAbsent'; see:
    //
    // https://stackoverflow.com/questions/14876967/lambdas-and-putifabsent
    // http://cs.oswego.edu/pipermail/concurrency-interest/2011-August/008176.html
    // http://cs.oswego.edu/pipermail/concurrency-interest/2011-August/008193.html
    //
    // The pattern is:
    //
    // Map<K, V> map = new HashMap<K, V>();
    // K key;
    //
    // Old:
    //
    // V value;
    // synchronized (map) {
    //   if ( (value = map.get(key)) == null ) {
    //     map.put(key, (value = new V(key)));
    //   }
    // }
    //
    // Or:
    //
    // ConcurrentHashMap<K, V> map = new ConcurrentHashMap<K, V>();
    // K key;
    // V value = map.putIfAbsent(key, new V(key));
    //
    // New:
    //
    // ConcurrentMap<K, V> map = new ConcurrentHashMap<K, V>();
    // K key;
    // V value = map.computeIfAbsent(key, #{key -> new V(key)} };
    //
    // Note that computeIfAbsent has limitations: Only the single
    // key/value pair may be updated.  The lambda which provides
    // the value must be "quick".

    private static class ZipFileHandlesLock {
        // EMPTY
    }
    private static final ZipFileHandlesLock zipFileHandlesLock = new ZipFileHandlesLock();
    private static final LinkedHashMap<String, ZipFileHandle> zipFileHandles;

    static {
        final int handleMax = ZipCachingProperties.ZIP_CACHE_HANDLE_MAX;
        int initialAllocation;
        if ( handleMax == -1 ) {
            initialAllocation = 16;
        } else {
            initialAllocation = handleMax;
        }

        // 'true' means accesses dynamically re-order the mapping, placing the accessed entry first.
        zipFileHandles = new LinkedHashMap<String, ZipFileHandle>(initialAllocation, 0.75f, true) {
            private static final long serialVersionUID = 1L;

            /**
             * Test if a new eldest entry is to be removed from the zip handle
             * cache.
             *
             * This limit on the number of handles which are retained has
             * important implications.  See the {@link ZipCachingServiceImpl}
             * class comment for more information.
             *
             * @param eldest The current eldest entry.  Unused by this implementation,
             *     which only checks the size of the mapping.
             *
             * @return True or false telling if the entry is to be removed.
             */
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ZipFileHandle> eldest) {
                return ( (handleMax != -1) && (size() > handleMax) );
            }
        };
    }

    /**
     * Answer a zip file handle for a specified path.
     *
     * The handle is not initially opened.  (See {@link #openZipFile(String)}
     * for more details.)
     *
     * @param path The path to the handle which is to be retrieved.
     *
     * @return The zip file handle for the path.
     *
     * @throws IOException Thrown in case of a problem obtaining the
     *     zip file handle.  Not currently thrown.
     */
    private static ZipFileHandle getZipFileHandle(String path) throws IOException {
        synchronized ( zipFileHandlesLock ) {
            ZipFileHandle handle = zipFileHandles.get(path);
            if ( handle == null ) {
                handle = new ZipFileHandleImpl(path);
                zipFileHandles.put(path, handle);
            }
            return handle;
        }
    }
}
