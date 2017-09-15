/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal.cache;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.authentication.cache.CacheEvictionListener;
import com.ibm.ws.security.authentication.cache.CacheKeyProvider;
import com.ibm.ws.security.authentication.internal.cache.keyproviders.BasicAuthCacheKeyProvider;
import com.ibm.ws.security.authentication.internal.cache.keyproviders.SSOTokenBytesCacheKeyProvider;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 *
 */
public class AuthCacheServiceTest {

    private static SharedOutputManager outputMgr;

    private final Mockery mockery = new JUnit4Mockery();
    private SingleSignonToken ssoToken;
    private Subject testSubject;
    private final String testUser = "user1";
    private final String testUserSecurityName = "user1SecurityName";
    private final String testUserUniqueSecurityName = "user1UniqueSecurityName";
    private final String testPassword = "user1pwd";
    private final String testRealm = "BasicRealm";
    private List<Object> lookupKeys;
    private final ComponentContext componentContext = mockery.mock(ComponentContext.class);
    private AuthCacheServiceImpl authCacheService;
    private final CacheKeyProvider basicAuthCacheKeyProvider = new BasicAuthCacheKeyProvider();
    private final CacheKeyProvider ssoTokenBytesCacheKeyProvider = new SSOTokenBytesCacheKeyProvider();
    private final Object basicAuthCacheKey = BasicAuthCacheKeyProvider.createLookupKey(testRealm, testUser, testPassword);

    private Subject invalidSubject;
    private final String invalidSubjectUser = "invalidSubjectUser";
    private final String invalidSubjectUserPassword = "invalidSubjectUserPassword";
    private final String invalidSubjectUserUniqueSecurityName = "invalidSubjectUserUniqueSecurityName";
    private final String invalidSubjectLookupKey = BasicAuthCacheKeyProvider.createLookupKey(testRealm, invalidSubjectUser, invalidSubjectUserPassword);

    private final Set<AuthCacheServiceImpl> registeredAuthCacheServicesForDeactivation = new HashSet<AuthCacheServiceImpl>();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        ssoToken = createSSOToken();
        testSubject = createTestSubjectWithToken(testUserSecurityName, testUserUniqueSecurityName, ssoToken);
        invalidSubject = createTestSubject(invalidSubjectUser, invalidSubjectUserUniqueSecurityName);
        populateLookupKeys();
        authCacheService = createActivatedAuthCacheService(getDefaultConfigProperties(), null, basicAuthCacheKeyProvider, ssoTokenBytesCacheKeyProvider);
    }

    private SingleSignonToken createSSOToken() {
        final SingleSignonToken ssoToken = mockery.mock(SingleSignonToken.class);
        mockery.checking(new Expectations() {
            {
                allowing(ssoToken).getBytes();
                will(returnValue("testUser".getBytes()));
            }
        });
        return ssoToken;
    }

    private Subject createTestSubject(String securityName, String uniqueSecurityName) throws Exception {
        Subject subject = new Subject();
        WSCredential credential = createCredential(securityName, uniqueSecurityName);
        subject.getPublicCredentials().add(credential);
        return subject;
    }

    private Subject createTestSubjectWithToken(String securityName, String uniqueSecurityName, SingleSignonToken ssoToken) throws Exception {
        Subject subject = createTestSubject(securityName, uniqueSecurityName);
        subject.getPrivateCredentials().add(ssoToken);
        return subject;
    }

    private WSCredential createCredential(final String securityName, final String uniqueSecurityName) throws Exception {
        final WSCredential credential = mockery.mock(WSCredential.class, securityName + "WSCredential");
        mockery.checking(new Expectations() {
            {
                allowing(credential).getRealmName();
                will(returnValue(testRealm));
                allowing(credential).getSecurityName();
                will(returnValue(securityName));
                allowing(credential).getUniqueSecurityName();
                will(returnValue(uniqueSecurityName));
            }
        });
        return credential;
    }

    private void populateLookupKeys() {
        lookupKeys = new ArrayList<Object>();
        lookupKeys.add(getSSOTokenCacheKey(ssoToken));
        lookupKeys.add(basicAuthCacheKey);
    }

    private Map<String, Object> getDefaultConfigProperties() {
        Map<String, Object> newProperties = new HashMap<String, Object>();
        newProperties.put("initialSize", 50);
        newProperties.put("maxSize", 25000);
        newProperties.put("timeout", 600L);
        newProperties.put("allowBasicAuthLookup", true);
        return newProperties;
    }

    @After
    public void tearDown() throws Exception {
        deactivateAuthCacheServicesForStoppingEvictionTasks();
        outputMgr.resetStreams();
    }

    private void deactivateAuthCacheServicesForStoppingEvictionTasks() {
        for (AuthCacheServiceImpl authenticationService : registeredAuthCacheServicesForDeactivation) {
            authenticationService.deactivate(componentContext);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test
    public void testGetSubject_SSOTokenBytes() {
        final String methodName = "testGetSubject_SSOTokenBytes";
        try {
            authCacheService.insert(testSubject);

            Subject actualSubject = authCacheService.getSubject(getSSOTokenCacheKey(testSubject));

            assertSame("The subject must be found in the cache by its SSO token.", testSubject, actualSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetSubject_SSOTokenBytes_NullKeyReturnsNull() {
        final String methodName = "testGetSubject_SSOTokenBytes_NullKeyReturnsNull";
        try {
            authCacheService.insert(testSubject);

            Subject actualSubject = authCacheService.getSubject(null);

            assertNull("There must not be a subject.", actualSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private SingleSignonToken getSSOToken(Subject subject) {
        SingleSignonToken ssoToken = null;
        Set<SingleSignonToken> ssoTokens = subject.getPrivateCredentials(SingleSignonToken.class);
        Iterator<SingleSignonToken> ssoTokensIterator = ssoTokens.iterator();
        if (ssoTokensIterator.hasNext()) {
            ssoToken = ssoTokensIterator.next();
        }
        return ssoToken;
    }

    private String getSSOTokenCacheKey(Subject subject) {
        SingleSignonToken token = getSSOToken(subject);
        return getSSOTokenCacheKey(token);
    }

    private String getSSOTokenCacheKey(SingleSignonToken ssoToken) {
        return Base64Coder.toString(Base64Coder.base64Encode(ssoToken.getBytes()));
    }

    @Test
    public void testGetSubject_UseridPassword() {
        final String methodName = "testGetSubject_RealmUseridPassword";
        try {
            authCacheService.insert(testSubject, testUser, testPassword);

            Subject actualSubject = authCacheService.getSubject(basicAuthCacheKey);

            assertSame("The subject must be found in the cache by its <realm>:<userid>:<hashedPassword>", testSubject, actualSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetSubject_UseridPassword_AllowBasicAuthLookupFalseReturnsNull() {
        final String methodName = "testGetSubject_UseridPassword_AllowLookupByBasicAuthFalseReturnNull";
        try {
            Map<String, Object> newProperties = getConfigProperties();
            AuthCacheServiceImpl authCacheService = createActivatedAuthCacheService(newProperties, null, basicAuthCacheKeyProvider);
            authCacheService.insert(testSubject, testUser, testPassword);

            Subject actualSubject = authCacheService.getSubject(basicAuthCacheKey);

            assertNull("There must not be a subject.", actualSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetSubject_CachingByUseridPassword_GetByOtherKeys() {
        final String methodName = "testGetSubject_CachingByUseridPassword_GetByOtherKeys";
        try {
            authCacheService.insert(testSubject, testUser, testPassword);

            Subject actualSubject = null;
            for (Object lookupKey : lookupKeys) {
                actualSubject = authCacheService.getSubject(lookupKey);
                assertSame("The subject must be found in the cache when using the " + lookupKey + " key.",
                           testSubject, actualSubject);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetSubject_UseridPassword_NullKeyReturnsNull() {
        final String methodName = "testGetSubject_UseridPassword_NullKeyReturnsNull";
        try {
            authCacheService.insert(testSubject, testUser, testPassword);

            Subject actualSubject = authCacheService.getSubject(null);

            assertNull("There must not be a subject.", actualSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testModified_CreateNewCache() {
        final String methodName = "testModified_CreateNewCache";
        try {
            Map<String, Object> newProperties = getConfigProperties();
            AuthCacheServiceImpl authCacheService = createActivatedAuthCacheService(newProperties, null, basicAuthCacheKeyProvider);
            authCacheService.insert(testSubject, testUser, testPassword);

            Subject actualSubject = authCacheService.getSubject(basicAuthCacheKey);
            assertNull("There must not be a subject.", actualSubject);

            newProperties.put("allowBasicAuthLookup", true);
            authCacheService.modified(newProperties);
            authCacheService.insert(testSubject, testUser, testPassword);
            actualSubject = authCacheService.getSubject(basicAuthCacheKey);

            assertSame("The subject must be found in the cache.", testSubject, actualSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private Map<String, Object> getConfigProperties() {
        Map<String, Object> newProperties = new HashMap<String, Object>();
        newProperties.put("initialSize", 100);
        newProperties.put("maxSize", 10000);
        newProperties.put("timeout", 300L);
        newProperties.put("allowBasicAuthLookup", false);
        return newProperties;
    }

    @Test
    public void testRemove_BySSOTokenBytes() {
        final String methodName = "testRemove_BySSOTokenBytes";
        try {
            authCacheService.insert(testSubject);

            SingleSignonToken ssoToken = getSSOToken(testSubject);
            authCacheService.remove(ssoToken.getBytes());
            Subject actualSubject = authCacheService.getSubject(ssoToken.getBytes());
            assertNull("There must not be a subject.", actualSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testRemove_ByNullDoesNothing() {
        final String methodName = "testRemove_ByNullDoesNothing";
        try {
            authCacheService.insert(testSubject);

            authCacheService.remove(null);

            SingleSignonToken ssoToken = getSSOToken(testSubject);
            Subject actualSubject = authCacheService.getSubject(getSSOTokenCacheKey(ssoToken));
            assertSame("The subject must still be found in the cache.", testSubject, actualSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testRemove_CannotRetrieveSubjectByAnyPreviousKey() {
        final String methodName = "testRemove_CannotRetrieveSubjectByAnyPreviousKey";
        try {
            AuthCacheServiceImpl authCacheService = createActivatedAuthCacheService(getDefaultConfigProperties(), null,
                                                                                    basicAuthCacheKeyProvider, ssoTokenBytesCacheKeyProvider);
            authCacheService.insert(testSubject, testUser, testPassword);

            SingleSignonToken ssoToken = getSSOToken(testSubject);
            authCacheService.remove(getSSOTokenCacheKey(ssoToken));

            Subject actualSubject = null;
            for (Object lookupKey : lookupKeys) {
                actualSubject = authCacheService.getSubject(lookupKey);
                assertNull("The subject was removed. There must not be a subject when using the " + lookupKey + " key.",
                           actualSubject);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testRemoveAllEntries_CannotRetrieveSubjectByAnyPreviousKey() {
        final String methodName = "testRemoveAllEntries_CannotRetrieveSubjectByAnyPreviousKey";
        try {
            authCacheService.insert(testSubject);

            authCacheService.removeAllEntries();

            Subject actualSubject = null;
            for (Object lookupKey : lookupKeys) {
                actualSubject = authCacheService.getSubject(lookupKey);
                assertNull("The subject was removed. There must not be a subject when using the " + lookupKey + " key.",
                           actualSubject);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testRemoveAllEntries_TriggeredByChangeNotification_CannotRetrieveSubjectByAnyPreviousKey() {
        final String methodName = "testRemoveAllEntries_TriggeredByChangeNotification_CannotRetrieveSubjectByAnyPreviousKey";
        try {
            authCacheService.insert(testSubject);

            authCacheService.notifyChange();

            Subject actualSubject = null;
            for (Object lookupKey : lookupKeys) {
                actualSubject = authCacheService.getSubject(lookupKey);
                assertNull("The subject was removed. There must not be a subject when using the " + lookupKey + " key.",
                           actualSubject);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void getSubject_InvalidSubject_NoSubjectValidator_ReturnsSubject() {
        final String methodName = "getSubject_InvalidSubject_NoSubjectValidator_ReturnsSubject";
        try {
            AuthCacheServiceImpl authCacheService = createActivatedAuthCacheService(getDefaultConfigProperties(), null, basicAuthCacheKeyProvider);
            authCacheService.insert(invalidSubject, invalidSubjectUser, invalidSubjectUserPassword);

            Subject actualSubject = authCacheService.getSubject(invalidSubjectLookupKey);

            assertSame("The subject must still be found in the cache.", invalidSubject, actualSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void getSubject_InvalidSubject_SubjectValidator_ReturnsNull() {
        final String methodName = "getSubject_InvalidSubject_SubjectValidator_ReturnsNull";
        try {
            CacheEvictionListener cacheEvictionListener = createCacheEvictionListener();
            final ServiceReference<CredentialsService> credentialServiceRef = createCredentialsServiceReference(invalidSubject, false);
            AuthCacheServiceImpl authCacheService = createActivatedAuthCacheService(getDefaultConfigProperties(), credentialServiceRef, basicAuthCacheKeyProvider);
            authCacheService.setCacheEvictionListener(cacheEvictionListener);
            authCacheService.insert(invalidSubject, invalidSubjectUser, invalidSubjectUserPassword);

            Subject actualSubject = authCacheService.getSubject(invalidSubjectLookupKey);

            assertNull("The invalid subject must be removed. There must not be a subject.", actualSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @SuppressWarnings("unchecked")
    private CacheEvictionListener createCacheEvictionListener() {
        final CacheEvictionListener cacheEvictionListener = mockery.mock(CacheEvictionListener.class);
        mockery.checking(new Expectations() {
            {
                atLeast(1).of(cacheEvictionListener).evicted(with(any(List.class)));
            }
        });
        return cacheEvictionListener;
    }

    @Test
    public void getSubject_ValidSubject_SubjectValidator_ReturnsSubject() {
        final String methodName = "getSubject_ValidSubject_SubjectValidator_ReturnsSubject";
        try {
            final ServiceReference<CredentialsService> credentialServiceRef = createCredentialsServiceReference(testSubject, true);
            AuthCacheServiceImpl authCacheService = createActivatedAuthCacheService(getDefaultConfigProperties(), credentialServiceRef, basicAuthCacheKeyProvider);
            authCacheService.insert(testSubject, testUser, testPassword);

            Subject actualSubject = authCacheService.getSubject(basicAuthCacheKey);

            assertNotNull("The valid subject must not be removed. There must be a valid subject.", actualSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @SuppressWarnings("unchecked")
    private ServiceReference<CredentialsService> createCredentialsServiceReference(final Subject subject, final boolean valid) {
        final ServiceReference<CredentialsService> credentialServiceRef = mockery.mock(ServiceReference.class, "credentialServiceRef");
        final CredentialsService credentialService = mockery.mock(CredentialsService.class);
        mockery.checking(new Expectations() {
            {
                allowing(componentContext).locateService(AuthCacheServiceImpl.KEY_CREDENTIAL_SERVICE, credentialServiceRef);
                will(returnValue(credentialService));
                allowing(credentialService).isSubjectValid(subject);
                will(returnValue(valid));
            }
        });
        return credentialServiceRef;
    }

    private AuthCacheServiceImpl createActivatedAuthCacheService(Map<String, Object> newProperties,
                                                                 final ServiceReference<CredentialsService> credentialServiceRef,
                                                                 CacheKeyProvider... providers) {
        AuthCacheServiceImpl authCacheService = new AuthCacheServiceImpl();

        if (credentialServiceRef != null) {
            authCacheService.setCredentialService(credentialServiceRef);
        }
        for (CacheKeyProvider provider : providers) {
            authCacheService.setCacheKeyProvider(provider);
        }

        authCacheService.activate(componentContext, newProperties);
        registeredAuthCacheServicesForDeactivation.add(authCacheService);
        return authCacheService;
    }
}
