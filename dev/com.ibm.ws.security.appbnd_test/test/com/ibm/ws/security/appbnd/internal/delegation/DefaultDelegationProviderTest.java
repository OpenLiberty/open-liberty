/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.appbnd.internal.delegation;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.javaee.dd.appbnd.SecurityRole;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
public class DefaultDelegationProviderTest {

    private static final String APP1_NAME = "TestApp";
    private static final String APP2_NAME = "AnotherTestApp";
    private static final String RUNAS_ROLE = "testRole";
    private static final String RUNAS_ROLE_NOT_IN_BINDINGS = "badRole";
    private static final String APP1_MAPPED_USER = "user1";
    private static final String APP1_MAPPED_PASSWORD = "user1pwd";
    private static final String APP2_MAPPED_USER = "user2";
    private static final String APP2_MAPPED_PASSWORD = "user2pwd";
    private static SharedOutputManager outputMgr;
    private final Mockery mockery = new JUnit4Mockery();
    private List<SecurityRole> app1SecurityRoles;
    private List<SecurityRole> app2SecurityRoles;
    private AuthenticationData app1AuthenticationData;
    private AuthenticationData app2AuthenticationData;
    private final Subject app1RunAsSubject = new Subject();
    private final Subject app2RunAsSubject = new Subject();
    private DefaultDelegationProvider defaultDelegationProvider;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        SecurityRolesBuilder securityRolesBuilder = new SecurityRolesBuilder(mockery);
        securityRolesBuilder.buildSecurityRole(RUNAS_ROLE, APP1_MAPPED_USER, APP1_MAPPED_PASSWORD);
        app1SecurityRoles = securityRolesBuilder.getSecurityRoles().getSecurityRoles();

        securityRolesBuilder.buildSecurityRole(RUNAS_ROLE, APP2_MAPPED_USER, APP2_MAPPED_PASSWORD);
        app2SecurityRoles = securityRolesBuilder.getSecurityRoles().getSecurityRoles();

        app1AuthenticationData = createAuthenticationData(APP1_MAPPED_USER, APP1_MAPPED_PASSWORD);
        app2AuthenticationData = createAuthenticationData(APP2_MAPPED_USER, APP2_MAPPED_PASSWORD);

        AuthenticationService authenticationService = createAuthenticationService();
        SecurityService securityService = createSecurityService(authenticationService);
        defaultDelegationProvider = createPopulatedMethodDelegationProvider(securityService);
    }

    private AuthenticationData createAuthenticationData(String userName, String password) {
        final AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.USERNAME, userName);
        authenticationData.set(AuthenticationData.PASSWORD, password.toCharArray());
        return authenticationData;
    }

    private AuthenticationService createAuthenticationService() throws AuthenticationException {
        final AuthenticationService authenticationService = mockery.mock(AuthenticationService.class);
        mockery.checking(new Expectations() {
            {
                allowing(authenticationService).authenticate(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, app1AuthenticationData, null);
                will(returnValue(app1RunAsSubject));
                allowing(authenticationService).authenticate(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, app2AuthenticationData, null);
                will(returnValue(app2RunAsSubject));
            }
        });
        return authenticationService;
    }

    private SecurityService createSecurityService(final AuthenticationService authenticationService) {
        final SecurityService securityService = mockery.mock(SecurityService.class);
        mockery.checking(new Expectations() {
            {
                allowing(securityService).getAuthenticationService();
                will(returnValue(authenticationService));
            }
        });
        return securityService;
    }

    private DefaultDelegationProvider createPopulatedMethodDelegationProvider(SecurityService securityService) throws UnableToAdaptException {
        DefaultDelegationProvider defaultDelegationProvider = createMethodDelegationProviderDouble(securityService);
        defaultDelegationProvider.createAppToSecurityRolesMapping(APP1_NAME, app1SecurityRoles);
        defaultDelegationProvider.createAppToSecurityRolesMapping(APP2_NAME, app2SecurityRoles);
        return defaultDelegationProvider;
    }

    private DefaultDelegationProvider createMethodDelegationProviderDouble(SecurityService securityService) {
        DefaultDelegationProvider defaultDelegationProvider = new DefaultDelegationProvider() {

            @Override
            protected AuthenticationData createAuthenticationData(String username, String password) {
                if (username.equalsIgnoreCase(APP1_MAPPED_USER) && password.equalsIgnoreCase(APP1_MAPPED_PASSWORD)) {
                    return app1AuthenticationData;
                } else if (username.equalsIgnoreCase(APP2_MAPPED_USER) && password.equalsIgnoreCase(APP2_MAPPED_PASSWORD)) {
                    return app2AuthenticationData;
                } else {
                    return new WSAuthenticationData();
                }
            }
        };
        defaultDelegationProvider.setSecurityService(securityService);
        return defaultDelegationProvider;
    }

    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test
    public void testGetRunAsSubject() {
        final String methodName = "testGetRunAsSubject";
        try {
            Subject currentRunAsSubject = defaultDelegationProvider.getRunAsSubject(RUNAS_ROLE, APP1_NAME);
            assertSame("The RunAs subject must be the same as the test RunAs subject.", app1RunAsSubject, currentRunAsSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetRunAsSubjectWithBadRoleReturnsNullSubject() {
        final String expectedMessage = "CWWKS9112W.*" + RUNAS_ROLE_NOT_IN_BINDINGS + "*.*" + APP1_NAME;
        final String methodName = "testGetRunAsSubjectWithBadRoleReturnsNullSubject";
        try {
            Subject currentRunAsSubject = defaultDelegationProvider.getRunAsSubject(RUNAS_ROLE_NOT_IN_BINDINGS, APP1_NAME);
            assertNull("The RunAs subject must be null when the role is not in the bindings.", currentRunAsSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        assertTrue("The expected warning message should have been found: " + expectedMessage, outputMgr.checkForMessages(expectedMessage));
    }

    @Test
    public void testGetRunAsSubjectSameRoleAnotherAppReturnsAnotherSubject() {
        final String methodName = "testGetRunAsSubjectSameRoleAnotherAppReturnsAnotherSubject";
        try {
            Subject currentRunAsSubject = defaultDelegationProvider.getRunAsSubject(RUNAS_ROLE, APP2_NAME);
            assertSame("The RunAs subject must be the same as the app2 RunAs subject.", app2RunAsSubject, currentRunAsSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testRemoveMappings() {
        final String methodName = "testRemoveMappings";
        try {
            defaultDelegationProvider.removeRoleToRunAsMapping(APP1_NAME);
            Subject currentRunAsSubject = defaultDelegationProvider.getRunAsSubject(RUNAS_ROLE, APP1_NAME);
            assertNull("The RunAs subject must be null since the mappings were removed.", currentRunAsSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testRemoveMappingsCanStillGetRunAsSubjectForAnotherApp() {
        final String methodName = "testRemoveMappingsCanStillGetRunAsSubjectForAnotherApp";
        try {
            defaultDelegationProvider.removeRoleToRunAsMapping(APP1_NAME);
            Subject currentRunAsSubject = defaultDelegationProvider.getRunAsSubject(RUNAS_ROLE, APP2_NAME);
            assertSame("The RunAs subject must be the same as the app2 RunAs subject.", app2RunAsSubject, currentRunAsSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testRemoveMappingsWithUnexistingAppDoesNotThrowException() {
        final String methodName = "testRemoveMappingsWithUnexistingAppDoesNotThrowException";
        try {
            defaultDelegationProvider.removeRoleToRunAsMapping("AppNameThatDoesNotExist");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
