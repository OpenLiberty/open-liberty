/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.ws.security.registry.UserRegistryService;

import test.common.SharedOutputManager;

/**
 * Test the SecurityService in the context of configuration
 * data inside the server.xml.
 */
@SuppressWarnings("unchecked")
public class SecurityServiceImplBasedOnServerXMLTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final Hashtable<String, Object> dict = new Hashtable<String, Object>();
    private final Map<String, Object> props = new HashMap<String, Object>();

    private SecurityConfiguration sysDomainSecConfig;
    private final ServiceReference<SecurityConfiguration> sysDomainRef = mock.mock(ServiceReference.class, "sysDomainRef");
    private final ServiceReference<AuthenticationService> sysAuthenticationServiceRef = mock.mock(ServiceReference.class, "sysAuthenticationServiceRef");
    private final ServiceReference<AuthorizationService> sysAuthorizationServiceRef = mock.mock(ServiceReference.class, "sysAuthorizationServiceRef");
    private final ServiceReference<UserRegistryService> sysUserRegistryServiceRef = mock.mock(ServiceReference.class, "sysUserRegistryServiceRef");

    private SecurityConfiguration appDomainSecConfig;
    private final ServiceReference<SecurityConfiguration> appDomainRef = mock.mock(ServiceReference.class, "appDomainRef");
    private final ServiceReference<AuthenticationService> appAuthenticationServiceRef = mock.mock(ServiceReference.class, "appAuthenticationServiceRef");
    private final ServiceReference<AuthorizationService> appAuthorizationServiceRef = mock.mock(ServiceReference.class, "appAuthorizationServiceRef");
    private final ServiceReference<UserRegistryService> appUserRegistryServiceRef = mock.mock(ServiceReference.class, "appUserRegistryServiceRef");

    private SecurityServiceImpl secServ;
    private Map<String, Object> serviceProps;

    @Before
    public void setUp() {
        dict.put(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "sysDomain");
        dict.put(SecurityServiceImpl.CFG_KEY_DEFAULT_APP_DOMAIN, "appDomain");
        dict.put(SecurityServiceImpl.KEY_CONFIG_SOURCE, "file");
        props.putAll(dict);

        // Must be defined before the expectations
        sysDomainSecConfig = new SecurityConfiguration();
        Map<String, Object> sysProperties = new HashMap<String, Object>();
        sysProperties.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "sysAuthenticationService");
        sysProperties.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "sysAuthorizationService");
        sysProperties.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "sysUserRegistryService");
        sysDomainSecConfig.activate(sysProperties);

        appDomainSecConfig = new SecurityConfiguration();
        Map<String, Object> appProperties = new HashMap<String, Object>();
        appProperties.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "appAuthenticationService");
        appProperties.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "appAuthorizationService");
        appProperties.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "appUserRegistryService");
        appDomainSecConfig.activate(appProperties);

        final BundleContext mockBundleContext = mock.mock(BundleContext.class);
        mock.checking(new Expectations() {
            {
                allowing(appAuthenticationServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(appAuthenticationServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(appAuthorizationServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(appAuthorizationServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(appDomainRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(appDomainRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(appUserRegistryServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(appUserRegistryServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(sysAuthenticationServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(sysAuthenticationServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(sysAuthorizationServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(sysAuthorizationServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(sysDomainRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(sysDomainRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(sysUserRegistryServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(sysUserRegistryServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(sysUserRegistryServiceRef).getProperty("realm");
                will(returnValue("sysUserRegistryServiceRealm"));

                allowing(cc).getProperties();
                will(returnValue(dict));

                allowing(sysDomainRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue("sysDomain"));
                allowing(sysDomainRef).getProperty(SecurityServiceImpl.KEY_SERVICE_ID);
                will(returnValue(101));
                allowing(sysDomainRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(sysDomainRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(cc).locateService(SecurityServiceImpl.KEY_CONFIGURATION, sysDomainRef);
                will(returnValue(sysDomainSecConfig));

                allowing(appDomainRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue("appDomain"));
                allowing(appDomainRef).getProperty(SecurityServiceImpl.KEY_SERVICE_ID);
                will(returnValue(102));
                allowing(cc).locateService(SecurityServiceImpl.KEY_CONFIGURATION, appDomainRef);
                will(returnValue(appDomainSecConfig));

                allowing(sysAuthenticationServiceRef).getProperty(SecurityServiceImpl.KEY_CONFIG_SOURCE);
                will(returnValue("file"));
                allowing(sysAuthenticationServiceRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue("sysAuthenticationService"));
                allowing(sysAuthenticationServiceRef).getProperty(SecurityServiceImpl.KEY_SERVICE_ID);
                will(returnValue(110));

                allowing(sysAuthorizationServiceRef).getProperty(SecurityServiceImpl.KEY_CONFIG_SOURCE);
                will(returnValue("file"));
                allowing(sysAuthorizationServiceRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue("sysAuthorizationService"));
                allowing(sysAuthorizationServiceRef).getProperty(SecurityServiceImpl.KEY_SERVICE_ID);
                will(returnValue(111));

                allowing(sysUserRegistryServiceRef).getProperty(SecurityServiceImpl.KEY_CONFIG_SOURCE);
                will(returnValue("file"));
                allowing(sysUserRegistryServiceRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue("sysUserRegistryService"));
                allowing(sysUserRegistryServiceRef).getProperty(SecurityServiceImpl.KEY_SERVICE_ID);
                will(returnValue(112));
                allowing(sysUserRegistryServiceRef).getProperty("config.displayId");
                will(returnValue("userRegistry[sysUserRegistryService]"));

                allowing(appAuthenticationServiceRef).getProperty(SecurityServiceImpl.KEY_CONFIG_SOURCE);
                will(returnValue("file"));
                allowing(appAuthenticationServiceRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue("appAuthenticationService"));
                allowing(appAuthenticationServiceRef).getProperty(SecurityServiceImpl.KEY_SERVICE_ID);
                will(returnValue(120));

                allowing(appAuthorizationServiceRef).getProperty(SecurityServiceImpl.KEY_CONFIG_SOURCE);
                will(returnValue("file"));
                allowing(appAuthorizationServiceRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue("appAuthorizationService"));
                allowing(appAuthorizationServiceRef).getProperty(SecurityServiceImpl.KEY_SERVICE_ID);
                will(returnValue(121));

                allowing(appUserRegistryServiceRef).getProperty(SecurityServiceImpl.KEY_CONFIG_SOURCE);
                will(returnValue("file"));
                allowing(appUserRegistryServiceRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue("appUserRegistryService"));
                allowing(appUserRegistryServiceRef).getProperty(SecurityServiceImpl.KEY_SERVICE_ID);
                will(returnValue(122));
                allowing(appUserRegistryServiceRef).getProperty("realm");
                will(returnValue("appUserRegistryServiceRealm"));
                allowing(appUserRegistryServiceRef).getProperty("config.displayId");
                will(returnValue("userRegistry[appUserRegistryServiceRealm]"));

                allowing(cc).getBundleContext();
                will(returnValue(mockBundleContext));
                ignoring(mockBundleContext);
            }
        });

        secServ = new SecurityServiceImpl();
        secServ.setConfiguration(sysDomainRef);
        secServ.setConfiguration(appDomainRef);
        secServ.setAuthentication(sysAuthenticationServiceRef);
        secServ.setAuthorization(sysAuthorizationServiceRef);
        secServ.setUserRegistry(sysUserRegistryServiceRef);
        secServ.setAuthentication(appAuthenticationServiceRef);
        secServ.setAuthorization(appAuthorizationServiceRef);
        secServ.setUserRegistry(appUserRegistryServiceRef);
        serviceProps = secServ.activate(cc, props);
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();

        secServ.deactivate(cc, props);
        secServ.unsetConfiguration(sysDomainRef);
        secServ.unsetConfiguration(appDomainRef);
        secServ.unsetAuthentication(sysAuthenticationServiceRef);
        secServ.unsetAuthorization(sysAuthorizationServiceRef);
        secServ.unsetUserRegistry(sysUserRegistryServiceRef);
        secServ.unsetAuthentication(appAuthenticationServiceRef);
        secServ.unsetAuthorization(appAuthorizationServiceRef);
        secServ.unsetUserRegistry(appUserRegistryServiceRef);
        secServ = null;
    }

    /**
     * @param service
     * @param e
     */
    private void handleExpectedExceptionAndMessage(String service, IllegalArgumentException e) {
        assertEquals("The exception should be a fully variably subsituted message of the right type",
                     "CWWKS0002E: A configuration exception has occurred. No " + service
                                                                                                      + " attribute is defined for the <security> element.",
                     e.getMessage());

        assertTrue("Expected error message was not logged",
                   outputMgr.checkForStandardErr("CWWKS0002E: A configuration exception has occurred. No " + service
                                                 + " attribute is defined for the <security> element."));
    }

    /**
     * Drive activation of the SecurityServiceImpl and validate that it
     * results in an IllegalArgumentException, because {@code service} is {@code reason} from the {@code cc} properties.
     *
     * @param cc
     * @param service
     * @param reason
     */
    private void activateShouldFailWith(ComponentContext cc, String service, String reason) {
        SecurityServiceImpl secServ = new SecurityServiceImpl();
        try {
            secServ.activate(cc, props);
            fail("Should have thrown an IllegalArgumentException since the " + service + " is " + reason);
        } catch (IllegalArgumentException e) {
            handleExpectedExceptionAndMessage(service, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#activate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void activateWithMissingSystemDomain() {
        dict.remove(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN);
        props.remove(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN);

        activateShouldFailWith(cc, SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "missing");
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#activate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void activateWithEmptySystemDomain() {
        dict.put(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "");
        props.put(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "");

        activateShouldFailWith(cc, SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "empty");
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#activate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void activateWithMissingDefaultAppDomain() {
        dict.put(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "sysDomain");

        SecurityServiceImpl secServ = new SecurityServiceImpl();
        secServ.activate(cc, props);
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#activate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void activateWithEmptyDefaultAppDomain() {
        dict.put(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "sysDomain");
        dict.put(SecurityServiceImpl.CFG_KEY_DEFAULT_APP_DOMAIN, "");

        SecurityServiceImpl secServ = new SecurityServiceImpl();
        secServ.activate(cc, props);
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#activate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void activateWithAllConfigs() {
        dict.put(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "sysDomain");
        dict.put(SecurityServiceImpl.CFG_KEY_DEFAULT_APP_DOMAIN, "appDomain");

        SecurityServiceImpl secServ = new SecurityServiceImpl();
        secServ.activate(cc, props);
    }

    @Test
    public void checkServiceProperties() {

        for (Map.Entry<String, Object> prop : props.entrySet()) {
            assertEquals(prop.getValue(), serviceProps.get(prop.getKey()));
        }
        assertEquals(4 + props.size(), serviceProps.size());
        assertEquals("appAuthenticationService", ((String[]) serviceProps.get(SecurityServiceImpl.KEY_AUTHENTICATION))[0]);
        assertEquals("sysAuthorizationService", ((String[]) serviceProps.get(SecurityServiceImpl.KEY_AUTHORIZATION))[0]);
        // Can not match with an expected value at a given index, because array order is not guaranteed when size > 1
        List<String> configDomains = Arrays.asList((String[]) serviceProps.get(SecurityServiceImpl.KEY_CONFIGURATION));
        assertEquals("The ServiceProperties for SecurityServiceImpl.KEY_CONFIGURATION should be of size 2",
                     2, configDomains.size());
        assertTrue("'appDomain' was not found as a configured configuration domain",
                   configDomains.contains("appDomain"));
        assertTrue("'sysDomain' was not found as a configured configuration domain",
                   configDomains.contains("sysDomain"));
        List<String> configUserRegistryServiceRealm = Arrays.asList((String[]) serviceProps.get(SecurityServiceImpl.KEY_USERREGISTRY));
        assertEquals("The ServiceProperties for SecurityServiceImpl.KEY_USERREGISTRY should be of length 2",
                     2, configUserRegistryServiceRealm.size());
        assertTrue("'appUserRegistryServiceRealm' was not found as a configured configuration domain",
                   configUserRegistryServiceRealm.contains("appUserRegistryServiceRealm"));
        assertTrue("'sysUserRegistryServiceRealm' was not found as a configured configuration domain",
                   configUserRegistryServiceRealm.contains("sysUserRegistryServiceRealm"));
    }

    /**
     * Drive modification of the SecurityServiceImpl and validate that it
     * results in an IllegalArgumentException, because {@code service} is {@code reason} from the {@code properties}.
     *
     * @param properties
     * @param service
     * @param reason
     */
    private void modifyShouldFailWith(final Map<String, Object> properties, String service, String reason) {
        try {
            secServ.modify(properties);
            fail("Should have thrown an IllegalArgumentException since the " + service + " is " + reason);
        } catch (IllegalArgumentException e) {
            handleExpectedExceptionAndMessage(service, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#modify(java.util.Map)}.
     */
    @Test
    public void modifyWithMissingSystemDomain() {
        Map<String, Object> props = new HashMap<String, Object>();
        modifyShouldFailWith(props, SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "missing");
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#modify(java.util.Map)}.
     */
    @Test
    public void modifyWithEmptySystemDomain() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "");
        modifyShouldFailWith(props, SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "empty");
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#modify(java.util.Map)}.
     */
    @Test
    public void modifyWithMissingDefaultAppDomain() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "sysDomain");
        secServ.modify(props);
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#modify(java.util.Map)}.
     */
    @Test
    public void modifyWithEmptyDefaultAppDomain() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "sysDomain");
        props.put(SecurityServiceImpl.CFG_KEY_DEFAULT_APP_DOMAIN, "");
        secServ.modify(props);
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#modify(java.util.Map)}.
     */
    @Test
    public void modifyWithAllConfigs() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "sysDomain");
        props.put(SecurityServiceImpl.CFG_KEY_DEFAULT_APP_DOMAIN, "appDomain");
        secServ.modify(props);
    }

    /**
     * @param service
     * @param e
     */
    private void handleExpectedUndefinedDomainExceptionAndMessage(String domainName, String attribute, IllegalArgumentException e) {
        assertEquals("The exception should be a fully variably subsituted message of the right type",
                     "CWWKS0003E: A configuration exception has occurred. The specified security configuration, referenced by identifier " + domainName + " for attribute "
                                                                                                      + attribute
                                                                                                      + " in the <security> element, is not defined.",
                     e.getMessage());

        assertTrue("Expected error message was not logged",
                   outputMgr.checkForStandardErr("CWWKS0003E: A configuration exception has occurred. The specified security configuration, referenced by identifier "
                                                 + domainName + " for attribute " + attribute + " in the <security> element, is not defined."));
    }

    /**
     * @param service
     * @param e
     */
    private void handleExpectedUndefinedServiceExceptionAndMessage(String identifier, String attribute, IllegalArgumentException e) {
        assertEquals("The exception should be a fully variably subsituted message of the right type",
                     "CWWKS0004E: A configuration exception has occurred. The specified element referenced by identifier " + identifier + " for attribute " + attribute
                                                                                                      + " in the <securityConfiguration> element is not defined.",
                     e.getMessage());
        assertTrue("Expected error message was not logged",
                   outputMgr.checkForStandardErr("CWWKS0004E: A configuration exception has occurred. The specified element referenced by identifier " + identifier
                                                 + " for attribute " + attribute
                                                 + " in the <securityConfiguration> element is not defined."));
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getAuthenticationService()}.
     */
    @Test
    public void getAuthenticationServiceUndefinedSecurityConfiguration() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "undefinedDomain");
        secServ.modify(props);

        try {
            secServ.getAuthenticationService();
            fail("Expected an IllegalArgumentException since the " + SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN + " reference is not valid");
        } catch (IllegalArgumentException e) {
            handleExpectedUndefinedDomainExceptionAndMessage("undefinedDomain", SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getAuthenticationService()}.
     */
    @Test
    public void getAuthenticationServiceForUndefinedService() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "undefinedService");
        props.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "authorizationService");
        props.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "userRegistryService");
        sysDomainSecConfig.modify(props);

        try {
            secServ.getAuthenticationService();
            fail("Expected an IllegalArgumentException since the " + SecurityServiceImpl.KEY_AUTHENTICATION + " reference is not valid");
        } catch (IllegalArgumentException e) {
            handleExpectedUndefinedServiceExceptionAndMessage("undefinedService", SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getAuthenticationService()}.
     */
    @Test
    public void getAuthenticationService() {
        final AuthenticationService sysDomainAuthnServ = mock.mock(AuthenticationService.class, "sysDomainAuthnServ");
        final AuthenticationService appDomainAuthnServ = mock.mock(AuthenticationService.class, "appDomainAuthnServ");
        final AuthorizationService sysDomainAuthzServ = mock.mock(AuthorizationService.class, "sysDomainAuthzServ");
        final AuthorizationService appDomainAuthzServ = mock.mock(AuthorizationService.class, "appDomainAuthzServ");

        mock.checking(new Expectations() {
            {
                one(cc).locateService(SecurityServiceImpl.KEY_AUTHENTICATION, sysAuthenticationServiceRef);
                will(returnValue(sysDomainAuthnServ));

                one(cc).locateService(SecurityServiceImpl.KEY_AUTHENTICATION, appAuthenticationServiceRef);
                will(returnValue(appDomainAuthnServ));

                one(cc).locateService(SecurityServiceImpl.KEY_AUTHORIZATION, sysAuthorizationServiceRef);
                will(returnValue(sysDomainAuthzServ));

                one(cc).locateService(SecurityServiceImpl.KEY_AUTHORIZATION, appAuthorizationServiceRef);
                will(returnValue(appDomainAuthzServ));
            }
        });

        AuthenticationService serviceBefore = secServ.getAuthenticationService();
        assertNotNull(serviceBefore);
        assertSame(serviceBefore, secServ.getAuthenticationService());

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "appDomain");
        secServ.modify(props);

        AuthenticationService serviceAfter = secServ.getAuthenticationService();
        assertNotNull(serviceAfter);
        assertNotSame(serviceBefore, serviceAfter);
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getAuthorizationService()}.
     */
    @Test
    public void getAuthorizationServiceUndefinedConfiguration() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "undefinedDomain");
        secServ.modify(props);

        try {
            secServ.getAuthorizationService();
            fail("Expected an IllegalArgumentException since the " + SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN + " reference is not valid");
        } catch (IllegalArgumentException e) {
            handleExpectedUndefinedDomainExceptionAndMessage("undefinedDomain", SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getAuthorizationService()}.
     */
    @Test
    public void getAuthorizationServiceForUndefinedService() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "authenticationService");
        props.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "undefinedService");
        props.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "userRegistryService");
        sysDomainSecConfig.modify(props);

        try {
            secServ.getAuthorizationService();
            fail("Expected an IllegalArgumentException since the " + SecurityServiceImpl.KEY_AUTHORIZATION + " reference is not valid");
        } catch (IllegalArgumentException e) {
            handleExpectedUndefinedServiceExceptionAndMessage("undefinedService", SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getAuthorizationService()}.
     */
    @Test
    public void getAuthorizationService() {
        final AuthorizationService sysDomainAuthzServ = mock.mock(AuthorizationService.class, "sysDomainAuthzServ");
        final AuthorizationService appDomainAuthzServ = mock.mock(AuthorizationService.class, "appDomainAuthzServ");
        mock.checking(new Expectations() {
            {
                one(cc).locateService(SecurityServiceImpl.KEY_AUTHORIZATION, sysAuthorizationServiceRef);
                will(returnValue(sysDomainAuthzServ));

                one(cc).locateService(SecurityServiceImpl.KEY_AUTHORIZATION, appAuthorizationServiceRef);
                will(returnValue(appDomainAuthzServ));
            }
        });
        AuthorizationService serviceBefore = secServ.getAuthorizationService();
        assertNotNull(serviceBefore);
        assertSame(serviceBefore, secServ.getAuthorizationService());

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "appDomain");
        secServ.modify(props);

        AuthorizationService serviceAfter = secServ.getAuthorizationService();
        assertNotNull(serviceAfter);
        assertNotSame(serviceBefore, serviceAfter);
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getUserRegistryService()}.
     */
    @Test
    public void getUserRegistryServiceUndefinedConfiguration() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "undefinedDomain");
        secServ.modify(props);

        try {
            secServ.getUserRegistryService();
            fail("Expected an IllegalArgumentException since the " + SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN + " reference is not valid");
        } catch (IllegalArgumentException e) {
            handleExpectedUndefinedDomainExceptionAndMessage("undefinedDomain", SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getUserRegistryService()}.
     */
    @Test
    public void getUserRegistryServiceForUndefinedService() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "authenticationService");
        props.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "authorizationService");
        props.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "undefinedService");
        sysDomainSecConfig.modify(props);

        try {
            secServ.getUserRegistryService();
            fail("Expected an IllegalArgumentException since the " + SecurityServiceImpl.KEY_USERREGISTRY + " reference is not valid");
        } catch (IllegalArgumentException e) {
            handleExpectedUndefinedServiceExceptionAndMessage("undefinedService", SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getUserRegistryService()}.
     */
    @Test
    public void getUserRegistryService() {
        final UserRegistryService sysDomainUserRegServ = mock.mock(UserRegistryService.class, "sysDomainUserRegServ");
        final UserRegistryService appDomainUserRegServ = mock.mock(UserRegistryService.class, "appDomainUserRegServ");
        mock.checking(new Expectations() {
            {
                one(cc).locateService(SecurityServiceImpl.KEY_USERREGISTRY, sysUserRegistryServiceRef);
                will(returnValue(sysDomainUserRegServ));

                one(cc).locateService(SecurityServiceImpl.KEY_USERREGISTRY, appUserRegistryServiceRef);
                will(returnValue(appDomainUserRegServ));
            }
        });
        UserRegistryService serviceBefore = secServ.getUserRegistryService();
        assertNotNull(serviceBefore);
        assertSame(serviceBefore, secServ.getUserRegistryService());

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityServiceImpl.CFG_KEY_SYSTEM_DOMAIN, "appDomain");
        secServ.modify(props);

        UserRegistryService serviceAfter = secServ.getUserRegistryService();
        assertNotNull(serviceAfter);
        assertNotSame(serviceBefore, serviceAfter);
    }

}
