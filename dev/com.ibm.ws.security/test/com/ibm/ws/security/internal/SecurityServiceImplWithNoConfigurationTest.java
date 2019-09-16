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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
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
 * Test the SecurityService in the context of no configuration.
 * Important: no configuration means no ids for the
 * sub-services, and no SecurityConfigurations.
 */
@SuppressWarnings("unchecked")
public class SecurityServiceImplWithNoConfigurationTest {

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
    private final Dictionary<String, Object> dict = new Hashtable<String, Object>();
    private final Map<String, Object> props = new HashMap<String, Object>();

    private final ServiceReference<AuthenticationService> sysAuthenticationServiceRef = mock.mock(ServiceReference.class, "sysAuthenticationServiceRef");
    private final ServiceReference<AuthorizationService> sysAuthorizationServiceRef = mock.mock(ServiceReference.class, "sysAuthorizationServiceRef");
    private final ServiceReference<UserRegistryService> sysUserRegistryServiceRef = mock.mock(ServiceReference.class, "sysUserRegistryServiceRef");

    private final ServiceReference<AuthenticationService> appAuthenticationServiceRef = mock.mock(ServiceReference.class, "appAuthenticationServiceRef");
    private final ServiceReference<AuthorizationService> appAuthorizationServiceRef = mock.mock(ServiceReference.class, "appAuthorizationServiceRef");
    private final ServiceReference<UserRegistryService> appUserRegistryServiceRef = mock.mock(ServiceReference.class, "appUserRegistryServiceRef");

    private final ServiceReference<AuthorizationService> builtinAuthorizationServiceRef = mock.mock(ServiceReference.class, "builtinAuthorizationServiceRef");

    private SecurityServiceImpl secServ;

    @Before
    public void setUp() {
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

                allowing(appUserRegistryServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(appUserRegistryServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(sysAuthenticationServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(sysAuthenticationServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(sysAuthorizationServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(sysAuthorizationServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(sysAuthorizationServiceRef).getProperty(AuthorizationService.AUTHORIZATION_TYPE);
                will(returnValue("sysAuthz"));

                allowing(sysUserRegistryServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(sysUserRegistryServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(cc).getProperties();
                will(returnValue(dict));

                allowing(sysAuthenticationServiceRef).getProperty(SecurityServiceImpl.KEY_CONFIG_SOURCE);
                will(returnValue(null));
                allowing(sysAuthenticationServiceRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue(null));
                allowing(sysAuthenticationServiceRef).getProperty(SecurityServiceImpl.KEY_SERVICE_ID);
                will(returnValue(110));

                allowing(sysAuthorizationServiceRef).getProperty(SecurityServiceImpl.KEY_CONFIG_SOURCE);
                will(returnValue(null));
                allowing(sysAuthorizationServiceRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue(null));
                allowing(sysAuthorizationServiceRef).getProperty(SecurityServiceImpl.KEY_SERVICE_ID);
                will(returnValue(111));

                allowing(sysUserRegistryServiceRef).getProperty(SecurityServiceImpl.KEY_CONFIG_SOURCE);
                will(returnValue(null));
                allowing(sysUserRegistryServiceRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue("sysDomain"));
                allowing(sysUserRegistryServiceRef).getProperty(SecurityServiceImpl.KEY_SERVICE_ID);
                will(returnValue(112));

                allowing(appAuthenticationServiceRef).getProperty(SecurityServiceImpl.KEY_CONFIG_SOURCE);
                will(returnValue(null));
                allowing(appAuthenticationServiceRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue(null));
                allowing(appAuthenticationServiceRef).getProperty(SecurityServiceImpl.KEY_SERVICE_ID);
                will(returnValue(120));

                allowing(appAuthorizationServiceRef).getProperty(SecurityServiceImpl.KEY_CONFIG_SOURCE);
                will(returnValue(null));
                allowing(appAuthorizationServiceRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue(null));
                allowing(appAuthorizationServiceRef).getProperty(SecurityServiceImpl.KEY_SERVICE_ID);
                will(returnValue(121));
                allowing(appAuthorizationServiceRef).getProperty(AuthorizationService.AUTHORIZATION_TYPE);
                will(returnValue("appAuthz"));

                allowing(appUserRegistryServiceRef).getProperty(SecurityServiceImpl.KEY_CONFIG_SOURCE);
                will(returnValue(null));
                allowing(appUserRegistryServiceRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue("appDomain"));
                allowing(appUserRegistryServiceRef).getProperty(SecurityServiceImpl.KEY_SERVICE_ID);
                will(returnValue(122));

                allowing(builtinAuthorizationServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(builtinAuthorizationServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(builtinAuthorizationServiceRef).getProperty(SecurityServiceImpl.KEY_CONFIG_SOURCE);
                will(returnValue(null));
                allowing(builtinAuthorizationServiceRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue(null));
                allowing(builtinAuthorizationServiceRef).getProperty(SecurityServiceImpl.KEY_SERVICE_ID);
                will(returnValue(121));
                allowing(builtinAuthorizationServiceRef).getProperty(AuthorizationService.AUTHORIZATION_TYPE);
                will(returnValue("Builtin"));

                allowing(cc).getBundleContext();
                will(returnValue(mockBundleContext));
                ignoring(mockBundleContext);
            }
        });

        secServ = new SecurityServiceImpl();
        secServ.activate(cc, props);
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();

        secServ.deactivate(cc, props);
        secServ.unsetAuthentication(sysAuthenticationServiceRef);
        secServ.unsetAuthorization(sysAuthorizationServiceRef);
        secServ.unsetUserRegistry(sysUserRegistryServiceRef);
        secServ.unsetAuthentication(appAuthenticationServiceRef);
        secServ.unsetAuthorization(appAuthorizationServiceRef);
        secServ.unsetUserRegistry(appUserRegistryServiceRef);
        secServ = null;
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#activate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void activateWithNoSecurityConfiguration() {
        SecurityServiceImpl secServ = new SecurityServiceImpl();
        secServ.activate(cc, props);
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#modify(java.util.Map)}.
     */
    @Test
    public void modifyWithNoSecurityConfiguration() {
        Map<String, Object> props = new HashMap<String, Object>();
        secServ.modify(props);
    }

    /**
     * Check the expected exception message and log message in the case where
     * no service is available in the auto-detect path.
     *
     * @param e
     * @param service
     */
    private void handleExpectedNoServiceExceptionAndMessage(Exception e, String service) {
        assertEquals("The exception should be a fully variably subsituted message of the right type",
                     "CWWKS0005E: A configuration exception has occurred. No available " + service + " service.",
                     e.getMessage());

        assertTrue("Expected error message was not logged",
                   outputMgr.checkForStandardErr("CWWKS0005E: A configuration exception has occurred. No available " + service + " service."));
    }

    /**
     * Check the expected exception message and log message in the case where
     * there are multiple services available in the auto-detect path.
     *
     * @param e
     * @param service
     */
    private void handleExpectedMultipleServiceExceptionAndMessage(Exception e, String service) {
        assertEquals("The exception should be a fully variably subsituted message of the right type",
                     "CWWKS0006E: A configuration exception has occurred. There are multiple available " + service + " services; the system cannot determine which to use.",
                     e.getMessage());

        assertTrue("Expected error message was not logged",
                   outputMgr.checkForStandardErr("CWWKS0006E: A configuration exception has occurred. There are multiple available " + service
                                                 + " services; the system cannot determine which to use."));
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getAuthenticationService()}.
     */
    @Test
    public void getAuthenticationService_noService() {
        try {
            secServ.getAuthenticationService();
            fail("Expected IllegalStateException since there is no " + SecurityServiceImpl.KEY_AUTHENTICATION + " service");
        } catch (IllegalStateException e) {
            handleExpectedNoServiceExceptionAndMessage(e, SecurityServiceImpl.KEY_AUTHENTICATION);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getAuthenticationService()}.
     */
    @Test
    public void getAuthenticationService_oneService() {
        final AuthenticationService sysDomainAuthnServ = mock.mock(AuthenticationService.class, "sysDomainAuthnServ");
        mock.checking(new Expectations() {
            {
                one(cc).locateService(SecurityServiceImpl.KEY_AUTHENTICATION, sysAuthenticationServiceRef);
                will(returnValue(sysDomainAuthnServ));
            }
        });

        secServ.setAuthentication(sysAuthenticationServiceRef);

        AuthenticationService service = secServ.getAuthenticationService();
        assertNotNull(service);
        assertSame(service, secServ.getAuthenticationService());
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getAuthenticationService()}.
     */
    @Test
    public void getAuthenticationService_multipleServices() {
        final AuthenticationService sysDomainAuthnServ = mock.mock(AuthenticationService.class, "sysDomainAuthnServ");
        final AuthenticationService appAuthnServ = mock.mock(AuthenticationService.class, "appAuthnServ");
        mock.checking(new Expectations() {
            {
                one(cc).locateService(SecurityServiceImpl.KEY_AUTHENTICATION, sysAuthenticationServiceRef);
                will(returnValue(sysDomainAuthnServ));
                one(cc).locateService(SecurityServiceImpl.KEY_AUTHENTICATION, appAuthenticationServiceRef);
                will(returnValue(appAuthnServ));
            }
        });

        secServ.setAuthentication(sysAuthenticationServiceRef);
        secServ.setAuthentication(appAuthenticationServiceRef);

        try {
            secServ.getAuthenticationService();
            fail("Expected IllegalStateException since there are multiple " + SecurityServiceImpl.KEY_AUTHENTICATION + " services");
        } catch (IllegalStateException e) {
            handleExpectedMultipleServiceExceptionAndMessage(e, SecurityServiceImpl.KEY_AUTHENTICATION);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getAuthorizationService()}.
     */
    @Test
    public void getAuthorizationService_noService() {
        try {
            secServ.getAuthorizationService();
            fail("Expected IllegalStateException since there is no " + SecurityServiceImpl.KEY_AUTHORIZATION + " service");
        } catch (IllegalStateException e) {
            handleExpectedNoServiceExceptionAndMessage(e, SecurityServiceImpl.KEY_AUTHORIZATION);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getAuthorizationService()}.
     */
    @Test
    public void getAuthorizationService_oneService() {
        final AuthorizationService sysDomainAuthzServ = mock.mock(AuthorizationService.class, "sysDomainAuthzServ");
        mock.checking(new Expectations() {
            {
                one(cc).locateService(SecurityServiceImpl.KEY_AUTHORIZATION, sysAuthorizationServiceRef);
                will(returnValue(sysDomainAuthzServ));
            }
        });

        secServ.setAuthorization(sysAuthorizationServiceRef);

        AuthorizationService service = secServ.getAuthorizationService();
        assertNotNull(service);
        assertSame(service, secServ.getAuthorizationService());
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getAuthorizationService()}.
     * <p>
     * This is a special case. In the case where BOTH a builtin authorization service
     * and a non-builtin authorization service exist, AND there is no configuration
     * to instruct which to use, then we want to use the non-builtin one.
     */
    @Test
    public void getAuthorizationService_multipleServicesOneBuiltin() {
        secServ.setAuthorization(builtinAuthorizationServiceRef);
        secServ.setAuthorization(sysAuthorizationServiceRef);

        final AuthorizationService sysDomainAuthzServ = mock.mock(AuthorizationService.class, "sysDomainAuthzServ");
        final AuthorizationService builtinAuthzServ = mock.mock(AuthorizationService.class, "builtinAuthzServ");
        mock.checking(new Expectations() {
            {
                one(cc).locateService(SecurityServiceImpl.KEY_AUTHORIZATION, sysAuthorizationServiceRef);
                will(returnValue(sysDomainAuthzServ));
                one(cc).locateService(SecurityServiceImpl.KEY_AUTHORIZATION, builtinAuthorizationServiceRef);
                will(returnValue(builtinAuthzServ));
            }
        });

        AuthorizationService service = secServ.getAuthorizationService();
        assertNotNull("Serivce should not be null", service);
        assertSame("Serivce should be the system authz service",
                   sysDomainAuthzServ, service);
        assertSame("A subsequent get should return the same service",
                   service, secServ.getAuthorizationService());
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getAuthorizationService()}.
     * <p>
     * This is a special case. In the case where multiple authorization services
     * exist but neither are the builtin, we are unable to determine which to use
     * which is a configuration error.
     */
    @Test
    public void getAuthorizationService_multipleServicesNoneBuiltin() {
        final AuthorizationService sysAuthzService = mock.mock(AuthorizationService.class, "sysAuthzService");
        final AuthorizationService appAuthzService = mock.mock(AuthorizationService.class, "appAuthzService");
        mock.checking(new Expectations() {
            {
                one(cc).locateService(SecurityServiceImpl.KEY_AUTHORIZATION, sysAuthorizationServiceRef);
                will(returnValue(sysAuthzService));
                one(cc).locateService(SecurityServiceImpl.KEY_AUTHORIZATION, appAuthorizationServiceRef);
                will(returnValue(appAuthzService));
            }
        });

        secServ.setAuthorization(sysAuthorizationServiceRef);
        secServ.setAuthorization(appAuthorizationServiceRef);

        try {
            secServ.getAuthorizationService();
            fail("Expected IllegalStateException since there are multiple " + SecurityServiceImpl.KEY_AUTHORIZATION + " services");
        } catch (IllegalStateException e) {
            handleExpectedMultipleServiceExceptionAndMessage(e, SecurityServiceImpl.KEY_AUTHORIZATION);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getUserRegistryService()}.
     */
    @Test
    public void getUserRegistryService_noService() {
        try {
            secServ.getUserRegistryService();
            fail("Expected IllegalStateException since there is no " + SecurityServiceImpl.KEY_USERREGISTRY + " service");
        } catch (IllegalStateException e) {
            handleExpectedNoServiceExceptionAndMessage(e, SecurityServiceImpl.KEY_USERREGISTRY);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getUserRegistryService()}.
     */
    @Test
    public void getUserRegistryService_oneService() {
        final UserRegistryService sysDomainUserRegServ = mock.mock(UserRegistryService.class, "sysDomainUserRegServ");
        mock.checking(new Expectations() {
            {
                one(cc).locateService(SecurityServiceImpl.KEY_USERREGISTRY, sysUserRegistryServiceRef);
                will(returnValue(sysDomainUserRegServ));
                allowing(sysUserRegistryServiceRef).getProperty("realm");
                will(returnValue("sysDomainUserRegServ"));
                allowing(sysUserRegistryServiceRef).getProperty("config.displayId");
                will(returnValue("com.ibm.ws.security.registry.internal.UserRegistryRefConfig"));
            }
        });

        secServ.setUserRegistry(sysUserRegistryServiceRef);

        UserRegistryService service = secServ.getUserRegistryService();
        assertNotNull(service);
        assertSame(service, secServ.getUserRegistryService());
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityServiceImpl#getUserRegistryService()}.
     */
    @Test
    public void getUserRegistryService_multipleServices() {
        final UserRegistryService sysDomainUserRegServ = mock.mock(UserRegistryService.class, "sysDomainUserRegServ");
        final UserRegistryService appUserRegServ = mock.mock(UserRegistryService.class, "appUserRegServ");
        mock.checking(new Expectations() {
            {
                one(cc).locateService(SecurityServiceImpl.KEY_USERREGISTRY, sysUserRegistryServiceRef);
                will(returnValue(sysDomainUserRegServ));
                allowing(sysUserRegistryServiceRef).getProperty("realm");
                will(returnValue("sysDomainUserRegServ"));
                allowing(sysUserRegistryServiceRef).getProperty("config.displayId");
                will(returnValue("userRegistry[sysDomainUserRegServ]"));

                one(cc).locateService(SecurityServiceImpl.KEY_USERREGISTRY, appUserRegistryServiceRef);
                will(returnValue(appUserRegServ));
                allowing(appUserRegistryServiceRef).getProperty("realm");
                will(returnValue("appDomainUserRegServ"));
                allowing(appUserRegistryServiceRef).getProperty("config.displayId");
                will(returnValue("userRegistry[appDomainUserRegServ]"));
            }
        });

        secServ.setUserRegistry(sysUserRegistryServiceRef);
        secServ.setUserRegistry(appUserRegistryServiceRef);

        try {
            secServ.getUserRegistryService();
            fail("Expected IllegalStateException since there are multiple " + SecurityServiceImpl.KEY_USERREGISTRY + " services");
        } catch (IllegalStateException e) {
            handleExpectedMultipleServiceExceptionAndMessage(e, SecurityServiceImpl.KEY_USERREGISTRY);
        }
    }

}
