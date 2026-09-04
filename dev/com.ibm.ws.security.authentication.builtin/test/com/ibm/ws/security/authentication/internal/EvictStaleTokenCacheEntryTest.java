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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.HashMap;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.authentication.internal.jaas.JAASServiceImpl;
import com.ibm.ws.security.authentication.jaas.modules.LoginModuleHelper;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.security.token.ltpa.LTPAConfiguration;
import com.ibm.wsspi.security.token.SingleSignonToken;

import test.common.SharedOutputManager;

/**
 * Unit tests for {@code AuthenticationServiceImpl#evictStaleTokenCacheEntry}.
 *
 * The method is private, so tests invoke it via reflection.  The observable
 * side-effect is whether {@code AuthCacheService#remove(String)} is called
 * (clone occurred — keys differ) or not called (no clone — keys match or no
 * TOKEN key was present in the incoming AuthenticationData).
 */
@SuppressWarnings("unchecked")
public class EvictStaleTokenCacheEntryTest {

    private static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final ComponentContext componentContext = mockery.mock(ComponentContext.class);
    private final AuthCacheService authCacheService = mockery.mock(AuthCacheService.class);
    private final UserRegistryService userRegistryService = mockery.mock(UserRegistryService.class);
    private final CredentialsService credentialsService = mockery.mock(CredentialsService.class);
    private final JAASServiceImpl jaasService = mockery.mock(JAASServiceImpl.class);
    private final LTPAConfiguration ltpaConfig = mockery.mock(LTPAConfiguration.class);

    private final ServiceReference<AuthCacheService> authCacheRef =
        mockery.mock(ServiceReference.class, "authCacheRef");
    private final ServiceReference<UserRegistryService> userRegRef =
        mockery.mock(ServiceReference.class, "userRegRef");
    private final ServiceReference<CredentialsService> credRef =
        mockery.mock(ServiceReference.class, "credRef");
    private final ServiceReference<LTPAConfiguration> ltpaConfigRef =
        mockery.mock(ServiceReference.class, "ltpaConfigRef");

    private AuthenticationServiceImpl service;

    /** The reflected private method under test. */
    private Method evictMethod;

    @Before
    public void setUp() throws Exception {
        // Common service-locator expectations shared by all tests
        mockery.checking(new Expectations() {
            {
                allowing(componentContext).locateService(AuthenticationServiceImpl.KEY_AUTH_CACHE_SERVICE, authCacheRef);
                will(returnValue(authCacheService));
                allowing(componentContext).locateService(AuthenticationServiceImpl.KEY_USER_REGISTRY_SERVICE, userRegRef);
                will(returnValue(userRegistryService));
                allowing(componentContext).locateService(AuthenticationServiceImpl.KEY_CREDENTIALS_SERVICE, credRef);
                will(returnValue(credentialsService));
                allowing(componentContext).locateService(AuthenticationServiceImpl.KEY_LTPA_CONFIGURATION, ltpaConfigRef);
                will(returnValue(ltpaConfig));

                allowing(jaasService).setAuthenticationService(with(any(AuthenticationServiceImpl.class)));
                allowing(jaasService).unsetAuthenticationService(with(any(AuthenticationServiceImpl.class)));
            }
        });

        service = new AuthenticationServiceImpl();
        service.setAuthCacheService(authCacheRef);
        service.setUserRegistryService(userRegRef);
        service.setCredentialsService(credRef);
        service.setLtpaConfiguration(ltpaConfigRef);
        service.setJaasService(jaasService);
        service.activate(componentContext, new HashMap<>());

        // Expose the private method
        evictMethod = AuthenticationServiceImpl.class.getDeclaredMethod(
            "evictStaleTokenCacheEntry",
            AuthCacheService.class,
            AuthenticationData.class,
            Subject.class);
        evictMethod.setAccessible(true);

        LoginModuleHelper.setTestJaasService(jaasService);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper: build a Subject with a SingleSignonToken returning given bytes
    // ──────────────────────────────────────────────────────────────────────────

    private Subject subjectWithSsoToken(byte[] bytes) throws Exception {
        SingleSignonToken ssoToken = mockery.mock(SingleSignonToken.class, "sso-" + Base64Coder.base64EncodeToString(bytes));
        mockery.checking(new Expectations() {
            {
                allowing(ssoToken).getBytes();
                will(returnValue(bytes));
            }
        });
        Subject subject = new Subject();
        subject.getPrivateCredentials().add(ssoToken);
        return subject;
    }

    private void callEvict(AuthenticationData authData, Subject subject) throws Exception {
        evictMethod.invoke(service, authCacheService, authData, subject);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 1 — no TOKEN/TOKEN64 in AuthData → nothing removed
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    public void testNoTokenInAuthData_noEviction() throws Exception {
        // AuthenticationData carries neither TOKEN64 nor TOKEN
        AuthenticationData authData = new WSAuthenticationData();

        // Expect remove() is NEVER called
        mockery.checking(new Expectations() {
            {
                never(authCacheService).remove(with(any(String.class)));
            }
        });

        Subject subject = subjectWithSsoToken(new byte[] { 0x01, 0x02, 0x03 });
        callEvict(authData, subject);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 2 — TOKEN64 in AuthData matches subject's SSO bytes → no eviction
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    public void testToken64MatchesSsoBytes_noEviction() throws Exception {
        byte[] bytes = new byte[] { (byte) 0xAA, (byte) 0xBB, (byte) 0xCC };
        String token64 = Base64Coder.base64EncodeToString(bytes);

        AuthenticationData authData = new WSAuthenticationData();
        authData.set(AuthenticationData.TOKEN64, token64);

        // remove() must NOT be called — old key == new key (no clone)
        mockery.checking(new Expectations() {
            {
                never(authCacheService).remove(with(any(String.class)));
            }
        });

        Subject subject = subjectWithSsoToken(bytes);
        callEvict(authData, subject);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 3 — TOKEN64 differs from subject's SSO bytes → stale entry removed
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    public void testToken64DifferentFromSsoBytes_evicts() throws Exception {
        byte[] oldBytes = new byte[] { 0x01, 0x02, 0x03 };
        byte[] newBytes = new byte[] { 0x04, 0x05, 0x06 };

        String oldToken64 = Base64Coder.base64EncodeToString(oldBytes);

        AuthenticationData authData = new WSAuthenticationData();
        authData.set(AuthenticationData.TOKEN64, oldToken64);

        // remove() MUST be called exactly once with the old key
        mockery.checking(new Expectations() {
            {
                one(authCacheService).remove(oldToken64);
            }
        });

        Subject subject = subjectWithSsoToken(newBytes);
        callEvict(authData, subject);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 4 — TOKEN (raw bytes) in AuthData differs from subject SSO bytes →
    //          stale entry removed (TOKEN path encodes bytes to base64 key)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    public void testTokenBytesDifferentFromSsoBytes_evicts() throws Exception {
        byte[] oldBytes = new byte[] { 0x10, 0x20, 0x30 };
        byte[] newBytes = new byte[] { 0x40, 0x50, 0x60 };

        String expectedOldKey = Base64Coder.toString(Base64Coder.base64Encode(oldBytes));

        AuthenticationData authData = new WSAuthenticationData();
        authData.set(AuthenticationData.TOKEN, oldBytes);

        mockery.checking(new Expectations() {
            {
                one(authCacheService).remove(expectedOldKey);
            }
        });

        Subject subject = subjectWithSsoToken(newBytes);
        callEvict(authData, subject);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 5 — TOKEN (raw bytes) in AuthData matches subject SSO bytes → no eviction
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    public void testTokenBytesMatchSsoBytes_noEviction() throws Exception {
        byte[] bytes = new byte[] { 0x10, 0x20, 0x30 };

        AuthenticationData authData = new WSAuthenticationData();
        authData.set(AuthenticationData.TOKEN, bytes);

        mockery.checking(new Expectations() {
            {
                never(authCacheService).remove(with(any(String.class)));
            }
        });

        Subject subject = subjectWithSsoToken(bytes);
        callEvict(authData, subject);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 6 — Subject has no SSO token → nothing removed (no NPE)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    public void testSubjectWithNoSsoToken_noEviction() throws Exception {
        AuthenticationData authData = new WSAuthenticationData();
        authData.set(AuthenticationData.TOKEN64, "someToken==");

        mockery.checking(new Expectations() {
            {
                never(authCacheService).remove(with(any(String.class)));
            }
        });

        Subject subjectWithoutSso = new Subject(); // no SingleSignonToken credential
        callEvict(authData, subjectWithoutSso);
    }
}
