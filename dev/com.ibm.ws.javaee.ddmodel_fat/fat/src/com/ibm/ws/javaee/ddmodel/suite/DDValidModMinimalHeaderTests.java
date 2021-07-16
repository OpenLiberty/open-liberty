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

public class DDValidModMinimalHeaderTests {

    public static final String[] ALLOWED_ERRORS = new String[] {
        // EMPTY
    };

    @BeforeClass
    public static void setUp() throws Exception {
        CommonTests.commonSetUp(
            DDValidModMinimalHeaderTests.class,
            "server_mod.xml",
            CommonTests.setUpTestModulesMinimalHeaders);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        CommonTests.commonTearDown(
            DDValidModMinimalHeaderTests.class,
            CommonTests.tearDownTestModules,
            ALLOWED_ERRORS);
    }

    //

    @Test
    public void testAutoInstall_Module_MH() throws Exception {
        CommonTests.test(DDValidModMinimalHeaderTests.class, "testAutoInstall");    
    }

    @Test
    public void testBindingsConfig_Module_MH() throws Exception {
        CommonTests.test(DDValidModMinimalHeaderTests.class, "testBindingsConfig");
    }   
    
    @Test
    public void testWebExtension_Module_MH() throws Exception {
        CommonTests.test(DDValidModMinimalHeaderTests.class, "testWebExtension");
    }
    
    @Test
    public void testWebBindings_Module_MH() throws Exception {
        CommonTests.test(DDValidModMinimalHeaderTests.class, "testWebBindings");
    }
    
    @Test
    public void testEJBBindings_Module_MH() throws Exception {
        CommonTests.test(DDValidModMinimalHeaderTests.class, "testEJBBindings");
    }
    
    @Test
    public void testEJBExtension_Module_MH() throws Exception {
        CommonTests.test(DDValidModMinimalHeaderTests.class, "testEJBExtension");
    }
    
    @Test
    public void testApplicationExtensionFromWebApp_Module_MH() throws Exception {
        CommonTests.test(DDValidModMinimalHeaderTests.class, "testApplicationExtensionFromWebApp");
    }
    
    @Test
    public void testSecurityRoleOverridesFromWebApp_Module_MH() throws Exception {
        CommonTests.test(DDValidModMinimalHeaderTests.class, "testSecurityRoleOverridesFromWebApp");
    }
    
    @Test
    public void testManagedBeanBindings_Module_MH() throws Exception {
        CommonTests.test(DDValidModMinimalHeaderTests.class, "testManagedBeanBindings");
    }
}
