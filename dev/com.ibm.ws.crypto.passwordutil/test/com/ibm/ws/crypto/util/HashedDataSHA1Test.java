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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Arrays;
import java.io.UnsupportedEncodingException;


public class HashedDataSHA1Test extends BaseHashedDataTest {
    byte[] expectDefault = { 0x01, 0x30, 0x00, 0x00, 0x00, 0x04, 0x73, 0x61, 0x6C, 0x74, 0x40, 0x00, 0x00, 0x00, 0x20, 0x0A,
                            (byte) (0xFC & 0xFF), (byte) (0xB9 & 0xFF), (byte) (0xE1 & 0xFF), (byte) (0xC2 & 0xFF), 0x1A,
                            (byte) (0xE2 & 0xFF), (byte) (0xCA & 0xFF), 0x56, 0x53, (byte) (0xF1 & 0xFF), (byte) (0xFD & 0xFF),
                            0x73, (byte) (0xC5 & 0xFF), (byte) (0x88 & 0xFF), (byte) (0x8D & 0xFF), 0x32, 0x0A, (byte) (0xFE & 0xFF),
                            (byte) (0x97 & 0xFF), (byte) (0x99 & 0xFF), (byte) (0xDB & 0xFF), 0x11, (byte) (0xFA & 0xFF), 0x13,
                            (byte) (0xB5 & 0xFF), 0x16, 0x31, 0x6B, (byte) (0xB3 & 0xFF), (byte) (0xFD & 0xFF), 0x07 };


    byte[] expectCustom = { 0x01, 0x20, 0x00, 0x00, 0x03, (byte) (0xDD & 0xFF), 0x50, 0x00, 0x00, 0x00, 0x40, 0x30, 0x00, 0x00, 0x00,
                            0x05, 0x73, 0x61, 0x6C, 0x74, 0x32, 0x40, 0x00, 0x00, 0x00, 0x08, (byte) (0x90 & 0xFF), (byte) (0xCC & 0xFF),
                            0x41, 0x0D, 0x6D, (byte) (0x93 & 0xFF), (byte) (0xD1 & 0xFF), 0x1A };

    private final static String ALGORITHM = "PBKDF2WithHmacSHA1";
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