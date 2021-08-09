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
package com.ibm.ws.security.oauth20.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.oauth20.OAuth20Authenticator;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

import test.common.SharedOutputManager;

public class OAuth20ServiceImplTest {
    private static SharedOutputManager outputMgr;
    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final ComponentContext cc = mockery.mock(ComponentContext.class, "service");
    private final HttpServletRequest req = mockery.mock(HttpServletRequest.class, "req");
    private final HttpServletResponse res = mockery.mock(HttpServletResponse.class, "res");
    private final OAuth20Authenticator authenticator = mockery.mock(OAuth20Authenticator.class, "authenticator");
    private final ProviderAuthenticationResult result = mockery.mock(ProviderAuthenticationResult.class, "result");
    private final ServiceReference<OAuth20Provider> oauth20ref = mockery.mock(ServiceReference.class, "oauth20ref");
    private final OAuth20Provider oauth20Provider = mockery.mock(OAuth20Provider.class, "oauth20Provider");

    ConcurrentServiceReferenceMap<String, OidcServerConfig> oidcServerConfigRef = new ConcurrentServiceReferenceMap<String, OidcServerConfig>("oidcServerConfig");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void setUpAfterClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.restoreStreams();
    }

    @Test
    public void testDummyMethods() {
        String methodName = "testDummyMethod";
        System.out.println("================== " + methodName + " ====================");
        Map<String, Object> properties = new HashMap<String, Object>();
        OAuth20ServiceImpl service = new OAuth20ServiceImpl();
        service.authenticator = authenticator;
        service.activate(cc);
        service.modify(properties);
        service.deactivate(cc);
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(authenticator).authenticate(req, res);
                    will(returnValue(result));
                    allowing(result).getStatus();
                    will(returnValue(AuthResult.SUCCESS));
                }
            });

            ProviderAuthenticationResult result1 = service.authenticate(req, res);
            assertEquals("Does not get back the expecting result(1)", result, result1);
            ProviderAuthenticationResult result2 = service.authenticate(req, res, oidcServerConfigRef);
            assertEquals("Does not get back the expecting result(2)", result, result2);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /** {@inheritDoc} */
    @Test
    public void testIsOauthAuthorizeRequest() {
        String methodName = "testIsOauthAuthorizeRequest";
        System.out.println("================== " + methodName + " ====================");
        OAuth20ServiceImpl service = new OAuth20ServiceImpl();

        try {
            mockery.checking(new Expectations() {
                {
                    //allowing(oauth20ref).getService(with(any(String.class)));
                    //will(returnValue(oauth20Provider));
                    one(oauth20ref).getProperty("id");
                    will(returnValue("unit-Sample"));
                    one(oauth20ref).getProperty("service.id");
                    will(returnValue(Long.valueOf(998)));
                    one(oauth20ref).getProperty("service.ranking");
                    will(returnValue(Integer.valueOf(0)));
                    one(req).getContextPath();
                    will(returnValue("/oauth2"));
                    one(req).getRequestURI();
                    will(returnValue("/oauth2/endpoint/unit-Sample/authorize"));
                    one(req).getPathInfo();
                    will(returnValue("/unit-Sample/authorize"));
                    one(cc).locateService("oauth20Provider", oauth20ref);
                    will(returnValue(oauth20Provider));
                    //one(oauth20Provider).isMiscUri(req);
                    //will(returnValue(true));

                    one(req).getContextPath();
                    will(returnValue("/oidc"));
                    one(req).getRequestURI();
                    will(returnValue("/oidc/endpoint/unit-Sample/authorize"));
                    one(req).getPathInfo();
                    will(returnValue("/unit-Sample/authorize"));
                    //one(cc).locateService("oauth20Provider", oauth20ref);
                    //will(returnValue(oauth20Provider));
                    //one(oauth20Provider).isMiscUri(req);
                    //will(returnValue(false));
                }
            });
            service.setOauth20Provider(oauth20ref);
            service.activate(cc);

            assertTrue("Does not get back true ", service.isOauthSpecificURI(req, true));
            assertFalse("Does not get back false ", service.isOauthSpecificURI(req, true));

            mockery.checking(new Expectations() {
                {

                    one(req).getContextPath();
                    will(returnValue("/oauth2"));
                    one(req).getRequestURI();
                    will(returnValue("/oauth2/endpoint/unit-Sample/authorize"));
                    one(req).getPathInfo();
                    will(returnValue("/unit-Sample/authorize"));
                    one(cc).locateService("oauth20Provider", oauth20ref);
                    will(returnValue(oauth20Provider));
                    //one(oauth20Provider).isMiscUri(req);
                    //will(returnValue(true));
                    one(req).getPathInfo();
                    will(returnValue("/unit-Sample/authorize"));

                    //one(oauth20ref).getProperty("id");
                    //will(returnValue("unit-Sample"));
                    //one(oauth20ref).getProperty("service.id");
                    //will(returnValue(Long.valueOf(998)));
                    //one(oauth20ref).getProperty("service.ranking");
                    //will(returnValue(Integer.valueOf(0)));
                    one(req).getContextPath();
                    will(returnValue("/oidc"));
                    one(req).getRequestURI();
                    will(returnValue("/oidc/endpoint/unit-Sample/authorize"));
                    //one(req).getPathInfo();
                    //will(returnValue("/unit-Sample/authorize"));
                    //one(cc).locateService("oauth20Provider", oauth20ref);
                    //will(returnValue(oauth20Provider));
                    one(oauth20Provider).isMiscUri(req);
                    will(returnValue(false));
                }
            });
            //service.setOAuth20Provider(oauth20ref);
            //service.activate(cc);

            assertTrue("Does not get back true ", service.isOauthSpecificURI(req, false));
            assertFalse("Does not get back false ", service.isOauthSpecificURI(req, false));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        } finally {
            service.deactivate(cc);
        }
    }

}