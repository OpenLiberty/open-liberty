/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.crypto.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HashedData {
    private final static String DEFAULT_ALGORITHM = PasswordHashGenerator.getDefaultAlgorithm();
    private final static int DEFAULT_ITERATION = PasswordHashGenerator.getDefaultIteration();
    private final static int DEFAULT_OUTPUT_LENGTH = PasswordHashGenerator.getDefaultOutputLength();
    private final static int LR_LENGTH = 4; // length of length field in byte array.
    private final static int TAG_LENGTH = 1; // length of tag field in byte array.
    private final static byte TAG_VERSION_V1 = (byte) 0x01;
    private final static byte TAG_ALGORITHM = (byte) 0x10;
    private final static byte TAG_ITERATION = (byte) 0x20;
    private final static byte TAG_SALT = (byte) 0x30;
    private final static byte TAG_DIGEST = (byte) 0x40;
    private final static byte TAG_OUTPUT_LENGTH = (byte) 0x50;
    private final char[] plain;
    private String algorithm;
    private int iteration;
    private int length;
    private byte[] salt;
    private byte[] digest;
    private final byte[] bytes;
    private static final Class<?> CLASS_NAME = HashedData.class;
    private final static Logger logger = Logger.getLogger(CLASS_NAME.getCanonicalName());

    HashedData() {
        plain = null;
        algorithm = null;
        iteration = -1;
        salt = null;
        digest = null;
        bytes = null;
        length = 0;
    }

    HashedData(char[] plain, String algorithm, byte[] salt, int iteration, int length, byte[] digest) {
        this.plain = plain;
        this.algorithm = algorithm;
        this.iteration = iteration;
        this.salt = salt;
        this.digest = digest;
        this.length = length;
        bytes = null;
    }

// format
//  TAG_VERSION <TAG_xxx          LENGTH     Data> * any
//    0x01        0xX0 <Big Endian Int value><data>
    HashedData(byte[] input) throws InvalidPasswordCipherException {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("ctor:HashedData");
            logger.fine(PasswordHashGenerator.hexDump(input));
        }
        if (input == null) {
            throw new InvalidPasswordCipherException("Invalid format: null object.");
        }
        try {
            ByteArrayInputStream buffer = new ByteArrayInputStream(input);
            if (TAG_VERSION_V1 != (byte) (buffer.read() & 0xFF)) {
                throw new InvalidPasswordCipherException("Invalid format: invalid data identifier.");
            }
            algorithm = DEFAULT_ALGORITHM;
            iteration = DEFAULT_ITERATION;
            length = DEFAULT_OUTPUT_LENGTH;
            plain = null;
            salt = null;
            digest = null;
            bytes = input;
            while (buffer.available() > 0) {
                byte id = (byte) (buffer.read() & 0xff);
                switch (id) {
                    case TAG_ALGORITHM:
                        algorithm = readString(buffer);
                        break;
                    case TAG_ITERATION:
                        iteration = readInt(buffer);
                        break;
                    case TAG_SALT:
                        salt = readByte(buffer);
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("salt length : " + salt.length);
                            logger.fine(PasswordHashGenerator.hexDump(salt));
                        }
                        break;
                    case TAG_DIGEST:
                        digest = readByte(buffer);
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("digest length : " + digest.length);
                            logger.fine(PasswordHashGenerator.hexDump(digest));
                        }
                        break;
                    case TAG_OUTPUT_LENGTH:
                        length = readInt(buffer);
                        break;
                    default:
                        throw new InvalidPasswordCipherException("Invalid format: data contains unknown identifier.");
                }
            }
            if (salt == null || digest == null) {
                throw new InvalidPasswordCipherException("Invalid format: one of required data is missing.");
            }
        } catch (InvalidPasswordCipherException ipe) {
            throw ipe;
        } catch (Exception e) {
            throw (InvalidPasswordCipherException) new InvalidPasswordCipherException(e.getMessage()).initCause(e);
        }
    }

    public byte[] toBytes() throws InvalidPasswordCipherException {
        byte[] output = null;
        if (bytes != null) {
            output = new byte[bytes.length];
            System.arraycopy(bytes, 0, output, 0, bytes.length);
            return output;
        } else if (algorithm == null || algorithm.length() == 0 || iteration == 0 || salt == null || salt.length == 0) {
            return output;
        }

        try {
            if (digest == null) {
                getDigest();
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            buffer.write(TAG_VERSION_V1);
            if (!DEFAULT_ALGORITHM.equals(algorithm)) {
                buffer.write(TAG_ALGORITHM);
                writeString(buffer, algorithm);
            }
            if (iteration != DEFAULT_ITERATION) {
                buffer.write(TAG_ITERATION);
                writeInt(buffer, iteration);
            }
            if (length != DEFAULT_OUTPUT_LENGTH) {
                buffer.write(TAG_OUTPUT_LENGTH);
                writeInt(buffer, length);
            }
            buffer.write(TAG_SALT);
            writeByte(buffer, salt);
            buffer.write(TAG_DIGEST);
            writeByte(buffer, digest);
            output = buffer.toByteArray();
        } catch (Exception e) {
            throw (InvalidPasswordCipherException) new InvalidPasswordCipherException("An error while serializing object").initCause(e);
        }
        return output;
    }

    public char[] getPlain() {
        char[] output = null;
        if (plain != null) {
            output = plain.clone();
        }
        return output;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public int getIteration() {
        return iteration;
    }

    public byte[] getSalt() {
        byte[] output = null;
        if (salt != null) {
            output = salt.clone();
        }
        return output;
    }

    public byte[] getDigest() throws InvalidPasswordCipherException {
        if (digest == null) {
            // ToDo: generate it
            digest = PasswordHashGenerator.digest(this);
        }
        byte[] output = null;
        if (digest != null) {
            output = digest.clone();
        }
        return output;
    }

    public int getOutputLength() {
        return length;
    }

    private int readInt(ByteArrayInputStream input) throws NumberFormatException {
        if (input != null) {
            byte[] data = new byte[LR_LENGTH];
            input.read(data, 0, LR_LENGTH);
            return readInt(data, 0);
        } else {
            throw new NumberFormatException("null object");
        }
    }

    private int readInt(final byte[] input, final int offset) throws NumberFormatException {
        int output = 0;
        if (input != null && input.length >= LR_LENGTH + offset) {
            for (int i = 0; i < LR_LENGTH; i++) {
                output = (output << 8) | (input[i + offset] & 0xff);
            }
        } else {
            throw new NumberFormatException("either length of byte array or offset is not valid.");
        }
        return output;
    }

    private String readString(ByteArrayInputStream input) throws InvalidPasswordCipherException, UnsupportedEncodingException {
        String output = null;
        if (input != null) {
            int length = readInt(input);
            if (length > 0) {
                byte[] data = new byte[length];
                input.read(data, 0, length);
                output = new String(data, StandardCharsets.UTF_8);
            }
        } else {
            throw new InvalidPasswordCipherException("null object");
        }
        return output;
    }

    private byte[] readByte(ByteArrayInputStream input) throws InvalidPasswordCipherException {
        byte[] output = null;
        if (input != null) {
            int length = readInt(input);
            if (length > 0) {
                output = new byte[length];
                input.read(output, 0, length);
            }
        } else {
            throw new InvalidPasswordCipherException("null object");
        }
        return output;
    }

    private void writeInt(ByteArrayOutputStream output, int value) throws InvalidPasswordCipherException {
        if (output != null) {
            byte[] b = toByte(value);
            output.write(b, 0, b.length);
        } else {
            throw new InvalidPasswordCipherException("null object");
        }
    }

    private void writeString(ByteArrayOutputStream output, String value) throws InvalidPasswordCipherException, UnsupportedEncodingException {
        if (output != null && value != null) {
            byte[] b = toByte(value.length());
            output.write(b, 0, b.length);
            b = value.getBytes(StandardCharsets.UTF_8);
            output.write(b, 0, b.length);
        } else {
            throw new InvalidPasswordCipherException("null object");
        }
    }

    private void writeByte(ByteArrayOutputStream output, byte[] value) throws InvalidPasswordCipherException {
        if (output != null && value != null) {
            byte[] b = toByte(value.length);
            output.write(b, 0, b.length);
            output.write(value, 0, value.length);
        } else {
            throw new InvalidPasswordCipherException("null object");
        }
    }

    private byte[] toByte(int input) {
        byte[] output = new byte[4];
        output[0] = (byte) ((input >> 24) & 0xff);
        output[1] = (byte) ((input >> 16) & 0xff);
        output[2] = (byte) ((input >> 8) & 0xff);
        output[3] = (byte) ((input >> 0) & 0xff);
        return output;
    }
}
