/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.bval.v20.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import bval.v20.cdi.web.BeanValCDIServlet;
import bval.v20.multixml.web.BeanValidationTestServlet;
import bval.v20.valueextractor.web.ValueExtractorServlet;
import bval.v20.web.BeanVal20TestServlet;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class BeanVal20Test extends FATServletClient {

    public static final String REG_APP = "bvalApp";
    public static final String CDI_APP = "bvalCDIApp";
    public static final String MULTI_VAL_APP = "MultipleValidationXmlWeb";
    public static final String VAL_EXT_APP = "bvalValueExtractorApp";

    @Server("beanval.v20_fat")
    @TestServlets({
                    @TestServlet(servlet = BeanVal20TestServlet.class, contextRoot = REG_APP),
                    @TestServlet(servlet = BeanValCDIServlet.class, contextRoot = CDI_APP),
                    @TestServlet(servlet = BeanValidationTestServlet.class, contextRoot = MULTI_VAL_APP),
                    @TestServlet(servlet = ValueExtractorServlet.class, contextRoot = VAL_EXT_APP)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive multiValXmlEjb1 = ShrinkHelper.buildJavaArchive("MultipleValidationXmlEjb1.jar", "bval.v20.ejb1.*");

        JavaArchive multiValXmlEjb2 = ShrinkHelper.buildJavaArchive("MultipleValidationXmlEjb2.jar", "bval.v20.ejb2.*");

        WebArchive multiValXmlWar = ShrinkHelper.buildDefaultApp("MultipleValidationXmlWeb.war", "bval.v20.multixml.*");

        EnterpriseArchive multiValXmlEar = ShrinkWrap.create(EnterpriseArchive.class, "MultipleValidationXmlEjb.ear");
        multiValXmlEar.addAsModule(multiValXmlEjb1);
        multiValXmlEar.addAsModule(multiValXmlEjb2);
        multiValXmlEar.addAsModule(multiValXmlWar);
        ShrinkHelper.addDirectory(multiValXmlEar, "test-applications/MultipleValidationXmlEjb.ear/resources");

        ShrinkHelper.defaultDropinApp(server, REG_APP, "bval.v20.web");
        ShrinkHelper.defaultDropinApp(server, CDI_APP, "bval.v20.cdi.web");
        ShrinkHelper.defaultDropinApp(server, VAL_EXT_APP, "bval.v20.valueextractor.web");
        ShrinkHelper.exportToServer(server, "dropins", multiValXmlEar);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server but ignore any ConstraintViolationExceptions thrown by validation tests
        server.stopServer("ConstraintViolationException");
    }
}
