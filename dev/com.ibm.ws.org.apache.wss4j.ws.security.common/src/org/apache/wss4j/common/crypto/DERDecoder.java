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

package org.apache.wss4j.common.crypto;

import java.math.BigInteger;

import org.apache.wss4j.common.ext.WSSecurityException;

/**
 * Provides the means to navigate through a DER-encoded byte array, to help
 * in decoding the contents.
 * <p>
 * It maintains a "current position" in the array that advances with each
 * operation, providing a simple means to handle the type-length-value
 * encoding of DER. For example
 * <pre>
 *   decoder.expect(TYPE);
 *   int length = decoder.getLength();
 *   byte[] value = decoder.getBytes(len);
 * </pre>
 */
public class DERDecoder {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DERDecoder.class);

    /** DER type identifier for a bit string value */
    public static final byte TYPE_BIT_STRING = 0x03;
    /** DER type identifier for a octet string value */
    public static final byte TYPE_OCTET_STRING = 0x04;
    /** DER type identifier for a sequence value */
    public static final byte TYPE_SEQUENCE = 0x30;

    private byte[] arr;
    private int pos;

    /**
     * Construct a DERDecoder for the given byte array.
     *
     * @param derEncoded the DER-encoded array to decode.
     * @throws WSSecurityException if the given array is null.
     */
    public DERDecoder(byte[] derEncoded) throws WSSecurityException {
        if (derEncoded == null) {
            throw new WSSecurityException(
                    WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN,
                    "noSKIHandling",
                    new Object[] {"Invalid DER string"}
            );
        }
        arr = derEncoded;
        reset();
    }


    /**
     * Reset the current position to the start of the array.
     */
    public void reset() {
        pos = 0;
    }

    /**
     * Advance the current position by the given number of bytes.
     *
     * @param length the number of bytes to skip.
     * @throws WSSecurityException if length is negative.
     */
    public void skip(int length) throws WSSecurityException {
        if (length < 0) {
            throw new WSSecurityException(
                    WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN,
                    "noSKIHandling",
                    new Object[] {"Unsupported DER format"}
            );
        }
        pos += length;
    }

    /**
     * Confirm that the byte at the current position matches the given value.
     *
     * @param val the expected next byte.
     * @throws WSSecurityException
     *         if the current position is at the end of the array, or if the
     *         byte at the current position doesn't match the expected value.
     */
    public void expect(int val) throws WSSecurityException {
        expect((byte)(val & 0xFF));
    }

    /**
     * Confirm that the byte at the current position matches the given value.
     *
     * @param val the expected next byte.
     * @throws WSSecurityException
     *         if the current position is at the end of the array, or if the
     *         byte at the current position doesn't match the expected value.
     */
    public void expect(byte val) throws WSSecurityException {
        if (!test(val)) {
            LOG.debug("DER mismatch: expected " + val + ", got " + arr[pos]);
            throw new WSSecurityException(
                    WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN,
                    "noSKIHandling",
                    new Object[] {"Invalid DER format"}
            );
        }
        pos++;
    }

    /**
     * Test if the byte at the current position matches the given value.
     *
     * @param val the value to test for a match with the current byte.
     * @return true if the byte at the current position matches the given value.
     * @throws WSSecurityException if the current position is at the end of
     *                             the array.
     */
    public boolean test(byte val) throws WSSecurityException {  //NOPMD
        if (pos >= arr.length) {
            throw new WSSecurityException(
                    WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN,
                    "noSKIHandling",
                    new Object[] {"Invalid DER format"}
            );
        }
        return arr[pos] == val;
    }

    /**
     * Get the DER length at the current position.
     * <p>
     * DER length is encoded as
     * <ul>
     * <li>If the first byte is 0x00 to 0x7F, it describes the actual length.
     * <li>If the first byte is 0x80 + n with 0<n<0x7F, the actual length is
     * described in the following 'n' bytes.
     * <li>The length value 0x80, used only in constructed types, is
     * defined as "indefinite length".
     * </ul>
     *
     * @return the length, -1 for indefinite length.
     * @throws WSSecurityException
     *         if the current position is at the end of the array or there is
     *         an incomplete length specification.
     */
    public int getLength() throws WSSecurityException {
        if (pos >= arr.length) {
            throw new WSSecurityException(
                    WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN,
                    "noSKIHandling",
                    new Object[] {"Invalid DER format"}
            );
        }
        int len;
        if ((arr[pos] & 0xFF) <= 0x7F) {
            len = arr[pos++];
        } else {
            int nbytes = arr[pos++] & 0x7F;
            if (pos + nbytes > arr.length) {
                throw new WSSecurityException(
                        WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN,
                        "noSKIHandling",
                        new Object[] {"Invalid DER format"}
                );
            }
            byte[] lenBytes = new byte[nbytes];
            System.arraycopy(arr, pos, lenBytes, 0, lenBytes.length);
            len = new BigInteger(1, lenBytes).intValue();
            pos += nbytes;
        }
        return len;
    }

    /**
     * Return an array of bytes from the current position.
     *
     * @param length the number of bytes to return.
     * @return an array of the requested number of bytes from the current
     *         position.
     * @throws WSSecurityException
     *         if the current position is at the end of the array, or the
     *         length is negative.
     */
    public byte[] getBytes(int length) throws WSSecurityException {
        if (pos + length > arr.length) {
            throw new WSSecurityException(
                    WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN,
                    "noSKIHandling",
                    new Object[] {"Invalid DER format"}
             );
        } else if (length < 0) {
            throw new WSSecurityException(
                    WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN,
                    "noSKIHandling",
                    new Object[] {"Unsupported DER format"}
            );
        }
        byte[] value = new byte[length];
        System.arraycopy(arr, pos, value, 0, length);
        pos += length;
        return value;
    }

}
