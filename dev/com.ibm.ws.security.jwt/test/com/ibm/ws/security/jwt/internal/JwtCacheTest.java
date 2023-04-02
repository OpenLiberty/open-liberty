/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.jwt.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.jmock.Expectations;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class JwtCacheTest extends CommonTestClass {

    private final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final JwtContext jwtContext = mockery.mock(JwtContext.class);
    private final JwtClaims jwtClaims = mockery.mock(JwtClaims.class);
    private final NumericDate date = mockery.mock(NumericDate.class);

    private final String configId = "myJwtConfigId";

    private JwtCache cache;

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        cache = new JwtCache(1000 * 60);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_put_emptyCache() {
        String jwt = testName.getMethodName();
        long clockSkew = 300;

        assertEquals("Cache should have been empty but wasn't.", 0, cache.size());

        cache.put(jwt, configId, jwtContext, clockSkew);

        assertEquals("Cache size did not match expected size.", 1, cache.size());
    }

    @Test
    public void test_put_sameEntry() {
        String jwt = testName.getMethodName();
        long clockSkew = 300;

        assertEquals("Cache should have been empty but wasn't.", 0, cache.size());

        cache.put(jwt, configId, jwtContext, clockSkew);

        assertEquals("Cache size did not match expected size.", 1, cache.size());

        cache.put(jwt, configId, jwtContext, clockSkew);

        assertEquals("Cache size did not match expected size.", 1, cache.size());
    }

    @Test
    public void test_put_sameKey_differentValues() {
        JwtContext jwtContext2 = mockery.mock(JwtContext.class, "jwtContext2");
        String jwt = testName.getMethodName();
        long clockSkew = 300;

        assertEquals("Cache should have been empty but wasn't.", 0, cache.size());

        cache.put(jwt, configId, jwtContext, clockSkew);

        assertEquals("Cache size did not match expected size.", 1, cache.size());

        cache.put(jwt, configId, jwtContext2, clockSkew);

        assertEquals("Cache size did not match expected size.", 1, cache.size());
    }

    @Test
    public void test_put_differentKey_sameValue() {
        String jwt = testName.getMethodName();
        long clockSkew = 300;

        assertEquals("Cache should have been empty but wasn't.", 0, cache.size());

        cache.put(jwt, configId, jwtContext, clockSkew);

        assertEquals("Cache size did not match expected size.", 1, cache.size());

        cache.put(jwt + "2", configId, jwtContext, clockSkew);

        assertEquals("Cache size did not match expected size.", 2, cache.size());
    }

    @Test
    public void test_get_emptyCache() {
        String jwt = testName.getMethodName();

        Object returnedValue = cache.get(jwt, configId);
        assertNull("Should not have found an entry but did: " + returnedValue, returnedValue);
    }

    @Test
    public void test_get_cacheDoesNotContainKey() {
        String jwt = testName.getMethodName();

        cache.put(jwt, configId, jwtContext, 0);
        cache.put(jwt + "2", configId, jwtContext, 0);

        Object returnedValue = cache.get(jwt + "3", configId);
        assertNull("Should not have found an entry but did: " + returnedValue, returnedValue);
    }

    @Test
    public void test_get_cachedContextMissingClaims() {
        String jwt = testName.getMethodName();

        cache.put(jwt, configId, jwtContext, 0);

        mockery.checking(new Expectations() {
            {
                one(jwtContext).getJwtClaims();
                will(returnValue(null));
            }
        });
        Object returnedValue = cache.get(jwt, configId);
        assertNull("Should have returned null for a cached entry that's missing claims, but got: " + returnedValue, returnedValue);
    }

    @Test
    public void test_get_cachedContextValid() {
        try {
            String jwt = testName.getMethodName();

            cache.put(jwt, configId, jwtContext, 0);

            mockery.checking(new Expectations() {
                {
                    one(jwtContext).getJwtClaims();
                    will(returnValue(jwtClaims));
                    one(jwtClaims).getExpirationTime();
                    will(returnValue(date));
                    one(date).getValueInMillis();
                    will(returnValue(System.currentTimeMillis() + (1000 * 60 * 60 * 2)));
                }
            });
            Object returnedValue = cache.get(jwt, configId);
            assertEquals("Returned value did not match the initial value put in the cache.", jwtContext, returnedValue);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
