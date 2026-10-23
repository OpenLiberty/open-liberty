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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.authentication.internal.jaas.JAASServiceImpl;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.security.token.ltpa.LTPAConfiguration;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.wsspi.security.token.AttributeNameConstants;

import test.common.SharedOutputManager;

/**
 * Unit tests for {@code AuthenticationServiceImpl#shouldRefreshCachedToken}.
 *
 * Focuses on the {@code dynamicExpirationValidation} paths where the expiration
 * stored in the token (and WSCredential) is {@code creationTime + inactivityTimeout}
 * rather than the absolute configured expiration.
 *
 * The method is private, so tests invoke it via reflection.
 */
@SuppressWarnings("unchecked")
public class ShouldRefreshCachedTokenTest {

    private static final long MILLIS_PER_MINUTE = 60_000L;

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

    @SuppressWarnings("rawtypes")
    private final ServiceReference<AuthCacheService> authCacheRef =
        mockery.mock(ServiceReference.class, "authCacheRef");
    @SuppressWarnings("rawtypes")
    private final ServiceReference<UserRegistryService> userRegRef =
        mockery.mock(ServiceReference.class, "userRegRef");
    @SuppressWarnings("rawtypes")
    private final ServiceReference<CredentialsService> credRef =
        mockery.mock(ServiceReference.class, "credRef");
    @SuppressWarnings("rawtypes")
    private final ServiceReference<LTPAConfiguration> ltpaConfigRef =
        mockery.mock(ServiceReference.class, "ltpaConfigRef");

    private AuthenticationServiceImpl service;

    /** Reflected private method under test. */
    private Method shouldRefreshMethod;

    /** Saved beta system property — restored after each test. */
    private String savedBetaProperty;

    @Before
    public void setUp() throws Exception {
        savedBetaProperty = System.getProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY);
        System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, "true");

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

        shouldRefreshMethod = AuthenticationServiceImpl.class.getDeclaredMethod(
            "shouldRefreshCachedToken", Subject.class);
        shouldRefreshMethod.setAccessible(true);
    }

    @After
    public void tearDown() {
        if (savedBetaProperty == null) {
            System.clearProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY);
        } else {
            System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, savedBetaProperty);
        }
        mockery.assertIsSatisfied();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private boolean callShouldRefresh(Subject subject) throws Exception {
        return (Boolean) shouldRefreshMethod.invoke(service, subject);
    }

    /**
     * Build a Subject with a WSCredential that returns the given values.
     *
     * @param storedExpiration   value returned by {@code WSCredential.getExpiration()}
     * @param creationTimeMillis value returned by {@code WSCredential.get(WSTOKEN_CREATION_TIME)},
     *                           or {@code null} to simulate a legacy token with no creationTime
     */
    private Subject subjectWithCredential(long storedExpiration, Long creationTimeMillis) throws Exception {
        WSCredential cred = mockery.mock(WSCredential.class,
                                        "cred-" + storedExpiration + "-" + creationTimeMillis);
        mockery.checking(new Expectations() {
            {
                allowing(cred).getExpiration();
                will(returnValue(storedExpiration));
                allowing(cred).get(AttributeNameConstants.WSTOKEN_CREATION_TIME);
                will(returnValue(creationTimeMillis));
            }
        });
        Subject subject = new Subject();
        subject.getPublicCredentials().add(cred);
        return subject;
    }

    /** Configure the ltpaConfig mock with the given settings. */
    private void configureLtpa(long inactivityMinutes, long refreshThresholdMinutes,
                                long expirationMinutes, boolean dynamicExpirationValidation) {
        mockery.checking(new Expectations() {
            {
                allowing(ltpaConfig).getInactivityTimeout();
                will(returnValue(inactivityMinutes));
                allowing(ltpaConfig).getRefreshThreshold();
                will(returnValue(refreshThresholdMinutes));
                allowing(ltpaConfig).getTokenExpiration();
                will(returnValue(expirationMinutes));
                allowing(ltpaConfig).isDynamicExpirationValidation();
                will(returnValue(dynamicExpirationValidation));
            }
        });
    }

    // ── dynamicExpirationValidation = false (baseline) ────────────────────────

    /**
     * With dynamicExpirationValidation=false: a freshly created token is far from
     * the inactivity threshold — no refresh needed.
     */
    @Test
    public void dynamicOff_freshToken_noRefresh() throws Exception {
        // expiration=120m, inactivity=30m, threshold=10m, dynamicExpiration=false
        long inactivity = 30, threshold = 10, expiration = 120;
        configureLtpa(inactivity, threshold, expiration, false);

        long creationTime = System.currentTimeMillis();
        // Stored expiration = creationTime + 120m (normal)
        long storedExpiry = creationTime + expiration * MILLIS_PER_MINUTE;

        Subject subject = subjectWithCredential(storedExpiry, creationTime);
        assertFalse("Fresh token far from threshold must not trigger refresh",
                    callShouldRefresh(subject));
    }

    /**
     * With dynamicExpirationValidation=false: creationTime backdated so inactivity
     * time remaining <= threshold → refresh required.
     */
    @Test
    public void dynamicOff_withinThreshold_refreshNeeded() throws Exception {
        long inactivity = 30, threshold = 10, expiration = 120;
        configureLtpa(inactivity, threshold, expiration, false);

        // Backdate by 21 minutes → inactivity remaining = 30 - 21 = 9m < 10m threshold
        long creationTime = System.currentTimeMillis() - 21 * MILLIS_PER_MINUTE;
        long storedExpiry = creationTime + expiration * MILLIS_PER_MINUTE;

        Subject subject = subjectWithCredential(storedExpiry, creationTime);
        assertTrue("Token within inactivity threshold must trigger refresh",
                   callShouldRefresh(subject));
    }

    /**
     * With dynamicExpirationValidation=false: absolute expiration exceeded → refresh
     * (the caller will discard the cached subject and force full re-authentication).
     */
    @Test
    public void dynamicOff_absoluteExpirationExceeded_refreshNeeded() throws Exception {
        long inactivity = 30, threshold = 10, expiration = 120;
        configureLtpa(inactivity, threshold, expiration, false);

        // Stored expiry in the past
        long storedExpiry = System.currentTimeMillis() - MILLIS_PER_MINUTE;
        long creationTime = storedExpiry - expiration * MILLIS_PER_MINUTE;

        Subject subject = subjectWithCredential(storedExpiry, creationTime);
        assertTrue("Expired token must trigger refresh",
                   callShouldRefresh(subject));
    }

    // ── dynamicExpirationValidation = true ────────────────────────────────────

    /**
     * With dynamicExpirationValidation=true: a freshly created token stores
     * {@code creationTime + inactivityTimeout} as its expiration. The method must
     * recompute the absolute deadline as {@code creationTime + configuredExpiration}
     * and must NOT treat the short stored value as expiry.
     */
    @Test
    public void dynamicOn_freshToken_storedExpiryIsShort_noFalseRefresh() throws Exception {
        // expiration=120m, inactivity=10m, threshold=5m, dynamicExpiration=true
        long inactivity = 10, threshold = 5, expiration = 120;
        configureLtpa(inactivity, threshold, expiration, true);

        long creationTime = System.currentTimeMillis();
        // Stored expiry = creationTime + inactivity (10m) — much shorter than 120m absolute
        long storedExpiry = creationTime + inactivity * MILLIS_PER_MINUTE;

        Subject subject = subjectWithCredential(storedExpiry, creationTime);
        assertFalse("Fresh token must not trigger refresh when dynamicExpirationValidation=true: " +
                    "stored expiry is short but absolute deadline is 120m away",
                    callShouldRefresh(subject));
    }

    /**
     * With dynamicExpirationValidation=true: creationTime backdated so inactivity
     * time remaining <= threshold → refresh required.
     */
    @Test
    public void dynamicOn_withinInactivityThreshold_refreshNeeded() throws Exception {
        long inactivity = 10, threshold = 5, expiration = 120;
        configureLtpa(inactivity, threshold, expiration, true);

        // Backdate by 6 minutes → inactivity remaining = 10 - 6 = 4m < 5m threshold
        long creationTime = System.currentTimeMillis() - 6 * MILLIS_PER_MINUTE;
        long storedExpiry = creationTime + inactivity * MILLIS_PER_MINUTE;

        Subject subject = subjectWithCredential(storedExpiry, creationTime);
        assertTrue("Token within inactivity threshold must trigger refresh when dynamicExpirationValidation=true",
                   callShouldRefresh(subject));
    }

    /**
     * With dynamicExpirationValidation=true: creationTime old enough that the
     * absolute expiration (creationTime + configuredExpiration) is exceeded → refresh.
     */
    @Test
    public void dynamicOn_absoluteExpirationExceeded_refreshNeeded() throws Exception {
        long inactivity = 10, threshold = 5, expiration = 120;
        configureLtpa(inactivity, threshold, expiration, true);

        // Backdate by 121 minutes → absolute expiration exceeded
        long creationTime = System.currentTimeMillis() - 121 * MILLIS_PER_MINUTE;
        long storedExpiry = creationTime + inactivity * MILLIS_PER_MINUTE; // already past, but irrelevant

        Subject subject = subjectWithCredential(storedExpiry, creationTime);
        assertTrue("Absolutely expired token must trigger refresh when dynamicExpirationValidation=true",
                   callShouldRefresh(subject));
    }

    /**
     * With dynamicExpirationValidation=true: inactivity timeout exceeded but absolute
     * expiration is still in the future → refresh (stale inactivity window).
     */
    @Test
    public void dynamicOn_inactivityExceededButAbsoluteNotExpired_refreshNeeded() throws Exception {
        long inactivity = 10, threshold = 5, expiration = 120;
        configureLtpa(inactivity, threshold, expiration, true);

        // Backdate by 11 minutes → inactivity exceeded (11 > 10), absolute is fine (11 < 120)
        long creationTime = System.currentTimeMillis() - 11 * MILLIS_PER_MINUTE;
        long storedExpiry = creationTime + inactivity * MILLIS_PER_MINUTE;

        Subject subject = subjectWithCredential(storedExpiry, creationTime);
        assertTrue("Inactivity-exceeded token must trigger refresh when dynamicExpirationValidation=true",
                   callShouldRefresh(subject));
    }

    // ── no creationTime fallback ──────────────────────────────────────────────

    /**
     * With no creationTime in WSCredential (legacy token), the method falls back to
     * the stored expiration. Token not yet expired → no refresh.
     */
    @Test
    public void noCreationTime_storedExpiryValid_noRefresh() throws Exception {
        long inactivity = 30, threshold = 10, expiration = 120;
        configureLtpa(inactivity, threshold, expiration, false);

        long storedExpiry = System.currentTimeMillis() + 60 * MILLIS_PER_MINUTE;

        Subject subject = subjectWithCredential(storedExpiry, null /* no creationTime */);
        assertFalse("Legacy token with valid stored expiry must not trigger refresh",
                    callShouldRefresh(subject));
    }

    /**
     * With no creationTime in WSCredential (legacy token), stored expiration has
     * elapsed → refresh.
     */
    @Test
    public void noCreationTime_storedExpiryElapsed_refreshNeeded() throws Exception {
        long inactivity = 30, threshold = 10, expiration = 120;
        configureLtpa(inactivity, threshold, expiration, false);

        long storedExpiry = System.currentTimeMillis() - MILLIS_PER_MINUTE; // already past

        Subject subject = subjectWithCredential(storedExpiry, null /* no creationTime */);
        assertTrue("Legacy token with elapsed stored expiry must trigger refresh",
                   callShouldRefresh(subject));
    }

    /**
     * dynamicExpirationValidation=true with no creationTime: the stored expiry
     * (short inactivity window) has elapsed but we cannot recompute the absolute
     * expiration — falls back to stored expiry and reports expired.
     */
    @Test
    public void dynamicOn_noCreationTime_storedExpiryElapsed_refreshNeeded() throws Exception {
        long inactivity = 10, threshold = 5, expiration = 120;
        configureLtpa(inactivity, threshold, expiration, true);

        // Stored = creationTime + inactivity, already past (legacy token from old server)
        long storedExpiry = System.currentTimeMillis() - MILLIS_PER_MINUTE;

        Subject subject = subjectWithCredential(storedExpiry, null /* no creationTime */);
        assertTrue("Legacy token with elapsed stored expiry must trigger refresh even with dynamicExpirationValidation=true",
                   callShouldRefresh(subject));
    }

    // ── beta guard ────────────────────────────────────────────────────────────

    /**
     * When beta edition is disabled, shouldRefreshCachedToken must always return false
     * regardless of configuration.
     */
    @Test
    public void betaDisabled_alwaysReturnsFalse() throws Exception {
        System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, "false");

        // Even with inactivity configured, beta guard short-circuits
        configureLtpa(10, 5, 120, true);

        long creationTime = System.currentTimeMillis() - 6 * MILLIS_PER_MINUTE; // would trigger
        long storedExpiry = creationTime + 10 * MILLIS_PER_MINUTE;

        Subject subject = subjectWithCredential(storedExpiry, creationTime);
        assertFalse("Beta guard must prevent refresh when beta edition is disabled",
                    callShouldRefresh(subject));
    }

    // ── inactivity disabled ───────────────────────────────────────────────────

    /**
     * When inactivityTimeout == 0 the feature is disabled; method returns false
     * without consulting WSCredential.
     */
    @Test
    public void inactivityDisabled_alwaysReturnsFalse() throws Exception {
        mockery.checking(new Expectations() {
            {
                // Only getInactivityTimeout() is called (early-exit guard)
                allowing(ltpaConfig).getInactivityTimeout();
                will(returnValue(0L));
            }
        });

        // Subject with a credential that should NOT be queried
        long creationTime = System.currentTimeMillis() - 6 * MILLIS_PER_MINUTE;
        long storedExpiry = creationTime + 10 * MILLIS_PER_MINUTE;
        Subject subject = subjectWithCredential(storedExpiry, creationTime);

        assertFalse("Disabled inactivity feature must return false without inspecting credentials",
                    callShouldRefresh(subject));
    }
}
