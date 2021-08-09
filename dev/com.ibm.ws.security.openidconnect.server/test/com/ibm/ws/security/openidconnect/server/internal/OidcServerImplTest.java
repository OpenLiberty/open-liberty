/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

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

import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.security.openidconnect.web.OidcRequest;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.oauth20.OAuth20Authenticator;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

import test.common.SharedOutputManager;

public class OidcServerImplTest {
    private static SharedOutputManager outputMgr;
    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final ComponentContext cc = mockery.mock(ComponentContext.class, "service");
    private final HttpServletRequest req = mockery.mock(HttpServletRequest.class, "req");
    private final HttpServletResponse res = mockery.mock(HttpServletResponse.class, "res");
    private final ServiceReference<OidcServerConfig> ref = mockery.mock(ServiceReference.class, "ref");

    private final OAuth20Authenticator authenticator = mockery.mock(OAuth20Authenticator.class, "authenticator");
    private final ProviderAuthenticationResult result = mockery.mock(ProviderAuthenticationResult.class, "result");

    private final OidcRequest oidcRequest = mockery.mock(OidcRequest.class, "oidcRequest");
    private final ConfigUtils configUtils = mockery.mock(ConfigUtils.class);
    private final HashMap<String, OidcServerConfig> oidcMap = mockery.mock(HashMap.class, "oidcMap");
    private final OidcServerConfig oidcServerConfig = mockery.mock(OidcServerConfig.class, "oidcServerConfig");

    ConcurrentServiceReferenceMap<String, OidcServerConfig> oidcServerConfigRef = mockery.mock(ConcurrentServiceReferenceMap.class, "oidcServerConfigRef");

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

        try {
            mockery.checking(new Expectations() {
                {
                    allowing(ref).getProperty("id");
                    will(returnValue("OidcServerConfig_1"));
                    allowing(oidcServerConfigRef).putReference("OidcServerConfig_1", ref);
                    allowing(oidcServerConfigRef).removeReference("OidcServerConfig_1", ref);
                    allowing(oidcServerConfigRef).activate(cc);
                    allowing(oidcServerConfigRef).deactivate(cc);
                }
            });
            OidcServerImpl service = new OidcServerImpl();
            ProviderAuthenticationResult result = service.authenticate(req, res, null);
            assertNull("Does not get back null", result);

            service.activate(cc);
            service.modify(properties);
            service.deactivate(cc);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /** {@inheritDoc} */
    @Test
    public void testIsIdcAuthorizeRequest() {
        String methodName = "testIsOauthAuthorizeRequest";
        System.out.println("================== " + methodName + " ====================");
        OidcServerImpl service = new OidcServerImpl();
        service.configUtils = this.configUtils;
        service.oidcMap = this.oidcMap;
        final Pattern pattern = Pattern.compile("/oidc/(endpoint|providers)/[\\w-]+/(authorize|registration)");
        try {
            mockery.checking(new Expectations() {
                {
                    one(req).getContextPath();
                    will(returnValue("/oauth2"));

                    one(req).getContextPath();
                    will(returnValue("/oidc"));
                    one(req).getRequestURI();
                    will(returnValue("/oidc/endpoint/unit-Sample/authorize")); //
                    one(req).getPathInfo();
                    will(returnValue("/unitSample/authorize"));
                    one(oidcMap).get("unitSample");
                    will(returnValue(oidcServerConfig));
                    one(oidcServerConfig).getProtectedEndpointsPattern();
                    will(returnValue(pattern));
                    one(req).getRequestURI();
                    will(returnValue("/oidc/endpoint/unit-Sample/authorize"));
                }
            });

            assertFalse("Does not get back false", service.isOIDCSpecificURI(req, true));
            assertTrue("Does not get back true ", service.isOIDCSpecificURI(req, true));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /** {@inheritDoc} */
    @Test
    public void testIsIdcSpecificRequest() {
        String methodName = "testIsOauthAuthorizeRequest";
        System.out.println("================== " + methodName + " ====================");
        OidcServerImpl service = new OidcServerImpl();
        service.configUtils = this.configUtils;
        service.oidcMap = this.oidcMap;
        final Pattern pattern = Pattern.compile("/oidc/(endpoint|providers)/[\\w-]+/.*");
        final Pattern nonPattern = Pattern.compile("/oidc/(endpoint|providers)/[\\w-]+/(end_session|check_session_iframe)");
        try {
            mockery.checking(new Expectations() {
                {
                    one(req).getContextPath();
                    will(returnValue("/oauth2"));

                    one(req).getContextPath();
                    will(returnValue("/oidc"));
                    one(req).getRequestURI();
                    will(returnValue("/oidc/endpoint/unit-Sample/authorize"));
                    one(req).getPathInfo();
                    will(returnValue("/unitSample/authorize"));
                    one(oidcMap).get("unitSample");
                    will(returnValue(oidcServerConfig));
                    one(oidcServerConfig).getEndpointsPattern();
                    will(returnValue(pattern));
                    one(oidcServerConfig).getNonEndpointsPattern();
                    will(returnValue(nonPattern));
                    one(req).getRequestURI();
                    will(returnValue("/oidc/endpoint/unit-Sample/authorize"));
                }
            });

            assertFalse("Does not get back false", service.isOIDCSpecificURI(req, false));
            assertTrue("Does not get back true ", service.isOIDCSpecificURI(req, false));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}