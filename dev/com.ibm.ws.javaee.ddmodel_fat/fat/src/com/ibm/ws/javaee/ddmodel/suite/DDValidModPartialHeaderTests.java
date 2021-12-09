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

    // Expected:
    // CWWCK27789W: The deployment descriptor ServletTest.war : META-INF/permissions.xml, at line 2, with version 7, specifies namespace http://junk but should have namespace http://xmlns.jcp.org/xml/ns/javaee.
    // CWWCK27789W: The deployment descriptor EJBTest.jar : META-INF/ejb-jar.xml, at line 2, with version 2.1, specifies namespace http://junk but should have namespace http://java.sun.com/xml/ns/j2ee.
    // CWWCK27789W: The deployment descriptor ServletTest.war : WEB-INF/web.xml, at line 2, with version 3.0, specifies namespace http://junk but should have namespace http://java.sun.com/xml/ns/javaee.
    // CWWCK27788W: The deployment descriptor ServletTest.war : WEB-INF/ibm-web-bnd.xml, at line 2, specifies namespace http://junk but should have namespace http://websphere.ibm.com/xml/ns/javaee.
    // CWWCK27788W: The deployment descriptor ServletTest.war : WEB-INF/ibm-web-ext.xml, at line 2, specifies namespace http://junk but should have namespace http://websphere.ibm.com/xml/ns/javaee.
    // CWWCK27788W: The deployment descriptor ServletTest.war : WEB-INF/ibm-web-ext.xml, at line 2, specifies namespace http://junk but should have namespace http://websphere.ibm.com/xml/ns/javaee.
    // CWWCK27788W: The deployment descriptor ServletTest.war : WEB-INF/ibm-web-ext.xml, at line 2, specifies namespace http://junk but should have namespace http://websphere.ibm.com/xml/ns/javaee.
    // CWWCK27788W: The deployment descriptor ServletTest.war : WEB-INF/ibm-web-bnd.xml, at line 2, specifies namespace http://junk but should have namespace http://websphere.ibm.com/xml/ns/javaee.
    // CWWCK27788W: The deployment descriptor EJBTest.jar : META-INF/ibm-managed-bean-bnd.xml, at line 1, specifies namespace http://junk but should have namespace http://websphere.ibm.com/xml/ns/javaee.    
    
    public static final String[] ALLOWED_ERRORS = new String[] {
            "CWWCK27788W",
            "CWWCK27789W"
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
    public void testWebExtension_Module_PH() throws Exception {
        CommonTests.test(DDValidModPartialHeaderTests.class, "testWebExtension");
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
    public void testEJBExtension_Module_PH() throws Exception {
        CommonTests.test(DDValidModPartialHeaderTests.class, "testEJBExtension");
    }
    
    @Test
    public void testApplicationExtensionFromWebApp_Module_PH() throws Exception {
        CommonTests.test(DDValidModPartialHeaderTests.class, "testApplicationExtensionFromWebApp");
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
