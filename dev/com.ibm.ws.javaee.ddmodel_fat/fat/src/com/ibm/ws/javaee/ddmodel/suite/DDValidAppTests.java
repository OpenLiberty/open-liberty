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

public class DDValidAppTests {

    public static final String[] ALLOWED_ERRORS = new String[] {
        // EMPTY
    };

    @BeforeClass
    public static void setUp() throws Exception {
        CommonTests.commonSetUp(
            DDValidAppTests.class,
            "server_app.xml",
            CommonTests.setUpTestApp);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        CommonTests.commonTearDown(
            DDValidAppTests.class,
            CommonTests.tearDownTestApp,
            ALLOWED_ERRORS);
    }

    //

    @Test
    public void testAutoInstall_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testAutoInstall");    
    }

    @Test
    public void testConfig_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testConfig");
    }

    @Test
    public void testBindingsConfig_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testBindingsConfig");
    }
    
    @Test
    public void testSecurityRoleOverrides_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testSecurityRoleOverrides");
    }
    
    @Test
    public void testWebExtension_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testWebExtension");
    }
    
    @Test
    public void testWebExtensionNoBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testWebExtensionNoBindings");
    }
    
    @Test
    public void testWebBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testWebBindings");
    }
    
    @Test
    public void testWebBindingsNoBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testWebBindingsNoBindings");
    }
    
    @Test
    public void testEJBBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testEJBBindings");
    }
    
    @Test
    public void testEJBExtension_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testEJBExtension");
    }
    
    @Test
    public void testEJBBindingsNoBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testEJBBindingsNoBindings");
    }
    
    @Test
    public void testEJBExtensionNoBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testEJBExtensionNoBindings");
    }
    
    @Test
    public void testApplicationExtension_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testApplicationExtension");
    }
    
    @Test
    public void testManagedBeanBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testManagedBeanBindings");
    }
    
    @Test
    public void testWebserviceBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testWebserviceBindings");
    }
    @Test
    public void testWebserviceBindingsNoBindings_Application() throws Exception {
        CommonTests.test(DDValidAppTests.class, "testWebserviceBindingsNoBindings");
    }
}
