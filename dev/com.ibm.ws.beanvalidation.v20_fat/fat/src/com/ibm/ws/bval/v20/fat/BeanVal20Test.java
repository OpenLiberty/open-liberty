/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.bval.v20.fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.addDirectory;
import static com.ibm.websphere.simplicity.ShrinkHelper.buildDefaultApp;
import static com.ibm.websphere.simplicity.ShrinkHelper.buildJavaArchive;
import static com.ibm.websphere.simplicity.ShrinkHelper.defaultDropinApp;
import static com.ibm.websphere.simplicity.ShrinkHelper.exportToServer;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import bval.v20.cdi.web.BeanValCDIServlet;
import bval.v20.customprovider.CustomProviderTestServlet;
import bval.v20.hibernateconfig.web.BeanValidationServlet;
import bval.v20.multixml.web.BeanValidationTestServlet;
import bval.v20.valueextractor.web.ValueExtractorServlet;
import bval.v20.web.BeanVal20TestServlet;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class BeanVal20Test extends FATServletClient {

    public static final String REG_APP = "bvalApp";
    public static final String CDI_APP = "bvalCDIApp";
    public static final String MULTI_VAL_APP = "MultipleValidationXmlWeb";
    public static final String VAL_EXT_APP = "bvalValueExtractorApp";
    public static final String HIBERNATE_DEFAULT_APP = "HibernateConfig";
    public static final String CUSTOM_PROVIDER_APP = "customBvalProviderApp";

    private static final String CUSTOM_PROVIDER_CDI_WARNING = "CWNBV0200W.*" + CUSTOM_PROVIDER_APP;

    @Server("beanval.v20_fat")
    @TestServlets({
                    @TestServlet(servlet = BeanVal20TestServlet.class, contextRoot = REG_APP),
                    @TestServlet(servlet = BeanValCDIServlet.class, contextRoot = CDI_APP),
                    @TestServlet(servlet = BeanValidationTestServlet.class, contextRoot = MULTI_VAL_APP),
                    @TestServlet(servlet = ValueExtractorServlet.class, contextRoot = VAL_EXT_APP),
                    @TestServlet(servlet = BeanValidationServlet.class, contextRoot = HIBERNATE_DEFAULT_APP),
                    @TestServlet(servlet = CustomProviderTestServlet.class, contextRoot = CUSTOM_PROVIDER_APP)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive multipleValidationXmlEjb1_jar = buildJavaArchive("MultipleValidationXmlEjb1.jar", "bval.v20.ejb1.*");
        JavaArchive multipleValidationXmlEjb2_jar = buildJavaArchive("MultipleValidationXmlEjb2.jar", "bval.v20.ejb2.*");
        WebArchive multipleValidationXmlWeb_war = buildDefaultApp("MultipleValidationXmlWeb.war", "bval.v20.multixml.*");

        if (JakartaEE9Action.isActive()) {
            multipleValidationXmlEjb1_jar.move("/META-INF/constraints/constraints-house_EE9.xml", "/META-INF/constraints/constraints-house.xml");
            multipleValidationXmlEjb2_jar.move("/META-INF/constraints/constraints-house_EE9.xml", "/META-INF/constraints/constraints-house.xml");
            multipleValidationXmlWeb_war.move("/WEB-INF/constraints3/constraints-house_EE9.xml", "/WEB-INF/constraints3/constraints-house.xml");
        }
        EnterpriseArchive multiValXmlEar = ShrinkWrap.create(EnterpriseArchive.class, "MultipleValidationXmlEjb.ear")
                        .addAsModule(multipleValidationXmlEjb1_jar)
                        .addAsModule(multipleValidationXmlEjb2_jar)
                        .addAsModule(multipleValidationXmlWeb_war);
        addDirectory(multiValXmlEar, "test-applications/MultipleValidationXmlEjb.ear/resources");
        exportToServer(server, "dropins", multiValXmlEar);

        defaultDropinApp(server, REG_APP, "bval.v20.web");
        defaultDropinApp(server, CDI_APP, "bval.v20.cdi.web");
        defaultDropinApp(server, VAL_EXT_APP, "bval.v20.valueextractor.web");
        defaultDropinApp(server, HIBERNATE_DEFAULT_APP, "bval.v20.hibernateconfig.web");
        WebArchive war = ShrinkHelper.buildDefaultApp(CUSTOM_PROVIDER_APP, "bval.v20.customprovider");
        ShrinkHelper.exportDropinAppToServer(server, war);
        defaultDropinApp(server, CUSTOM_PROVIDER_APP, "bval.v20.customprovider");

        server.startServer();

        // Verify that we get CWNBV0200W for CUSTOM_PROVIDER_APP at deployment time
        Assert.assertNotNull("Expected to find warning at server start time: " + CUSTOM_PROVIDER_CDI_WARNING,
                             server.waitForStringInLog(CUSTOM_PROVIDER_CDI_WARNING));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server but ignore any ConstraintViolationExceptions thrown by validation tests
        server.stopServer("ConstraintViolationException",
                          CUSTOM_PROVIDER_CDI_WARNING);
    }
}
