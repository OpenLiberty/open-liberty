/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.intfc.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.UserRegistry;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper;
import com.ibm.ws.security.registry.internal.UserRegistryWrapper;

/**
 *
 */
@SuppressWarnings("unchecked")
public class WSSecurityServiceImplTest {

    private static final String CUSTOM_REALM = "customRealm";
    private final Mockery mockery = new JUnit4Mockery();
    private final ComponentContext componentContext = mockery.mock(ComponentContext.class);
    private final ServiceReference<SecurityService> securityServiceReference = mockery.mock(ServiceReference.class, "securityServiceReference");
    private final SecurityService securityService = mockery.mock(SecurityService.class);
    private final UserRegistryService userRegistryService = mockery.mock(UserRegistryService.class);
    private final UserRegistry customUserRegistry = mockery.mock(UserRegistry.class);
    private final CustomUserRegistryWrapper customUserRegistryWrapper = new CustomUserRegistryWrapper(customUserRegistry);

    private final WSSecurityServiceImpl wsSecurityServiceImpl = new WSSecurityServiceImpl();

    @Before
    public void setUp() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(componentContext).locateService("securityService", securityServiceReference);
                will(returnValue(securityService));
                allowing(securityService).getUserRegistryService();
                will(returnValue(userRegistryService));
                allowing(userRegistryService).isUserRegistryConfigured();
                will(returnValue(true));
                allowing(userRegistryService).getUserRegistry();
                will(returnValue(customUserRegistryWrapper));
                allowing(customUserRegistry).getRealm();
                will(returnValue(CUSTOM_REALM));

                allowing(userRegistryService).getExternalUserRegistry(customUserRegistryWrapper);
                will(returnValue(customUserRegistry));
            }
        });

        wsSecurityServiceImpl.setSecurityService(securityServiceReference);
        wsSecurityServiceImpl.activate(componentContext);
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
        wsSecurityServiceImpl.unsetSecurityService(securityServiceReference);
        wsSecurityServiceImpl.deactivate(componentContext);
    }

    @Test
    public void getUserRegistry_custom() throws Exception {
        UserRegistry userRegistry = wsSecurityServiceImpl.getUserRegistry(CUSTOM_REALM);

        assertFalse("The UserRegistry object should not be a UserRegistryWrapper.", userRegistry instanceof UserRegistryWrapper);
        assertEquals("The UserRegistry object should be the actual custom user registry.", customUserRegistry, userRegistry);
    }

}
