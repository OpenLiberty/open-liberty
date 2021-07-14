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

public class DDValidModPartialHeaderTests {

    public static final String[] ALLOWED_ERRORS = new String[] {
        // EMPTY
    };

    @BeforeClass
    public static void setUp() throws Exception {
        CommonTests.commonSetUp(
            DDValidModPartialHeaderTests.class,
            "server_mod.xml",
            CommonTests.setUpTestModulesPartialHeaders);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        CommonTests.commonTearDown(
            DDValidModPartialHeaderTests.class,
            CommonTests.tearDownTestModules,
            ALLOWED_ERRORS);
    }

    //

    @Test
    public void testAutoInstall_Module_PH() throws Exception {
        CommonTests.test(DDValidModPartialHeaderTests.class, "testAutoInstall");
    }

    @Test
    public void testBindingsConfig_Module_PH() throws Exception {
        CommonTests.test(DDValidModPartialHeaderTests.class, "testBindingsConfig");
    }   
    
    @Test
    public void testWebExtensions_Module_PH() throws Exception {
        CommonTests.test(DDValidModPartialHeaderTests.class, "testWebExtensions");
    }
    
    @Test
    public void testWebBindings_Module_PH() throws Exception {
        CommonTests.test(DDValidModPartialHeaderTests.class, "testWebBindings");
    }
    
    @Test
    public void testEJBBindings_Module_PH() throws Exception {
        CommonTests.test(DDValidModPartialHeaderTests.class, "testEJBBindings");
    }
    
    @Test
    public void testEJBExtensions_Module_PH() throws Exception {
        CommonTests.test(DDValidModPartialHeaderTests.class, "testEJBExtensions");
    }
    
    @Test
    public void testAppExtensionFromWebApp_Module_PH() throws Exception {
        CommonTests.test(DDValidModPartialHeaderTests.class, "testAppExtensionFromWebApp");
    }
    
    @Test
    public void testSecurityRoleOverridesFromWebApp_Module_PH() throws Exception {
        CommonTests.test(DDValidModPartialHeaderTests.class, "testSecurityRoleOverridesFromWebApp");
    }
    
    @Test
    public void testManagedBeanBindings_Module_PH() throws Exception {
        CommonTests.test(DDValidModPartialHeaderTests.class, "testManagedBeanBindings");
    }
}
