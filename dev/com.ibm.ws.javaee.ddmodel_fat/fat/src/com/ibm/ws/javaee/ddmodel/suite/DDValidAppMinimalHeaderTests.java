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
    public void testAutoInstall_Application() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testAutoInstall");    
    }

    @Test
    public void testConfigurationSide_Application() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testConfigurationSide");
    }

    @Test
    public void testBasicBindingConfiguration_Application() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testBasicBindingConfiguration");
    }
    
    @Test
    public void testSecurityRoleOverrides_Application() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testSecurityRoleOverrides");
    }
    
    @Test
    public void testWebExtensions_Application() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testWebExtensions");
    }
    
    @Test
    public void testWebBindings_Application() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testWebBindings");
    }
    
    @Test
    public void testEJBBindings_Application() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testEJBBindings");
    }
    
    @Test
    public void testEJBExtensions_Application() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testEJBExtensions");
    }
    
    @Test
    public void testApplicationExtensions_Application() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testApplicationExtensions");
    }
    
    @Test
    public void testManagedBeanBindings_Application() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testManagedBeanBindings");
    }
    
    @Test
    public void testWebserviceBindings_Application() throws Exception {
        CommonTests.test(DDValidAppMinimalHeaderTests.class, "testWebserviceBindings");
    }
}
