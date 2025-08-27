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

package org.apache.xml.security.utils;

import org.apache.xml.security.exceptions.DERDecodingException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.math.BigInteger;
import java.security.PublicKey;
import org.apache.xml.security.algorithms.implementations.SignatureECDSA;

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
public class DERDecoderUtils {
    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(SignatureECDSA.class);

    /**
     * DER type identifier for a bit string value
     */
    public static final byte TYPE_BIT_STRING = 0x03;
    /**
     * DER type identifier for a octet string value
     */
    public static final byte TYPE_OCTET_STRING = 0x04;
    /**
     * DER type identifier for a sequence value
     */
    public static final byte TYPE_SEQUENCE = 0x30;
    /**
     * DER type identifier for ASN.1 "OBJECT IDENTIFIER" value.
     */
    public static final byte TYPE_OBJECT_IDENTIFIER = 0x06;

    /**
     * Simple method parses an ASN.1 encoded byte array. The encoding uses "DER", a BER/1 subset, that means a triple { typeId, length, data }.
     * with the following structure:
     * <p>
     * <pre>
     *  PublicKeyInfo ::= SEQUENCE {
     *      algorithm   AlgorithmIdentifier,
     *      PublicKey   BIT STRING
     *  }
     * </pre>
     * <p>
     * Where AlgorithmIdentifier is formatted as:
     * <pre>
     *  AlgorithmIdentifier ::= SEQUENCE {
     *      algorithm   OBJECT IDENTIFIER,
     *      parameters  ANY DEFINED BY algorithm OPTIONAL
     *  }
     *</pre>
     * @param derEncodedIS the DER-encoded input stream to decode.
     * @throws DERDecodingException in case of decoding error or if given InputStream is null or empty.
     * @throws IOException if an I/O error occurs.
     */
    public static byte[] getAlgorithmIdBytes(InputStream derEncodedIS) throws DERDecodingException, IOException {
        if (derEncodedIS == null || derEncodedIS.available() <= 0) {
            throw new DERDecodingException("DER decoding error: Null data");
        }

        validateType(derEncodedIS.read(), TYPE_SEQUENCE);
        readLength(derEncodedIS);
        validateType(derEncodedIS.read(), TYPE_SEQUENCE);
        readLength(derEncodedIS);

        return readObjectIdentifier(derEncodedIS);
    }

    /**
     * Read the next object identifier from the given DER-encoded input stream.
     * <p>
     * @param derEncodedIS the DER-encoded input stream to decode.
     * @return the object identifier as a byte array.
     * @throws DERDecodingException if parse error occurs.
     */
    public static byte[] readObjectIdentifier(InputStream derEncodedIS) throws DERDecodingException {
        try {
            validateType(derEncodedIS.read(), TYPE_OBJECT_IDENTIFIER);
            int length = readLength(derEncodedIS);
            LOG.debug("DER decoding algorithm id bytes");

            // Liberty Change Start: backport Java 11 method to be compatible with Java 8
            // return derEncodedIS.readNBytes(length);
            return readNBytes(length, derEncodedIS);
        } catch (IOException ex) {
            throw new DERDecodingException("Error occurred while reading the input stream.", ex);
        }
    }

    /**
     *  Mimic Java 11 behavior where we read N-number of bytes from InputStream
     */
    private static byte[] readNBytes(int length, InputStream derEncodedIS) throws IOException {
        
        byte[] bytes = new byte[length];
        DataInputStream dataInputStream = new DataInputStream(derEncodedIS);
        dataInputStream.readFully(bytes);
        return bytes;
    }
    /**
     * The method extracts the algorithm OID from the public key and returns it as "dot encoded" OID string.
     *
     * @param publicKey the public key for which method returns algorithm ID.
     * @return String representing the algorithm ID.
     * @throws DERDecodingException if the algorithm ID cannot be determined.
     */
    public static String getAlgorithmIdFromPublicKey(PublicKey publicKey) throws DERDecodingException {
        String keyFormat = publicKey.getFormat();
        if (!("X.509".equalsIgnoreCase(keyFormat)
                || "X509".equalsIgnoreCase(keyFormat))) {
            throw new DERDecodingException("Unknown key format [" + keyFormat
                    + "]! Support for X.509-encoded public keys only!");
        }
        try (InputStream inputStream = new ByteArrayInputStream(publicKey.getEncoded())) {
            byte[] keyAlgOidBytes = getAlgorithmIdBytes(inputStream);
            String alg = decodeOID(keyAlgOidBytes);
            if (alg.equals(KeyUtils.KeyAlgorithmType.EC.getOid())) {
                keyAlgOidBytes = readObjectIdentifier(inputStream);
                alg = decodeOID(keyAlgOidBytes);
            }
            return alg;
        } catch (IOException ex) {
            throw new DERDecodingException("Error reading public key", ex);
        }
    }

    private static void validateType(int iType, byte expectedType) throws DERDecodingException {
        validateType((byte) (iType & 0xFF), expectedType);
    }

    private static void validateType(byte type, byte expectedType) throws DERDecodingException {
        if (type != expectedType) {
            throw new DERDecodingException("DER decoding error: Expected type [" + expectedType + "] but got [" + type + "]");
        }
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
     * @throws DERDecodingException if the current position is at the end of the array or there is
     *                              an incomplete length specification.
     * @throws IOException          if an I/O error occurs.
     */
    public static int readLength(InputStream derEncodedIs) throws DERDecodingException, IOException {
        if (derEncodedIs.available() <= 0) {
            throw new DERDecodingException("Invalid DER format");
        }

        int value = derEncodedIs.read();

        if ((value & 0x080) == 0x00) { // short form, 1 byte size
            return value;
        }
        // number of bytes used to encode length
        int byteCount = value & 0x07f;
        //byteCount == 0 indicates indefinite length encoded data.
        if (byteCount == 0) {
            return -1;
        }

        // byteCount > 4 not able to handle more than 4Gb of data (max int size) tmp > 4 indicates.
        if (byteCount > 4) {
            throw new DERDecodingException("Data length byte size: [" + byteCount + "] is incorrect/too big");
        }
        byte[] intSizeBytes = readNBytes(byteCount, derEncodedIs);
        return new BigInteger(1, intSizeBytes).intValue();
    }

    /**
     * The first two nodes of the OID are encoded onto a single byte.
     * The first node is multiplied by the decimal 40 and the result is added to the value of the second node.
     * Node values less than or equal to 127 are encoded in one byte.
     * Node values greater than or equal to 128 are encoded on multiple bytes.
     * Bit 7 of the leftmost byte is set to one. Bits 0 through 6 of each byte contains the encoded value.
     *
     * @param oidBytes the byte array containing the OID
     * @return the decoded OID as a string
     */
    public static String decodeOID(byte[] oidBytes) {

        int length = oidBytes.length;
        StringBuilder sb = new StringBuilder(length * 4);

        int fromPos = 0;
        for (int i = 0; i < length; i++) {
            // if the 8th bit is set, it means the next byte is part of the current segment
            if ((oidBytes[i] & 0x80) != 0) {
                continue;
            }

            // decode the OID segment
            long decodedValue = decodeBytes(oidBytes, fromPos, i - fromPos + 1);
            if (fromPos == 0) {
                // first OID segment consists of two numbers
                if (decodedValue < 80) {
                    sb.append(decodedValue / 40);
                    decodedValue = decodedValue % 40;
                } else {
                    sb.append('2');
                    decodedValue = decodedValue - 80;
                }
            }

            //add next OID segment
            sb.append('.');
            sb.append(decodedValue);
            fromPos = i + 1;
        }
        return sb.toString();
    }

    /**
     * Decode a byte array into a long value. The most significant bit of each  byte is ignored because it
     * is used as a continuation flag. Bits are shifted to the left so that the most significant 7 bits are in first byte
     * next 7 bits in second byte and so on.
     *
     * @param inBytes  the input byte array
     * @param iOffset start point inside <code>inBytes</code>
     * @param iLength number of bytes to decode
     * @return long value decoded from the byte array.
     */
    private static long decodeBytes(byte[] inBytes, int iOffset, int iLength) {
        // check if the OID segment is too big to decode with long value!
        if (iLength > 8) {
            throw new IllegalArgumentException("OID segment too long to parse: ["+iLength+"]");
        }
        if (iLength > 1) {
            int iSteps = iLength - 1;
            return ((long) (inBytes[iOffset] & 0x07f) << 7 * iSteps)
                    + decodeBytes(inBytes, iOffset + 1, iSteps);
        } else {
            return inBytes[iOffset] & 0x07f;
        }
    }
}
