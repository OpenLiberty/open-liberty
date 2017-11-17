/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.cdi.beans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;


import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import javax.security.auth.message.MessageInfo;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.callback.PasswordValidationCallback;

import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.CallerOnlyCredential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.security.enterprise.identitystore.CredentialValidationResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

import test.common.SharedOutputManager;

public class CustomFormAuthenticationMechanismTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @SuppressWarnings("rawtypes")
    private CDI cdi;
    private Instance<IdentityStoreHandler> iish;
    private CustomFormAuthenticationMechanism cfam;

    private HttpMessageContext hmc;
    private MessageInfo mi;
    private HttpServletRequest req;
    private HttpServletResponse res;
    private Subject cs;
    private CallbackHandler ch;
    private AuthenticationParameters ap;
    private final Map<String, String> mm = new HashMap<String, String>();
    
    private final String ISH_ID = "IdentityStore1";
    private final String USER1 = "user1";
    private final String PASSWORD1 = "s3cur1ty";
 

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.javaeesec.*=all");

    @Rule
    public final TestName testName = new TestName();

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
    }

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        cdi = mockery.mock(CDI.class);
        iish = mockery.mock(Instance.class);
        ap = mockery.mock(AuthenticationParameters.class);
        hmc = mockery.mock(HttpMessageContext.class);
        mi = mockery.mock(MessageInfo.class);
        req = mockery.mock(HttpServletRequest.class);
        res = mockery.mock(HttpServletResponse.class);
        ch = mockery.mock(CallbackHandler.class);
        cs = new Subject();

        cfam = new CustomFormAuthenticationMechanism() {
            @SuppressWarnings("rawtypes")
            @Override
            protected CDI getCDI() {
                return cdi;
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestValidIdAndPWIdentityStoreHandler() throws Exception {
        IdentityStoreHandler mish = new MyIdentityStoreHandler();
        withMessageContext(ap).withMessageInfo().withUsernamePassword(USER1, PASSWORD1).withBeanInstance(mish).withSetStatusToResponse(HttpServletResponse.SC_OK);

        AuthenticationStatus status = cfam.validateRequest(req, res, hmc);
        assertEquals("The result should be SUCCESS", AuthenticationStatus.SUCCESS, status);
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestInvalidIdAndPWIdentityStoreHandler() throws Exception {
        IdentityStoreHandler mish = new MyIdentityStoreHandler();
        withMessageContext(ap).withUsernamePassword(USER1, "invalid").withBeanInstance(mish).withSetStatusToResponse(HttpServletResponse.SC_FORBIDDEN);

        AuthenticationStatus status = cfam.validateRequest(req, res, hmc);
        assertEquals("The result should be SEND_FAILURE", AuthenticationStatus.SEND_FAILURE, status);
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestValidIdAndPWNoIdentityStoreHandlerCallbackHandler() throws Exception {
        final MyCallbackHandler mch = new MyCallbackHandler();
        withMessageContext(ap).withMessageInfo().withHandler(mch).withUsernamePassword(USER1, PASSWORD1).withBeanInstance(null).withSetStatusToResponse(HttpServletResponse.SC_OK);

        AuthenticationStatus status = cfam.validateRequest(req, res, hmc);
        assertEquals("The result should be SUCCESS", AuthenticationStatus.SUCCESS, status);
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestInvalidIdAndPWNoIdentityStoreHandlerCallbackHandler() throws Exception {
        final MyCallbackHandler mch = new MyCallbackHandler();
        withMessageContext(ap).withHandler(mch).withUsernamePassword(USER1, "invalid").withBeanInstance(null).withSetStatusToResponse(HttpServletResponse.SC_FORBIDDEN);

        AuthenticationStatus status = cfam.validateRequest(req, res, hmc);
        assertEquals("The result should be SEND_FAILURE", AuthenticationStatus.SEND_FAILURE, status);
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestValidIdAndPWNoIdentityStoreHandlerCallbackHandlerException() throws Exception {
        final String msg = "An Exception by CallbackHandler";
        IOException ex = new IOException(msg);
        withMessageContext(ap).withHandler(ch).withUsernamePassword(USER1, PASSWORD1).withBeanInstance(null).withCallbackHandlerException(ex);

        try {
            AuthenticationStatus status = cfam.validateRequest(req, res, hmc);
            fail("AuthenticationException should be thrown.");
        } catch (AuthenticationException e) {
            assertTrue("CWWKS1930W  message was not logged", outputMgr.checkForStandardOut("CWWKS1930W:"));
            assertTrue("The message does not match with the expectation", e.getMessage().contains(msg));
        }
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestInvalidCredential() throws Exception {
        CallerOnlyCredential coc = new CallerOnlyCredential(USER1);
        withMessageContext(ap).withHandler(ch).withCredential(coc).withBeanInstance(null);

        try {
            AuthenticationStatus status = cfam.validateRequest(req, res, hmc);
            fail("AuthenticationException should be thrown.");
        } catch (AuthenticationException e) {
            assertTrue("CWWKS1927E  message was not logged", outputMgr.checkForStandardErr("CWWKS1927E:"));
            assertTrue("The message should contains CWWKS1927E", e.getMessage().contains("CWWKS1927E"));
        }
    }
    /**
     *   
     */
    @Test
    public void testValidateRequestNoIdAndPWAuthReqFalseProtectedTrue() throws Exception {
        withMessageContext(ap).withUsernamePassword(null, null).withAuthenticationRequest(false).withProtected(true);
 
        AuthenticationStatus status = cfam.validateRequest(req, res, hmc);
        assertEquals("The result should be SEND_CONTINUE", AuthenticationStatus.SEND_CONTINUE, status);
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestNoIdAndPWAuthReqTrueProtectedFalse() throws Exception {
        withMessageContext(ap).withUsernamePassword(null, null).withAuthenticationRequest(true);
 
        AuthenticationStatus status = cfam.validateRequest(req, res, hmc);
        assertEquals("The result should be SEND_CONTINUE", AuthenticationStatus.SEND_CONTINUE, status);
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestNoIdAndPWAuthReqFalseProtectedFalse() throws Exception {
        withMessageContext(ap).withUsernamePassword(null, null).withAuthenticationRequest(false).withProtected(false);
 
        AuthenticationStatus status = cfam.validateRequest(req, res, hmc);
        assertEquals("The result should be NOT_DONE", AuthenticationStatus.NOT_DONE, status);
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestNoIdAndPW() throws Exception {
        withMessageContext(null);
 
        AuthenticationStatus status = cfam.validateRequest(req, res, hmc);
        assertEquals("The result should be SEND_CONTINUE", AuthenticationStatus.SEND_CONTINUE, status);
    }


    /*************** support methods **************/
    @SuppressWarnings("unchecked")
    private CustomFormAuthenticationMechanismTest withBeanInstance(final IdentityStoreHandler value) throws Exception {
        final Instance<IdentityStoreHandler> inst = value != null ? iish:null;
        mockery.checking(new Expectations() {
            {
                one(cdi).select(IdentityStoreHandler.class);
                will(returnValue(iish));
                allowing(iish).isUnsatisfied();
                will(returnValue(false));
                allowing(iish).isAmbiguous();
                will(returnValue(false));
                allowing(iish).get();
                will(returnValue(value));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private CustomFormAuthenticationMechanismTest withMessageContext(final AuthenticationParameters authParams) throws Exception {
        
        mockery.checking(new Expectations() {
            {
                one(hmc).getClientSubject();
                will(returnValue(cs));
                one(hmc).getRequest();
                will(returnValue(req));
                one(hmc).getResponse();
                will(returnValue(res));
                one(hmc).getAuthParameters();
                will(returnValue(authParams));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private CustomFormAuthenticationMechanismTest withMessageInfo() throws Exception {
        
        mockery.checking(new Expectations() {
            {
                one(hmc).getMessageInfo();
                will(returnValue(mi));
                one(mi).getMap();
                will(returnValue(mm));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private CustomFormAuthenticationMechanismTest withHandler(final CallbackHandler handler) throws Exception {
        
        mockery.checking(new Expectations() {
            {
                one(hmc).getHandler();
                will(returnValue(handler));
            }
        });
        return this;
    }

    private CustomFormAuthenticationMechanismTest withUsernamePassword(final String username, final String password) throws Exception {
        final UsernamePasswordCredential upc = (username != null && password != null) ? new UsernamePasswordCredential(username, password) : null;
        mockery.checking(new Expectations() {
            {
                one(ap).getCredential();
                will(returnValue(upc));
            }
        });
        return this;
    }

    private CustomFormAuthenticationMechanismTest withCredential(final Credential cred) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(ap).getCredential();
                will(returnValue(cred));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private CustomFormAuthenticationMechanismTest withAuthenticationRequest(final boolean value) throws Exception {
        
        mockery.checking(new Expectations() {
            {
                one(hmc).isAuthenticationRequest();
                will(returnValue(value));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private CustomFormAuthenticationMechanismTest withProtected(final boolean value) throws Exception {
        
        mockery.checking(new Expectations() {
            {
                one(hmc).isProtected();
                will(returnValue(value));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private CustomFormAuthenticationMechanismTest withCallbackHandlerException(final Exception e) throws Exception {
        
        mockery.checking(new Expectations() {
            {
                one(ch).handle(with(any(Callback[].class)));
                will(throwException(e));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private CustomFormAuthenticationMechanismTest withSetStatusToResponse(final int value) {
        
        mockery.checking(new Expectations() {
            {
                one(res).setStatus(value);
            }
        });
        return this;
    }

    class MyCallbackHandler implements CallbackHandler {
        public void handle (Callback[] callbacks) {
            PasswordValidationCallback pwcb  = (PasswordValidationCallback)callbacks[0];
            String userid = pwcb.getUsername();
            String password = new String(pwcb.getPassword());
            boolean result = USER1.equals(userid) && PASSWORD1.equals(password);
            pwcb.setResult(result);
        }
    }

    class MyIdentityStoreHandler implements IdentityStoreHandler {
        public CredentialValidationResult validate(Credential cred) {
            CredentialValidationResult result = null;
            String userid = ((UsernamePasswordCredential)cred).getCaller();
            String password = ((UsernamePasswordCredential)cred).getPasswordAsString();
            if(USER1.equals(userid) && PASSWORD1.equals(password)) {
                result = new CredentialValidationResult(ISH_ID, USER1, USER1, USER1, new HashSet<String>());
            } else {
                result = CredentialValidationResult.INVALID_RESULT;
            }
            return result;
        }
    }
}
