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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.artifact.zip.cache.ZipFileHandle;
import com.ibm.ws.artifact.zip.internal.Utils;

/**
 *
 */
public class ZipFileHandleImpl implements ZipFileHandle {

    static final TraceComponent tc = Tr.register(ZipFileHandleImpl.class);

    private final String path;
    private final File file;
    private ZipFile zipFile;
    private int refs;

    ZipFileHandleImpl(String path) {
        this.path = path;
        this.file = new File(path);
    }

    public ZipFile getZipFile() {
        return zipFile;
    }

    @Override
    public synchronized ZipFile open() throws IOException {
        if (zipFile == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "[" + this.hashCode() + "] Opening FileHandle to " + path);
            zipFile = Utils.newZipFile(file);
        }
        refs++;
        if (tc.isDebugEnabled())
            Tr.debug(tc, "[" + this.hashCode() + "] refCount now " + refs);
        return zipFile;
    }

    @Override
    public synchronized void close() {
        //quick exit if anyone is trying to close us when we are closed!
        if (refs == 0) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "[" + this.hashCode() + "] attempt to call close when closed & ref at zero.. caused by.. ");
                Exception e = new Exception();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                e.printStackTrace(ps);
                Tr.debug(tc, baos.toString());
            }
            return;
        }

        if (--refs != 0) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "[" + this.hashCode() + "] refCount now " + refs);
            return;
        }

        // PK96275
        // Make sure the zipFile has been opened, as it is
        // possible for a newly created ZipFileHandle to be kicked
        // out of the zipFileCache before it's been opened.
        if (zipFile != null) {
            try {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "[" + this.hashCode() + "] Closing handle to path " + path);
                zipFile.close();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "[" + this.hashCode() + "] refCount now " + refs);
            } catch (IOException ex) {
                //instrumented ffdc.
            } finally {
                zipFile = null;
            }
        }
    }

    final private static int MAX_CACHE_ENTRY_SIZE = 8192;
    final private static int MAX_CACHE_ENTRIES = 16;
    final private static Map<String, byte[]> dataCache = Collections.synchronizedMap(new CacheHashMap<String, byte[]>(MAX_CACHE_ENTRIES));

    private byte[] readDataToByteArray(InputStream in) throws IOException {
        if (in == null) {
            return null;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int readCount;
            byte[] data = new byte[8192];
            while ((readCount = in.read(data, 0, data.length)) != -1) {
                baos.write(data, 0, readCount);
            }
            //really make sure we've got all the data =)
            baos.flush();
            return baos.toByteArray();
        } finally {
            try {
                in.close();
            } catch (IOException io) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "IO Exception closing input stream while caching zip entry", io);
                }
            }
        }
    }

    public long getLastModified() {
        return AccessController.doPrivileged(new PrivilegedAction<Long>() {
            @Override
            public Long run() {
                return file.lastModified();
            }

        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.artifact.zip.cache.ZipFileHandle#getInputStream(java.util.zip.ZipFile, java.util.zip.ZipEntry)
     */
    @Override
    public InputStream getInputStream(ZipFile zf, ZipEntry ze) throws IOException {
        //entry was small enough that it might be in cache, or need to be put there.
        long size = ze.getSize();
        if (size < MAX_CACHE_ENTRY_SIZE && size > 0 && !ze.getName().endsWith(".class")) {
            //build a key that includes the entry crc, if the zip changes, we'll only return stale data if the crc clashes.. 
            //which is pretty remote.. 
            String path = ze.getName();
            path += ":::" + ze.getCrc();
            path += ":::" + getLastModified();

            byte[] data = dataCache.get(path);
            if (data != null) {
                return new ByteArrayInputStream(data);
            } else {
                data = readDataToByteArray(zf.getInputStream(ze));
                if (data != null) {
                    //no sync block means that we might in a race condition
                    //retrieve the inputstream multiple times, but only the 
                    //last one will end up in the cache, this is fine.
                    dataCache.put(path, data);
                    return new ByteArrayInputStream(data);
                }
            }
            return null;
        }

        //entry was above the entry cache size, get from zip itself.
        return zf.getInputStream(ze);
    }
}
