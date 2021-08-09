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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.callback.PasswordValidationCallback;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.security.enterprise.credential.CallerOnlyCredential;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
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

import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.javaeesec.CDIHelperTestWrapper;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

import test.common.SharedOutputManager;

public class CustomFormAuthenticationMechanismTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @SuppressWarnings("rawtypes")
    private CDI cdi;
    private Instance<IdentityStore> iis;
    private Instance<IdentityStoreHandler> iish;
    private CustomFormAuthenticationMechanism cfam;
    private IdentityStore ids;
    private BeanManager bm;
    private CDIService cdis;
    private CDIHelperTestWrapper cdiHelperTestWrapper;

    private HttpMessageContext hmc;
    private MessageInfo mi;
    private HttpServletRequest request;
    private HttpServletResponse res;
    private Subject cs;
    private CallbackHandler ch;
    private WebAppSecurityConfig webAppSecurityConfig;
    private AuthenticationParameters ap;
    private final Map<String, String> mm = new HashMap<String, String>();
    private CallerOnlyCredential coCred;
    private BasicAuthenticationCredential baCred;
    private UsernamePasswordCredential upCred, invalidUpCred;
    private boolean isRegistryAvailable = true;

    private final String ISH_ID = "IdentityStore1";
    private final String USER1 = "user1";
    private final String PASSWORD1 = "s3cur1ty";
    private final String INVALID_PASSWORD = "invalid";

//    @SuppressWarnings("restriction")
//    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.javaeesec.*=all");

    @Rule
    public final TestName testName = new TestName();

    /**
     * @throws java.lang.Exception
     */
//    @SuppressWarnings("restriction")
//    @BeforeClass
//    public static void setUpBeforeClass() throws Exception {
//        outputMgr.captureStreams();
//    }

    /**
     * @throws java.lang.Exception
     */
//    @SuppressWarnings("restriction")
//    @AfterClass
//    public static void tearDownAfterClass() throws Exception {
//        outputMgr.dumpStreams();
//        outputMgr.resetStreams();
//        outputMgr.restoreStreams();
//    }

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        cdi = mockery.mock(CDI.class);
        iis = mockery.mock(Instance.class, "ids");
        iish = mockery.mock(Instance.class, "idshandle");
        ids = mockery.mock(IdentityStore.class);
        bm = mockery.mock(BeanManager.class, "bm");
        ap = mockery.mock(AuthenticationParameters.class);
        hmc = mockery.mock(HttpMessageContext.class);
        mi = mockery.mock(MessageInfo.class);
        request = mockery.mock(HttpServletRequest.class);
        res = mockery.mock(HttpServletResponse.class);
        ch = mockery.mock(CallbackHandler.class);
        cs = new Subject();
        cdis = mockery.mock(CDIService.class);
        cdiHelperTestWrapper = new CDIHelperTestWrapper(mockery, null);
        cdiHelperTestWrapper.setCDIService(cdis);

        Utils utils = new Utils() {
            @Override
            protected boolean isRegistryAvailable() {
                return isRegistryAvailable;
            }
        };
        cfam = new CustomFormAuthenticationMechanism(utils) {
            @SuppressWarnings("rawtypes")
            @Override
            protected CDI getCDI() {
                return cdi;
            }
        };

        coCred = new CallerOnlyCredential(USER1);
        upCred = new UsernamePasswordCredential(USER1, PASSWORD1);
        invalidUpCred = new UsernamePasswordCredential(USER1, INVALID_PASSWORD);
        baCred = new BasicAuthenticationCredential(Base64Coder.base64Encode(USER1 + ":" + PASSWORD1));
        webAppSecurityConfig = mockery.mock(WebAppSecurityConfig.class);
        setRequestExpections(request, webAppSecurityConfig);
    }

    private void setRequestExpections(HttpServletRequest request, final WebAppSecurityConfig webAppSecurityConfig) {
        mockery.checking(new Expectations() {
            {
                allowing(request).getAttribute("com.ibm.ws.webcontainer.security.WebAppSecurityConfig");
                will(returnValue(webAppSecurityConfig));
            }
        });
    }

    @SuppressWarnings("restriction")
    @After
    public void tearDown() throws Exception {
        cdiHelperTestWrapper.unsetCDIService(cdis);
        mockery.assertIsSatisfied();
//        outputMgr.dumpStreams();
//        outputMgr.resetStreams();
    }

    @Test
    public void testValidateRequestValidIdAndPWIdentityStoreHandler() throws Exception {
        IdentityStoreHandler mish = new MyIdentityStoreHandler();
        withMessageContext(ap).withIsNewAuthentication(false).withGetResponse().withMessageInfo();
        withUsernamePassword(USER1, PASSWORD1).withIDSBeanInstance(ids, false, false).withIDSHandlerBeanInstance(mish).withSetStatusToResponse(HttpServletResponse.SC_OK);

        AuthenticationStatus status = cfam.validateRequest(request, res, hmc);
        assertEquals("The result should be SUCCESS", AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void testValidateRequestInvalidIdAndPWIdentityStoreHandler() throws Exception {
        IdentityStoreHandler mish = new MyIdentityStoreHandler();
        withMessageContext(ap).withIsNewAuthentication(false).withGetResponse();
        withUsernamePassword(USER1, "invalid").withIDSBeanInstance(ids, false, false).withIDSHandlerBeanInstance(mish).withSetStatusToResponse(HttpServletResponse.SC_UNAUTHORIZED);

        AuthenticationStatus status = cfam.validateRequest(request, res, hmc);
        assertEquals("The result should be SEND_FAILURE", AuthenticationStatus.SEND_FAILURE, status);
    }

    @Test
    public void testValidateRequestValidIdAndPWNoIdentityStoreHandlerCallbackHandler() throws Exception {
        final MyCallbackHandler mch = new MyCallbackHandler();
        withMessageContext(ap).withIsNewAuthentication(false).withGetResponse().withMessageInfo().withHandler(mch);
        withUsernamePassword(USER1, PASSWORD1).withIDSBeanInstance(null, true, false).withSetStatusToResponse(HttpServletResponse.SC_OK);

        AuthenticationStatus status = cfam.validateRequest(request, res, hmc);
        assertEquals("The result should be SUCCESS", AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void testValidateRequestValidIdAndPWNoIdentityStoreHandlerNoUserRegistry() throws Exception {
        withMessageContext(ap).withIsNewAuthentication(false).withGetResponse();
        withUsernamePassword(USER1, PASSWORD1).withIDSBeanInstance(null, true, false).withSetStatusToResponse(HttpServletResponse.SC_OK);
        isRegistryAvailable = false;
        AuthenticationStatus status = cfam.validateRequest(request, res, hmc);
        isRegistryAvailable = true;
        assertEquals("The result should be NOT_DONE", AuthenticationStatus.NOT_DONE, status);
    }

    @Test
    public void testValidateRequestInvalidIdAndPWNoIdentityStoreHandlerCallbackHandler() throws Exception {
        final MyCallbackHandler mch = new MyCallbackHandler();
        withMessageContext(ap).withIsNewAuthentication(false).withGetResponse().withHandler(mch);
        withUsernamePassword(USER1, "invalid").withIDSBeanInstance(null, false, true).withSetStatusToResponse(HttpServletResponse.SC_UNAUTHORIZED);

        AuthenticationStatus status = cfam.validateRequest(request, res, hmc);
        assertEquals("The result should be SEND_FAILURE", AuthenticationStatus.SEND_FAILURE, status);
    }

    @Test
    public void testValidateRequestValidIdAndPWNoIdentityStoreHandlerCallbackHandlerException() throws Exception {
        final String msg = "An Exception by CallbackHandler";
        IOException ex = new IOException(msg);
        withMessageContext(ap).withIsNewAuthentication(false).withGetResponse().withHandler(ch);
        withUsernamePassword(USER1, PASSWORD1).withIDSBeanInstance(null, true, false).withCallbackHandlerException(ex);

        try {
            cfam.validateRequest(request, res, hmc);
            fail("AuthenticationException should be thrown.");
        } catch (AuthenticationException e) {
            assertTrue("The message does not match with the expectation", e.getMessage().contains(msg));
        }
    }

    @SuppressWarnings("restriction")
    @Test
    public void testValidateRequestInvalidCredential() throws Exception {
        CallerOnlyCredential coc = new CallerOnlyCredential(USER1);
        withMessageContext(ap).withIsNewAuthentication(false).withGetResponse().withHandler(ch);
        withCredential(coc).withIDSBeanInstance(null, false, true);

        try {
            cfam.validateRequest(request, res, hmc);
            fail("AuthenticationException should be thrown.");
        } catch (AuthenticationException e) {
//            assertTrue("CWWKS1927E  message was not logged", outputMgr.checkForStandardErr("CWWKS1927E:"));
            assertTrue("The message should contains CWWKS1927E", e.getMessage().contains("CWWKS1927E"));
        }
    }

    @Test
    public void testValidateRequestNoIdAndPWAuthReqFalseProtectedTrue() throws Exception {
        withMessageContext(ap);
        withUsernamePassword(null, null).withAuthenticationRequest(false).withProtected(true);

        AuthenticationStatus status = cfam.validateRequest(request, res, hmc);
        assertEquals("The result should be SEND_CONTINUE", AuthenticationStatus.SEND_CONTINUE, status);
    }

    @Test
    public void testValidateRequestNoIdAndPWAuthReqTrueProtectedFalse() throws Exception {
        withMessageContext(ap);
        withUsernamePassword(null, null).withAuthenticationRequest(true);

        AuthenticationStatus status = cfam.validateRequest(request, res, hmc);
        assertEquals("The result should be SEND_CONTINUE", AuthenticationStatus.SEND_CONTINUE, status);
    }

    @Test
    public void testValidateRequestNoIdAndPWAuthReqFalseProtectedFalse() throws Exception {
        withMessageContext(ap);
        withUsernamePassword(null, null).withAuthenticationRequest(false).withProtected(false);

        AuthenticationStatus status = cfam.validateRequest(request, res, hmc);
        assertEquals("The result should be NOT_DONE", AuthenticationStatus.NOT_DONE, status);
    }

    @Test
    public void testValidateRequestNoIdAndPW() throws Exception {
        withMessageContext(null);

        AuthenticationStatus status = cfam.validateRequest(request, res, hmc);
        assertEquals("The result should be SEND_CONTINUE", AuthenticationStatus.SEND_CONTINUE, status);
    }

    @Test
    public void testValidateRequestNewAuthenticateBasicAuthCredSuccess() throws Exception {
        IdentityStoreHandler mish = new MyIdentityStoreHandler();
        withMessageContext(ap).withNewAuthenticate(baCred).withMessageInfo();
        withIDSBeanInstance(ids, false, false).withIDSHandlerBeanInstance(mish);

        AuthenticationStatus status = cfam.validateRequest(request, res, hmc);
        assertEquals("The result should be SUCCESS", AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void testValidateRequestNewAuthenticateUsernamePasswordCredSuccess() throws Exception {
        IdentityStoreHandler mish = new MyIdentityStoreHandler();
        withMessageContext(ap).withNewAuthenticate(upCred).withMessageInfo();
        withIDSBeanInstance(ids, false, false).withIDSHandlerBeanInstance(mish);

        AuthenticationStatus status = cfam.validateRequest(request, res, hmc);
        assertEquals("The result should be SUCCESS", AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void testValidateRequestNewAuthenticateInvalidUsernamePasswordCredFailure() throws Exception {
        IdentityStoreHandler mish = new MyIdentityStoreHandler();
        withMessageContext(ap).withNewAuthenticate(invalidUpCred);
        withIDSBeanInstance(ids, false, false).withIDSHandlerBeanInstance(mish);

        AuthenticationStatus status = cfam.validateRequest(request, res, hmc);
        assertEquals("The result should be SEND_FAILURE", AuthenticationStatus.SEND_FAILURE, status);
    }

    @Test
    public void testValidateRequestNewAuthenticateInvalidCredentialFailure() throws Exception {
        IdentityStoreHandler mish = new MyIdentityStoreHandler();
        withMessageContext(ap).withNewAuthenticate(coCred);
        withIDSBeanInstance(ids, false, false).withIDSHandlerBeanInstance(mish);

        AuthenticationStatus status = cfam.validateRequest(request, res, hmc);
        assertEquals("The result should be SEND_FAILURE", AuthenticationStatus.SEND_FAILURE, status);
    }

    /*************** support methods **************/
    @SuppressWarnings("unchecked")
    private CustomFormAuthenticationMechanismTest withIDSBeanInstance(final IdentityStore value, final boolean isUnsatisfied, final boolean isAmbiguous) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(cdi).select(IdentityStore.class);
                will(returnValue(iis));
                allowing(iis).isUnsatisfied();
                will(returnValue(isUnsatisfied));
                allowing(iis).isAmbiguous();
                will(returnValue(isAmbiguous));
                allowing(iis).get();
                will(returnValue(value));
                atMost(1).of(cdi).getBeanManager();
                will(returnValue(bm));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private CustomFormAuthenticationMechanismTest withIDSHandlerBeanInstance(final IdentityStoreHandler value) throws Exception {
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

    private CustomFormAuthenticationMechanismTest withMessageContext(final AuthenticationParameters ap) throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(hmc).getClientSubject();
                will(returnValue(cs));
                allowing(hmc).getAuthParameters();
                will(returnValue(ap));
                allowing(hmc).getRequest();
                will(returnValue(request));
            }
        });
        return this;
    }

    private CustomFormAuthenticationMechanismTest withIsNewAuthentication(final boolean value) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(ap).isNewAuthentication();
                will(returnValue(value));
            }
        });
        return this;
    }

    private CustomFormAuthenticationMechanismTest withGetResponse() throws Exception {

        mockery.checking(new Expectations() {
            {
                one(hmc).getResponse();
                will(returnValue(res));
            }
        });
        return this;
    }

    private CustomFormAuthenticationMechanismTest withMessageInfo() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(hmc).getMessageInfo();
                will(returnValue(mi));
                allowing(mi).getMap();
                will(returnValue(mm));
            }
        });
        return this;
    }

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

    private CustomFormAuthenticationMechanismTest withAuthenticationRequest(final boolean value) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(hmc).isAuthenticationRequest();
                will(returnValue(value));
            }
        });
        return this;
    }

    private CustomFormAuthenticationMechanismTest withProtected(final boolean value) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(hmc).isProtected();
                will(returnValue(value));
            }
        });
        return this;
    }

    private CustomFormAuthenticationMechanismTest withCallbackHandlerException(final Exception e) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(ch).handle(with(any(Callback[].class)));
                will(throwException(e));
            }
        });
        return this;
    }

    private CustomFormAuthenticationMechanismTest withSetStatusToResponse(final int value) {
        mockery.checking(new Expectations() {
            {
                one(res).setStatus(value);
            }
        });
        return this;
    }

    private CustomFormAuthenticationMechanismTest withNewAuthenticate(Credential cred) {
        setNewAuthenticateExpectations().withAuthParamsExpectations(ap).withCredentialExpectations(cred);
        return this;
    }

    private CustomFormAuthenticationMechanismTest setNewAuthenticateExpectations() {
        mockery.checking(new Expectations() {
            {
                never(hmc).getResponse();
            }
        });
        return this;
    }

    private CustomFormAuthenticationMechanismTest withAuthParamsExpectations(final AuthenticationParameters ap) {
        mockery.checking(new Expectations() {
            {
                one(ap).isNewAuthentication();
                will(returnValue(true));
            }
        });
        return this;
    }

    private CustomFormAuthenticationMechanismTest withCredentialExpectations(final Credential cred) {
        mockery.checking(new Expectations() {
            {
                allowing(ap).getCredential();
                will(returnValue(cred));
            }
        });
        return this;
    }

    class MyCallbackHandler implements CallbackHandler {
        @Override
        public void handle(Callback[] callbacks) {
            PasswordValidationCallback pwcb = (PasswordValidationCallback) callbacks[0];
            String userid = pwcb.getUsername();
            String password = new String(pwcb.getPassword());
            boolean result = USER1.equals(userid) && PASSWORD1.equals(password);
            pwcb.setResult(result);
        }
    }

    class MyIdentityStoreHandler implements IdentityStoreHandler {
        @Override
        public CredentialValidationResult validate(Credential cred) {
            CredentialValidationResult result = null;
            String userid = null;
            String password = null;
            if (cred instanceof BasicAuthenticationCredential) {
                userid = ((BasicAuthenticationCredential) cred).getCaller();
                password = ((BasicAuthenticationCredential) cred).getPasswordAsString();
            } else if (cred instanceof UsernamePasswordCredential) {
                userid = ((UsernamePasswordCredential) cred).getCaller();
                password = ((UsernamePasswordCredential) cred).getPasswordAsString();
            } else if (cred instanceof CallerOnlyCredential) {
                userid = ((CallerOnlyCredential) cred).getCaller();
            }
            if (USER1.equals(userid) && PASSWORD1.equals(password)) {
                result = new CredentialValidationResult(ISH_ID, USER1, USER1, USER1, new HashSet<String>());
            } else {
                result = CredentialValidationResult.INVALID_RESULT;
            }
            return result;
        }
    }

}
