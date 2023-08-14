/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.repository.transport.client;

import java.io.File;
import java.io.IOException;

/**
 * Caches data read from a file
 * <p>
 * The cache is invalidated if the file size or last modified date is changed.
 */
public class FileDataCache<T> {

    private long fileLastModified = 0;
    private long fileSize = 0;
    private T cachedData;
    private final File file;
    /**
     * Object to synchronize on when checking the file for changes.
     * <p>
     * We allow this to be passed in so that callers can avoid the possibility of deadlock if they also do synchronization.
     */
    private final Object lock;

    /**
     * @param file the file which, if changed, invalidates the cache
     * @param lock the object to synchronize on when checking if the file has changed
     */
    public FileDataCache(File file, Object lock) {
        this.file = file;
        this.lock = lock;
    }

    /**
     * Return data from the cache, or read and cache it if it's not present
     *
     * @param dataSupplier the supplier to use to get the data if the cache is empty or invalidated
     * @return the data
     * @throws IOException if there is an error reading the file
     */
    public T get(FileSupplier<T> dataSupplier) throws IOException {
        synchronized (lock) {
            if (file == null || !file.canRead()) {
                // Never cache if we haven't been given a file or can't read it
                return dataSupplier.get();
            }

            if (cachedData == null || file.lastModified() != fileLastModified || file.length() != fileSize) {
                cachedData = null;
                fileLastModified = file.lastModified();
                fileSize = file.length();

                cachedData = dataSupplier.get();
            }

            return cachedData;
        }
    }

    @FunctionalInterface
    public interface FileSupplier<T> {
        T get() throws IOException;
    }
}
