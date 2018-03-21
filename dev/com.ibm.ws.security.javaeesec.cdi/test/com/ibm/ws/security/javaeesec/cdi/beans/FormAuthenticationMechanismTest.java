/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.security.enterprise.credential.CallerOnlyCredential;
import javax.security.enterprise.credential.Credential;
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
import com.ibm.ws.common.internal.encoder.Base64Coder;

public class FormAuthenticationMechanismTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @SuppressWarnings("rawtypes")
    private CDI cdi;
    private Instance<IdentityStoreHandler> iish;
    private FormAuthenticationMechanism fam;

    private HttpMessageContext hmc;
    private MessageInfo mi;
    private HttpServletRequest req;
    private HttpServletResponse res;
    private Subject cs;
    private CallbackHandler ch;
    private AuthenticationParameters ap;
    private final Map<String, String> mm = new HashMap<String, String>();
    private CallerOnlyCredential coCred;
    private BasicAuthenticationCredential baCred;
    private UsernamePasswordCredential upCred, invalidUpCred;

    private final String ISH_ID = "IdentityStore1";
    private final String USER1 = "user1";
    private final String PASSWORD1 = "s3cur1ty";
    private final String INVALID_PASSWORD = "invalid";
 

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

        fam = new FormAuthenticationMechanism() {
            @SuppressWarnings("rawtypes")
            @Override
            protected CDI getCDI() {
                return cdi;
            }
        };

        coCred = new CallerOnlyCredential(USER1);
        upCred = new UsernamePasswordCredential(USER1, PASSWORD1);
        invalidUpCred = new UsernamePasswordCredential(USER1, INVALID_PASSWORD);
        baCred = new BasicAuthenticationCredential(Base64Coder.base64Encode(USER1+ ":" + PASSWORD1));
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
    public void testValidateRequestAuthReqFalseValidIdAndPWIdentityStoreHandler() throws Exception {
        IdentityStoreHandler mish = new MyIdentityStoreHandler();
        withMessageContext().withMessageInfo().withUsernamePassword(USER1, PASSWORD1).withAuthenticationRequest(false).withBeanInstance(mish).withSetStatusToResponse(HttpServletResponse.SC_OK);

        AuthenticationStatus status = fam.validateRequest(req, res, hmc);
        assertEquals("The result should be SUCCESS", AuthenticationStatus.SUCCESS, status);
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestAuthReqFalseInvalidIdAndPWIdentityStoreHandler() throws Exception {
        IdentityStoreHandler mish = new MyIdentityStoreHandler();
        withMessageContext().withUsernamePassword(USER1, "invalid").withAuthenticationRequest(false).withBeanInstance(mish).withSetStatusToResponse(HttpServletResponse.SC_FORBIDDEN);

        AuthenticationStatus status = fam.validateRequest(req, res, hmc);
        assertEquals("The result should be SEND_FAILURE", AuthenticationStatus.SEND_FAILURE, status);
    }


    /**
     *   
     */
    @Test
    public void testValidateRequestAuthReqFalseValidIdAndPWNoIdentityStoreHandlerCallbackHandler() throws Exception {
        final MyCallbackHandler mch = new MyCallbackHandler();
        withMessageContext().withHandler(mch).withMessageInfo().withUsernamePassword(USER1, PASSWORD1).withAuthenticationRequest(false).withBeanInstance(null).withSetStatusToResponse(HttpServletResponse.SC_OK);

        AuthenticationStatus status = fam.validateRequest(req, res, hmc);
        assertEquals("The result should be SUCCESS", AuthenticationStatus.SUCCESS, status);
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestAuthReqFalseInvalidIdAndPWNoIdentityStoreHandlerCallbackHandler() throws Exception {
        final MyCallbackHandler mch = new MyCallbackHandler();
        withMessageContext().withHandler(mch).withUsernamePassword(USER1, "invalid").withAuthenticationRequest(false).withBeanInstance(null).withSetStatusToResponse(HttpServletResponse.SC_FORBIDDEN);

        AuthenticationStatus status = fam.validateRequest(req, res, hmc);
        assertEquals("The result should be SEND_FAILURE", AuthenticationStatus.SEND_FAILURE, status);
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestAuthReqTrueValidIdAndPWIdentityStoreHandler() throws Exception {
        IdentityStoreHandler mish = new MyIdentityStoreHandler();
        withMessageContext().withMessageInfo().withUsernamePassword(USER1, PASSWORD1).withAuthenticationRequest(true).withBeanInstance(mish).withSetStatusToResponse(HttpServletResponse.SC_OK);

        AuthenticationStatus status = fam.validateRequest(req, res, hmc);
        assertEquals("The result should be SUCCESS", AuthenticationStatus.SUCCESS, status);
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestAuthReqTrueInvalidIdAndPWIdentityStoreHandler() throws Exception {
        IdentityStoreHandler mish = new MyIdentityStoreHandler();
        withMessageContext().withUsernamePassword(USER1, "invalid").withAuthenticationRequest(true).withBeanInstance(mish).withSetStatusToResponse(HttpServletResponse.SC_FORBIDDEN);

        AuthenticationStatus status = fam.validateRequest(req, res, hmc);
        assertEquals("The result should be SEND_FAILURE", AuthenticationStatus.SEND_FAILURE, status);
    }


    /**
     *   
     */
    @Test
    public void testValidateRequestAuthReqTrueValidIdAndPWNoIdentityStoreHandlerCallbackHandler() throws Exception {
        final MyCallbackHandler mch = new MyCallbackHandler();
        withMessageContext().withMessageInfo().withHandler(mch).withUsernamePassword(USER1, PASSWORD1).withAuthenticationRequest(true).withBeanInstance(null).withSetStatusToResponse(HttpServletResponse.SC_OK);

        AuthenticationStatus status = fam.validateRequest(req, res, hmc);
        assertEquals("The result should be SUCCESS", AuthenticationStatus.SUCCESS, status);
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestAuthReqTrueInvalidIdAndPWNoIdentityStoreHandlerCallbackHandler() throws Exception {
        final MyCallbackHandler mch = new MyCallbackHandler();
        withMessageContext().withHandler(mch).withUsernamePassword(USER1, "invalid").withAuthenticationRequest(true).withBeanInstance(null).withSetStatusToResponse(HttpServletResponse.SC_FORBIDDEN);

        AuthenticationStatus status = fam.validateRequest(req, res, hmc);
        assertEquals("The result should be SEND_FAILURE", AuthenticationStatus.SEND_FAILURE, status);
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestAuthReqTrueValidIdAndPWNoIdentityStoreHandlerCallbackHandlerException() throws Exception {
        final String msg = "An Exception by CallbackHandler";
        IOException ex = new IOException(msg);
        withMessageContext().withHandler(ch).withUsernamePassword(USER1, PASSWORD1).withAuthenticationRequest(true).withBeanInstance(null).withCallbackHandlerException(ex);

        try {
            AuthenticationStatus status = fam.validateRequest(req, res, hmc);
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
    public void testValidateRequestAuthReqTrueValidIdAndPWNoIdentityStoreHandlerNoCallbackHandler() throws Exception {
        withMessageContext().withHandler(null).withUsernamePassword(USER1, PASSWORD1).withAuthenticationRequest(true).withBeanInstance(null).withSetStatusToResponse(HttpServletResponse.SC_FORBIDDEN);

        AuthenticationStatus status = fam.validateRequest(req, res, hmc);
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestAuthReqFalseNoIdAndPWProtectedTrue() throws Exception {
        withMessageContext().withUsernamePassword(null, null).withAuthenticationRequest(false).withProtected(true);
 
        AuthenticationStatus status = fam.validateRequest(req, res, hmc);
        assertEquals("The result should be SEND_CONTINUE", AuthenticationStatus.SEND_CONTINUE, status);
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestAuthReqFalseNoIdAndPWProtectedFalse() throws Exception {
        withMessageContext().withUsernamePassword(null, null).withAuthenticationRequest(false).withProtected(false);
 
        AuthenticationStatus status = fam.validateRequest(req, res, hmc);
        assertEquals("The result should be NOT_DONE", AuthenticationStatus.NOT_DONE, status);
    }

    /**
     *   
     */
    @Test
    public void testValidateRequestAuthReqTrueNoIdAndPW() throws Exception {
        withMessageContext().withUsernamePassword(null, null).withAuthenticationRequest(true);
 
        AuthenticationStatus status = fam.validateRequest(req, res, hmc);
        assertEquals("The result should be SEND_CONTINUE", AuthenticationStatus.SEND_CONTINUE, status);
    }

    @Test
    public void testValidateRequestNewAuthenticateBasicAuthCredSuccess() throws Exception {
        IdentityStoreHandler mish = new MyIdentityStoreHandler();
        withNewAuthenticate(baCred).withMessageInfo().withBeanInstance(mish);

        AuthenticationStatus status = fam.validateRequest(req, res, hmc);
        assertEquals("The result should be SUCCESS", AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void testValidateRequestNewAuthenticateUsernamePasswordCredSuccess() throws Exception {
        IdentityStoreHandler mish = new MyIdentityStoreHandler();
        withNewAuthenticate(upCred).withMessageInfo().withBeanInstance(mish);

        AuthenticationStatus status = fam.validateRequest(req, res, hmc);
        assertEquals("The result should be SUCCESS", AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void testValidateRequestNewAuthenticateInvalidUsernamePasswordCredFailure() throws Exception {
        IdentityStoreHandler mish = new MyIdentityStoreHandler();
        withNewAuthenticate(invalidUpCred).withBeanInstance(mish);

        AuthenticationStatus status = fam.validateRequest(req, res, hmc);
        assertEquals("The result should be SEND_FAILURE", AuthenticationStatus.SEND_FAILURE, status);
    }

    @Test
    public void testValidateRequestNewAuthenticateInvalidCredentialFailure() throws Exception {
        IdentityStoreHandler mish = new MyIdentityStoreHandler();
        withNewAuthenticate(coCred).withBeanInstance(mish);

        AuthenticationStatus status = fam.validateRequest(req, res, hmc);
        assertEquals("The result should be SEND_FAILURE", AuthenticationStatus.SEND_FAILURE, status);
    }

    /*************** support methods **************/
    @SuppressWarnings("unchecked")
    private FormAuthenticationMechanismTest withBeanInstance(final IdentityStoreHandler value) throws Exception {
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
    private FormAuthenticationMechanismTest withMessageContext() throws Exception {
        
        mockery.checking(new Expectations() {
            {
                one(hmc).getClientSubject();
                will(returnValue(cs));
                one(hmc).getAuthParameters();
                will(returnValue(null));
                one(hmc).getRequest();
                will(returnValue(req));
                one(hmc).getResponse();
                will(returnValue(res));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private FormAuthenticationMechanismTest withMessageInfo() throws Exception {
        
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
    private FormAuthenticationMechanismTest withHandler(final CallbackHandler handler) throws Exception {
        
        mockery.checking(new Expectations() {
            {
                one(hmc).getHandler();
                will(returnValue(handler));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private FormAuthenticationMechanismTest withUsernamePassword(final String username, final String password) throws Exception {
        
        mockery.checking(new Expectations() {
            {
                one(req).getRequestURI();
                will(returnValue("/j_security_check"));
                one(req).getParameter("j_username");
                will(returnValue(username));
                one(req).getParameter("j_password");
                will(returnValue(password));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private FormAuthenticationMechanismTest withAuthenticationRequest(final boolean value) throws Exception {
        
        mockery.checking(new Expectations() {
            {
                one(hmc).isAuthenticationRequest();
                will(returnValue(value));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private FormAuthenticationMechanismTest withProtected(final boolean value) throws Exception {
        
        mockery.checking(new Expectations() {
            {
                one(hmc).isProtected();
                will(returnValue(value));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private FormAuthenticationMechanismTest withCallbackHandlerException(final Exception e) throws Exception {
        
        mockery.checking(new Expectations() {
            {
                one(ch).handle(with(any(Callback[].class)));
                will(throwException(e));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private FormAuthenticationMechanismTest withSetStatusToResponse(final int value) {
        
        mockery.checking(new Expectations() {
            {
                one(res).setStatus(value);
            }
        });
        return this;
    }

    private FormAuthenticationMechanismTest withNewAuthenticate(Credential cred) {
        setNewAuthenticateExpectations().withAuthParamsExpectations(ap).withCredentialExpectations(cred);
        return this;
    }

    private FormAuthenticationMechanismTest setNewAuthenticateExpectations() {
        mockery.checking(new Expectations() {
            {
                one(hmc).getClientSubject();
                will(returnValue(cs));
                never(hmc).getRequest();
                never(hmc).getResponse();
            }
        });
        return this;
    }

    private FormAuthenticationMechanismTest withAuthParamsExpectations(final AuthenticationParameters ap) {
        mockery.checking(new Expectations() {
            {
                one(hmc).getAuthParameters();
                will(returnValue(ap));
            }
        });
        return this;
    }

    private FormAuthenticationMechanismTest withCredentialExpectations(final Credential cred) {
        mockery.checking(new Expectations() {
            {
                allowing(ap).getCredential();
                will(returnValue(cred));
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
            String userid = null;
            String password = null;
            if (cred instanceof BasicAuthenticationCredential) {
                userid = ((BasicAuthenticationCredential)cred).getCaller();
                password = ((BasicAuthenticationCredential)cred).getPasswordAsString();
            } else if (cred instanceof UsernamePasswordCredential) {
                userid = ((UsernamePasswordCredential)cred).getCaller();
                password = ((UsernamePasswordCredential)cred).getPasswordAsString();
            } else if (cred instanceof CallerOnlyCredential) {
                userid = ((CallerOnlyCredential)cred).getCaller();
            } 
            if(USER1.equals(userid) && PASSWORD1.equals(password)) {
                result = new CredentialValidationResult(ISH_ID, USER1, USER1, USER1, new HashSet<String>());
            } else {
                result = CredentialValidationResult.INVALID_RESULT;
            }
            return result;
        }
    }
}
