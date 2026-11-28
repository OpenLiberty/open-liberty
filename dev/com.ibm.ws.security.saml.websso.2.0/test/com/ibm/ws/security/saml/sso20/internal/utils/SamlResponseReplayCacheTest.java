/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;

import com.ibm.ws.security.saml.error.SamlException;

import test.common.SharedOutputManager;

public class SamlResponseReplayCacheTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static final String PROVIDER = "defaultSP";
    private static final String ISSUER = "https://idp.example.com";
    private static final long FIVE_MINUTES = 5 * 60 * 1000L;

    private SamlResponseReplayCache cache;

    @Before
    public void setUp() {
        cache = new SamlResponseReplayCache(0, 0, 0, PROVIDER);
    }

    @After
    public void tearDown() {
        cache.shutdown();
    }

    private static SubjectConfirmation confirmation(String method, Long notOnOrAfterMs) {
        SubjectConfirmation sc = mock(SubjectConfirmation.class);
        when(sc.getMethod()).thenReturn(method);
        if (notOnOrAfterMs != null) {
            SubjectConfirmationData scd = mock(SubjectConfirmationData.class);
            when(scd.getNotOnOrAfter()).thenReturn(Instant.ofEpochMilli(notOnOrAfterMs));
            when(sc.getSubjectConfirmationData()).thenReturn(scd);
        }
        return sc;
    }

    private static Assertion assertion(String id, String issuer, SubjectConfirmation... confirmations) {
        Assertion a = mock(Assertion.class);
        when(a.getID()).thenReturn(id);
        Issuer iss = mock(Issuer.class);
        when(iss.getValue()).thenReturn(issuer);
        when(a.getIssuer()).thenReturn(iss);
        Subject subject = mock(Subject.class);
        List<SubjectConfirmation> list = new ArrayList<SubjectConfirmation>();
        for (SubjectConfirmation sc : confirmations) {
            list.add(sc);
        }
        when(subject.getSubjectConfirmations()).thenReturn(list);
        when(a.getSubject()).thenReturn(subject);
        return a;
    }

    private static Assertion bearerAssertion(String id, long expiryMs) {
        return assertion(id, ISSUER, confirmation(SubjectConfirmation.METHOD_BEARER, expiryMs));
    }

    private static long inFuture(long ms) {
        return System.currentTimeMillis() + ms;
    }

    /** Stub shared store. */
    private static class StubStore implements SamlReplayStore {
        final AtomicInteger calls = new AtomicInteger();
        volatile boolean replay;
        volatile boolean fail;

        @Override
        public boolean registerIfAbsent(String key, long expiryMs) throws SamlReplayStoreException {
            calls.incrementAndGet();
            if (fail) {
                throw new SamlReplayStoreException("store down");
            }
            return replay;
        }
    }

    @Test
    public void firstUseIsNotReplaySecondUseIs() throws Exception {
        Assertion a = bearerAssertion("id1", inFuture(60000));
        assertFalse(cache.detectReplayAndStoreAssertion(a));
        assertTrue(cache.detectReplayAndStoreAssertion(a));
    }

    @Test
    public void expiredAssertionIsNotReplay() throws Exception {
        Assertion a = bearerAssertion("id1", System.currentTimeMillis() - 60000);
        assertFalse(cache.detectReplayAndStoreAssertion(a));
        assertFalse(cache.detectReplayAndStoreAssertion(a));
    }

    @Test
    public void evictionHonoursClockSkew() throws Exception {
        SamlResponseReplayCache skewed = new SamlResponseReplayCache(0, 0, FIVE_MINUTES, PROVIDER);
        try {
            // Expired one minute ago, but still inside the 5 minute clock skew.
            Assertion a = bearerAssertion("id1", System.currentTimeMillis() - 60000);
            assertFalse(skewed.detectReplayAndStoreAssertion(a));
            skewed.evictStaleEntries();
            assertTrue("entry inside the skew window must survive eviction", skewed.detectReplayAndStoreAssertion(a));
        } finally {
            skewed.shutdown();
        }
    }

    @Test
    public void usesBearerConfirmationNotFirstConfirmation() throws Exception {
        long expiry = inFuture(60000);
        // First confirmation is holder-of-key and has no SubjectConfirmationData.
        Assertion a = assertion("id1", ISSUER,
                                confirmation("urn:oasis:names:tc:SAML:2.0:cm:holder-of-key", null),
                                confirmation(SubjectConfirmation.METHOD_BEARER, expiry));
        assertEquals(expiry, cache.getReplayExpiryMs(a));
        assertFalse(cache.detectReplayAndStoreAssertion(a));
        assertTrue(cache.detectReplayAndStoreAssertion(a));
    }

    @Test
    public void keyIsScopedByProviderAndIssuer() throws Exception {
        long expiry = inFuture(60000);
        Assertion a = assertion("id1", ISSUER, confirmation(SubjectConfirmation.METHOD_BEARER, expiry));
        Assertion otherIssuer = assertion("id1", "https://other-idp.example.com", confirmation(SubjectConfirmation.METHOD_BEARER, expiry));
        SamlResponseReplayCache otherProvider = new SamlResponseReplayCache(0, 0, 0, "otherSP");
        try {
            assertNotEquals(cache.buildKey(a), cache.buildKey(otherIssuer));
            assertNotEquals(cache.buildKey(a), otherProvider.buildKey(a));
            assertFalse(cache.detectReplayAndStoreAssertion(a));
            assertFalse("same id from another issuer is not a replay", cache.detectReplayAndStoreAssertion(otherIssuer));
        } finally {
            otherProvider.shutdown();
        }
    }

    @Test
    public void concurrentSubmissionOfSameAssertionYieldsOneAcceptance() throws Exception {
        final Assertion a = bearerAssertion("id1", inFuture(60000));
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        final CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
        try {
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return cache.detectReplayAndStoreAssertion(a);
                }));
            }
            start.countDown();
            int accepted = 0;
            for (Future<Boolean> f : results) {
                if (!f.get(10, TimeUnit.SECONDS)) {
                    accepted++;
                }
            }
            assertEquals(1, accepted);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void sharedStoreReportingReplayIsReplay() throws Exception {
        StubStore store = new StubStore();
        store.replay = true;
        cache.setSharedStore(store, false);
        assertTrue(cache.detectReplayAndStoreAssertion(bearerAssertion("id1", inFuture(60000))));
        assertEquals(1, store.calls.get());
    }

    @Test
    public void sharedStoreReportingFirstUseIsNotReplay() throws Exception {
        StubStore store = new StubStore();
        store.replay = false;
        cache.setSharedStore(store, false);
        assertFalse(cache.detectReplayAndStoreAssertion(bearerAssertion("id1", inFuture(60000))));
        assertEquals(1, store.calls.get());
    }

    @Test
    public void localReplayShortCircuitsSharedStore() throws Exception {
        StubStore store = new StubStore();
        cache.setSharedStore(store, false);
        Assertion a = bearerAssertion("id1", inFuture(60000));
        assertFalse(cache.detectReplayAndStoreAssertion(a));
        assertEquals(1, store.calls.get());
        assertTrue(cache.detectReplayAndStoreAssertion(a));
        assertEquals("shared store must not be called for a local replay", 1, store.calls.get());
    }

    @Test
    public void storeFailureFailsClosedByDefault() throws Exception {
        StubStore store = new StubStore();
        store.fail = true;
        cache.setSharedStore(store, false);
        try {
            cache.detectReplayAndStoreAssertion(bearerAssertion("id1", inFuture(60000)));
            fail("Expected SamlException when the shared store is unavailable and failOpen=false");
        } catch (SamlException expected) {
            // expected
        }
    }

    @Test
    public void storeFailureFailsOpenWhenConfigured() throws Exception {
        StubStore store = new StubStore();
        store.fail = true;
        cache.setSharedStore(store, true);
        Assertion a = bearerAssertion("id1", inFuture(60000));
        assertFalse(cache.detectReplayAndStoreAssertion(a));
        assertTrue("local detection still works while failing open", cache.detectReplayAndStoreAssertion(a));
    }

    @Test
    public void clearingSharedStoreRestoresLocalOnlyBehaviour() throws Exception {
        StubStore store = new StubStore();
        store.replay = true;
        cache.setSharedStore(store, false);
        cache.setSharedStore(null, false);
        assertFalse(cache.detectReplayAndStoreAssertion(bearerAssertion("id1", inFuture(60000))));
        assertEquals(0, store.calls.get());
    }
}
