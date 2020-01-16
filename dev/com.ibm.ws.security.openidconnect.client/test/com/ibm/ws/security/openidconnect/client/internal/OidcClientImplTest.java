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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

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
import com.ibm.ws.security.openidconnect.client.AccessTokenAuthenticator;
import com.ibm.ws.security.openidconnect.client.OidcClientAuthenticator;
import com.ibm.ws.security.openidconnect.clients.common.ClientConstants;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientRequest;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.PostParameterHelper;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.openidconnect.OidcClient;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

import test.common.SharedOutputManager;

public class OidcClientImplTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final String MY_OIDC_CLIENT = "openidConnectClient1";
    private final String MY_OIDC_CLIENT2 = "openidConnectClient2";
    private final String authFilterId = "muAuthFilterId";

    private final ComponentContext cc = mock.mock(ComponentContext.class);

    @SuppressWarnings("unchecked")
    private final AtomicServiceReference<OidcClient> oidcClientRef = mock.mock(AtomicServiceReference.class, "oidcClientRef");

    @SuppressWarnings("unchecked")
    private final ConcurrentServiceReferenceMap<String, OidcClientConfig> oidcClientConfigRef = mock.mock(ConcurrentServiceReferenceMap.class, "oidcClientConfigRef");

    @SuppressWarnings("unchecked")
    private final ServiceReference<OidcClientConfig> oidcClientConfigServiceRef = mock.mock(ServiceReference.class, "oidcClientConfigServiceRef");
    private final ServiceReference<OidcClientConfig> oidcClientConfigServiceRef2 = mock.mock(ServiceReference.class, "oidcClientConfigServiceRef2");
    private final OidcClientConfig oidcClientConfig = mock.mock(OidcClientConfig.class, "oidcClientConfig");
    private final OidcClientConfig oidcClientConfig2 = mock.mock(OidcClientConfig.class, "oidcClientConfig2");
    private final OidcClientConfig oidcClientConfig3 = mock.mock(OidcClientConfig.class, "oidcClientConfig3");
    private final ConcurrentServiceReferenceMap<String, AuthenticationFilter> authFilterServiceRef = mock.mock(ConcurrentServiceReferenceMap.class, "authFilterServiceRef");
    private final AuthenticationFilter filter1 = mock.mock(AuthenticationFilter.class, "filter1");
    private final AuthenticationFilter filter2 = mock.mock(AuthenticationFilter.class, "filter2");
    private final OidcClientAuthenticator oidcClientAuthenticator = mock.mock(OidcClientAuthenticator.class);

    private final AccessTokenAuthenticator accessTokenAuthenticator = mock.mock(AccessTokenAuthenticator.class);

    private final SRTServletRequest req = mock.mock(SRTServletRequest.class, "req");
    private final HttpServletResponse resp = mock.mock(HttpServletResponse.class, "resp");

    private final OidcClientRequest oidcClientRequest = mock.mock(OidcClientRequest.class);

    @Before
    public void setUp() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(oidcClientRef).activate(cc);
                allowing(req).getContextPath();
                allowing(cc).locateService("oidcClientConfig");
                will(returnValue(oidcClientConfig));
                allowing(cc).locateService("oidcClientConfig2");
                will(returnValue(oidcClientConfig2));
                allowing(cc).locateService("oidcClientConfig", oidcClientConfigServiceRef);
                will(returnValue(oidcClientConfig));
                allowing(oidcClientConfig).getAuthFilterId();
                will(returnValue("theFilter"));
                allowing(oidcClientConfig2).getAuthFilterId();
                will(returnValue("theFilter"));
                allowing(oidcClientConfigRef).getReference(MY_OIDC_CLIENT);
                will(returnValue(oidcClientConfigServiceRef));
                allowing(oidcClientConfigServiceRef).getProperty("id");
                will(returnValue(MY_OIDC_CLIENT));
                allowing(oidcClientConfigServiceRef2).getProperty("id");
                will(returnValue(MY_OIDC_CLIENT2));
                allowing(oidcClientConfigServiceRef).getProperty("service.id");
                will(returnValue(99L));
                allowing(oidcClientConfigServiceRef).getProperty("service.ranking");
                will(returnValue(99L));
                allowing(oidcClientConfigServiceRef2).getProperty("service.id");
                will(returnValue(99L));
                allowing(oidcClientConfigServiceRef2).getProperty("service.ranking");
                will(returnValue(99L));

                allowing(oidcClientConfig).getContextPath();
                will(returnValue("/foo"));
            }
        });
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @Test
    public void testWarnIfAuthFilterUseDuplicated() {
        String methodName = "testWarnIfAuthFilterUseDuplicated";
        final OidcClientImpl oidcClient = new OidcClientImpl();
        try {
            ArrayList<OidcClientConfig> configs = new ArrayList<OidcClientConfig>();
            configs.add(oidcClientConfig);
            configs.add(oidcClientConfig2);

            oidcClient.warnIfAuthFilterUseDuplicated(configs.iterator());

            assertTrue("expected message not found", outputMgr.checkForStandardOut("CWWKS1530W"));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testWarnIfAmbiguousAuthFilters() {
        String methodName = "testWarnIfAmbiguousAuthFilters";
        final OidcClientImpl oidcClient = new OidcClientImpl();

        try {
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig3).isValidConfig();
                    will(returnValue(true));
                    allowing(oidcClientConfig).isValidConfig();
                    will(returnValue(true));
                    allowing(oidcClientConfig3).getAuthFilterId();
                    will(returnValue("filter2"));
                    allowing(authFilterServiceRef).getService("theFilter");
                    will(returnValue(filter1));
                    allowing(authFilterServiceRef).getService("filter2");
                    will(returnValue(filter2));
                    allowing(filter1).isAccepted(req);
                    will(returnValue(true));
                    allowing(filter2).isAccepted(req);
                    will(returnValue(true));
                    allowing(req).getRequestURL();
                    will(returnValue(new StringBuffer("foourl")));
                }
            });
            ArrayList<OidcClientConfig> configs = new ArrayList<OidcClientConfig>();
            configs.add(oidcClientConfig);
            configs.add(oidcClientConfig3);

            oidcClient.warnIfAmbiguousAuthFilters(configs.iterator(), req, authFilterServiceRef);

            assertTrue("expected message not found", outputMgr.checkForStandardOut("CWWKS1531W"));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testConstructor() {
        final String methodName = "testConstructor";
        final OidcClientImpl oidcClient = new OidcClientImpl();
        try {
            assertNotNull("There must be an oidc client", oidcClient);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testDeactivate() {
        final String methodName = "testDeactivate";
        final OidcClientImpl oidcClient = new OidcClientImpl();
        try {
            ServiceReference<OidcClientConfig> oidcConfigServiceRef = oidcClientConfigRef.getReference(MY_OIDC_CLIENT);
            oidcClient.activate(cc);
            oidcClient.setOidcClientConfig(oidcConfigServiceRef);
            oidcClient.setOidcClientAuthenticator(oidcClientAuthenticator);
            oidcClient.deactivate(cc);

            assertNull("oidcClientAuthenticator should be null", oidcClient.getOidcClientAuthenticator());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testAuthenticate() {
        final String methodName = "testAuthenticate";
        final OidcClientImpl oidcClient = new OidcClientImpl();
        ArrayList<OidcClientConfig> oidcClientCfgs = new ArrayList<OidcClientConfig>();
        oidcClientCfgs.add(oidcClientConfig);

        final ProviderAuthenticationResult authResult = new ProviderAuthenticationResult(AuthResult.SUCCESS, 200);
        try {
            mockOidcClientConfig();
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientRequest).getOidcClientConfig();
                    will(returnValue(oidcClientConfig));
                    allowing(oidcClientConfig).getRedirectUrlFromServerToClient();
                    allowing(req).getHeader(with(any(String.class)));
                    will(returnValue(null));
                    allowing(req).getParameter("oidc_client");
                    will(returnValue(null));

                    allowing(oidcClientAuthenticator).authenticate(req, resp, oidcClientConfig); //, referrerURLCookieHandler);
                    will(returnValue(authResult));
                    allowing(oidcClientConfig).getClientId();

                    allowing(oidcClientConfig).getInboundPropagation();
                    will(returnValue(ClientConstants.PROPAGATION_NONE));
                    allowing(oidcClientConfig).getValidationEndpointUrl();

                    allowing(accessTokenAuthenticator).authenticate(req, resp, oidcClientConfig, oidcClientRequest);
                    will(returnValue(authResult));

                }
            });
            ServiceReference<OidcClientConfig> oidcConfigServiceRef = oidcClientConfigRef.getReference(MY_OIDC_CLIENT);
            oidcClient.activate(cc);
            oidcClient.setOidcClientConfig(oidcConfigServiceRef);
            oidcClient.setOidcClientAuthenticator(oidcClientAuthenticator);

            oidcClient.setAccessTokenAuthenticator(accessTokenAuthenticator);
            oidcClient.initOidcClientAuth = false;
            ProviderAuthenticationResult oidcAuthResult = oidcClient.authenticate(req, resp, MY_OIDC_CLIENT, oidcClientRequest);
            assertEquals("Authentication result should be ?", AuthResult.SUCCESS, oidcAuthResult.getStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetOidcProvider_oidc_client_null() {
        final String methodName = "testGetOidcProvider_oidc_client_null";
        final OidcClientImpl oidcClient = new OidcClientImpl();
        ArrayList<OidcClientConfig> oidcClientCfgs = new ArrayList<OidcClientConfig>();
        oidcClientCfgs.add(oidcClientConfig);
        try {
            mockOidcClientConfig();
            mock.checking(new Expectations() {
                {
                    allowing(req).getHeader(with(any(String.class)));
                    will(returnValue(null));
                    allowing(req).getParameter("oidc_client");
                    will(returnValue(null));
                    allowing(oidcClientConfig).isValidConfig();
                    will(returnValue(true));
                    allowing(req).getMethod();
                    will(returnValue("GET"));
                    allowing(req).getRequestURL();
                    will(returnValue(new StringBuffer("foourl")));
                }
            });
            ServiceReference<OidcClientConfig> oidcConfigServiceRef = oidcClientConfigRef.getReference(MY_OIDC_CLIENT);
            oidcClient.activate(cc);
            oidcClient.setOidcClientConfig(oidcConfigServiceRef);
            oidcClient.initBeforeSso = false; // disable
            assertEquals("Provider should be myOidcClient", MY_OIDC_CLIENT, oidcClient.getOidcProvider(req));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetOidcProvider_not_found() {
        final String methodName = "testGetOidcProvider_not_found";
        final OidcClientImpl oidcClient = new OidcClientImpl();
        ArrayList<OidcClientConfig> oidcClientCfgs = new ArrayList<OidcClientConfig>();
        oidcClientCfgs.add(oidcClientConfig);
        try {
            mockOidcClientConfig();
            mock.checking(new Expectations() {
                {
                    allowing(req).getHeader(with(any(String.class)));
                    will(returnValue(null));
                    allowing(req).getParameter("oidc_client");
                    will(returnValue("myOidcClient2"));
                    allowing(oidcClientConfig).getIssuerIdentifier();
                    will(returnValue(null));
                    allowing(oidcClientConfig).isValidConfig();
                    will(returnValue(true));
                    allowing(req).getMethod();
                    will(returnValue("GET"));
                }
            });
            ServiceReference<OidcClientConfig> oidcConfigServiceRef = oidcClientConfigRef.getReference(MY_OIDC_CLIENT);
            oidcClient.activate(cc);
            oidcClient.setOidcClientConfig(oidcConfigServiceRef);
            oidcClient.initBeforeSso = false; // disable

            assertNull("Provider should be null", oidcClient.getOidcProvider(req));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetOidcProvider() {
        final String methodName = "testGetOidcProvider";
        final OidcClientImpl oidcClient = new OidcClientImpl();
        ArrayList<OidcClientConfig> oidcClientCfgs = new ArrayList<OidcClientConfig>();
        oidcClientCfgs.add(oidcClientConfig);
        final HashMap map = new HashMap();
        try {
            mockOidcClientConfig();
            mock.checking(new Expectations() {
                {
                    allowing(req).getHeader(with(any(String.class)));
                    will(returnValue(null));
                    allowing(oidcClientConfig).getAuthFilterId();
                    will(returnValue(authFilterId));
                    allowing(req).getParameter("oidc_client");
                    will(returnValue(MY_OIDC_CLIENT));
                    allowing(oidcClientConfig).isValidConfig();
                    will(returnValue(true));
                    allowing(req).getMethod();
                    will(returnValue("POST"));
                    allowing(req).getAttribute(PostParameterHelper.ATTRIB_HASH_MAP);
                    will(returnValue(map));
                    allowing(req).getRequestURL();
                    will(returnValue(new StringBuffer("foourl")));

                }
            });
            ServiceReference<OidcClientConfig> oidcConfigServiceRef = oidcClientConfigRef.getReference(MY_OIDC_CLIENT);
            oidcClient.activate(cc);
            oidcClient.setOidcClientConfig(oidcConfigServiceRef);
            oidcClient.initBeforeSso = false; // disable
            assertEquals("Provider should be myOidcClient", MY_OIDC_CLIENT, oidcClient.getOidcProvider(req));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetProviderConfig() {
        final String methodName = "testGetProviderConfig";
        final OidcClientImpl oidcClient = new OidcClientImpl();
        ArrayList<OidcClientConfig> oidcClientCfgs = new ArrayList<OidcClientConfig>();
        oidcClientCfgs.add(oidcClientConfig);
        final Iterator<OidcClientConfig> oidcClientCfgIterator = oidcClientCfgs.iterator();
        try {
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfigRef).getServices();
                    will(returnValue(oidcClientCfgIterator));
                    allowing(oidcClientConfig).getAuthFilterId();
                    will(returnValue(null));
                    allowing(oidcClientConfig).getId();
                    will(returnValue(MY_OIDC_CLIENT));
                    allowing(oidcClientConfig).isValidConfig();
                    will(returnValue(true));
                }
            });
            ServiceReference<OidcClientConfig> oidcConfigServiceRef = oidcClientConfigRef.getReference(MY_OIDC_CLIENT);
            oidcClient.activate(cc);
            oidcClient.setOidcClientConfig(oidcConfigServiceRef);
            Iterator<OidcClientConfig> oidcClientConfigs = oidcClientConfigRef.getServices();

            assertEquals("Provider should be myOidcClient", MY_OIDC_CLIENT, oidcClient.getProviderConfig(oidcClientConfigs, MY_OIDC_CLIENT, req));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testIsMapIdentityToRegistryUser() {
        final String methodName = "testIsMapIdentityToRegistryUser";
        final OidcClientImpl oidcClient = new OidcClientImpl();
        ArrayList<OidcClientConfig> oidcClientCfgs = new ArrayList<OidcClientConfig>();
        oidcClientCfgs.add(oidcClientConfig);
        try {
            mockOidcClientConfig();
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).isMapIdentityToRegistryUser();
                    will(returnValue(true));
                }
            });
            ServiceReference<OidcClientConfig> oidcConfigServiceRef = oidcClientConfigRef.getReference(MY_OIDC_CLIENT);
            oidcClient.activate(cc);
            oidcClient.setOidcClientConfig(oidcConfigServiceRef);
            assertTrue("MapIdentityToRegistryUser should be true", oidcClient.isMapIdentityToRegistryUser(MY_OIDC_CLIENT));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    public void mockOidcClientConfig() {
        ArrayList<OidcClientConfig> oidcClientCfgs = new ArrayList<OidcClientConfig>();
        oidcClientCfgs.add(oidcClientConfig);
        final Iterator<OidcClientConfig> oidcClientCfgIterator = oidcClientCfgs.iterator();
        mock.checking(new Expectations() {
            {
                allowing(oidcClientConfigRef).getServices();
                will(returnValue(oidcClientCfgIterator));
                allowing(oidcClientConfig).getId();
                will(returnValue(MY_OIDC_CLIENT));
                allowing(oidcClientConfig).getAuthFilterId();
                will(returnValue(authFilterId));
                allowing(cc).locateService("oidcClientConfig", oidcClientConfigServiceRef);
                will(returnValue(oidcClientConfig));

                allowing(oidcClientConfig).isMapIdentityToRegistryUser();
                will(returnValue(true));
            }
        });
    }
}
