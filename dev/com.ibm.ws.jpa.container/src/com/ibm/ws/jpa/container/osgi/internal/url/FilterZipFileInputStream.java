/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.container.osgi.internal.url;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This stream returns bytes for a JAR file that represents a subset of the
 * entries in a ZipFile.
 */
public class FilterZipFileInputStream extends InputStream {
    /**
     * The ZipFile containing the entries. It will be closed when the stream
     * is closed.
     */
    private final ZipFile zipFile;

    /**
     * The entries of the ZipFile.
     */
    private final Enumeration<? extends ZipEntry> entries;

    /**
     * The prefix that entries must have to be reported and that will be
     * removed from entries in the output.
     */
    private final String entryPrefix;

    /**
     * The output stream used to write zip entries. This ZipOutputStream
     * writes directly to {@link #buffer}.
     */
    private final ZipOutputStream out = new ZipOutputStream(new InternalBufferOutputStream());

    /**
     * The buffer for this input stream. This is the input buffer for this,
     * but the output buffer for {@link #out}.
     */
    byte[] buffer = new byte[8192];

    /**
     * The position of the next byte to read from {@link #buffer}, with an
     * exclusive upper bound of {@link #max}.
     */
    int pos;

    /**
     * The maximum number of valid bytes in {@link #buffer}.
     */
    int max;

    /**
     * The input stream for the current entry, or null if there is no current
     * entry.
     */
    private InputStream entryInput;

    /**
     * The temporary buffer for reading input from a zip entry.
     */
    private final byte[] entryInputBuffer = new byte[8192];

    public FilterZipFileInputStream(File zipFile, String entryPath) throws ZipException, IOException {
        if (zipFile == null) {
            throw new IllegalArgumentException("FilterZipFileInputStream zipFile cannot be null.");
        }

        this.zipFile = new ZipFile(zipFile);
        this.entryPrefix = entryPath;

        // If the entryPath does not identify a specific plain file within the addressed
        // archive file, then iterate through all of the entries at the specified path root.
        // Otherwise, only include the ZipEntry plain file directly referenced.
        if (entryPrefix == null || entryPrefix.length() == 0 || entryPrefix.endsWith("/")) {
            this.entries = this.zipFile.entries();
        } else {
            final ZipEntry targetEntry = this.zipFile.getEntry(entryPrefix);
            this.entries = new Enumeration<ZipEntry>() {
                boolean readEntry = false;
                ZipEntry tEntry = targetEntry;

                @Override
                public boolean hasMoreElements() {
                    return !readEntry;
                }

                @Override
                public ZipEntry nextElement() {
                    if (readEntry) {
                        throw new NoSuchElementException();
                    }
                    readEntry = true;
                    return tEntry;
                }
            };
        }
    }

    @Override
    @Trivial
    public int read() throws IOException {
        if (pos == max && !refill()) {
            return -1;
        }
        return buffer[pos++] & 0xff;
    }

    @Override
    public void close() throws IOException {
        if (entryInput != null) {
            entryInput.close();
        }

        zipFile.close();
    }

    @Trivial
    private boolean refill() throws IOException {
        pos = max = 0;

        // Write as many entries as possible until ZipOutputStream flushes
        // some data to our internal buffer (via BufferOutputStream), and
        // then return that data to the caller.

        for (;;) {
            // First, copy any remaining input from the open entry.
            if (entryInput != null) {
                for (;;) {
                    int read;
                    if ((read = entryInput.read(entryInputBuffer, 0, entryInputBuffer.length)) == -1) {
                        entryInput.close();
                        entryInput = null;

                        out.closeEntry();
                        if (max != 0) {
                            return true;
                        }

                        // Try the next entry.
                        break;
                    }

                    out.write(entryInputBuffer, 0, read);
                    if (max != 0) {
                        return true;
                    }
                }
            }

            // Otherwise, try to find a new entry that matches.
            for (;;) {
                if (!entries.hasMoreElements()) {
                    out.close();
                    // Closing the ZipOutputStream probably writes trailing bytes.
                    return max != 0;
                }

                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entryName.startsWith(entryPrefix) && !entry.isDirectory()) { // !entryName.equals(entryPrefix)) {
                    entryInput = zipFile.getInputStream(entry);

                    out.putNextEntry(new ZipEntry(entryName.substring(entryPrefix.length())));
                    if (max != 0) {
                        return true;
                    }

                    // Read the entry input immediately.
                    break;
                }
            }
        }
    }

    /**
     * Internal output stream that writes to the internal buffer.
     */
    private class InternalBufferOutputStream extends OutputStream {
        @Override
        @Trivial
        public void write(int b) throws IOException {
            if (max == buffer.length) {
                // We must grow the buffer as needed by the ZipOutputStream.
                buffer = Arrays.copyOf(buffer, max + max);
            }

            buffer[max++] = (byte) b;
        }
    }
}
