/*******************************************************************************
 * Copyright (c) 2020,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.suite.core;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class DDValidAppPartialHeaderTests extends CommonTests_Core {
    public static final Class<?> TEST_CLASS = DDValidAppPartialHeaderTests.class;

    // Expected:
    // CWWCK27789W: The deployment descriptor Test.ear : META-INF/application.xml, at line 2, with version 5, specifies namespace http://junk but should have namespace http://java.sun.com/xml/ns/javaee.
    // CWWCK27789W: The deployment descriptor Test.ear : META-INF/permissions.xml, at line 2, with version 7, specifies namespace http://junk but should have namespace http://xmlns.jcp.org/xml/ns/javaee.
    // CWWCK27789W: The deployment descriptor Test.ear : ServletTest.war : WEB-INF/web.xml, at line 2, with version 3.0, specifies namespace http://junk but should have namespace http://java.sun.com/xml/ns/javaee.
    // CWWCK27789W: The deployment descriptor Test.ear : EJBTest.jar : META-INF/ejb-jar.xml, at line 2, with version 2.1, specifies namespace http://junk but should have namespace http://java.sun.com/xml/ns/j2ee.
    // CWWCK27788W: The deployment descriptor Test.ear : ServletTest.war : WEB-INF/ibm-web-bnd.xml, at line 2, specifies namespace http://junk but should have namespace http://websphere.ibm.com/xml/ns/javaee.
    // CWWCK27788W: The deployment descriptor Test.ear : ServletTest.war : WEB-INF/ibm-web-ext.xml, at line 2, specifies namespace http://junk but should have namespace http://websphere.ibm.com/xml/ns/javaee.
    // CWWCK27788W: The deployment descriptor Test.ear : ServletTest.war : WEB-INF/ibm-web-ext.xml, at line 2, specifies namespace http://junk but should have namespace http://websphere.ibm.com/xml/ns/javaee.
    // CWWCK27788W: The deployment descriptor Test.ear : META-INF/ibm-application-ext.xml, at line 2, specifies namespace http://junk but should have namespace http://websphere.ibm.com/xml/ns/javaee.
    // CWWCK27788W: The deployment descriptor Test.ear : META-INF/ibm-application-ext.xml, at line 2, specifies namespace http://junk but should have namespace http://websphere.ibm.com/xml/ns/javaee.
    // CWWCK27788W: The deployment descriptor Test.ear : META-INF/ibm-application-bnd.xml, at line 2, specifies namespace http://junk but should have namespace http://websphere.ibm.com/xml/ns/javaee.
    // CWWCK27788W: The deployment descriptor Test.ear : ServletTest.war : WEB-INF/ibm-web-ext.xml, at line 2, specifies namespace http://junk but should have namespace http://websphere.ibm.com/xml/ns/javaee.
    // CWWCK27788W: The deployment descriptor Test.ear : ServletTest.war : WEB-INF/ibm-web-bnd.xml, at line 2, specifies namespace http://junk but should have namespace http://websphere.ibm.com/xml/ns/javaee.
    // CWWCK27788W: The deployment descriptor Test.ear : META-INF/ibm-application-ext.xml, at line 2, specifies namespace http://junk but should have namespace http://websphere.ibm.com/xml/ns/javaee.
    // CWWCK27788W: The deployment descriptor Test.ear : EJBTest.jar : META-INF/ibm-managed-bean-bnd.xml, at line 1, specifies namespace http://junk but should have namespace http://websphere.ibm.com/xml/ns/javaee.
    // CWWCK27788W: The deployment descriptor Test.ear : ServletTest.war : WEB-INF/ibm-ws-bnd.xml, at line 2, specifies namespace http://junk but should have namespace http://websphere.ibm.com/xml/ns/javaee.
    
    public static final String[] ALLOWED_ERRORS = new String[] {
            "CWWCK27788W",
            "CWWCK27789W"
    };

    @BeforeClass
    public static void setUp() throws Exception {
        commonSetUp(TEST_CLASS, "server_app.xml", setUpTestAppPartialHeaders);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        commonTearDown(TEST_CLASS, tearDownTestApp, ALLOWED_ERRORS);
    }

    //

    @Test
    public void testAutoInstall_Application_PH() throws Exception {
        test(TEST_CLASS, "testAutoInstall");    
    }

    @Test
    public void testConfig_Application_PH() throws Exception {
        test(TEST_CLASS, "testConfig");
    }

    @Test
    public void testBindingsConfig_Application_PH() throws Exception {
        test(TEST_CLASS, "testBindingsConfig");
    }
    
    @Test
    public void testSecurityRoleOverrides_Application_PH() throws Exception {
        test(TEST_CLASS, "testSecurityRoleOverrides");
    }
    
    @Test
    public void testWebExtension_Application_PH() throws Exception {
        test(TEST_CLASS, "testWebExtension");
    }
    
    @Test
    public void testWebBindings_Application_PH() throws Exception {
        test(TEST_CLASS, "testWebBindings");
    }
    
    @Test
    public void testEJBBindings_Application_PH() throws Exception {
        test(TEST_CLASS, "testEJBBindings");
    }
    
    @Test
    public void testEJBExtension_Application_PH() throws Exception {
        test(TEST_CLASS, "testEJBExtension");
    }
    
    @Test
    public void testApplicationExtension_Application_PH() throws Exception {
        test(TEST_CLASS, "testApplicationExtension");
    }
    
    @Test
    public void testManagedBeanBindings_Application_PH() throws Exception {
        test(TEST_CLASS, "testManagedBeanBindings");
    }
    
    @Test
    public void testWebserviceBindings_Application_PH() throws Exception {
        test(TEST_CLASS, "testWebserviceBindings");
    }
}
