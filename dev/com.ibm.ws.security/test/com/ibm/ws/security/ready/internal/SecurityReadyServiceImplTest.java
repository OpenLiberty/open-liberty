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
package com.ibm.ws.security.ready.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.ws.security.ready.SecurityReadyService;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.ws.security.token.TokenService;

import test.common.SharedOutputManager;

/**
 *
 */
@SuppressWarnings("unchecked")
public class SecurityReadyServiceImplTest {
    private static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final ServiceReference<TokenService> tokenService = mock.mock(ServiceReference.class, "tokenService");
    private final ServiceReference<TokenManager> tokenManager = mock.mock(ServiceReference.class, "tokenManager");
    private final ServiceReference<AuthenticationService> authenticationService = mock.mock(ServiceReference.class, "authenticationService");
    private final ServiceReference<AuthorizationService> authorizationService = mock.mock(ServiceReference.class, "authorizationService");
    private final ServiceReference<UserRegistry> userRegistry = mock.mock(ServiceReference.class, "userRegistry");
    private final ServiceReference<UserRegistryService> userRegistryService = mock.mock(ServiceReference.class, "userRegistryService");
    private SecurityReadyServiceImpl readyService;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        readyService = new SecurityReadyServiceImpl();
    }

    @After
    public void tearDown() {
        readyService = null;

        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        outputMgr.restoreStreams();
    }

    /**
     * Test method for {@link com.ibm.ws.security.ready.internal.SecurityReadyServiceImpl#activate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void activate() {
        try {
            readyService.activate(cc);

            assertTrue("Expected start message not output",
                       outputMgr.checkForMessages("CWWKS0007I: The security service is starting..."));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.ready.internal.SecurityReadyServiceImpl#deactivate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void deactivate() {
        try {
            readyService.deactivate(cc);
            assertTrue("Expected stop message not output",
                       outputMgr.checkForMessages("CWWKS0009I: The security service has stopped."));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.ready.internal.SecurityReadyServiceImpl#isSecurityReady()}.
     */
    @Test
    public void isSecurityReady_false() {
        assertFalse("The security service should not be ready, but was",
                    readyService.isSecurityReady());
    }

    /**
     * Test method for {@link com.ibm.ws.security.ready.internal.SecurityReadyServiceImpl#isSecurityReady()}.
     */
    @Test
    public void isSecurityReady_true() {
        try {
            final BundleContext bc = mock.mock(BundleContext.class);
            final ServiceRegistration<SecurityReadyService> reg = mock.mock(ServiceRegistration.class);
            mock.checking(new Expectations() {
                {
                    one(cc).getBundleContext();
                    will(returnValue(bc));

                    (allowing(bc)).registerService(with(SecurityReadyService.class), with(readyService), with(any(Dictionary.class)));
                    will(returnValue(reg));
                }
            });

            readyService.setTokenService(tokenService);
            readyService.setTokenManager(tokenManager);
            readyService.setAuthenticationService(authenticationService);
            readyService.setAuthorizationService(authorizationService);
            readyService.setUserRegistry(userRegistry);
            readyService.setUserRegistryService(userRegistryService);

            readyService.activate(cc);

            assertSecurityServiceIsReadyWithCorrectMessages();
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.ready.internal.SecurityReadyServiceImpl#isSecurityReady()}.
     */
    @Test
    public void isSecurityReady_dynamic() {
        try {
            final BundleContext bc = mock.mock(BundleContext.class);
            final ServiceRegistration<SecurityReadyService> reg = mock.mock(ServiceRegistration.class);
            mock.checking(new Expectations() {
                {
                    one(cc).getBundleContext();
                    will(returnValue(bc));

                    (allowing(bc)).registerService(with(SecurityReadyService.class), with(readyService), with(any(Dictionary.class)));
                    will(returnValue(reg));

                    (allowing(reg)).unregister();
                }
            });

            readyService.setTokenService(tokenService);
            readyService.setTokenManager(tokenManager);
            readyService.setAuthenticationService(authenticationService);
            readyService.setAuthorizationService(authorizationService);
            readyService.setUserRegistry(userRegistry);
            readyService.setUserRegistryService(userRegistryService);

            readyService.activate(cc);

            assertSecurityServiceIsReadyWithCorrectMessages();

            readyService.unsetTokenService(tokenService);
            assertFalse("The security service should not be ready, but was",
                        readyService.isSecurityReady());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    private void assertSecurityServiceIsReadyWithCorrectMessages() {
        assertTrue("Expected start message not output",
                   outputMgr.checkForMessages("CWWKS0007I: The security service is starting..."));
        assertTrue("The security service should be ready, but wasn't",
                   readyService.isSecurityReady());
        assertTrue("Expected ready message not output",
                   outputMgr.checkForMessages("CWWKS0008I: The security service is ready."));
    }
}
