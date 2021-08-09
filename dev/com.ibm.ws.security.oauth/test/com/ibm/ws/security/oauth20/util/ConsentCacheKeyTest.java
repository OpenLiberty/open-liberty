/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

/**
 *
 */
public class ConsentCacheKeyTest {
    private static final String CLIENT_ID = "client_id";
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String SCOPE = "scope";
    private static final String RESOURCE_ID = "resource_id";
    private static final int KEY_LIFETIME_SECONDS = 60;

    private static SharedOutputManager outputMgr;

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    @Test
    public void testGetLifetime() {
        ConsentCacheKey key = new ConsentCacheKey(CLIENT_ID, REDIRECT_URI, SCOPE, RESOURCE_ID, KEY_LIFETIME_SECONDS);
        assertEquals("Incorrect key lifetime value.", KEY_LIFETIME_SECONDS, key.getLifetime());
        key = new ConsentCacheKey(CLIENT_ID, REDIRECT_URI, SCOPE, RESOURCE_ID, 0);
        assertEquals("Incorrect key lifetime value when initialized to 0.", 0, key.getLifetime());
        key = new ConsentCacheKey(CLIENT_ID, REDIRECT_URI, SCOPE, RESOURCE_ID, -100);
        assertEquals("Negative key lifetime value was incorrectly set.", 0, key.getLifetime());
    }

    @Test
    public void testGetExpiration() {
        String methodName = "testGetExpiration";
        try {
            final long startTime = new Date().getTime();
            ConsentCacheKey key = new ConsentCacheKey(CLIENT_ID, REDIRECT_URI, SCOPE, RESOURCE_ID, KEY_LIFETIME_SECONDS);
            ConsentCacheKey keyZeroLifetime = new ConsentCacheKey(CLIENT_ID, REDIRECT_URI, SCOPE, RESOURCE_ID, 0);
            ConsentCacheKey keyNegLifetime = new ConsentCacheKey(CLIENT_ID, REDIRECT_URI, SCOPE, RESOURCE_ID, -100);
            final long endTime = new Date().getTime();
            final long expirationLowerBound = startTime + (KEY_LIFETIME_SECONDS * 1000L);
            final long expirationUpperBound = endTime + (KEY_LIFETIME_SECONDS * 1000L);

            assertTrue("Expiration date is earlier than expected.", key.getExpiration() >= expirationLowerBound);
            assertTrue("Expiration date is later than expected.", key.getExpiration() <= expirationUpperBound);
            assertTrue("Cache key initiated with lifetime of 0 expires earlier than expected.", keyZeroLifetime.getExpiration() >= startTime);
            assertTrue("Cache key initiated with lifetime of 0 expires later than expected.", keyZeroLifetime.getExpiration() <= endTime);
            assertTrue("Cache key initiated with a negative lifetime expires earlier than expected.", keyNegLifetime.getExpiration() >= startTime);
            assertTrue("Cache key initiated with a negative lifetime expires later than expected.", keyNegLifetime.getExpiration() <= endTime);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testIsValid() {
        ConsentCacheKey key = new ConsentCacheKey(CLIENT_ID, REDIRECT_URI, SCOPE, RESOURCE_ID, 2);
        assertTrue("Cache key should be valid.", key.isValid());
        try {
            Thread.sleep(2000);
            for (int i = 0; i < 10; i++) {
                Thread.sleep(500);
                if (key.isValid())
                    break;
            }
            assertFalse("Cache key should not be valid.", key.isValid());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            fail("Sleep was interrupted");
        }

    }

    @Test
    public void testKeyEquality() {
        ConsentCacheKey key = new ConsentCacheKey(CLIENT_ID, REDIRECT_URI, SCOPE, RESOURCE_ID, KEY_LIFETIME_SECONDS);
        ConsentCacheKey equalKey = new ConsentCacheKey(CLIENT_ID, REDIRECT_URI, SCOPE, RESOURCE_ID, KEY_LIFETIME_SECONDS);
        ConsentCacheKey equalKeyDiffLifetime = new ConsentCacheKey(CLIENT_ID, REDIRECT_URI, SCOPE, RESOURCE_ID, (KEY_LIFETIME_SECONDS + 1));
        ConsentCacheKey unequalKeyClientId = new ConsentCacheKey("fail", REDIRECT_URI, SCOPE, RESOURCE_ID, KEY_LIFETIME_SECONDS);
        ConsentCacheKey unequalKeyClientIdNull = new ConsentCacheKey(null, REDIRECT_URI, SCOPE, RESOURCE_ID, KEY_LIFETIME_SECONDS);
        ConsentCacheKey unequalKeyRedirectUri = new ConsentCacheKey(CLIENT_ID, "fail", SCOPE, RESOURCE_ID, KEY_LIFETIME_SECONDS);
        ConsentCacheKey unequalKeyRedirectUriNull = new ConsentCacheKey(CLIENT_ID, null, SCOPE, RESOURCE_ID, KEY_LIFETIME_SECONDS);
        ConsentCacheKey unequalKeyScope = new ConsentCacheKey(CLIENT_ID, REDIRECT_URI, "fail", RESOURCE_ID, KEY_LIFETIME_SECONDS);
        ConsentCacheKey unequalKeyScopeNull = new ConsentCacheKey(CLIENT_ID, REDIRECT_URI, null, RESOURCE_ID, KEY_LIFETIME_SECONDS);
        ConsentCacheKey unequalKeyResourceId = new ConsentCacheKey(CLIENT_ID, REDIRECT_URI, SCOPE, "fail", KEY_LIFETIME_SECONDS);
        ConsentCacheKey unequalKeyResourceIdNull = new ConsentCacheKey(CLIENT_ID, REDIRECT_URI, SCOPE, null, KEY_LIFETIME_SECONDS);
        ConsentCacheKey nullKey = new ConsentCacheKey(null, null, null, null, KEY_LIFETIME_SECONDS);
        ConsentCacheKey nullKey2 = new ConsentCacheKey(null, null, null, null, (KEY_LIFETIME_SECONDS + 1));

        // Positive tests
        assertTrue("A cache key should be equal to itself.", key.equals(key));
        assertTrue("A cache key should be equal to an identical cache key.", key.equals(equalKey));
        assertTrue("A cache key should be equal to another key with all equivalent members except for key lifetime.", key.equals(equalKeyDiffLifetime));
        assertTrue("Cache keys with all null members should be equivalent.", nullKey.equals(nullKey2));

        // Negative tests
        assertFalse("A non-null cache key should not equal null.", key.equals(null));
        assertFalse("A cache key should not equal an object of a different class.", key.equals(new Object()));
        assertFalse("A cache key with null members should not equal another key with non-null members.", key.equals(nullKey));
        assertFalse("Identical cache keys should have identical client IDs.", key.equals(unequalKeyClientId));
        assertFalse("Identical cache keys should have identical redirect URIs.", key.equals(unequalKeyRedirectUri));
        assertFalse("Identical cache keys should have identical scopes.", key.equals(unequalKeyScope));
        assertFalse("Identical cache keys should have identical resource IDs.", key.equals(unequalKeyResourceId));
        assertFalse("Cache keys with null client ID should not equal keys with non-null client ID.", unequalKeyClientIdNull.equals(key));
        assertFalse("Cache keys with null redirect URI should not equal keys with non-null redirect URI.", unequalKeyRedirectUriNull.equals(key));
        assertFalse("Cache keys with null scope should not equal keys with non-null scope.", unequalKeyScopeNull.equals(key));
        assertFalse("Cache keys with null resource ID should not equal keys with non-null resource ID.", unequalKeyResourceIdNull.equals(key));

        // Make sure the key can be found in some type of collection
        Map<ConsentCacheKey, String> map = new HashMap<ConsentCacheKey, String>();
        map.put(key, "key");
        map.put(unequalKeyClientId, "unequalKeyClientId");
        map.put(nullKey, "null key");
        assertTrue("Cache key could not be found in collection.", map.containsKey(key));
        assertTrue("Cache key could not be found in collection using an identical, but different, cache key object.", map.containsKey(equalKey));
        assertTrue("Cache key with null members could not be found in collection.", map.containsKey(nullKey));
        assertFalse("Entry found using a cache key that was not included in collection.", map.containsKey(unequalKeyRedirectUri));
    }
}
