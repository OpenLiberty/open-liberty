/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.jee.webservices;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.jee.FATSuite;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class CDI12WebServicesTest extends FATServletClient {

    public static final String APP_NAME = "resourceWebServices";
    public static final String SERVER_NAME = "cdi12WebServicesServer";

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @Server(SERVER_NAME)
    @TestServlet(servlet = com.ibm.ws.cdi.jee.webservices.client.TestWebServicesServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive resourceWebServicesProvider = ShrinkWrap.create(WebArchive.class,
                                                                   APP_NAME + "Provider.war");
        Package providerPackage = com.ibm.ws.cdi.jee.webservices.provider.SayHelloService.class.getPackage();
        resourceWebServicesProvider.addPackage(providerPackage);
        resourceWebServicesProvider.addAsWebInfResource(providerPackage, "web.xml", "web.xml");

        WebArchive resourceWebServicesClient = ShrinkWrap.create(WebArchive.class,
                                                                 APP_NAME + ".war");
        resourceWebServicesClient.addClass(com.ibm.ws.cdi.jee.webservices.client.TestWebServicesServlet.class);
        resourceWebServicesClient.addPackage(com.ibm.ws.cdi.jee.webservices.client.services.SayHelloPojoService.class.getPackage());
        resourceWebServicesClient.addAsManifestResource(com.ibm.ws.cdi.jee.webservices.client.services.SayHelloPojoService.class.getPackage(),
                                                        "EmployPojoService.wsdl",
                                                        "resources/wsdl/EmployPojoService.wsdl");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        ear.addAsModule(resourceWebServicesClient);
        ear.addAsModule(resourceWebServicesProvider);

        ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}
