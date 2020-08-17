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

package org.apache.cxf.attachment;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * An implementation of a FilterInputStream that decodes the
 * stream data in BASE64 encoding format.  This version does the
 * decoding "on the fly" rather than decoding a single block of
 * data.  Since this version is intended for use by the MimeUtilty class,
 * it also handles line breaks in the encoded data.
 */
@Trivial
public class Base64DecoderStream extends FilterInputStream {

    static final String MAIL_BASE64_IGNOREERRORS = "mail.mime.base64.ignoreerrors";

    // number of decodeable units we'll try to process at one time.  We'll attempt to read that much
    // data from the input stream and decode in blocks.
    static final int BUFFERED_UNITS = 2000;

    // can be overridden by a system property.
    protected boolean ignoreErrors;

    // buffer for reading in chars for decoding (which can support larger bulk reads)
    protected char[] encodedChars = new char[BUFFERED_UNITS * 4];
    // a buffer for one decoding unit's worth of data (3 bytes).
    protected byte[] decodedChars;
    // count of characters in the buffer
    protected int decodedCount;
    // index of the next decoded character
    protected int decodedIndex;


    public Base64DecoderStream(InputStream in) {
        super(in);
    }

    /**
     * Test for the existance of decoded characters in our buffer
     * of decoded data.
     *
     * @return True if we currently have buffered characters.
     */
    private boolean dataAvailable() {
        return decodedCount != 0;
    }

    /**
     * Decode a requested number of bytes of data into a buffer.
     *
     * @return true if we were able to obtain more data, false otherwise.
     */
    private boolean decodeStreamData() throws IOException {
        decodedIndex = 0;

        // fill up a data buffer with input data
        int readCharacters = fillEncodedBuffer();

        if (readCharacters > 0) {
            try {
                decodedChars = Base64Utility.decodeChunk(encodedChars, 0, readCharacters);
            } catch (Base64Exception e) {
                throw new IOException(e);
            }
            decodedCount = decodedChars.length;
            return true;
        }
        return false;
    }


    /**
     * Retrieve a single byte from the decoded characters buffer.
     *
     * @return The decoded character or -1 if there was an EOF condition.
     */
    private int getByte() throws IOException {
        if (!dataAvailable() && !decodeStreamData()) {
            return -1;
        }
        decodedCount--;
        // we need to ensure this doesn't get sign extended
        return decodedChars[decodedIndex++] & 0xff;
    }

    private int getBytes(byte[] data, int offset, int length) throws IOException {

        int readCharacters = 0;
        while (length > 0) {
            // need data?  Try to get some
            if (!dataAvailable() && !decodeStreamData()) {
                // if we can't get this, return a count of how much we did get (which may be -1).
                return readCharacters > 0 ? readCharacters : -1;
            }

            // now copy some of the data from the decoded buffer to the target buffer
            int copyCount = Math.min(decodedCount, length);
            System.arraycopy(decodedChars, decodedIndex, data, offset, copyCount);
            decodedIndex += copyCount;
            decodedCount -= copyCount;
            offset += copyCount;
            length -= copyCount;
            readCharacters += copyCount;
        }
        return readCharacters;
    }


    /**
     * Fill our buffer of input characters for decoding from the
     * stream.  This will attempt read a full buffer, but will
     * terminate on an EOF or read error.  This will filter out
     * non-Base64 encoding chars and will only return a valid
     * multiple of 4 number of bytes.
     *
     * @return The count of characters read.
     */
    private int fillEncodedBuffer() throws IOException {
        int readCharacters = 0;

        while (true) {
            // get the next character from the stream
            int ch = in.read();
            // did we hit an EOF condition?
            if (ch == -1) {
                // now check to see if this is normal, or potentially an error
                // if we didn't get characters as a multiple of 4, we may need to complain about this.
                if ((readCharacters % 4) != 0) {
                    throw new IOException("Base64 encoding error, data truncated: " + readCharacters + " "
                                          + new String(encodedChars, 0, readCharacters));
                }
                // return the count.
                return readCharacters;
            } else if (Base64Utility.isValidBase64(ch)) {
                // if this character is valid in a Base64 stream, copy it to the buffer.
                encodedChars[readCharacters++] = (char)ch;
                // if we've filled up the buffer, time to quit.
                if (readCharacters >= encodedChars.length) {
                    return readCharacters;
                }
            }

            // we're filtering out whitespace and CRLF characters, so just ignore these
        }
    }


    // in order to function as a filter, these streams need to override the different
    // read() signature.

    public int read() throws IOException {
        return getByte();
    }


    public int read(byte [] buffer, int offset, int length) throws IOException {
        return getBytes(buffer, offset, length);
    }


    public boolean markSupported() {
        return false;
    }


    public int available() throws IOException {
        return ((in.available() / 4) * 3) + decodedCount;
    }
}