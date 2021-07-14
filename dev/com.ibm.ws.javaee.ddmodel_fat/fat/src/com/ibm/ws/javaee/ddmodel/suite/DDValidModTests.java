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

public class DDValidModTests {

    public static final String[] ALLOWED_ERRORS = new String[] {
        // EMPTY
    };

    @BeforeClass
    public static void setUp() throws Exception {
        CommonTests.commonSetUp(
            DDValidModTests.class,
            "server_mod.xml",
            CommonTests.setUpTestModules);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        CommonTests.commonTearDown(
            DDValidModTests.class,
            CommonTests.tearDownTestModules,
            ALLOWED_ERRORS);
    }

    //

    @Test
    public void testAutoInstall_Module() throws Exception {
        CommonTests.test(DDValidModTests.class, "testAutoInstall");    
    }

    @Test
    public void testBindingsConfig_Module() throws Exception {
        CommonTests.test(DDValidModTests.class, "testBindingsConfig");
    }   
    
    @Test
    public void testWebExtensions_Module() throws Exception {
        CommonTests.test(DDValidModTests.class, "testWebExtensions");
    }
    
    @Test
    public void testWebExtensionsNoBindings_Module() throws Exception {
        CommonTests.test(DDValidModTests.class, "testWebExtensionsNoBindings");
    }
    
    @Test
    public void testWebBindings_Module() throws Exception {
        CommonTests.test(DDValidModTests.class, "testWebBindings");
    }
    
    @Test
    public void testWebBindingsNoBindings_Module() throws Exception {
        CommonTests.test(DDValidModTests.class, "testWebBindingsNoBindings");
    }
    
    @Test
    public void testEJBBindings_Module() throws Exception {
        CommonTests.test(DDValidModTests.class, "testEJBBindings");
    }
    
    @Test
    public void testEJBExtensions_Module() throws Exception {
        CommonTests.test(DDValidModTests.class, "testEJBExtensions");
    }
    
    @Test
    public void testEJBBindingsNoBindings_Module() throws Exception {
        CommonTests.test(DDValidModTests.class, "testEJBBindingsNoBindings");
    }
    
    @Test
    public void testEJBExtensionsNoBindings_Module() throws Exception {
        CommonTests.test(DDValidModTests.class, "testEJBExtensionsNox");
    }
    
    @Test
    public void testApplicationExtensionFromWebApp_Module() throws Exception {
        CommonTests.test(DDValidModTests.class, "testApplicationExtensionFromWebApp");
    }
    
    @Test
    public void testSecurityRoleOverridesFromWebApp_Module() throws Exception {
        CommonTests.test(DDValidModTests.class, "testSecurityRoleOverridesFromWebApp");
    }
    
    @Test
    public void testManagedBeanBindings_Module() throws Exception {
        CommonTests.test(DDValidModTests.class, "testManagedBeanBindings");
    }
}
