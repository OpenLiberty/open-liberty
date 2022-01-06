/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.suite;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DDValidAppMinimalHeaderTests {

    public static final String[] ALLOWED_ERRORS = new String[] {
        // EMPTY
    };

    @BeforeClass
    public static void setUp() throws Exception {
        CommonTests.commonSetUp(
            DDValidAppMinimalHeaderTests.class,
            "server_app.xml",
            CommonTests.setUpTestAppMinimalHeaders);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        CommonTests.commonTearDown(
            DDValidAppMinimalHeaderTests.class,
            CommonTests.tearDownTestApp,
            ALLOWED_ERRORS);
    }

    //

    @Test
    public void testAutoInstall_Application_MH() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testAutoInstall");    
    }

    @Test
    public void testConfig_Application_MH() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testConfig");
    }

    @Test
    public void testBindingsConfig_Application_MH() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testBindingsConfig");
    }

    @Test
    public void testSecurityRoleOverrides_Application_MH() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testSecurityRoleOverrides");
    }

    @Test
    public void testWebExtension_Application_MH() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testWebExtension");
    }

    @Test
    public void testWebBindings_Application_MH() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testWebBindings");
    }
    
    @Test
    public void testEJBBindings_Application_MH() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testEJBBindings");
    }
    
    @Test
    public void testEJBExtension_Application_MH() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testEJBExtension");
    }
    
    @Test
    public void testApplicationExtension_Application_MH() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testApplicationExtension");
    }
    
    @Test
    public void testManagedBeanBindings_Application_MH() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testManagedBeanBindings");
    }
    
    @Test
    public void testWebserviceBindings_Application_MH() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testWebserviceBindings");
    }
}
