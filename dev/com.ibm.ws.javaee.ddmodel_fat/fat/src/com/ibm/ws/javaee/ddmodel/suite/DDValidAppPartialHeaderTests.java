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

public class DDValidAppPartialHeaderTests {

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
        CommonTests.commonSetUp(
            DDValidAppPartialHeaderTests.class,
            "server_app.xml",
            CommonTests.setUpTestAppPartialHeaders);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        CommonTests.commonTearDown(
            DDValidAppPartialHeaderTests.class,
            CommonTests.tearDownTestApp,
            ALLOWED_ERRORS);
    }

    //

    @Test
    public void testAutoInstall_Application_PH() throws Exception {
        CommonTests.test(DDValidAppPartialHeaderTests.class, "testAutoInstall");    
    }

    @Test
    public void testConfig_Application_PH() throws Exception {
        CommonTests.test(DDValidAppPartialHeaderTests.class, "testConfig");
    }

    @Test
    public void testBindingsConfig_Application_PH() throws Exception {
        CommonTests.test(DDValidAppPartialHeaderTests.class, "testBindingsConfig");
    }
    
    @Test
    public void testSecurityRoleOverrides_Application_PH() throws Exception {
        CommonTests.test(DDValidAppPartialHeaderTests.class, "testSecurityRoleOverrides");
    }
    
    @Test
    public void testWebExtension_Application_PH() throws Exception {
        CommonTests.test(DDValidAppPartialHeaderTests.class, "testWebExtension");
    }
    
    @Test
    public void testWebBindings_Application_PH() throws Exception {
        CommonTests.test(DDValidAppPartialHeaderTests.class, "testWebBindings");
    }
    
    @Test
    public void testEJBBindings_Application_PH() throws Exception {
        CommonTests.test(DDValidAppPartialHeaderTests.class, "testEJBBindings");
    }
    
    @Test
    public void testEJBExtension_Application_PH() throws Exception {
        CommonTests.test(DDValidAppPartialHeaderTests.class, "testEJBExtension");
    }
    
    @Test
    public void testApplicationExtension_Application_PH() throws Exception {
        CommonTests.test(DDValidAppPartialHeaderTests.class, "testApplicationExtension");
    }
    
    @Test
    public void testManagedBeanBindings_Application_PH() throws Exception {
        CommonTests.test(DDValidAppPartialHeaderTests.class, "testManagedBeanBindings");
    }
    
    @Test
    public void testWebserviceBindings_Application_PH() throws Exception {
        CommonTests.test(DDValidAppPartialHeaderTests.class, "testWebserviceBindings");
    }
}
