/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.io;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;

public class CachedOutputStream extends OutputStream {

    private static final File DEFAULT_TEMP_DIR;
    private static int defaultThreshold;
    private static long defaultMaxSize;
    private static String defaultCipherTransformation;
    private static boolean thresholdSysPropSet;

    static {
        String s = SystemPropertyAction.getPropertyOrNull(CachedConstants.OUTPUT_DIRECTORY_SYS_PROP);
        if (s != null) {
            File f = new File(s);
            if (f.exists() && f.isDirectory()) {
                DEFAULT_TEMP_DIR = f;
            } else {
                DEFAULT_TEMP_DIR = null;
            }
        } else {
            DEFAULT_TEMP_DIR = null;
        }

        setDefaultThreshold(-1);
        setDefaultMaxSize(-1);
        setDefaultCipherTransformation(null);
    }

    protected boolean outputLocked;
    protected OutputStream currentStream;

    private long threshold = defaultThreshold;
    private long maxSize = defaultMaxSize;
    private File outputDir = DEFAULT_TEMP_DIR;
    private String cipherTransformation = defaultCipherTransformation;

    private long totalLength;

    private boolean inmem;

    private boolean tempFileFailed;
    private File tempFile;
    private boolean allowDeleteOfFile = true;
    private CipherPair ciphers;

    private List<CachedOutputStreamCallback> callbacks;

    private List<Object> streamList = new ArrayList<>();
    private CachedOutputStreamCleaner cachedOutputStreamCleaner;

    public CachedOutputStream() {
        this(defaultThreshold);
    }

    public CachedOutputStream(long threshold) {
        this.threshold = threshold;
        currentStream = new LoadingByteArrayOutputStream(2048);
        inmem = true;
        readBusProperties();
    }

    private void readBusProperties() {
        Bus b = BusFactory.getThreadDefaultBus(false);
        if (b != null) {
            String v = getBusProperty(b, CachedConstants.THRESHOLD_BUS_PROP, null);
            if (v != null && threshold == defaultThreshold) {
                threshold = Integer.parseInt(v);
            }
            v = getBusProperty(b, CachedConstants.MAX_SIZE_BUS_PROP, null);
            if (v != null) {
                maxSize = Integer.parseInt(v);
            }
            v = getBusProperty(b, CachedConstants.CIPHER_TRANSFORMATION_BUS_PROP, null);
            if (v != null) {
                cipherTransformation = v;
            }
            v = getBusProperty(b, CachedConstants.OUTPUT_DIRECTORY_BUS_PROP, null);
            if (v != null) {
                File f = new File(v);
                if (f.exists() && f.isDirectory()) {
                    outputDir = f;
                }
            }
            
            cachedOutputStreamCleaner = b.getExtension(CachedOutputStreamCleaner.class);
        }
    }

    private static String getBusProperty(Bus b, String key, String dflt) {
        String v = (String)b.getProperty(key);
        return v != null ? v : dflt;
    }

    public void holdTempFile() {
        allowDeleteOfFile = false;
    }
    public void releaseTempFileHold() {
        allowDeleteOfFile = true;
    }

    public void registerCallback(CachedOutputStreamCallback cb) {
        if (null == callbacks) {
            callbacks = new ArrayList<>();
        }
        callbacks.add(cb);
    }

    public void deregisterCallback(CachedOutputStreamCallback cb) {
        if (null != callbacks) {
            callbacks.remove(cb);
        }
    }

    public List<CachedOutputStreamCallback> getCallbacks() {
        return callbacks == null ? null : Collections.unmodifiableList(callbacks);
    }

    /**
     * Perform any actions required on stream flush (freeze headers, reset
     * output stream ... etc.)
     */
    protected void doFlush() throws IOException {

    }

    public void flush() throws IOException {
        currentStream.flush();
        if (null != callbacks) {
            for (CachedOutputStreamCallback cb : callbacks) {
                cb.onFlush(this);
            }
        }
        doFlush();
    }

    /**
     * Perform any actions required on stream closure (handle response etc.)
     */
    protected void doClose() throws IOException {

    }

    /**
     * Perform any actions required after stream closure (close the other related stream etc.)
     */
    protected void postClose() throws IOException {

    }

    /**
     * Locks the output stream to prevent additional writes, but maintains
     * a pointer to it so an InputStream can be obtained
     * @throws IOException
     */
    public void lockOutputStream() throws IOException {
        if (outputLocked) {
            return;
        }
        currentStream.flush();
        outputLocked = true;
        if (null != callbacks) {
            for (CachedOutputStreamCallback cb : callbacks) {
                cb.onClose(this);
            }
        }
        doClose();
        streamList.remove(currentStream);
    }

    public void close() throws IOException {
        currentStream.flush();
        outputLocked = true;
        if (null != callbacks) {
            for (CachedOutputStreamCallback cb : callbacks) {
                cb.onClose(this);
            }
        }
        doClose();
        currentStream.close();
        if (ciphers != null) {
            ciphers.clean();
        }
        if (!maybeDeleteTempFile(currentStream)) {
            postClose();
        }
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof CachedOutputStream) {
            return currentStream.equals(((CachedOutputStream)obj).currentStream);
        }
        return currentStream.equals(obj);
    }

    /**
     * Replace the original stream with the new one, optionally copying the content of the old one
     * into the new one.
     * When with Attachment, needs to replace the xml writer stream with the stream used by
     * AttachmentSerializer or copy the cached output stream to the "real"
     * output stream, i.e. onto the wire.
     *
     * @param out the new output stream
     * @param copyOldContent flag indicating if the old content should be copied
     * @throws IOException
     */
    public void resetOut(OutputStream out, boolean copyOldContent) throws IOException {
        if (out == null) {
            out = new LoadingByteArrayOutputStream();
        }

        if (currentStream instanceof CachedOutputStream) {
            CachedOutputStream ac = (CachedOutputStream) currentStream;
            InputStream in = ac.getInputStream();
            IOUtils.copyAndCloseInput(in, out);
        } else {
            if (inmem) {
                if (currentStream instanceof ByteArrayOutputStream) {
                    ByteArrayOutputStream byteOut = (ByteArrayOutputStream) currentStream;
                    if (copyOldContent && byteOut.size() > 0) {
                        byteOut.writeTo(out);
                    }
                } else {
                    throw new IOException("Unknown format of currentStream");
                }
            } else {
                // read the file
                try {
                    currentStream.close();
                    if (copyOldContent) {
                        InputStream fin = createInputStream(tempFile);
                        IOUtils.copyAndCloseInput(fin, out);
                    }
                } finally {
                    streamList.remove(currentStream);
                    // we are not backed by file anymore, unregister from the cleaner
                    if (cachedOutputStreamCleaner != null) {
                        cachedOutputStreamCleaner.unregister(currentStream);
                        cachedOutputStreamCleaner.unregister(this);
                    }
                    deleteTempFile();
                    inmem = true;
                }
            }
        }
        currentStream = out;
        outputLocked = false;
    }

    public static void copyStream(InputStream in, OutputStream out, int bufferSize) throws IOException {
        IOUtils.copyAndCloseInput(in, out, bufferSize);
    }

    public long size() {
        return totalLength;
    }

    public byte[] getBytes() throws IOException {
        flush();
        if (inmem) {
            if (currentStream instanceof ByteArrayOutputStream) {
                return ((ByteArrayOutputStream)currentStream).toByteArray();
            }
            throw new IOException("Unknown format of currentStream");
        }
        // read the file
        try (InputStream fin = createInputStream(tempFile)) {
            return IOUtils.readBytesFromStream(fin);
        }
    }

    public void writeCacheTo(OutputStream out) throws IOException {
        flush();
        if (inmem) {
            if (currentStream instanceof ByteArrayOutputStream) {
                ((ByteArrayOutputStream)currentStream).writeTo(out);
            } else {
                throw new IOException("Unknown format of currentStream");
            }
        } else {
            // read the file
            InputStream fin = createInputStream(tempFile);
            IOUtils.copyAndCloseInput(fin, out);
        }
    }

    public void writeCacheTo(StringBuilder out, long limit) throws IOException {
        writeCacheTo(out, StandardCharsets.UTF_8.name(), limit);
    }

    public void writeCacheTo(StringBuilder out, String charsetName, long limit) throws IOException {
        flush();
        if (totalLength < limit
            || limit == -1) {
            writeCacheTo(out, charsetName);
            return;
        }

        long count = 0;
        if (inmem) {
            if (currentStream instanceof LoadingByteArrayOutputStream) {
                LoadingByteArrayOutputStream lout = (LoadingByteArrayOutputStream)currentStream;
                out.append(IOUtils.newStringFromBytes(lout.getRawBytes(), charsetName, 0, (int)limit));
            } else if (currentStream instanceof ByteArrayOutputStream) {
                byte[] bytes = ((ByteArrayOutputStream)currentStream).toByteArray();
                out.append(IOUtils.newStringFromBytes(bytes, charsetName, 0, (int)limit));
            } else {
                throw new IOException("Unknown format of currentStream");
            }
        } else {
            // read the file
            try (InputStream fin = createInputStream(tempFile);
                Reader reader = new InputStreamReader(fin, charsetName)) {
                char[] bytes = new char[1024];
                long x = reader.read(bytes);
                while (x != -1) {
                    if ((count + x) > limit) {
                        x = limit - count;
                    }
                    out.append(bytes, 0, (int)x);
                    count += x;

                    if (count >= limit) {
                        x = -1;
                    } else {
                        x = reader.read(bytes);
                    }
                }
            }
        }
    }

    public void writeCacheTo(StringBuilder out) throws IOException {
        writeCacheTo(out, StandardCharsets.UTF_8.name());
    }

    public void writeCacheTo(StringBuilder out, String charsetName) throws IOException {
        flush();
        if (inmem) {
            if (currentStream instanceof LoadingByteArrayOutputStream) {
                LoadingByteArrayOutputStream lout = (LoadingByteArrayOutputStream)currentStream;
                out.append(IOUtils.newStringFromBytes(lout.getRawBytes(), charsetName, 0, lout.size()));
            } else if (currentStream instanceof ByteArrayOutputStream) {
                byte[] bytes = ((ByteArrayOutputStream)currentStream).toByteArray();
                out.append(IOUtils.newStringFromBytes(bytes, charsetName));
            } else {
                throw new IOException("Unknown format of currentStream");
            }
        } else {
            // read the file
            try (InputStream fin = createInputStream(tempFile);
                Reader reader = new InputStreamReader(fin, charsetName)) {
                char[] bytes = new char[1024];
                int x = reader.read(bytes);
                while (x != -1) {
                    out.append(bytes, 0, x);
                    x = reader.read(bytes);
                }
            }
        }
    }


    /**
     * @return the underlying output stream
     */
    public OutputStream getOut() {
        return currentStream;
    }

    public int hashCode() {
        return currentStream.hashCode();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder().append('[')
            .append(CachedOutputStream.class.getName())
            .append(" Content: ");
        try {
            writeCacheTo(builder);
        } catch (IOException e) {
            //ignore
        }
        return builder.append(']').toString();
    }

    protected void onWrite() throws IOException {

    }

    private void enforceLimits() throws IOException {
        if (maxSize > 0 && totalLength > maxSize) {
            throw new CacheSizeExceededException();
        }
        if (inmem && totalLength > threshold && currentStream instanceof ByteArrayOutputStream) {
            createFileOutputStream();
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (!outputLocked) {
            onWrite();
            this.totalLength += len;
            enforceLimits();
            currentStream.write(b, off, len);
        }
    }

    public void write(byte[] b) throws IOException {
        if (!outputLocked) {
            onWrite();
            this.totalLength += b.length;
            enforceLimits();
            currentStream.write(b);
        }
    }

    public void write(int b) throws IOException {
        if (!outputLocked) {
            onWrite();
            this.totalLength++;
            enforceLimits();
            currentStream.write(b);
        }
    }

    private void createFileOutputStream() throws IOException {
        if (tempFileFailed) {
            return;
        }
        ByteArrayOutputStream bout = (ByteArrayOutputStream)currentStream;
        try {
            if (outputDir == null) {
                tempFile = FileUtils.createTempFile("cos", "tmp");
            } else {
                tempFile = FileUtils.createTempFile("cos", "tmp", outputDir, false);
            }

            currentStream = createOutputStream(tempFile);
            bout.writeTo(currentStream);
            inmem = false;
            streamList.add(currentStream);
            if (cachedOutputStreamCleaner != null) {
                cachedOutputStreamCleaner.register(this);
            }
        } catch (Exception ex) {
            //Could be IOException or SecurityException or other issues.
            //Don't care what, just keep it in memory.
            tempFileFailed = true;
            if (currentStream != bout) {
                currentStream.close();
            }
            deleteTempFile();
            inmem = true;
            currentStream = bout;
        }
    }

    public File getTempFile() {
        return tempFile != null && tempFile.exists() ? tempFile : null;
    }

    public InputStream getInputStream() throws IOException {
        flush();
        if (inmem) {
            if (currentStream instanceof LoadingByteArrayOutputStream) {
                return ((LoadingByteArrayOutputStream) currentStream).createInputStream();
            } else if (currentStream instanceof ByteArrayOutputStream) {
                return new ByteArrayInputStream(((ByteArrayOutputStream) currentStream).toByteArray());
            } else {
                return null;
            }
        }
        try {
            InputStream fileInputStream = new TransferableFileInputStream(tempFile);
            streamList.add(fileInputStream);
            if (cachedOutputStreamCleaner != null) {
                cachedOutputStreamCleaner.register(fileInputStream);
            }

            if (cipherTransformation != null) {
                fileInputStream = new CipherInputStream(fileInputStream, ciphers.getDecryptor()) {
                    boolean closed;
                    public void close() throws IOException {
                        if (!closed) {
                            super.close();
                            closed = true;
                        }
                    }
                };
            }

            return fileInputStream;
        } catch (FileNotFoundException e) {
            throw new IOException("Cached file was deleted, " + e.toString());
        }
    }

    private synchronized void deleteTempFile() {
        if (tempFile != null) {
            File file = tempFile;
            tempFile = null;
            FileUtils.delete(file);
        }
    }
    private boolean maybeDeleteTempFile(Closeable stream) {
        boolean postClosedInvoked = false;
        streamList.remove(stream);
        if (!inmem && tempFile != null && streamList.isEmpty() && allowDeleteOfFile) {
            if (currentStream != null) {
                try {
                    currentStream.close();
                    postClose();
                } catch (Exception e) {
                    //ignore
                }
                postClosedInvoked = true;
                if (cachedOutputStreamCleaner != null) {
                    cachedOutputStreamCleaner.unregister(this);
                }
            }
            deleteTempFile();
            currentStream = new LoadingByteArrayOutputStream(1024);
            inmem = true;
        }
        return postClosedInvoked;
    }

    public void setOutputDir(File outputDir) throws IOException {
        this.outputDir = outputDir;
    }

    public long getThreshold() {
        return threshold;
    }
    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public void setCipherTransformation(String cipherTransformation) {
        this.cipherTransformation = cipherTransformation;
    }

    public static void setDefaultMaxSize(long l) {
        if (l == -1) {
            String s = SystemPropertyAction.getProperty(CachedConstants.MAX_SIZE_SYS_PROP, "-1");
            l = Long.parseLong(s);
        }
        defaultMaxSize = l;
    }

    public static void setDefaultThreshold(int i) {
        if (i == -1) {
            i = SystemPropertyAction.getInteger(CachedConstants.THRESHOLD_SYS_PROP, -1);
            if (i <= 0) {
                i = 128 * 1024;
                thresholdSysPropSet = false; /* we not using system property value */
            } else {
                thresholdSysPropSet = true;
            }
        } else {
            thresholdSysPropSet = false; /* we not consulting system properties at all */
        }
        defaultThreshold = i;
    }

    /**
     * Returns true if the default threshold is explicitly set via CachedConstants.THRESHOLD_SYS_PROP
     */
    public static boolean isThresholdSysPropSet() {
        return thresholdSysPropSet;
    }

    public static void setDefaultCipherTransformation(String n) {
        if (n == null) {
            n = SystemPropertyAction.getPropertyOrNull(CachedConstants.CIPHER_TRANSFORMATION_SYS_PROP);
        }
        defaultCipherTransformation = n;
    }

    private OutputStream createOutputStream(File file) throws IOException {
        OutputStream out = new BufferedOutputStream(Files.newOutputStream(file.toPath()));
        if (cipherTransformation != null) {
            try {
                if (ciphers == null) {
                    ciphers = new CipherPair(cipherTransformation);
                }
            } catch (GeneralSecurityException e) {
                throw new IOException(e.getMessage(), e);
            }
            out = new CipherOutputStream(out, ciphers.getEncryptor()) {
                boolean closed;
                public void close() throws IOException {
                    if (!closed) {
                        super.close();
                        closed = true;
                    }
                }
            };
        }
        return out;
    }

    private InputStream createInputStream(File file) throws IOException {
        InputStream in = Files.newInputStream(file.toPath());
        if (cipherTransformation != null) {
            in = new CipherInputStream(in, ciphers.getDecryptor()) {
                boolean closed;
                public void close() throws IOException {
                    if (!closed) {
                        super.close();
                        closed = true;
                    }
                }
            };
        }
        return in;
    }

    private class TransferableFileInputStream extends FileInputStream implements Transferable {
        private boolean closed;
        private File sourceFile;

        TransferableFileInputStream(File sourceFile) throws FileNotFoundException {
            super(sourceFile);
            this.sourceFile = sourceFile;
        }

        public void close() throws IOException {
            if (!closed) {
                super.close();
                maybeDeleteTempFile(this);
                if (cachedOutputStreamCleaner != null) {
                    cachedOutputStreamCleaner.unregister(this);
                }
            }
            closed = true;
        }

        @Override
        public void transferTo(File destinationFile) throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
            //We've cached the file so try renaming.
            boolean transfered = sourceFile.renameTo(destinationFile);
            if (!transfered) {
                // Data is in memory, or we failed to rename the file, try copying
                // the stream instead.
                try (OutputStream out = Files.newOutputStream(destinationFile.toPath())) {
                    IOUtils.copyAndCloseInput(this, out);
                }
            }
        }
    }
}