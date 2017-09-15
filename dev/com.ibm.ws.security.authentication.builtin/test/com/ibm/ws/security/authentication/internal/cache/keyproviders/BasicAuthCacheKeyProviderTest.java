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
package com.ibm.ws.security.authentication.internal.cache.keyproviders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.authentication.cache.AuthCacheConfig;
import com.ibm.ws.security.authentication.cache.CacheContext;
import com.ibm.ws.security.authentication.cache.CacheKeyProvider;
import com.ibm.ws.security.authentication.cache.CacheObject;

/**
 *
 */
public class BasicAuthCacheKeyProviderTest {

    private static SharedOutputManager outputMgr;
    private static Mockery mockery = new JUnit4Mockery();
    private static CacheContext cacheContext;
    private static CacheObject cacheObject;
    private static String testRealm = "BasicRealm";
    private static String testUser = "user1";
    private static String testUserSecurityName = "user1SecurityName";
    private static String testUserUniqueSecurityName = "user1UniqueSecurityName";
    private static String testPassword = "user1pwd";
    private static String realmAndUserid;
    private static String realmAndSecurityName;
    private static String realmAndUniqueSecurityName;
    private static String realmUseridAndHashedPassword;
    private static String realmSecurityNameAndHashedPassword;
    private static String realmUniqueSecurityNameAndHashedPassword;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

        createCacheObject();
        createCacheContext();
        createExpectedKeys();
    }

    private static void createCacheObject() throws Exception {
        Subject testSubject = createTestSubject();
        cacheObject = new CacheObject(testSubject);
    }

    private static Subject createTestSubject() throws Exception {
        Subject subject = new Subject();
        addCredentialToSubject(subject);
        return subject;
    }

    private static void addCredentialToSubject(Subject subject) throws Exception {
        WSCredential credential = createCredential();
        subject.getPublicCredentials().add(credential);
    }

    private static WSCredential createCredential() throws Exception {
        final WSCredential credential = mockery.mock(WSCredential.class);
        mockery.checking(new Expectations() {
            {
                allowing(credential).getRealmName();
                will(returnValue(testRealm));
                allowing(credential).getSecurityName();
                will(returnValue(testUserSecurityName));
                allowing(credential).getUniqueSecurityName();
                will(returnValue(testUserUniqueSecurityName));
            }
        });
        return credential;
    }

    private static void createCacheContext() throws Exception {
        final AuthCacheConfig config = mockery.mock(AuthCacheConfig.class);
        mockery.checking(new Expectations() {
            {
                allowing(config).isBasicAuthLookupAllowed();
                will(returnValue(true));
            }
        });
        cacheContext = new CacheContext(config, cacheObject, testUser, testPassword);
    }

    private static void createExpectedKeys() throws NoSuchAlgorithmException {
        String hashedPassword = getHashedPassword(testPassword);
        realmUseridAndHashedPassword = testRealm + ":" + testUser + ":" + hashedPassword;
        realmSecurityNameAndHashedPassword = testRealm + ":" + testUserSecurityName + ":" + hashedPassword;
        realmUniqueSecurityNameAndHashedPassword = testRealm + ":" + testUserUniqueSecurityName + ":" + hashedPassword;
        realmAndUserid = testRealm + ":" + testUser;
        realmAndSecurityName = testRealm + ":" + testUserSecurityName;
        realmAndUniqueSecurityName = testRealm + ":" + testUserUniqueSecurityName;
    }

    private static String getHashedPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA");
        return Base64Coder.base64EncodeToString(messageDigest.digest(Base64Coder.getBytes(password)));
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
    public void testConstructor() {
        final String methodName = "testConstructor";
        try {
            CacheKeyProvider provider = new BasicAuthCacheKeyProvider();
            assertNotNull("There must be a basic auth cache key provider.", provider);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProvideKey() {
        final String methodName = "testProvideKey";
        try {
            CacheKeyProvider provider = new BasicAuthCacheKeyProvider();
            Set<Object> keys = (Set<Object>) provider.provideKey(cacheContext);

            assertTrue("The key must be the <realm>:<userid>:<hashedPassword>.", keys.contains(realmUseridAndHashedPassword));
            assertTrue("The key must be the <realm>:<securityName>:<hashedPassword>.", keys.contains(realmSecurityNameAndHashedPassword));
            assertTrue("The key must be the <realm>:<uniqueSecurityName>:<hashedPassword>.", keys.contains(realmUniqueSecurityNameAndHashedPassword));
            assertTrue("The key must be the <realm>:<userid>.", keys.contains(realmAndUserid));
            assertTrue("The key must be the <realm>:<securityName>.", keys.contains(realmAndSecurityName));
            assertTrue("The key must be the <realm>:<uniqueSecurityName>.", keys.contains(realmAndUniqueSecurityName));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProvideKey_AllowBasicAuthLookupFalseReturnsSetWithUserBasedKeysNoPassword() {
        final String methodName = "testProvideKey_AllowBasicAuthLookupFalseReturnsSetWithUserBasedKeysNoPassword";
        try {
            CacheKeyProvider provider = new BasicAuthCacheKeyProvider();
            CacheContext contextWithConfigBasicAuthLookupFalse = createCacheContextWithConfigBasicAuthLookupFalse();
            Set<Object> keys = (Set<Object>) provider.provideKey(contextWithConfigBasicAuthLookupFalse);

            assertTrue("The key must be the <realm>:<userid>.", keys.contains(realmAndUserid));
            assertFalse("The key <realm>:<userid>:<hashedPassword> must not be created.", keys.contains(realmUseridAndHashedPassword));
            assertFalse("The key <realm>:<securityName>:<hashedPassword> must not be created.", keys.contains(realmSecurityNameAndHashedPassword));
            assertFalse("The key <realm>:<uniqueSecurityName>:<hashedPassword> must not be created.", keys.contains(realmUniqueSecurityNameAndHashedPassword));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private CacheContext createCacheContextWithConfigBasicAuthLookupFalse() {
        Mockery mockery = new JUnit4Mockery();
        final AuthCacheConfig config = mockery.mock(AuthCacheConfig.class);
        mockery.checking(new Expectations() {
            {
                allowing(config).isBasicAuthLookupAllowed();
                will(returnValue(false));
            }
        });
        CacheContext contextWithConfigBasicAuthLookupFalse = new CacheContext(config, cacheObject, testUser, testPassword);
        return contextWithConfigBasicAuthLookupFalse;
    }

    @Test
    public void testCreateLookupKey() {
        final String methodName = "testCreateLookupKey";
        try {
            Object cacheKey = BasicAuthCacheKeyProvider.createLookupKey(testRealm, testUser, testPassword);
            assertEquals("The key must be the <realm>:<userid>:<hashedPassword>.", realmUseridAndHashedPassword, cacheKey);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testCreateLookupKeyWithUserid() {
        final String methodName = "testCreateLookupKeyWithUserid";
        try {
            Object cacheKey = BasicAuthCacheKeyProvider.createLookupKey(testRealm, testUser);
            assertEquals("The key must be the <realm>:<userid>:<hashedPassword>.", realmAndUserid, cacheKey);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testCreateLookupKeyWithUserid_NullParametersReturnsNull() {
        final String methodName = "testCreateLookupKeyWithUserid_NullParametersReturnsNull";
        try {
            Object cacheKey = BasicAuthCacheKeyProvider.createLookupKey(null, testUser);
            assertNull("There must not be a key.", cacheKey);
            cacheKey = BasicAuthCacheKeyProvider.createLookupKey(testRealm, null);
            assertNull("There must not be a key.", cacheKey);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testCreateLookupKey_MessageDigestIsReset() {
        final String methodName = "testCreateLookupKey_MessageDigestIsReset";
        try {
            Object firstCacheKey = BasicAuthCacheKeyProvider.createLookupKey(testRealm, testUser, testPassword);
            Object secondCacheKey = BasicAuthCacheKeyProvider.createLookupKey(testRealm, testUser, testPassword);

            assertEquals("The message digest must have been reset and the keys must be the equals.", firstCacheKey, secondCacheKey);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testCreateLookupKey_NullParametersReturnsNull() {
        final String methodName = "testCreateLookupKey_NullParametersReturnsNull";
        try {
            Object cacheKey = BasicAuthCacheKeyProvider.createLookupKey(null, testUser, testPassword);
            assertNull("There must not be a key.", cacheKey);
            cacheKey = BasicAuthCacheKeyProvider.createLookupKey(testRealm, null, testPassword);
            assertNull("There must not be a key.", cacheKey);
            cacheKey = BasicAuthCacheKeyProvider.createLookupKey(testRealm, testUser, null);
            assertNull("There must not be a key.", cacheKey);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
