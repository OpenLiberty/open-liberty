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

import javax.resource.spi.ManagedConnectionFactory;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

public class WSManagedConnectionFactoryCallbackTest {

    private static final String HINT = "Target ManagedConnectionFactory: ";

    private final Mockery mockery = new JUnit4Mockery();
    private ManagedConnectionFactory managedConnectionFactory;

    @Before
    public void setUp() {
        managedConnectionFactory = mockery.mock(ManagedConnectionFactory.class);
    }

    /*
     * Note that the interface's method is getManagedConnectionFacotry and it has to be retained
     * for compatibility with tWAS.
     */
    @Test
    public void testGetManagedConnectionFacotry() {
        WSManagedConnectionFactoryCallback callback = new WSManagedConnectionFactoryCallback(HINT);

        callback.setManagedConnectionFactory(managedConnectionFactory);
        assertEquals("The ManagedConnectionFactory must be set in the callback.", managedConnectionFactory, callback.getManagedConnectionFacotry());
    }
}
