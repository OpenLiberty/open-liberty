/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.artifact.zip.cache.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.artifact.zip.cache.ZipCachingProperties;
import com.ibm.ws.artifact.zip.cache.ZipFileHandle;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

/**
 * A handle to a zip file.
 *
 * These are expected to be held by {@link ZipCachingServiceImpl} and shared
 * between code which accesses the zip file.
 *
 * In addition to the caching provided by the zip caching service, each zip
 * file handle caches the last 16 entries which had 8K or fewer bytes.  That
 * means the zip caching service will use up to 64MB of storage.
 */
public class ZipFileHandleImpl implements ZipFileHandle {
    private static final TraceComponent tc = Tr.register(ZipFileHandleImpl.class);

    @Trivial
    ZipFileHandleImpl(String path) {
        this.path = path;
        this.file = new File(path);
    }

    private final String path;
    private final File file;

    @Trivial
    public String getPath() {
        return path;
    }

    @Trivial
    public File getFile() {
        return file;
    }

    @Trivial
    public long getLastModified() {
        return FileUtils.fileLastModified( getFile() );
    }

    //

    private class ZipFileLock {
        // EMPTY
    }
    private final ZipFileLock zipFileLock = new ZipFileLock();
    private ZipFile zipFile;
    private int openCount;

    //

    @Trivial
    private void debug(String methodName, String text) {
        if ( !tc.isDebugEnabled() ) {
            return;
        }

        String message =
            methodName +
            " ZipFileHandle@0x" + Integer.toHexString(hashCode()) +
            " (" + path + ", " + Integer.toString(openCount) + ")" +
            " " + text;
        Tr.debug(tc, message);
    }

    //

    private static final ZipFileReaper zipFileReaper;

    static {
        int useMaxPending = ZipCachingProperties.ZIP_CACHE_REAPER_MAX_PENDING;
        if ( useMaxPending == 0 ) {
            zipFileReaper = null;
        } else {
            zipFileReaper = new ZipFileReaper(
                "zip cache reaper",
                ZipCachingProperties.ZIP_CACHE_REAPER_MAX_PENDING,
                ZipCachingProperties.ZIP_CACHE_REAPER_SHORT_INTERVAL,
                ZipCachingProperties.ZIP_CACHE_REAPER_LONG_INTERVAL);
        }
    }

    /**
     * Open the zip file.  Create and assign the zip file if this is the first
     * open.  Increase the open count by one.
     *
     * If this is the first open and the zip file could not be created, the
     * open count is not increased.
     *
     * @return The zip file.
     */
    @Override
    @Trivial
    public ZipFile open() throws IOException {
        String methodName = "open";

        synchronized( zipFileLock ) {
            if ( zipFile == null ) {
                debug(methodName, "Opening");
                if ( zipFileReaper == null ) {
                    zipFile = ZipFileUtils.openZipFile(file); // throws IOException
                } else {
                    zipFile = zipFileReaper.open(path);
                }
            }

            openCount++;
            debug(methodName, "Opened");

            return zipFile;
        }
    }

    @Override
    @Trivial
    public void close() {
        String methodName = "close";

        boolean extraClose;

        synchronized ( zipFileLock ) {
            if ( !(extraClose = (openCount == 0)) ) {
                debug(methodName, "Closing");

                openCount = openCount - 1;

                if ( openCount == 0 ) {
                    if ( zipFileReaper == null ) {
                        ZipFile useZipFile = zipFile;
                        zipFile = null;
                        try {
                            useZipFile.close();
                        } catch ( IOException e ) {
                            // FFDC
                        }
                    } else {
                        zipFile = null;
                        zipFileReaper.close(path);
                    }
                }

                debug(methodName, "Closed");
            }
        }

        if ( extraClose && tc.isDebugEnabled() ) {
            debug(methodName, "Extra close");

            Exception e = new Exception();
            ByteArrayOutputStream stackStream = new ByteArrayOutputStream();
            PrintStream stackPrintStream = new PrintStream(stackStream);
            e.printStackTrace(stackPrintStream);

            Tr.debug( tc, stackStream.toString() );
        }
    }

    //

    private static class ZipEntriesLock {
        // EMPTY
    }
    private static final ZipEntriesLock zipEntriesLock = new ZipEntriesLock();
    private static final Map<String, byte[]> zipEntries;

    static {
        if ( (ZipCachingProperties.ZIP_CACHE_ENTRY_LIMIT == 0) ||
             (ZipCachingProperties.ZIP_CACHE_ENTRY_MAX == 0) ) {
            zipEntries = null;
        } else {
            zipEntries = new CacheHashMap<String, byte[]>(ZipCachingProperties.ZIP_CACHE_ENTRY_MAX);
        }
    }

    private static class CacheHashMap<K, V>
        extends LinkedHashMap<K, V> {

        private static final long serialVersionUID = 1L;

        private final int ivMaxSize;

        public CacheHashMap(int maxSize) {
            this(maxSize, 16, .75f, true);
        }

        public CacheHashMap(int maxSize, int initialCapacity, float loadFactor, boolean accessOrder) {
            super(initialCapacity, loadFactor, accessOrder);

            this.ivMaxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > ivMaxSize;
        }
    }

    private static final ByteArrayInputStream EMPTY_STREAM =
        new ByteArrayInputStream( new byte[0] );

    /**
     * Answer an input stream for an entry of a zip file.  When the entry is a
     * class entry which has 8K or fewer bytes, read all of the entry bytes immediately
     * and cache the bytes in this handle.  Subsequent input stream requests which
     * locate cached bytes will answer a stream on those bytes.
     *
     * @param useZipFile The zip file for which to answer an input stream
     * @param zipEntry The zip entry for which to answer the input stream.
     *
     * @return An input stream on the bytes of the entry.  Null for an directory
     *     type entry, or an entry which has zero bytes.
     *
     * @throws IOException Thrown if the entry bytes could not be read.
     */
    @Override
    @Trivial
    public InputStream getInputStream(ZipFile useZipFile, ZipEntry zipEntry) throws IOException {
        String methodName = "getInputStream";
        String entryName = zipEntry.getName();

        if ( zipEntry.isDirectory() ) {
            if ( tc.isDebugEnabled() ) {
                debug(methodName, "Entry [ " + entryName + " ] [ null ] (Not using cache: Directory entry)");
            }
            return null;
        }

        long entrySize = zipEntry.getSize();
        if ( entrySize == 0 ) {
            if ( tc.isDebugEnabled() ) {
                debug(methodName, "Entry [ " + entryName + " ] [ empty stream ] (Not using cache: Empty entry)");
            }
            return EMPTY_STREAM;
        }

        boolean doNotCache;
        String doNotCacheReason;
        if ( zipEntries == null ) { // No entry cache.
            doNotCache = true;
            doNotCacheReason = "Do not cache: Entry cache disabled";
        } else if ( entrySize > ZipCachingProperties.ZIP_CACHE_ENTRY_LIMIT) { // Too big for the cache
            doNotCache = true;
            doNotCacheReason = "Do not cache: Too big";
        } else if ( entryName.equals("META-INF/MANIFEST.MF") ) {
            doNotCache = false;
            doNotCacheReason = "Cache META-INF/MANIFEST.MF";
        } else if ( entryName.endsWith(".class") ) {
            doNotCache = false;
            doNotCacheReason = "Cache .class resources";
        } else {
            doNotCache = true;
            doNotCacheReason = "Do not cache: Not manifest or class resource";
        }
        if ( tc.isDebugEnabled() ) {
            debug(methodName, "Entry [ " + entryName + " ] [ non-null ] [ " + doNotCacheReason + " ]");
        }

        if ( doNotCache ) {
            return useZipFile.getInputStream(zipEntry); // throws IOException
        }

        // The addition of ":::" *seems* to allow for non-unique cache keys.  Duplicate
        // keys *are not* possible because the CRC and last-modified values are numeric.
        // Duplicate keys would be possible of the CRC or last-modified values, when
        // converted to strings, could contain ":::" character sequences.

        String entryCacheKey =
               entryName +
               ":::" + Long.toString( zipEntry.getCrc() ) +
               ":::" + Long.toString( getLastModified() );

        // Note that only the individual gets and puts are protected.
        //
        // That means that simultaneous get misses are possible, which
        // will result in double reads and double puts.
        //
        // That is unfortunate, but is harmless.
        //
        // The simultaneous puts are allowed because they should be very
        // rare.
        //
        // They are allowed because blocking entry gets while waiting for
        // reads could create large delays.

        byte[] entryBytes;
        synchronized( zipEntriesLock ) {
            entryBytes = zipEntries.get(entryCacheKey);
        }

        if ( entryBytes == null ) {
            InputStream inputStream = useZipFile.getInputStream(zipEntry); // throws IOException
            try {
                entryBytes = read(inputStream, (int) entrySize, entryName); // throws IOException
            } finally {
                inputStream.close(); // throws IOException
            }

            synchronized( zipEntriesLock ) {
                zipEntries.put(entryCacheKey, entryBytes);
            }
        }

        return new ByteArrayInputStream(entryBytes);
    }

    /**
     * Read an exact count of bytes from an input stream.
     *
     * @param inputStream The stream from which to read the bytes.
     *
     * @param size The number of bytes which are to be read.
     * @param name A name associated with the stream.
     *
     * @return The bytes read from the stream.
     *
     * @throws IOException Throw if the read failed, including the case where
     *     insufficient bytes were available to be read.
     */
    @Trivial
    private static byte[] read(InputStream inputStream, int size, String name) throws IOException {
        byte[] bytes = new byte[size];

        int readCount = inputStream.read(bytes, 0, size); // throws IOException
        if ( readCount != size ) {
            throw new IOException(
                "Read [ " + Integer.valueOf(readCount) + " ]" +
                " but expected to read [ " + Integer.valueOf(size) + " ] bytes" +
                " from [ " + name + " ]");
        }
        return bytes;
    }
}
