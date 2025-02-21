/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.io.UnsupportedEncodingException;

public class HashedDataSHA512Test extends BaseHashedDataTest {

    byte[] expectDefault = { 0x01, 0x10, 0x00, 0x00, 0x00, 0x14, 0x50, 0x42, 0x4B, 0x44, 0x46, 0x32, 0x57, 0x69, 0x74, 0x68, 0x48,
                            0x6D, 0x61, 0x63, 0x53, 0x48, 0x41, 0x35, 0x31, 0x32, 0x30, 0x00, 0x00, 0x00, 0x04, 0x73, 0x61, 0x6C,
                            0x74, 0x40, 0x00, 0x00, 0x00, 0x20, (byte) (0xA1 & 0xFF), 0x7A, 0x1E, (byte) (0x96 & 0xFF),
                            (byte) (0xA3 & 0xFF), 0x6F, 0x7D, (byte) (0xCC & 0xFF), (byte) (0xB0 & 0xFF), (byte) (0x99 & 0xFF),
                            0x76, (byte) (0x84 & 0xFF), (byte) (0xAB & 0xFF), 0x13, 0x3C, (byte) (0xFF & 0xFF), (byte) (0x80 & 0xFF),
                            0x47, 0x61, (byte) (0xE4 & 0xFF), 0x71, (byte) (0xFB & 0xFF), (byte) (0xD7 & 0xFF), 0x3C,
                            (byte) (0xF3 & 0xFF), 0x46, 0x41, (byte) (0x9C & 0xFF), (byte) (0xCA & 0xFF), 0x1F, 0x0A,
                            (byte) (0xD4 & 0xFF) };

    byte[] expectCustom = { 0x01, 0x10, 0x00, 0x00, 0x00, 0x14, 0x50, 0x42, 0x4B, 0x44, 0x46, 0x32, 0x57, 0x69, 0x74, 0x68, 0x48,
                            0x6D, 0x61, 0x63, 0x53, 0x48, 0x41, 0x35, 0x31, 0x32, 0x20, 0x00, 0x00, 0x03, (byte) (0xDD & 0xFF), 0x50,
                            0x00, 0x00, 0x00, 0x40, 0x30, 0x00, 0x00, 0x00, 0x05, 0x73, 0x61, 0x6C, 0x74, 0x32, 0x40, 0x00, 0x00,
                            0x00, 0x08, (byte) (0xAB & 0xFF), 0x00, 0x75, 0x2D, 0x7B, (byte) (0xE6 & 0xFF), (byte) (0x97 & 0xFF),
                            0x01 };

    private final static String ALGORITHM = "PBKDF2WithHmacSHA512";
    private final static int OUTPUT_LENGTH = 256;
    private final static int ITERATION = 6384;

    @Test
    public void testConstructorDefault() throws UnsupportedEncodingException {
        super.testConstructorDefault("password".toCharArray(), "salt".getBytes("UTF-8"), ALGORITHM, ITERATION, OUTPUT_LENGTH, expectDefault);
    }

    @Test
    public void testConstructorCustom() throws UnsupportedEncodingException {
        super.testConstructorDefault("password2".toCharArray(), "salt2".getBytes("UTF-8"), ALGORITHM, 989, 64, expectCustom);
    }

    @Test
    public void testConstructorDigest() throws UnsupportedEncodingException {
        super.testConstructorDigest("salt".getBytes("UTF-8"), ALGORITHM, ITERATION, OUTPUT_LENGTH, expectDefault);
    }

    @Test
    public void testConstructorDigestCustom() throws UnsupportedEncodingException {
        super.testConstructorDigest("salt2".getBytes("UTF-8"), ALGORITHM, 989, 64, expectCustom);
    }

    @Test
    public void testConstructorInvalidAlgorithm() throws UnsupportedEncodingException {
        super.testConstructorException("password2".toCharArray(), "salt2".getBytes("UTF-8"), "DoesnotExist", 989, 128);
    }

    @Test
    public void testConstructorInvalidIteration() throws UnsupportedEncodingException {
        super.testConstructorException("password2".toCharArray(), "salt2".getBytes("UTF-8"), ALGORITHM, -1, 512);
    }
}