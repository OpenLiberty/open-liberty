/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.identitystore;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.openliberty.security.jakartasec.identitystore.permissions.IdentityStorePermissionService;

/**
 * Test for the IdentityStorePermissionService
 */
public class IdentityStorePermissionServiceTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
    }

    /**
     * Test the implementation for Jakarta Security 1/2/3.0
     */

    @Test
    public void testIdentityStorePermissionService() {

        // should not throw exceptions
        IdentityStorePermissionService.checkPermission("getGroups");
        IdentityStorePermissionService.checkPermission("someOtherPermission", "read");
    }
}
