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
import static org.junit.Assert.assertTrue;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.ws.security.registry.UserRegistryService;

import test.common.SharedOutputManager;

/**
 * This test class is only responsible for testing the <b>
 * incorrect</b> paths of setting dependent services.
 */
@SuppressWarnings("unchecked")
public class SecurityServiceImplSetterCheckTest {
    private static SharedOutputManager outputMgr;
    private final Mockery mock = new JUnit4Mockery();
    private final ServiceReference<SecurityConfiguration> securityConfigurationRef = mock.mock(ServiceReference.class, "securityConfigurationRef");
    private final ServiceReference<AuthenticationService> authenticationServiceRef = mock.mock(ServiceReference.class, "authenticationServiceRef");
    private final ServiceReference<AuthorizationService> authorizationServiceRef = mock.mock(ServiceReference.class, "authorizationServiceRef");
    private final ServiceReference<UserRegistryService> userRegistryServiceRef = mock.mock(ServiceReference.class, "userRegistryServiceRef");

    @BeforeClass
    public static void setUpBeforeClass() {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        outputMgr.restoreStreams();
    }

    /**
     * Validates that the expected error message was logged for the specified
     * type.
     *
     * @param type
     */
    private void checkErrorMessageWasLogged(String type) {
        assertTrue("Expected error message CWWKS0001E was not logged, should have been generated from config element with no id attribute.",
                   outputMgr.checkForStandardErr("CWWKS0001E: A configuration exception has occurred. A configuration element of type " + type
                                                 + " does not define an id attribute."));
    }

    /**
     * If a SecurityConfiguration service does not define an ID, it is
     * incorrectly configured and can not be used, thus ignore the service.
     */
    @Test
    public void securityConfiguration_noID() {
        mock.checking(new Expectations() {
            {
                one(securityConfigurationRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue(null));
            }
        });

        SecurityServiceImpl secServ = new SecurityServiceImpl();
        secServ.setConfiguration(securityConfigurationRef);
        assertEquals("No entry should have been set in the configs map",
                     0, secServ.configs.size());
        checkErrorMessageWasLogged("securityConfiguration");
    }

    /**
     * If an Authentication service does not define an ID, and it is
     * configured in the file, then it is incorrectly configured and
     * can not be used, thus ignore the service.
     */
    @Test
    public void authenticationService_noID() {
        mock.checking(new Expectations() {
            {
                one(authenticationServiceRef).getProperty(SecurityServiceImpl.KEY_CONFIG_SOURCE);
                will(returnValue("file"));
                one(authenticationServiceRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue(null));
            }
        });

        SecurityServiceImpl secServ = new SecurityServiceImpl();
        secServ.setAuthentication(authenticationServiceRef);
        assertEquals("No entry should have been set in the authentication map",
                     0, secServ.authentication.size());
        checkErrorMessageWasLogged(SecurityServiceImpl.KEY_AUTHENTICATION);
    }

    /**
     * If an Authorization service does not define an ID, and it is
     * configured in the file, then it is incorrectly configured and
     * can not be used, thus ignore the service.
     */
    @Test
    public void authorizationService_noID() {
        mock.checking(new Expectations() {
            {
                one(authorizationServiceRef).getProperty(SecurityServiceImpl.KEY_CONFIG_SOURCE);
                will(returnValue("file"));
                one(authorizationServiceRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue(null));
            }
        });

        SecurityServiceImpl secServ = new SecurityServiceImpl();
        secServ.setAuthorization(authorizationServiceRef);
        assertEquals("No entry should have been set in the authorization map",
                     0, secServ.authorization.size());
        checkErrorMessageWasLogged(SecurityServiceImpl.KEY_AUTHORIZATION);
    }

    /**
     * If an UserRegistry service does not define an ID, and it is
     * configured in the file, then it is incorrectly configured and
     * can not be used, thus ignore the service.
     */
    @Test
    public void userRegistryService_noID() {
        mock.checking(new Expectations() {
            {
                one(userRegistryServiceRef).getProperty(SecurityServiceImpl.KEY_ID);
                will(returnValue(null));
                one(userRegistryServiceRef).getProperty("config.displayId");
                will(returnValue("userRegistry[default-0]"));
            }
        });

        SecurityServiceImpl secServ = new SecurityServiceImpl();
        secServ.setUserRegistry(userRegistryServiceRef);
        assertEquals("No entry should have been set in the userRegistry map",
                     0, secServ.userRegistry.size());
        checkErrorMessageWasLogged(SecurityServiceImpl.KEY_USERREGISTRY);
    }
}
