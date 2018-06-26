/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.ws.anno.jandex.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Reads a Jandex index file and returns the saved index. See {@link Indexer}
 * for a thorough description of how the Index data is produced.
 *
 * <p>
 * An IndexReader loads the stream passed to it's constructor and applies the
 * appropriate buffering. The Jandex index format is designed for efficient
 * reading and low final memory storage.
 *
 * <p>
 * <b>Thread-Safety</b>
 * </p>
 * IndexReader is not thread-safe and can not be shared between concurrent
 * threads. The resulting index, however, is.
 *
 * @author Jason T. Greene
 */
public final class LimitedIndexReader {

    /**
     * The latest index version supported by this version of Jandex.
     */
    private static final int MAGIC = 0xBABE1F15;
    private PackedDataInputStream input;
    private int version = -1;
    private IndexReaderImpl reader;

    /**
     * Constructs a new IndedReader using the passed stream. The stream is not
     * read from until the read method is called.
     *
     * @param input a stream which points to a jandex index file
     */
    public LimitedIndexReader(InputStream input) {
        this.input = new PackedDataInputStream(new BufferedInputStream(input));
    }

    /**
     * Read the index at the associated stream of this reader. This method can be called multiple
     * times if the stream contains multiple index files.
     *
     * @return the Index contained in the stream
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if the stream does not point to Jandex index data
     * @throws UnsupportedVersion if the index data is tagged with a version not known to this reader
     */
    public LimitedIndex read() throws IOException {
        if (version == -1) {
            readVersion();
        }

        return reader.read(version);
    }

    private void initReader(int version) throws IOException {
        IndexReaderImpl reader;
        if (version >= LimitedIndexReaderV1.MIN_VERSION && version <= LimitedIndexReaderV1.MAX_VERSION) {
            reader = new LimitedIndexReaderV1(input);
        } else if (version >= LimitedIndexReaderV2.MIN_VERSION && version <= LimitedIndexReaderV2.MAX_VERSION) {
            reader = new LimitedIndexReaderV2(input);
        } else {
            input.close();
            throw new UnsupportedVersion("Version: " + version);
        }

        this.reader = reader;
    }

    /**
     * Returns the version of the data contract stored in the index that was read. This version is incremented when
     * the contract adds new information. Generally this is used to determine if the underlying index contains
     * necessary information for analysis. As an example, generics are recorded in version 4; therefore, a tool that
     * requires generic data would need to reject/ignore version 3 data.
     * <br>
     * The data contract version should not be confused with the index file version, which represents the internal
     * storage format of the file. The index file version moves independently of the data contract.
     *
     * @return The data contract version of the index that was read
     * @throws IOException If the index could not be read
     */
    public int getDataVersion() throws IOException {
        if (version == -1) {
            readVersion();
        }

        return reader.toDataVersion(version);
    }

    /**
     * Returns the index file version. Note that the index file version may increment even though the underlying
     * data contract remains the same. In most cases, {@link #getDataVersion()} should be used instead of this method,
     * since applications are typically interested in the underlying contract of the data stored, and not the internal
     * implementation details of a Jandex index.
     *
     * @return the internal index file version
     * @throws IOException If the index could not be read
     */
    public int getIndexVersion() throws IOException {
        if (version == -1) {
            readVersion();
        }

        return version;
    }

    private void readVersion() throws IOException {
        if (input.readInt() != MAGIC) {
            input.close();
            throw new IllegalArgumentException("Not a jandex index");
        }

        version = input.readUnsignedByte();
        initReader(version);
    }
}
