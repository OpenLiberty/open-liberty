/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.internal;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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

import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.openidconnect.clients.common.ClientConstants;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.webcontainer.security.openidconnect.OidcClient;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

import test.common.SharedOutputManager;

public class OidcClientImplGetOidcProviderTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final ComponentContext cc = mock.mock(ComponentContext.class);

    @SuppressWarnings("unchecked")
    private final ConcurrentServiceReferenceMap<String, AuthenticationFilter> filterRef = mock.mock(ConcurrentServiceReferenceMap.class, "filterRef");

    @SuppressWarnings("unchecked")
    private final AtomicServiceReference<OidcClient> oidcClientRef = mock.mock(AtomicServiceReference.class, "oidcClientRef");

    @SuppressWarnings("unchecked")
    private final ConcurrentServiceReferenceMap<String, OidcClientConfig> oidcClientConfigRef = mock.mock(ConcurrentServiceReferenceMap.class, "oidcClientConfigRef");

    @SuppressWarnings("unchecked")
    private final ServiceReference<OidcClientConfig> oidcClientConfigServiceRef1 = mock.mock(ServiceReference.class, "oidcClientConfigServiceRef1");
    @SuppressWarnings("unchecked")
    private final ServiceReference<OidcClientConfig> oidcClientConfigServiceRef2 = mock.mock(ServiceReference.class, "oidcClientConfigServiceRef2");
    @SuppressWarnings("unchecked")
    private final ServiceReference<OidcClientConfig> oidcClientConfigServiceRef3 = mock.mock(ServiceReference.class, "oidcClientConfigServiceRef3");

    private final OidcClientConfig oidcClientConfig1 = mock.mock(OidcClientConfig.class, "oidClientConfig1");
    private final OidcClientConfig oidcClientConfig2 = mock.mock(OidcClientConfig.class, "oidClientConfig2");
    private final OidcClientConfig oidcClientConfig3 = mock.mock(OidcClientConfig.class, "oidClientConfig3");

    @SuppressWarnings("unchecked")
    private final ServiceReference<AuthenticationFilter> authFilterServiceRef1 = mock.mock(ServiceReference.class, "authFilterServiceRef1");
    @SuppressWarnings("unchecked")
    private final ServiceReference<AuthenticationFilter> authFilterServiceRef2 = mock.mock(ServiceReference.class, "authFilterServiceRef2");
    @SuppressWarnings("unchecked")
    private final ServiceReference<AuthenticationFilter> authFilterServiceRef3 = mock.mock(ServiceReference.class, "authFilterServiceRef3");

    private final AuthenticationFilter authFilter1 = mock.mock(AuthenticationFilter.class, "authFilter1");
    private final AuthenticationFilter authFilter2 = mock.mock(AuthenticationFilter.class, "authFilter2");
    private final AuthenticationFilter authFilter3 = mock.mock(AuthenticationFilter.class, "authFilter3");

    private final SRTServletRequest req = mock.mock(SRTServletRequest.class, "req");

    static boolean bInit = false;

    @Before
    public void setUp() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(oidcClientRef).activate(cc);

                allowing(req).getContextPath();
                will(returnValue("/foo"));

                allowing(oidcClientConfigRef).getReference("oidcClientConfigServiceRef1");
                will(returnValue(oidcClientConfigServiceRef1));
                allowing(oidcClientConfigRef).getReference("oidcClientConfigServiceRef2");
                will(returnValue(oidcClientConfigServiceRef2));
                allowing(oidcClientConfigRef).getReference("oidcClientConfigServiceRef3");
                will(returnValue(oidcClientConfigServiceRef3));

                allowing(cc).locateService("oidcClientConfig1");
                will(returnValue(oidcClientConfig1));
                allowing(cc).locateService("oidcClientConfig2");
                will(returnValue(oidcClientConfig2));
                allowing(cc).locateService("oidcClientConfig3");
                will(returnValue(oidcClientConfig3));

                allowing(oidcClientConfigServiceRef1).getProperty("id");
                will(returnValue("oidcClientConfigServiceRef1"));
                allowing(oidcClientConfigServiceRef1).getProperty("service.id");
                will(returnValue(991L));
                allowing(oidcClientConfigServiceRef1).getProperty("service.ranking");
                will(returnValue(991L));

                allowing(oidcClientConfigServiceRef2).getProperty("id");
                will(returnValue("oidcClientConfigServiceRef2"));
                allowing(oidcClientConfigServiceRef2).getProperty("service.id");
                will(returnValue(992L));
                allowing(oidcClientConfigServiceRef2).getProperty("service.ranking");
                will(returnValue(992L));

                allowing(oidcClientConfigServiceRef3).getProperty("id");
                will(returnValue("oidcClientConfigServiceRef3"));
                allowing(oidcClientConfigServiceRef3).getProperty("service.id");
                will(returnValue(993L));
                allowing(oidcClientConfigServiceRef3).getProperty("service.ranking");
                will(returnValue(993L));

            }
        });

        mockAuthFilter();
        mockNeedProviderHint12False();
    }

    public void mockAuthFilter() {
        mock.checking(new Expectations() {
            {
                allowing(filterRef).getReference("authFilterServiceRef1");
                will(returnValue(authFilterServiceRef1));
                allowing(filterRef).getReference("authFilterServiceRef2");
                will(returnValue(authFilterServiceRef2));
                allowing(filterRef).getReference("authFilterServiceRef3");
                will(returnValue(authFilterServiceRef3));

                allowing(cc).locateService("authFilter1");
                will(returnValue(authFilter1));
                allowing(cc).locateService("authFilter2");
                will(returnValue(authFilter2));
                allowing(cc).locateService("authFilter3");
                will(returnValue(authFilter3));

                allowing(authFilterServiceRef1).getProperty("id");
                will(returnValue("authFilterServiceRef1"));
                allowing(authFilterServiceRef1).getProperty("service.id");
                will(returnValue(995L));
                allowing(authFilterServiceRef1).getProperty("service.ranking");
                will(returnValue(995L));

                allowing(authFilterServiceRef2).getProperty("id");
                will(returnValue("authFilterServiceRef2"));
                allowing(authFilterServiceRef2).getProperty("service.id");
                will(returnValue(996L));
                allowing(authFilterServiceRef2).getProperty("service.ranking");
                will(returnValue(996L));

                allowing(authFilterServiceRef3).getProperty("id");
                will(returnValue("authFilterServiceRef3"));
                allowing(authFilterServiceRef3).getProperty("service.id");
                will(returnValue(9997L));
                allowing(authFilterServiceRef3).getProperty("service.ranking");
                will(returnValue(997L));

                allowing(cc).locateService("authFilter", authFilterServiceRef1);
                will(returnValue(authFilter1));
                allowing(cc).locateService("authFilter", authFilterServiceRef2);
                will(returnValue(authFilter2));
                allowing(cc).locateService("authFilter", authFilterServiceRef3);
                will(returnValue(authFilter3));

                allowing(authFilter1).isAccepted(req);
                will(returnValue(false));
                allowing(authFilter2).isAccepted(req);
                will(returnValue(false));
                allowing(authFilter3).isAccepted(req);
                will(returnValue(true));
            }
        });
    }

    public void mockNeedProviderHint12False() {
        ArrayList<OidcClientConfig> oidcClientCfgs = new ArrayList<OidcClientConfig>();
        oidcClientCfgs.add(oidcClientConfig1);
        oidcClientCfgs.add(oidcClientConfig2);
        oidcClientCfgs.add(oidcClientConfig3);
        final Iterator<OidcClientConfig> oidcClientCfgIterator = oidcClientCfgs.iterator();
        mock.checking(new Expectations() {
            {
                allowing(oidcClientConfigRef).getServices(); // we loop through all the oidcClientConfig
                will(returnValue(oidcClientCfgIterator));

                allowing(cc).locateService("oidcClientConfig", oidcClientConfigServiceRef1);
                will(returnValue(oidcClientConfig1));
                allowing(cc).locateService("oidcClientConfig", oidcClientConfigServiceRef2);
                will(returnValue(oidcClientConfig2));
                allowing(cc).locateService("oidcClientConfig", oidcClientConfigServiceRef3);
                will(returnValue(oidcClientConfig3));

                allowing(oidcClientConfig1).getId();
                will(returnValue("oidcClientConfig1"));
                allowing(oidcClientConfig2).getId();
                will(returnValue("oidcClientConfig2"));
                allowing(oidcClientConfig3).getId();
                will(returnValue("oidcClientConfig3"));

                allowing(oidcClientConfig1).getIssuerIdentifier();
                will(returnValue("oidcClientConfigIssuer1"));
                allowing(oidcClientConfig2).getIssuerIdentifier();
                will(returnValue("oidcClientConfigIssuer2"));
                allowing(oidcClientConfig3).getIssuerIdentifier();
                will(returnValue("oidcClientConfigIssuer3"));

                allowing(oidcClientConfig1).getAuthFilterId();
                will(returnValue("authFilterServiceRef1"));
                allowing(oidcClientConfig2).getAuthFilterId();
                will(returnValue("authFilterServiceRef2"));
                allowing(oidcClientConfig3).getAuthFilterId();
                will(returnValue("authFilterServiceRef3"));

                allowing(oidcClientConfig1).isDisableLtpaCookie();
                will(returnValue(true));
                allowing(oidcClientConfig2).isDisableLtpaCookie();
                will(returnValue(true));
                allowing(oidcClientConfig3).isDisableLtpaCookie();
                will(returnValue(true));

                allowing(oidcClientConfig1).isValidConfig();
                will(returnValue(true));
                allowing(oidcClientConfig2).isValidConfig();
                will(returnValue(true));
                allowing(oidcClientConfig3).isValidConfig();
                will(returnValue(true));

                allowing(oidcClientConfig1).isOidcclientRequestParameterSupported();
                will(returnValue(false));
                allowing(oidcClientConfig2).isOidcclientRequestParameterSupported();
                will(returnValue(false));

                allowing(oidcClientConfig3).getContextPath();
                will(returnValue("/bar"));
                allowing(oidcClientConfig2).getContextPath();
                will(returnValue("/bar"));
                allowing(oidcClientConfig1).getContextPath();
                will(returnValue("/bar"));
            }
        });
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    public void mockNeedProviderHint3False() {
        //mockNeedProviderHint12False();
        mock.checking(new Expectations() {
            {
                one(oidcClientConfig3).isOidcclientRequestParameterSupported();
                will(returnValue(false));
                allowing(req).getRequestURL();
                will(returnValue(new StringBuffer("foourl")));
            }
        });
    }

    public void mockNeedProviderHint3True() {
        //mockNeedProviderHint12False();
        mock.checking(new Expectations() {
            {
                one(oidcClientConfig3).isOidcclientRequestParameterSupported();
                will(returnValue(true));
                allowing(oidcClientConfig3).getContextPath();
                will(returnValue("/bar"));
                allowing(oidcClientConfig2).getContextPath();
                will(returnValue("/bar"));
                allowing(oidcClientConfig1).getContextPath();
                will(returnValue("/bar"));
            }
        });
    }

    OidcClientImpl initOidcClient() {
        OidcClientImpl oidcClient = new OidcClientImpl();
        // init test
        ServiceReference<OidcClientConfig> oidcConfigServiceRef1 = oidcClientConfigRef.getReference("oidcClientConfigServiceRef1");
        ServiceReference<OidcClientConfig> oidcConfigServiceRef2 = oidcClientConfigRef.getReference("oidcClientConfigServiceRef2");
        ServiceReference<OidcClientConfig> oidcConfigServiceRef3 = oidcClientConfigRef.getReference("oidcClientConfigServiceRef3");

        oidcClient.setOidcClientConfig(oidcConfigServiceRef1);
        oidcClient.setOidcClientConfig(oidcConfigServiceRef2);
        oidcClient.setOidcClientConfig(oidcConfigServiceRef3);

        ServiceReference<AuthenticationFilter> authFilterRef1 = filterRef.getReference("authFilterServiceRef1");
        ServiceReference<AuthenticationFilter> authFilterRef2 = filterRef.getReference("authFilterServiceRef2");
        ServiceReference<AuthenticationFilter> authFilterRef3 = filterRef.getReference("authFilterServiceRef3");

        oidcClient.setAuthFilter(authFilterRef1);
        oidcClient.setAuthFilter(authFilterRef2);
        oidcClient.setAuthFilter(authFilterRef3);

        oidcClient.activate(cc);
        return oidcClient;
    }

    @Test
    public void testNeedProviderHintTrue() {
        final String methodName = "NeedProviderHintTrue";

        try {
            // init test
            OidcClientImpl oidcClientGetParameter = initOidcClient();

            // test
            mockNeedProviderHint3True();

            // These expectations to call the getParameter() of HttpServletRequest
            @SuppressWarnings("rawtypes")
            final HashMap map = new HashMap();
            mock.checking(new Expectations() {
                {
                    // all these has to be called
                    one(req).getMethod();
                    will(returnValue("POST"));
                    one(req).getAttribute("ServletRequestWrapperHashmap");
                    will(returnValue(map));
                    allowing(req).getRequestURL();
                    will(returnValue(new StringBuffer("foourl")));

                    // this expectation will set the reqProviderHint to oidcClientConfig3
                    one(req).getHeader(ClientConstants.OIDC_AUTHN_HINT_HEADER);
                    will(returnValue(null));
                    one(req).getHeader(ClientConstants.OIDC_CLIENT);
                    will(returnValue(null));
                    one(req).getParameter(ClientConstants.OIDC_CLIENT);
                    will(returnValue("oidcClientConfig3")); // the reqProviderHine is oidcClientConfig3
                }
            });

            // since the reqProviderHine is oidcClientConfig3, the providerId has to be oidcClientConfig3
            String providerID = oidcClientGetParameter.getOidcProvider(req);
            assertTrue("Did not find oidcClientConfig3 but " + providerID, "oidcClientConfig3".equals(providerID));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testNeedProviderHintFalse() {
        final String methodName = "NeedProviderHintFalse";
        try {

            // init test
            OidcClientImpl oidcClientAuthFilter = initOidcClient();

            // test
            mockNeedProviderHint3False();

            // Since NeedProviderHintis false.
            // This will not need HttpServletRequest.getParameter()
            // No expectations for getParameter() of HttpServletRequest.
            // This goes down to authFilter path and all oidcClientConfig(s) are qualified
            String providerID = oidcClientAuthFilter.getOidcProvider(req);
            assertTrue("Did not find oidcClientConfig3 but " + providerID, "oidcClientConfig3".equals(providerID));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
