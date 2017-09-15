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

public class ByteGenerator {

    /*
     * An instance of the SecureRandom class that will be used to generate
     * random byte array strings.
     */
    private SecureRandom _random = null;

    /*
     * int size of the byte array that needs to be instantiated every time a new
     * session id has to be generated.
     */
    private final int _byteArraySize = 4;

    // Used for websocket accept key
    private final int _keyArraySize = 16;

    /*
     * logger for this class
     */
    private static final TraceComponent tc = Tr.register(ByteGenerator.class);

    /*
     * @param sessionIDLength -
     */
    @FFDCIgnore(Exception.class)
    public ByteGenerator() {

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
    public byte[] getID() {
        byte[] genBytes = new byte[_byteArraySize];
        synchronized (_random) {
            _random.nextBytes(genBytes);
        }
        return genBytes;
    }

    public byte[] getWebsocketKey() {
        byte[] genBytes = new byte[_keyArraySize];
        synchronized (_random) {
            _random.nextBytes(genBytes);
        }
        return genBytes;
    }

}