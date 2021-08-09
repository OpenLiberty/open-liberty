/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.auth.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that the WSMappingCallbackHandler populates the WSManagedConnectionFactoryCallback
 * and the WSMappingPropertiesCallback callbacks with their respective data.
 * Raw types are used to preserve the SPI and ensure the compatibility with tWAS.
 */
public class WSMappingCallbackHandlerTest {

    private final Mockery mockery = new JUnit4Mockery();

    private WSMappingCallbackHandler callbackHandler;
    @SuppressWarnings("rawtypes")
    private Map properties;
    private ManagedConnectionFactory managedConnectionFactory;
    private Callback[] callbacks;

    @SuppressWarnings("rawtypes")
    @Before
    public void setUp() {
        properties = new HashMap();
        managedConnectionFactory = mockery.mock(ManagedConnectionFactory.class);

        callbackHandler = new WSMappingCallbackHandler(properties, managedConnectionFactory);
    }

    @Test
    public void testHandleWithWSManagedConnectionFactoryCallback() throws Exception {
        callbacks = new Callback[1];
        callbacks[0] = new WSManagedConnectionFactoryCallback("Target ManagedConnectionFactory: ");
        callbackHandler.handle(callbacks);

        assertWSManagedConnectionFactoryCallback(callbacks[0]);
    }

    @Test
    public void testHandleWithWSMappingPropertiesCallback() throws Exception {
        callbacks = new Callback[1];
        callbacks[0] = new WSMappingPropertiesCallback("Mapping Properties (HashMap): ");
        callbackHandler.handle(callbacks);

        assertWSMappingPropertiesCallback(callbacks[0]);
    }

    @Test
    public void testHandleWithAllSupportedCallbacks() throws Exception {
        callbacks = new Callback[2];
        callbacks[0] = new WSManagedConnectionFactoryCallback("Target ManagedConnectionFactory: ");
        callbacks[1] = new WSMappingPropertiesCallback("Mapping Properties (HashMap): ");
        callbackHandler.handle(callbacks);

        assertWSManagedConnectionFactoryCallback(callbacks[0]);
        assertWSMappingPropertiesCallback(callbacks[1]);
    }

    private void assertWSManagedConnectionFactoryCallback(Callback callback) {
        assertEquals("The ManagedConnectionFactory must be set in the callback.", managedConnectionFactory,
                     ((WSManagedConnectionFactoryCallback) callback).getManagedConnectionFacotry());
    }

    private void assertWSMappingPropertiesCallback(Callback callback) {
        assertEquals("The properties must be set in the callback.", properties, ((WSMappingPropertiesCallback) callback).getProperties());
    }

    @Test
    public void testHandleWithUnsupportedCallbackThrowsException() throws Exception {
        callbacks = new Callback[1];
        try {
            callbacks[0] = new WSAppContextCallback("An unknown callback: ");
            callbackHandler.handle(callbacks);
            fail("The handle method must throw an UnsupportedCallbackException for the unsuported callback.");
        } catch (UnsupportedCallbackException e) {
            // TODO: Test for the serviceability message in the log.
            assertNotNull("The exception message must be set.", e.getMessage());
//            assertEquals("The UnsupportedCallbackException message must be set.",
//                         "CWWKSxxxxW: The WSMappingCallbackHandler SPI cannot handle the unsupported callback " + callbacks[0].toString() + ".",
//                         e.getMessage());
        }
    }

}
