/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Test;

import com.ibm.ws.security.openidconnect.client.jose4j.util.OidcTokenImplBase;

public class JtiNonceCacheTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private JtiNonceCache jtiCache = new JtiNonceCache(1, 0);

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testContain() throws Exception {
        OidcTokenImplBase token = createToken("1234", 5);

        ensureJtiIsCachedAndWait(token);

        assertNotFound(jtiCache.contain(token));
    }

    @Test
    public void testEvicted() throws Exception {
        jtiCache = new JtiNonceCache(1, 5000);
        OidcTokenImplBase token = createToken("4567", 5);

        ensureJtiIsCachedAndWait(token);

        Object entry = jtiCache.get(jtiCache.getCacheKey(token));
        assertNull("An entry must not be found in the cache after eviction.", entry);
    }

    private OidcTokenImplBase createToken(final String jwtId, long seconds) {
        OidcTokenImplBase token = mockery.mock(OidcTokenImplBase.class);
        final long expirationTimeInSeconds = (new Date()).getTime() / 1000 + seconds;

        mockery.checking(new Expectations() {
            {
                allowing(token).getJwtId();
                will(returnValue(jwtId));
                allowing(token).getIssuer();
                will(returnValue("issuer"));
                allowing(token).getExpirationTimeSeconds();
                will(returnValue(expirationTimeInSeconds));
            }
        });

        return token;
    }

    private void ensureJtiIsCachedAndWait(OidcTokenImplBase token) throws Exception {
        boolean found = jtiCache.contain(token);
        assertNotFound(found);
        found = jtiCache.contain(token);
        assertFound(found);
        Thread.sleep(10000);
    }

    private void assertNotFound(boolean found) {
        assertFalse("An entry must not be found in the cache.", found);
    }

    private void assertFound(boolean found) {
        assertTrue("An entry must be found in the cache.", found);
    }

}
