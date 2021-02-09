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
 * 
 * All modifications made by IBM from initial source -
 * https://github.com/wildfly/jandex/blob/master/src/main/java/org/jboss/jandex/IndexReader.java
 * commit - 36c2b049b7858205c6504308a5e162a4e943ff21
 */
package com.ibm.ws.annocache.jandex.internal;

import java.io.IOException;
import java.io.InputStream;

public final class SparseIndexReader {
    /** Required first four bytes of any Jandex index. */
    public static final int MAGIC = 0xBABE1F15;

    //

    /**
     * Create an index reader for a specified index version.
     * 
     * Create the reader on the specified input stream.  The stream must be
     * positioned immediately following the magic bytes and the version bytes.
     * 
     * @param input The input stream for the new reader.
     * @param version The version of index which is to be read.
     * 
     * @return A reader for the specified version.  Null if the version is not supported.
     */
    public static SparseIndexReaderVersion createReader(PackedDataInputStream input, int version) {
        if ( SparseIndexReaderVersionImpl_V1.accept(version) ) {
            return new SparseIndexReaderVersionImpl_V1(input, version);
        } else if ( SparseIndexReaderVersionImpl_V2.accept(version) ) {
            return new SparseIndexReaderVersionImpl_V2(input, version);
        } else {
            return null;
        }
    }

    /**
     * Create a reader on an input stream.
     * 
     * Read the triple of ( jandex magic bytes, index version, index ) from the input
     * stream.
     * 
     * Store the index version and index.
	 *
	 * Do not close the index: That is the responsibility of the caller.
	 *
     * @param input The stream on which to read the index.
     * 
     * @throws IOException Thrown if the read failed.
     */
    @SuppressWarnings("resource")
	public SparseIndexReader(InputStream input) throws IOException {
        PackedDataInputStream packedInput = new PackedDataInputStream(input);

        int actualBytes = packedInput.readInt(); // throws IOException
        if ( actualBytes != MAGIC ) {
            throw new IllegalArgumentException("Read [ 0x" + Integer.toHexString(actualBytes) + " ] expecting Jandex magic bytes [ 0x" + Integer.toHexString(MAGIC) + " ]");
        }

        this.version = packedInput.readUnsignedByte(); // throws IOException

        SparseIndexReaderVersion reader = createReader(packedInput, this.version);
        if ( reader == null ) {
            throw new IllegalArgumentException("Unsupported index version [ " + Integer.toString(version) + " ]");
        }

        this.sparseIndex = reader.read(); // throws IOException
    }

    //

    private final int version;

    public int getVersion() {
        return version;
    }

    private final SparseIndex sparseIndex;

    public SparseIndex getIndex() {
        return sparseIndex;
    }
}
