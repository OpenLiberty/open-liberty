/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.security.oauth20.AuthnContext;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.oauth20.TokenIntrospectProvider;

import test.common.SharedOutputManager;

/**
 *
 */
public class TokenIntrospectTest {

    private static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @Rule
    public final TestName testNameRule = new TestName();
    private String testName = null;

    private final PrintWriter writer = mock.mock(PrintWriter.class, "writer");
    private final HttpServletRequest req = mock.mock(HttpServletRequest.class, "req");
    private final HttpServletResponse resp = mock.mock(HttpServletResponse.class, "resp");
    private final OAuth20Token accessToken = mock.mock(OAuth20Token.class, "accessToken");;
    @SuppressWarnings("unchecked")
    private final ConcurrentServiceReferenceMap<String, TokenIntrospectProvider> tokenIntrospectProviderRef = mock.mock(ConcurrentServiceReferenceMap.class);
    private final TokenIntrospectProvider tokenIntrospectProvider = mock.mock(TokenIntrospectProvider.class);
    private final OAuth20Provider provider = mock.mock(OAuth20Provider.class, "provider");
    @SuppressWarnings("rawtypes")
    private final Iterator it = mock.mock(Iterator.class);
    private final OidcBaseClient client = mock.mock(OidcBaseClient.class);

    private final String scopes[] = { "scope", "scope2" };

    static boolean bSetup = false;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        try {
            testName = testNameRule.getMethodName();
            staticHashtable = new Hashtable(); // Get a new Hashtable each test case
            mock.checking(new Expectations() {
                {
                    allowing(tokenIntrospectProviderRef).isEmpty();
                    will(returnValue(true));
                    allowing(tokenIntrospectProviderRef).getServices();
                    will(returnValue(it));
                    allowing(accessToken).getTokenString();
                    will(returnValue("ThisIsAMockedTokenString"));
                    allowing(accessToken).getScope();
                    will(returnValue(scopes));
                    allowing(accessToken).getCreatedAt();
                    will(returnValue(89705645L));
                    allowing(accessToken).getLifetimeSeconds();
                    will(returnValue(1000));
                    allowing(accessToken).getUsername();
                    will(returnValue("bob"));
                    allowing(accessToken).getExtensionProperties();
                    will(returnValue(new HashMap()));

                    allowing(resp).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(resp).setStatus(HttpServletResponse.SC_OK);
                    allowing(resp).getWriter();
                    will(returnValue(writer));
                    allowing(resp).getHeader(with(any(String.class)));
                    will(returnValue(null));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();

                    // Keep the error message on the resp in case it happens
                    allowing(resp).sendError(with(any(Integer.class)), with(any(String.class)));
                    will(sendError());
                    allowing(resp).sendError(with(any(Integer.class)));
                    will(sendError());
                }
            });
        } catch (IOException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e.printStackTrace();
        }
        bSetup = true;
    }

    @After
    public void tearDown() {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void setUpAfterClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.restoreStreams();
    }

    /**
     * Test expired token
     *
     * Test method for com.ibm.ws.security.oauth20.web.OAuth20EndpointServlet.introspect()
     */
    @Test
    public void testIntrospectProviderNormal() {
        try {
            mock.checking(new Expectations() {
                {
                    one(it).hasNext();
                    will(returnValue(true));
                    one(it).next();
                    will(returnValue(tokenIntrospectProvider));
                    one(tokenIntrospectProviderRef).size();
                    will(returnValue(1));
                    one(tokenIntrospectProvider).getUserInfo(with(any(AuthnContext.class)));
                    will(returnValue("{\"username\":\"user1\",\"scope\":\"email scope1 scope2\"}")); // a json format string
                }
            });
            TokenIntrospect tokenIntrospect = new TokenIntrospect();
            TokenIntrospect.setTokenIntrospect(tokenIntrospectProviderRef);
            tokenIntrospect.callTokenIntrospect(req, resp, accessToken);
            assertEquals("error hastable should not have any parameters", staticHashtable.size(), 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName, t);
        }
    }

    /**
     * Test null
     *
     */
    @Test
    public void testIntrospectProviderNull() {
        try {

            mock.checking(new Expectations() {
                {
                    one(it).hasNext();
                    will(returnValue(true));
                    one(it).next();
                    will(returnValue(tokenIntrospectProvider));
                    one(tokenIntrospectProviderRef).size();
                    will(returnValue(1));
                    one(tokenIntrospectProvider).getUserInfo(with(any(AuthnContext.class)));
                    will(returnValue((String) null)); // null
                    one(it).hasNext();
                    will(returnValue(false));
                }
            });
            TokenIntrospect tokenIntrospect = new TokenIntrospect();
            TokenIntrospect.setTokenIntrospect(tokenIntrospectProviderRef);
            tokenIntrospect.callTokenIntrospect(req, resp, accessToken);
            assertEquals("error code is not 500", staticHashtable.get("parameter0"), 500);
            //we only call sendError with error status code 500 but no second parameter now
            //String errMsg = (String) staticHashtable.get("parameter1");
            //assertTrue("ErrMsg does not contain CWWKS1452E:" + errMsg, errMsg.indexOf("CWWKS1452E") >= 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName, t);
        }
    }

    /**
     * Test bad json string
     *
     */
    @Test
    public void testIntrospectProviderBadJson() {
        try {
            mock.checking(new Expectations() {
                {
                    one(it).hasNext();
                    will(returnValue(true));
                    one(it).next();
                    will(returnValue(tokenIntrospectProvider));
                    one(tokenIntrospectProviderRef).size();
                    will(returnValue(1));
                    one(tokenIntrospectProvider).getUserInfo(with(any(AuthnContext.class)));
                    will(returnValue("{][\"a\":\"po\"}]{]{[")); // bad json
                    one(it).hasNext();
                    will(returnValue(false));
                }
            });
            TokenIntrospect tokenIntrospect = new TokenIntrospect();
            TokenIntrospect.setTokenIntrospect(tokenIntrospectProviderRef);
            tokenIntrospect.callTokenIntrospect(req, resp, accessToken);
            assertEquals("error code is not 500", staticHashtable.get("parameter0"), 500);
            //we only call sendError with error status code 500 but no second parameter now
            //String errMsg = (String) staticHashtable.get("parameter1");
            //assertTrue("ErrMsg does not contain CWWKS1452E:" + errMsg, errMsg.indexOf("CWWKS1452E") >= 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName, t);
        }
    }

    @Test
    public void testIsClientAllowedToIntrospectToken_clientCanIntrospectTokenWithoutUsedBy() {
        try {
            mock.checking(new Expectations() {
                {
                    one(accessToken).getUsedBy();
                    will(returnValue(null));
                    one(client).getClientId();
                    will(returnValue("client id"));
                    allowing(accessToken).getGrantType();
                    will(returnValue(OAuth20Constants.GRANT_TYPE_APP_TOKEN));
                    allowing(provider).isLocalStoreUsed();
                    will(returnValue(true));
                }
            });
            TokenIntrospect tokenIntrospect = new TokenIntrospect();
            boolean result = tokenIntrospect.isClientAllowedToIntrospectToken(accessToken, client, provider);
            assertTrue("Any client should be able to introspect an access token that doesn't have used_by set.", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName, t);
        }
    }

    @Ignore
    @Test
    public void testIsClientAllowedToIntrospectToken_clientCannotIntrospectTokenWithEmptyUsedBy() {
        try {
            mock.checking(new Expectations() {
                {
                    one(accessToken).getUsedBy();
                    will(returnValue(new String[0]));
                    one(client).getClientId();
                    will(returnValue("some id"));
                    allowing(accessToken).getGrantType();
                    will(returnValue(OAuth20Constants.GRANT_TYPE_APP_TOKEN));
                    allowing(provider).isLocalStoreUsed();
                    will(returnValue(true));
                }
            });
            TokenIntrospect tokenIntrospect = new TokenIntrospect();
            boolean result = tokenIntrospect.isClientAllowedToIntrospectToken(accessToken, client, provider);
            assertFalse("Clients should not be able to introspect an access token that has an empty used_by value.", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName, t);
        }
    }

    @Ignore
    @Test
    public void testIsClientAllowedToIntrospectToken_clientCannotIntrospectTokenIfUsedByDoesntContainClientId() {
        try {
            mock.checking(new Expectations() {
                {
                    one(accessToken).getUsedBy();
                    will(returnValue(new String[] { "one id", "another id", "not your id" }));
                    one(client).getClientId();
                    will(returnValue("my id"));
                    allowing(accessToken).getGrantType();
                    will(returnValue(OAuth20Constants.GRANT_TYPE_APP_TOKEN));
                    allowing(provider).isLocalStoreUsed();
                    will(returnValue(true));
                }
            });
            TokenIntrospect tokenIntrospect = new TokenIntrospect();
            boolean result = tokenIntrospect.isClientAllowedToIntrospectToken(accessToken, client, provider);
            assertFalse("Clients should not be able to introsect an access token whose used_by value does not contain the client's client_id.", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName, t);
        }
    }

    @Test
    public void testIsClientAllowedToIntrospectToken_clientCanIntrospectTokenIfUsedByContainsClientId() {
        try {
            mock.checking(new Expectations() {
                {
                    one(accessToken).getUsedBy();
                    will(returnValue(new String[] { "one id", "another id", "not your id", "my id" }));
                    one(client).getClientId();
                    will(returnValue("my id"));
                    allowing(accessToken).getGrantType();
                    will(returnValue(OAuth20Constants.GRANT_TYPE_APP_TOKEN));
                    allowing(provider).isLocalStoreUsed();
                    will(returnValue(true));
                }
            });
            TokenIntrospect tokenIntrospect = new TokenIntrospect();
            boolean result = tokenIntrospect.isClientAllowedToIntrospectToken(accessToken, client, provider);
            assertTrue("Clients should be able to introspect an access token whose used_by value contains the client's client_id.", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName, t);
        }
    }

    @Test
    public void testIsClientAllowedToIntrospectToken_clientCanIntrospectTokenIfnotAppTokenGT() {
        try {
            mock.checking(new Expectations() {
                {
                    one(client).getClientId();
                    will(returnValue("my id"));
                    allowing(accessToken).getGrantType();
                    will(returnValue(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE));

                }
            });
            TokenIntrospect tokenIntrospect = new TokenIntrospect();
            boolean result = tokenIntrospect.isClientAllowedToIntrospectToken(accessToken, client, provider);
            assertTrue("Clients should be able to introspect an access token whose grant type is not app-token.", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName, t);
        }
    }

    static Hashtable<String, Object> staticHashtable = new Hashtable(); // set up every test case. see setUp()

    // This is a generic action to save the parameters of a junit mocked method.
    public static class SaveCallingParametersAction implements Action {
        Hashtable<String, Object> hashtable;
        Object returnObject;

        public SaveCallingParametersAction(Hashtable hashtable, Object returnObject) {
            this.hashtable = hashtable;
            this.returnObject = returnObject;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("save the calling parameters for verification later");
        }

        @Override
        public Object invoke(Invocation invocation) throws Throwable {
            int iCnt = invocation.getParameterCount();
            for (int iI = 0; iI < iCnt; iI++) {
                hashtable.put("parameter" + iI, invocation.getParameter(iI));
            }
            return returnObject; // if void we should return null. Otherwise return the expected returning object
        }
    }

    public static <T> Action sendError(T... newElements) {
        return new SaveCallingParametersAction(staticHashtable, (Object) null); // sendError return "void"
    }
}
