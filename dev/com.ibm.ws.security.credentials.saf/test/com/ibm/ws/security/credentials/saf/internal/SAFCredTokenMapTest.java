/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.credentials.saf.internal;

import static org.junit.Assert.assertSame;

import java.util.Random;

import org.junit.Test;

import com.ibm.wsspi.security.credentials.saf.SAFCredential;

/**
 *
 */
public class SAFCredTokenMapTest {

    @Test
    public void testBasic() {

        SAFCredTokenMap safCredTokenMap = new SAFCredTokenMap(null);

        SAFCredentialToken safCredToken1 = new SAFCredentialToken(getRandomBytes(80));
        SAFCredential safCred1 = new SAFCredentialImpl(null, null, (SAFCredential.Type) null);

        SAFCredentialToken safCredToken2 = new SAFCredentialToken(getRandomBytes(20));
        SAFCredential safCred2 = new SAFCredentialImpl(null, null, (SAFCredential.Type) null);

        safCredTokenMap.put(safCred1, safCredToken1);
        safCredTokenMap.put(safCred2, safCredToken2);

        assertSame(safCredToken1, safCredTokenMap.get(safCred1));
        assertSame(safCredToken2, safCredTokenMap.get(safCred2));

        // Verify the keys map.
        assertSame(safCred1, safCredTokenMap.getCredential(safCredToken1.getKey()));
        assertSame(safCred2, safCredTokenMap.getCredential(safCredToken2.getKey()));
    }

    /**
     * @return a byte array of the given length, populated with random bytes.
     */
    private byte[] getRandomBytes(int len) {
        byte[] bytes = new byte[len];
        new Random(System.nanoTime()).nextBytes(bytes);
        return bytes;
    }

}
