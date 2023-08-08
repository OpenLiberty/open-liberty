/*
 * Copyright (c) 2015,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.transport.iiop.server.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.yoko.osgi.locator.BundleProviderLoader;
import org.apache.yoko.osgi.locator.Register;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CSIIOP.TransportAddress;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.ws.ssl.optional.SSLSupportOptional;
import com.ibm.ws.transport.iiop.security.SecurityInitializer;
import com.ibm.ws.transport.iiop.security.ServerPolicy;
import com.ibm.ws.transport.iiop.security.ServerPolicyFactory;
import com.ibm.ws.transport.iiop.security.config.ssl.yoko.SocketFactory;

@Ignore
public class CSIv2SubsystemFactoryTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final String REALM = "testRealm";

    private CSIv2SubsystemFactory csiv2SubsystemFactory;

    private BundleContext bundleContext;
    private Bundle bundle;

    private Register providerRegistry;
    private SSLSupportOptional sslSupport;
    private JSSEHelper jsseHelper;
    private SecurityService securityService;
    private AuthenticationService authenticationService;
    private UserRegistryService userRegistryService;
    private UserRegistry userRegistry;
    private TokenManager tokenManager;
    UnauthenticatedSubjectService unauthenticatedSubjectService;
    private ORB orb;
    private Any any;
    private Policy serverSecurityPolicy;

    @Before
    public void setUp() throws Exception {
        orb = mockery.mock(ORB.class);
        any = mockery.mock(Any.class);
        userRegistryService = mockery.mock(UserRegistryService.class);
        userRegistry = mockery.mock(UserRegistry.class);
        serverSecurityPolicy = mockery.mock(Policy.class);
        activateCSIv2SubsystemFactory();
    }

    private void activateCSIv2SubsystemFactory() {
        bundleContext = mockery.mock(BundleContext.class);
        bundle = mockery.mock(Bundle.class);
        providerRegistry = mockery.mock(Register.class);
        sslSupport = mockery.mock(SSLSupportOptional.class);
        securityService = mockery.mock(SecurityService.class);
        tokenManager = mockery.mock(TokenManager.class);
        unauthenticatedSubjectService = mockery.mock(UnauthenticatedSubjectService.class);
        jsseHelper = mockery.mock(JSSEHelper.class);

        csiv2SubsystemFactory = new CSIv2SubsystemFactory() {
            CSIv2SubsystemFactory setup() {
                setRegister(providerRegistry);
                setSSLSupport(sslSupport, new HashMap<String, Object>());
                setSecurityService(securityService, new HashMap<String, Object>());
                setTokenManager(tokenManager);
                setUnuathenticatedSubjectService(unauthenticatedSubjectService);

                createActivationExpectations();
                activate(bundleContext);
                return this;
            }

            @Override
            protected void deactivate() {
                super.deactivate();
            }

        }.setup();

    }

    private void createActivationExpectations() {
        final BundleProviderLoader securityInitializerClass = new BundleProviderLoader(SecurityInitializer.class.getName(), SecurityInitializer.class.getName(), bundle, 1);
        final BundleProviderLoader connectionHelperClass = new BundleProviderLoader(SocketFactory.class.getName(), SocketFactory.class.getName(), bundle, 1);
        mockery.checking(new Expectations() {
            {
                allowing(bundleContext).getBundle();
                will(returnValue(bundle));
                one(providerRegistry).registerProvider(with(securityInitializerClass));
                one(providerRegistry).registerProvider(with(connectionHelperClass));
                allowing(bundle).getBundleId();
                will(returnValue(123L));
                one(sslSupport).getJSSEHelper();
                will(returnValue(jsseHelper));
            }
        });
    }

    private void createDeactivationExpectations() {
        final BundleProviderLoader securityInitializerClass = new BundleProviderLoader(SecurityInitializer.class.getName(), SecurityInitializer.class.getName(), bundle, 1);
        final BundleProviderLoader connectionHelperClass = new BundleProviderLoader(SocketFactory.class.getName(), SocketFactory.class.getName(), bundle, 1);
        mockery.checking(new Expectations() {
            {
                one(providerRegistry).unregisterProvider(with(securityInitializerClass));
                one(providerRegistry).unregisterProvider(with(connectionHelperClass));
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        createDeactivationExpectations();
        Method m = csiv2SubsystemFactory.getClass().getDeclaredMethod("deactivate");
        m.invoke(csiv2SubsystemFactory);
        mockery.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.server.security.CSIv2SubsystemFactory#getTargetPolicy(org.omg.CORBA.ORB, java.util.Map, java.util.Map)}.
     */
    @Test
    public void testGetTargetPolicy() throws Exception {
        createSecurityServiceExpectations();
        createRegistryExpectations();
        createORBExpectations();

        Map<String, Object> properties = new HashMap<String, Object>();
        Map<String, Object> extraConfig = new HashMap<String, Object>();
        Map<String, List<TransportAddress>> addrMap = new HashMap<String, List<TransportAddress>>();
        extraConfig.put(CSIv2SubsystemFactory.class.getName(), addrMap);

        Policy policy = csiv2SubsystemFactory.getTargetPolicy(orb, properties, extraConfig);

        assertNotNull("There must be a target security policy.", policy);
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.server.security.CSIv2SubsystemFactory#getTargetPolicy(org.omg.CORBA.ORB, java.util.Map, java.util.Map)}.
     */
    @Test(expected = java.lang.IllegalStateException.class)
    public void testGetTargetPolicyNoAddressInExtraConfig() throws Exception {
        Map<String, Object> properties = new HashMap<String, Object>();;
        Map<String, Object> extraConfig = new HashMap<String, Object>();;
        csiv2SubsystemFactory.getTargetPolicy(orb, properties, extraConfig);
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.server.security.CSIv2SubsystemFactory#getTargetPolicy(org.omg.CORBA.ORB, java.util.Map, java.util.Map)}.
     */
    @Test
    public void testGetTargetPolicyNoExtraConfigs() throws Exception {
        Map<String, Object> properties = new HashMap<String, Object>();
        Map<String, Object> extraConfig = null;
        Policy policy = csiv2SubsystemFactory.getTargetPolicy(orb, properties, extraConfig);

        assertNull("There must not be a target security policy.", policy);
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.server.security.CSIv2SubsystemFactory#getClientPolicy(org.omg.CORBA.ORB, java.util.Map)}.
     */
    @Test
    public void testGetClientPolicy() throws Exception {
        createSecurityServiceExpectations();
        createRegistryExpectations();

        Map<String, Object> properties = new HashMap<String, Object>();
        Policy policy = csiv2SubsystemFactory.getClientPolicy(orb, properties);

        assertNotNull("There must be a client security policy.", policy);
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.server.security.CSIv2SubsystemFactory#getInitializerClassName(boolean)}.
     */
    @Test
    public void testGetInitializerClassName() {
        assertTrue("There must be an initializer class name.", csiv2SubsystemFactory.getInitializerClassName(false).isEmpty() == false);
    }

//    /**
//     * Test method for {@link com.ibm.ws.transport.iiop.server.security.CSIv2SubsystemFactory#addTargetORBInitProperties(java.util.Properties, java.util.Map, java.util.Map)}.
//     */
//    @Test
//    public void testAddTargetORBInitProperties() {
//        createPropsForAddTargetORBInitProperties();
//
//        csiv2SubsystemFactory.addTargetORBInitProperties(initProperties, configProps, extraProperties);
//
//        String endpoint = (String) initProperties.get(ENDPOINT_KEY);
//        assertTrue("There must be an endpoint in the initProperties.", endpoint != null && endpoint.isEmpty() == false);
//        assertTrue("The endpoint must have the host and port.", endpoint.contains("iiop --bind " + host + " --host " + host + " --port " + iiopPortString));
//    }

//    /**
//     * Test method for {@link com.ibm.ws.transport.iiop.server.security.CSIv2SubsystemFactory#addTargetORBInitProperties(java.util.Properties, java.util.Map, java.util.Map)}.
//     */
//    @SuppressWarnings("unchecked")
//    @Test
//    public void testAddTargetORBInitPropertiesWithSSL() {
//        createPropsForAddTargetORBInitProperties();
//        configProps.put("iiopsPort", iiopsPort);
//        extraProperties.put("sslConfigName", sslConfigName);
//
//        csiv2SubsystemFactory.addTargetORBInitProperties(initProperties, configProps, extraProperties);
//
//        String endpoint = (String) initProperties.get(ENDPOINT_KEY);
//        assertTrue("The endpoint must have the sslConfigName.", endpoint.contains("--sslConfigName " + sslConfigName));
//        Map<String, List<TransportAddress>> addrMap = (Map<String, List<TransportAddress>>) extraProperties.get(CSIv2SubsystemFactory.class.getName());
//        List<TransportAddress> transportAddresses = addrMap.get(sslConfigName);
//        TransportAddress transportAddress = transportAddresses.get(0);
//        assertEquals("The transport address must have a host.", host, transportAddress.host_name);
//        assertEquals("The transport address must have a port.", iiopsPortAsShort, transportAddress.port);
//    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.server.security.CSIv2SubsystemFactory#addTargetORBInitArgs(java.util.Map, java.util.List)}.
     */
    @Test
    public void testAddTargetORBInitArgs() {
        Map<String, Object> targetProperties = new HashMap<String, Object>();
        List<String> args = new ArrayList<String>();
        csiv2SubsystemFactory.addTargetORBInitArgs(targetProperties, args);

        assertEquals("The args must contain an -IIOPconnectionHelper entry.", "-IIOPconnectionHelper", args.get(0));
        assertEquals("The IIOPconnectionHelper class name must be set.", SocketFactory.class.getName(), args.get(1));

    }

    private void createSecurityServiceExpectations() {
        mockery.checking(new Expectations() {
            {
                one(securityService).getAuthenticationService();
                will(returnValue(authenticationService));
                allowing(securityService).getUserRegistryService();
                will(returnValue(userRegistryService));
            }
        });
    }

    private void createRegistryExpectations() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(userRegistryService).isUserRegistryConfigured();
                will(returnValue(true));
                one(userRegistryService).getUserRegistry();
                will(returnValue(userRegistry));
                one(userRegistry).getRealm();
                will(returnValue(REALM));
            }
        });
    }

    private void createORBExpectations() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(orb).create_any();
                will(returnValue(any));
                one(any).insert_Value(with(any(ServerPolicy.Config.class)));
                one(orb).create_policy(ServerPolicyFactory.POLICY_TYPE, any);
                will(returnValue(serverSecurityPolicy));
            }
        });
    }

}
