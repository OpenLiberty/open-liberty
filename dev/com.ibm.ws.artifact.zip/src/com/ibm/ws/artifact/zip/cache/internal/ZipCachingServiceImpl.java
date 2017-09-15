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
package com.ibm.ws.artifact.zip.cache.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.ibm.ws.artifact.zip.cache.ZipCachingService;
import com.ibm.ws.artifact.zip.cache.ZipFileHandle;

/**
 *
 */
public class ZipCachingServiceImpl implements ZipCachingService {

    final private static int MAXCACHE = 250;
    final private static Map<String, ZipFileHandle> cache = Collections.synchronizedMap(new CacheHashMap<String, ZipFileHandle>(MAXCACHE));

    // PK72252 - Returns a ZipFileHandle that has been referenced.  Callers are
    // required to call close() when the ZipFile is no longer needed.
    private static ZipFileHandle internalOpenZipFile(String path) throws IOException {
        ZipFileHandle handle;

        //MUST NOT RETURN NULL.

        //optimistic threadsafe cache usage.. 
        handle = cache.get(path);
        //handle was not in cache.. add it & remove oldest if needed.
        if (handle == null) {
            handle = new ZipFileHandleImpl(path);
            //because we were not sync'd on anything, this could mean 2 threads built their own handle,
            //but only the last thread will win & get it's handle into the cache.. not too much of a problem.
            cache.put(path, handle);
        }

        return handle;
    }

    public ZipFileHandle openZipFile(String path) throws IOException {
        return ZipCachingServiceImpl.internalOpenZipFile(path);
    }

}
