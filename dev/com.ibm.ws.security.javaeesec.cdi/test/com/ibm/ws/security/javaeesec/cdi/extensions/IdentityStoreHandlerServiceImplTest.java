/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.cdi.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.security.auth.Subject;
import javax.security.enterprise.CallerPrincipal;
import javax.security.enterprise.credential.CallerOnlyCredential;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStoreHandler;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.javaeesec.CDIHelperTestWrapper;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesUtils;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class IdentityStoreHandlerServiceImplTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private IdentityStoreHandlerServiceImpl ishsi;
    private ModulePropertiesUtils mpu;
    @SuppressWarnings("rawtypes")
    private CDI cdi;
    private IdentityStoreHandler identityStoreHandler;
    private String principalName;
    private String password;
    private CallerPrincipal callerPrincipal;
    private String realmName;
    private Set<String> groups;
    private CredentialValidationResult validResult;
    private BeanManager bm;
    private CDIService cdis;
    private CDIHelperTestWrapper cdiHelperTestWrapper;

    @Before
    public void setUp() {
        cdi = mockery.mock(CDI.class);
        mpu = mockery.mock(ModulePropertiesUtils.class);
        ishsi = new IdentityStoreHandlerServiceImpl() {
            @SuppressWarnings("rawtypes")
            @Override
            protected CDI getCDI() {
                return cdi;
            }

            @Override
            protected ModulePropertiesUtils getModulePropertiesUtils() {
                return mpu;
            }
        };

        identityStoreHandler = mockery.mock(IdentityStoreHandler.class);
        principalName = "user1";
        password = "user1pwd";
        realmName = "defaultRealm";
        callerPrincipal = new CallerPrincipal(principalName);
        groups = new HashSet<String>();
        validResult = new CredentialValidationResult(callerPrincipal, groups);

        bm = mockery.mock(BeanManager.class, "bm1");
        cdis = mockery.mock(CDIService.class);
        cdiHelperTestWrapper = new CDIHelperTestWrapper(mockery, null);
        cdiHelperTestWrapper.setCDIService(cdis);
    }

    @After
    public void tearDown() throws Exception {
        cdiHelperTestWrapper.unsetCDIService(cdis);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testIsIdentityStoreAvailable_True() throws Exception {
        withHttpAuthenticationMechanism(true);
        withIdentityStore();
        assertTrue("the result should be true.", ishsi.isIdentityStoreAvailable());
    }

    @Test
    public void testIsIdentityStoreHanderAvailable_FalseNoHAM() throws Exception {
        withHttpAuthenticationMechanism(false);
        assertFalse("the result should be false.", ishsi.isIdentityStoreAvailable());
    }

    @Test
    public void testCreateHashtableInSubjectWithUserIdAndPassword_SuccessWithIS() throws Exception {
        withHttpAuthenticationMechanism(true);
        withIdentityStoreHandlerResult(validResult);
        Subject subject = ishsi.createHashtableInSubject(principalName, password);
        System.out.println("created subject : " + subject);
        assertSubjectContents(subject, realmName, principalName, true);
    }

    @Test
    public void testCreateHashtableInSubjectWithUserId_SuccessWithIS() throws Exception {
        withHttpAuthenticationMechanism(true);
        withIdentityStoreHandlerResult(validResult);
        Subject subject = ishsi.createHashtableInSubject(principalName);
        System.out.println("created subject : " + subject);
        assertSubjectContents(subject, realmName, principalName, true);
    }

    @Test
    public void testCreateHashtableInSubjectWithUserIdAndPassword_FailureWrongPW() throws Exception {
        withHttpAuthenticationMechanism(true);
        withIdentityStoreHandler(identityStoreHandler).withInvalidResult(CredentialValidationResult.INVALID_RESULT);
        try {
            ishsi.createHashtableInSubject(principalName, "invalid");
            fail("An execption should be thrown if there is an invalid password.");
        } catch (AuthenticationException e) {
            String msg = "Authentication by IdentityStoreHandler was failed.";
            assertTrue("The message does not match with the expectation", e.getMessage().contains(msg));
        }
    }

    @Test
    public void testCreateHashtableInSubjectWithUserId_FailureWrongUserId() throws Exception {
        withHttpAuthenticationMechanism(true);
        withIdentityStoreHandler(identityStoreHandler).withInvalidResult(CredentialValidationResult.INVALID_RESULT);
        try {
            ishsi.createHashtableInSubject("invaliduser");
            fail("An execption should be thrown if there is an invalid password.");
        } catch (AuthenticationException e) {
            String msg = "Authentication by IdentityStoreHandler was failed.";
            assertTrue("The message does not match with the expectation", e.getMessage().contains(msg));
        }
    }

    @Test
    public void testCreateHashtableInSubjectWithUserIdAndPassword_FailureNoHAM() throws Exception {
        withHttpAuthenticationMechanism(false);
        try {
            ishsi.createHashtableInSubject(principalName, password);
            fail("An execption should be thrown if there is no HAM.");
        } catch (AuthenticationException e) {
            String msg = "HttpAuthenticationMechansim is not used in this module.";
            assertTrue("The message does not match with the expectation", e.getMessage().contains(msg));
        }
    }

    @Test
    public void testCreateHashtableInSubjectWithUserId_FailureNoHAM() throws Exception {
        withHttpAuthenticationMechanism(false);
        try {
            ishsi.createHashtableInSubject(principalName);
            fail("An execption should be thrown if there is no HAM.");
        } catch (AuthenticationException e) {
            String msg = "HttpAuthenticationMechansim is not used in this module.";
            assertTrue("The message does not match with the expectation", e.getMessage().contains(msg));
        }
    }

    @Test
    public void testCreateHashtableInSubjectWithUserIdAndPassword_FailureNoIDStoreHandler() throws Exception {
        withHttpAuthenticationMechanism(true);
        withoutIdentityStoreHandler(true, false);
        try {
            ishsi.createHashtableInSubject(principalName, password);
            fail("An execption should be thrown if there is no IDStoreHandler.");
        } catch (AuthenticationException e) {
            String msg = "IdentityStoreHandler does not exist.";
            assertTrue("The message does not match with the expectation", e.getMessage().contains(msg));
        }
    }

    @Test
    public void testCreateHashtableInSubjectWithUserId_FailureNoIDStoreHandler() throws Exception {
        withHttpAuthenticationMechanism(true);
        withoutIdentityStoreHandler(false, true);
        try {
            ishsi.createHashtableInSubject(principalName);
            fail("An execption should be thrown if there is no IDStoreHandler.");
        } catch (AuthenticationException e) {
            String msg = "IdentityStoreHandler does not exist.";
            assertTrue("The message does not match with the expectation", e.getMessage().contains(msg));
        }
    }

    private void withIdentityStoreHandlerResult(CredentialValidationResult result) {
        withIdentityStoreHandler(identityStoreHandler).withResult(result);
    }

    private IdentityStoreHandlerServiceImplTest withHttpAuthenticationMechanism(final boolean result) {
        mockery.checking(new Expectations() {
            {
                one(mpu).isHttpAuthenticationMechanism();
                will(returnValue(result));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private IdentityStoreHandlerServiceImplTest withIdentityStoreHandler(final IdentityStoreHandler identityStoreHandler) {
        final Instance<IdentityStoreHandler> storeHandlerInstance = mockery.mock(Instance.class);

        mockery.checking(new Expectations() {
            {
                one(cdi).select(IdentityStoreHandler.class);
                will(returnValue(storeHandlerInstance));
                one(storeHandlerInstance).isUnsatisfied();
                will(returnValue(false));
                one(storeHandlerInstance).isAmbiguous();
                will(returnValue(false));
                one(storeHandlerInstance).get();
                will(returnValue(identityStoreHandler));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private IdentityStoreHandlerServiceImplTest withIdentityStore() {
        final Instance<IdentityStore> IDStoreInstance = mockery.mock(Instance.class);

        mockery.checking(new Expectations() {
            {
                one(cdi).select(IdentityStore.class);
                will(returnValue(IDStoreInstance));
                one(IDStoreInstance).isUnsatisfied();
                will(returnValue(false));
                one(IDStoreInstance).isAmbiguous();
                will(returnValue(false));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private IdentityStoreHandlerServiceImplTest withoutIdentityStoreHandler(final boolean unsatisfied, final boolean ambiguous) {
        final Instance<IdentityStoreHandler> storeHandlerInstance = mockery.mock(Instance.class);

        mockery.checking(new Expectations() {
            {
                one(cdi).select(IdentityStoreHandler.class);
                will(returnValue(storeHandlerInstance));
                allowing(storeHandlerInstance).isUnsatisfied();
                will(returnValue(unsatisfied));
                allowing(storeHandlerInstance).isAmbiguous();
                will(returnValue(ambiguous));
                never(storeHandlerInstance).get();
                one(cdi).getBeanManager();
                will(returnValue(bm));
            }
        });
        return this;
    }

    private IdentityStoreHandlerServiceImplTest withResult(final CredentialValidationResult result) {
        mockery.checking(new Expectations() {
            {
                one(identityStoreHandler).validate(with(new Matcher<Credential>() {

                    @Override
                    public void describeTo(Description description) {}

                    @Override
                    public void _dont_implement_Matcher___instead_extend_BaseMatcher_() {}

                    @Override
                    public boolean matches(Object obj) {
                        if (obj instanceof UsernamePasswordCredential) {
                            UsernamePasswordCredential cred = (UsernamePasswordCredential) obj;
                            return "user1".equals(cred.getCaller()) && "user1pwd".equals(cred.getPasswordAsString());
                        } else if (obj instanceof CallerOnlyCredential) {
                            CallerOnlyCredential cred = (CallerOnlyCredential) obj;
                            return "user1".equals(cred.getCaller());
                        } else {
                            return false;
                        }
                    }
                }));
                will(returnValue(result));
            }
        });
        return this;
    }

    private IdentityStoreHandlerServiceImplTest withInvalidResult(final CredentialValidationResult result) {
        mockery.checking(new Expectations() {
            {
                one(identityStoreHandler).validate(with(new Matcher<Credential>() {

                    @Override
                    public void describeTo(Description description) {}

                    @Override
                    public void _dont_implement_Matcher___instead_extend_BaseMatcher_() {}

                    @Override
                    public boolean matches(Object obj) {
                        if (obj instanceof UsernamePasswordCredential) {
                            UsernamePasswordCredential cred = (UsernamePasswordCredential) obj;
                            return !"user1".equals(cred.getCaller()) || !"user1pwd".equals(cred.getPasswordAsString());
                        } else if (obj instanceof CallerOnlyCredential) {
                            CallerOnlyCredential cred = (CallerOnlyCredential) obj;
                            return !"user1".equals(cred.getCaller());
                        } else {
                            return false;
                        }
                    }
                }));
                will(returnValue(result));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private void assertSubjectContents(Subject subject, String realmName, String uniqueId, boolean existCacheKey) {
        Hashtable<String, ?> customProperties = getSubjectHashtable(subject);
        assertEquals("The assertion key must be set in the subject.", Boolean.TRUE, customProperties.get(AuthenticationConstants.INTERNAL_ASSERTION_KEY));
        assertEquals("The unique id must be set in the subject.", "user:" + realmName + "/" + uniqueId, customProperties.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID));
        assertEquals("The user id must be set in the subject.", principalName, customProperties.get(AttributeNameConstants.WSCREDENTIAL_USERID));
        assertEquals("The security name must be set in the subject.", principalName, customProperties.get(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME));
        List<String> subjectGroups = (List<String>) customProperties.get(AttributeNameConstants.WSCREDENTIAL_GROUPS);
        assertTrue("The groups must be set in the subject.", groups.containsAll(subjectGroups) && subjectGroups.containsAll(groups));
        assertEquals("The realm name must be set in the subject.", realmName, customProperties.get(AttributeNameConstants.WSCREDENTIAL_REALM));
        if (existCacheKey) {
            assertNotNull("The cache key must be set in the subject.", customProperties.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY));
        } else {
            assertNull("The cache key must not be set in the subject.", customProperties.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY));
        }
    }

    private Hashtable<String, ?> getSubjectHashtable(Subject subject) {
        String[] hashtableLoginProperties = { AttributeNameConstants.WSCREDENTIAL_UNIQUEID,
                                              AttributeNameConstants.WSCREDENTIAL_USERID,
                                              AttributeNameConstants.WSCREDENTIAL_SECURITYNAME,
                                              AttributeNameConstants.WSCREDENTIAL_GROUPS,
                                              AttributeNameConstants.WSCREDENTIAL_REALM,
                                              AttributeNameConstants.WSCREDENTIAL_CACHE_KEY,
                                              AuthenticationConstants.INTERNAL_ASSERTION_KEY };
        return new SubjectHelper().getHashtableFromSubject(subject, hashtableLoginProperties);
    }

}
