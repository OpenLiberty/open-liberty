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
package com.ibm.wsspi.security.registry;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.ws.security.intfc.WSSecurityService;
import com.ibm.ws.security.registry.internal.UserRegistryWrapper;

/**
 *
 */
@SuppressWarnings("unchecked")
public class RegistryHelperTest {
    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final ServiceReference<WSSecurityService> wsSecurityServiceRef = mock.mock(ServiceReference.class);
    private final WSSecurityService wsSecurityService = mock.mock(WSSecurityService.class);
    private UserRegistryWrapper urWrapper;
    private RegistryHelper registryHelper;

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(WSSecurityService.KEY_WS_SECURITY_SERVICE, wsSecurityServiceRef);
                will(returnValue(wsSecurityService));
            }
        });
        registryHelper = new RegistryHelper();
        registryHelper.setWsSecurityService(wsSecurityServiceRef);
        registryHelper.activate(cc);
        urWrapper = new UserRegistryWrapper(null);
    }

    @After
    public void tearDown() {
        registryHelper.deactivate(cc);
        registryHelper.unsetWsSecurityService(wsSecurityServiceRef);

        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.wsspi.security.registry.RegistryHelper#getUserRegistry(java.lang.String)}.
     */
    @Test
    public void getUserRegistry_noSecurityService() throws Exception {
        registryHelper.unsetWsSecurityService(wsSecurityServiceRef);
        assertNull("Null is expected when WSSecurityService is not available",
                   RegistryHelper.getUserRegistry(null));
    }

    /**
     * Test method for {@link com.ibm.wsspi.security.registry.RegistryHelper#getUserRegistry(java.lang.String)}.
     */
    @Test
    public void getUserRegistry() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(wsSecurityService).getUserRegistry(null);
                will(returnValue(urWrapper));
            }
        });
        assertTrue("UserRegistry object should be a UserRegistryWrapper",
                   RegistryHelper.getUserRegistry(null) instanceof UserRegistryWrapper);
    }

    /**
     * Test method for {@link com.ibm.wsspi.security.registry.RegistryHelper#getUserRegistry(java.lang.String)}.
     */
    @Test(expected = WSSecurityException.class)
    public void getUserRegistry_RegistryException() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(wsSecurityService).getUserRegistry(null);
                will(throwException(new WSSecurityException("expected")));
            }
        });
        RegistryHelper.getUserRegistry(null);
    }

    /**
     * Test method for {@link com.ibm.wsspi.security.registry.RegistryHelper#getInboundTrustedRealms(java.lang.String)}.
     */
    @Test
    public void getInboundTrustedRealms() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(wsSecurityService).getInboundTrustedRealms(null);
                will(returnValue(null));
            }
        });
        assertNull("The production code is not yet implemented and should return null",
                   RegistryHelper.getInboundTrustedRealms(null));
    }

    /**
     * Test method for {@link com.ibm.wsspi.security.registry.RegistryHelper#isRealmInboundTrusted(java.lang.String, java.lang.String)}.
     */
    @Test
    public void isRealmInboundTrusted() {
        mock.checking(new Expectations() {
            {
                allowing(wsSecurityService).isRealmInboundTrusted(null, null);
                will(returnValue(true));
            }
        });
        assertTrue("The production code is not yet implemented and should always return true",
                   RegistryHelper.isRealmInboundTrusted(null, null));
    }

}
