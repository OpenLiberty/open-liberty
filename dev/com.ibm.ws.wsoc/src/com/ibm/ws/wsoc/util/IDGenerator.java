/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.security.Security;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class IDGenerator {

    // This class was copied (shamelessly) from a class the Session code uses.

    /*
     * int variable that stores the length of sessions to create.
     */
    private int _sessionIDLength = 0;

    private static final char[] sBitChars = { 'G', '9', 'U', 'i', 'b', 'w', '-', '8', '6', 'z', 'u', 'p', 'J', 'R', 'S', 'h', 'K', '5', 'n', 'c', '4', 'C', 't', 'I', 'W', '7',
                                             'F',
                                             'e', 'M', 'g', 'q', '2', '3', 'V', 'Z', 'k', 'O', 'D', 'a', 'v', 'y', 'Y', 'P', 'X', 'E', 'N', '1', 'f', 'l', 'B', '0', 'L', 's', 'o',
                                             'A', 'T', 'd', 'x', 'm', 'r', 'Q', '_', 'j', 'H' };

    /*
     * The mask for the second bytes
     */
    private static final int[] sSecondByteMasks = { 0x0, 0x1, 0x3, 0x7, 0xf, 0x1f, 0x3f };

    /*
     * An instance of the SecureRandom class that will be used to generate
     * random byte array strings.
     */
    private SecureRandom _random = null;

    /*
     * int size of the byte array that needs to be instantiated every time a new
     * session id has to be generated.
     */
    private int _byteArraySize = 0;

    /*
     * logger for this class
     */
    private static final TraceComponent tc = Tr.register(IDGenerator.class);

    // ----------------------------------------
    // Class Constructor
    // ----------------------------------------
    public IDGenerator() {
        this(23); // default
    }

    /*
     * @param sessionIDLength -
     */
    @FFDCIgnore(Exception.class)
    public IDGenerator(int sessionIDLength) {
        _sessionIDLength = sessionIDLength;
        _byteArraySize = ((sessionIDLength * 6) / 8) + 1;

        String jceProvider = getSystemProperty("DEFAULT_JCE_PROVIDER");
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JCE Provider is " + jceProvider);
        }
        try {
            _random = SecureRandom.getInstance("IBMSecureRandom", jceProvider);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "SessionIdGeneratorImpl.UsingDefaultSecureRandom");
            }
            _random = new SecureRandom();
        }
        // force SecureRandom to seed itself
        byte[] genBytes = new byte[_byteArraySize];
        _random.nextBytes(genBytes);
        // supplement seed with timestamp and thread id
        _random.setSeed(System.currentTimeMillis());
        _random.setSeed(Thread.currentThread().getName().hashCode());
    }

    public static String getSystemProperty(final String key) {
        String value = AccessController.doPrivileged(new PrivilegedAction<String>()
        {
            @Override
            public String run()
            {
                return (Security.getProperty(key));
            }
        });
        return (value);
    }

    // ----------------------------------------
    // Public methods
    // ----------------------------------------
    public String getID() {
        byte[] genBytes = new byte[_byteArraySize];
        synchronized (_random) {
            _random.nextBytes(genBytes);
        }
        String id = convertSessionIdBytesToSessionId(genBytes, _sessionIDLength);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "sessionID=" + id);
        }
        return id;
    }

    /**
     * Method convertSessionIdBytesToSessionIdBase64 Converts the specified byte
     * array to a String to be used for the session id. The conversion is
     * performed breaking the byte array into groups of 6 bits, then taking each
     * group (value 0-63) and converting to a character 'A'-'Z',
     * 'a'-'z','0'-'9',_,-.
     * 
     * @param pBytes
     * @return String
     */
    public static String convertSessionIdBytesToSessionId(byte[] pBytes, int sessionIdLength) {
        char[] chars = new char[sessionIdLength];

        int byteNum = 0;
        int bitNum = 0;
        int pos = 0;
        while (byteNum < pBytes.length && pos < sessionIdLength) {
            int val = 0;

            // Get from the byte
            if (bitNum < 3) {
                val = (pBytes[byteNum] >> (2 - bitNum)) & 0x3f;
            }
            // Get from this byte and the next
            else {
                val = pBytes[byteNum] << (6 - (8 - bitNum));
                if (byteNum + 1 < pBytes.length) {
                    int secondVal = (pBytes[byteNum + 1] & 0x000000ff) >> 8 - (6 - (8 - bitNum));
                    secondVal &= sSecondByteMasks[6 - (8 - bitNum)];
                    val |= secondVal;
                }
                val &= 0x3f;
            }

            // Assign the character
            chars[pos++] = sBitChars[val];

            // Increment to the next character
            bitNum += 6;
            if (bitNum >= 8) {
                byteNum++;
                bitNum -= 8;
            }
        }

        return new String(chars);
    }

}