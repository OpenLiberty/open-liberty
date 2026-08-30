/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.jni;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Tests for NativeMethodUtils.
 */
public class NativeMethodUtilsTest {

    /**
     * Under test:
     * {@link com.ibm.ws.zos.jni.NativeMethodUtils#convertToASCII(List<byte[]>)}
     *
     * Ensure the conversion from ASCII to EBCDIC and back to ASCII is symmetric.
     */
    @Test
    public void asciiToEbcdicToAsciiConversionLists() throws Exception {
        List<String> asciiStrList = new ArrayList<String>();
        asciiStrList.add("plain");
        asciiStrList.add("old");
        asciiStrList.add("ascii");
        asciiStrList.add("string");
        asciiStrList.add("list");

        List<byte[]> ebcdicBytesList = new ArrayList<byte[]>();
        for (String asciiStr : asciiStrList) {
            byte[] b = NativeMethodUtils.convertToEBCDIC(asciiStr);
            ebcdicBytesList.add(b);
        }

        List<String> convStrList = NativeMethodUtils.convertToASCII(ebcdicBytesList);
        for (int i = 0; i < asciiStrList.size(); i++) {
            String asciiStr = asciiStrList.get(i);
            String convStr = convStrList.get(i);
            assertEquals(asciiStr, convStr);
        }
    }

    /**
     * Under test:
     * {@link com.ibm.ws.zos.jni.NativeMethodUtils#convertToASCII(byte[])}
     * {@link com.ibm.ws.zos.jni.NativeMethodUtils#convertToEBCDIC(String)}
     *
     * Ensure the conversion from ASCII to EBCDIC and back to ASCII is symmetric.
     */
    @Test
    public void asciiToEbcdicToAsciiConversions() throws Exception {
        String asciiStr = "plain old ascii string";
        byte[] b = NativeMethodUtils.convertToEBCDIC(asciiStr);
        String convStr = NativeMethodUtils.convertToASCII(b);
        assertEquals(asciiStr, convStr);
    }

    /**
     * Verify the a2e cache works as expected.
     */
    @Test
    public void a2eCache() throws Exception {
        String a1 = "plain old ascii string";
        String a2 = "plain old ";
        a2 += "ascii string"; // So compiler doesn't optimize and assign a1 and a2 to the same object.
        assertNotSame(a1, a2);
        String a3 = "different string";
        byte[] b = NativeMethodUtils.convertToEBCDIC(a1);
        byte[] c = NativeMethodUtils.convertToEBCDIC(a2);
        byte[] d = NativeMethodUtils.convertToEBCDIC(a3);
        assertSame(b, c);
        assertNotSame(c, d);
    }
}
