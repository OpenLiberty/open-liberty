/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.security.mp.jwt.impl.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtToken;

import test.common.SharedOutputManager;

public class JwtPrincipalMappingTest {

    protected final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.mp.jwt.*=all");

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /**
     * Build a JwtToken whose getClaims() returns a Claims backed by the given data map.
     * Claims extends Map<String,Object> so we use an anonymous implementation.
     */
    private JwtToken buildMockToken(Map<String, Object> claimsData) throws Exception {
        // Wrap the data map in a minimal Claims implementation
        Claims claims = new Claims() {
            private final HashMap<String, Object> delegate = new HashMap<String, Object>(claimsData);

            @Override public Object get(Object key) { return delegate.get(key); }
            @Override public boolean containsKey(Object key) { return delegate.containsKey(key); }
            @Override public boolean containsValue(Object value) { return delegate.containsValue(value); }
            @Override public Set<Map.Entry<String, Object>> entrySet() { return delegate.entrySet(); }
            @Override public boolean isEmpty() { return delegate.isEmpty(); }
            @Override public Set<String> keySet() { return delegate.keySet(); }
            @Override public Object put(String key, Object value) { return delegate.put(key, value); }
            @Override public void putAll(Map<? extends String, ? extends Object> m) { delegate.putAll(m); }
            @Override public Object remove(Object key) { return delegate.remove(key); }
            @Override public int size() { return delegate.size(); }
            @Override public Collection<Object> values() { return delegate.values(); }
            @Override public void clear() { delegate.clear(); }
            // Claims-specific methods (not exercised in these tests)
            @Override public String getIssuer() { return (String) delegate.get("iss"); }
            @Override public String getSubject() { return (String) delegate.get("sub"); }
            @Override public List<String> getAudience() { return null; }
            @Override public long getExpiration() { return 0; }
            @Override public long getNotBefore() { return 0; }
            @Override public long getIssuedAt() { return 0; }
            @Override public String getJwtId() { return null; }
            @Override public String getAuthorizedParty() { return null; }
            @Override public <T> T getClaim(String claimName, Class<T> requiredType) { return requiredType.cast(delegate.get(claimName)); }
            @Override public Map<String, Object> getAllClaims() { return delegate; }
            @Override public String toJsonString() { return delegate.toString(); }
        };

        JwtToken token = mockery.mock(JwtToken.class, "jwtToken_" + testName.getMethodName());
        mockery.checking(new Expectations() {
            {
                allowing(token).getClaims();
                will(returnValue(claims));
            }
        });
        return token;
    }

    @Test
    public void testGetMappedRealm_customAttr_claimPresent() {
        try {
            Map<String, Object> claims = new HashMap<String, Object>();
            claims.put("upn", "testuser");
            claims.put("tenant", "TenantA");
            JwtToken token = buildMockToken(claims);

            JwtPrincipalMapping mapping = new JwtPrincipalMapping(token, "upn", "groups", false, "tenant");
            assertEquals("Expected realm from 'tenant' claim", "TenantA", mapping.getMappedRealm());
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void testGetMappedRealm_defaultAttr_realmClaimPresent() {
        try {
            Map<String, Object> claims = new HashMap<String, Object>();
            claims.put("upn", "testuser");
            claims.put("realm", "DefaultRealm");
            JwtToken token = buildMockToken(claims);

            JwtPrincipalMapping mapping = new JwtPrincipalMapping(token, "upn", "groups", false, "realm");
            assertEquals("Expected realm from 'realm' claim", "DefaultRealm", mapping.getMappedRealm());
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void testGetMappedRealm_nullAttr_fallsBackToRealmConstant() {
        try {
            Map<String, Object> claims = new HashMap<String, Object>();
            claims.put("upn", "testuser");
            claims.put("realm", "FallbackRealm");
            JwtToken token = buildMockToken(claims);

            // null realmIdentifierAttr → falls back to REALM_CLAIM="realm"
            JwtPrincipalMapping mapping = new JwtPrincipalMapping(token, "upn", "groups", false, null);
            assertEquals("Expected realm from default 'realm' claim when attr is null",
                         "FallbackRealm", mapping.getMappedRealm());
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void testGetMappedRealm_claimAbsent() {
        try {
            Map<String, Object> claims = new HashMap<String, Object>();
            claims.put("upn", "testuser");
            // No "tenant" claim
            JwtToken token = buildMockToken(claims);

            JwtPrincipalMapping mapping = new JwtPrincipalMapping(token, "upn", "groups", false, "tenant");
            assertNull("Expected null realm when claim is absent", mapping.getMappedRealm());
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }
}
