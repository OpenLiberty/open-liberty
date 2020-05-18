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

import static com.ibm.websphere.simplicity.ShrinkHelper.defaultDropinApp;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import bval.v20.hibernateconfig.web.BeanValidationServlet;
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
    public static final String HIBERNATE_DEFAULT_APP = "HibernateConfig";
    public static final String CUSTOM_PROVIDER_APP = "customBvalProviderApp";

    private static final String CUSTOM_PROVIDER_CDI_WARNING = "CWNBV0200W.*" + CUSTOM_PROVIDER_APP;

    @Server("beanval.v20_fat")
    @TestServlets({
                    @TestServlet(servlet = BeanVal20TestServlet.class, contextRoot = REG_APP),
                    //TODO: Once CDI 3.0 and EJBLite 4.0 are ready, enable these four applications for Jakarta EE 9.
//                    @TestServlet(servlet = BeanValCDIServlet.class, contextRoot = CDI_APP),
//                    @TestServlet(servlet = BeanValidationTestServlet.class, contextRoot = MULTI_VAL_APP),
//                    @TestServlet(servlet = ValueExtractorServlet.class, contextRoot = VAL_EXT_APP),
                    @TestServlet(servlet = BeanValidationServlet.class, contextRoot = HIBERNATE_DEFAULT_APP),
//                    @TestServlet(servlet = CustomProviderTestServlet.class, contextRoot = CUSTOM_PROVIDER_APP)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        //TODO: Delete these two lines once CDI 3.0 and EJBLite 4.0 are ready
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        //TODO: Once CDI 3.0 and EJBLite 4.0 are ready, enable these four applications for Jakarta EE 9.
//        EnterpriseArchive multiValXmlEar = ShrinkWrap.create(EnterpriseArchive.class, "MultipleValidationXmlEjb.ear")
//                        .addAsModule(buildJavaArchive("MultipleValidationXmlEjb1.jar", "bval.v20.ejb1.*"))
//                        .addAsModule(buildJavaArchive("MultipleValidationXmlEjb2.jar", "bval.v20.ejb2.*"))
//                        .addAsModule(buildDefaultApp("MultipleValidationXmlWeb.war", "bval.v20.multixml.*"));
//        addDirectory(multiValXmlEar, "test-applications/MultipleValidationXmlEjb.ear/resources");
//        exportToServer(server, "dropins", multiValXmlEar);
//        defaultDropinApp(server, CDI_APP, "bval.v20.cdi.web");

        defaultDropinApp(server, REG_APP, "bval.v20.web");
//        defaultDropinApp(server, VAL_EXT_APP, "bval.v20.valueextractor.web");
        defaultDropinApp(server, HIBERNATE_DEFAULT_APP, "bval.v20.hibernateconfig.web");
//        WebArchive war = ShrinkHelper.buildDefaultApp(CUSTOM_PROVIDER_APP, "bval.v20.customprovider");
//        ShrinkHelper.exportDropinAppToServer(server, war);
//        defaultDropinApp(server, CUSTOM_PROVIDER_APP, "bval.v20.customprovider");

        server.startServer();

//      TODO: re-enable the CUSTOM_PROVIDER_CDI_WARNING once CDI 3.0 is enabled for Jakarta EE 9
//        // Verify that we get CWNBV0200W for CUSTOM_PROVIDER_APP at deployment time
//        Assert.assertNotNull("Expected to find warning at server start time: " + CUSTOM_PROVIDER_CDI_WARNING,
//                             server.waitForStringInLog(CUSTOM_PROVIDER_CDI_WARNING));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server but ignore any ConstraintViolationExceptions thrown by validation tests
        server.stopServer("ConstraintViolationException");
//        TODO: re-enable the CUSTOM_PROVIDER_CDI_WARNING once CDI 3.0 is enabled for Jakarta EE 9
//        server.stopServer("ConstraintViolationException",
//                          CUSTOM_PROVIDER_CDI_WARNING);
    }
}
