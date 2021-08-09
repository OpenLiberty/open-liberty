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
package io.openliberty.restfulWS30.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.restfulWS30.fat.xml.XmlWithoutJaxbTestServlet;

/**
 * Tests whether the server fails appropriately when the built-in JAXB Provider is not used when the {@code jaxb-3.0} feature is not enabled.
 */
@AllowedFFDC
@RunWith(FATRunner.class)
public class XmlWithoutJaxbTest extends FATServletClient {

    public static final String APP_NAME = "xml";
    public static final String SERVER_NAME = APP_NAME + "-without-jaxb";

    @Server(SERVER_NAME)
    @TestServlet(servlet = XmlWithoutJaxbTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, XmlWithoutJaxbTestServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        //TODO: investigate CDI scope errors and remove from stopServer method once resolved:
        //E SRVE0271E: Uncaught init() exception created by servlet [io.openliberty.restfulWS30.fat.appandresource.AppAndResource] in application [appandresource]: org.jboss.weld.contexts.ContextNotActiveException: WELD-001303: No active contexts for scope type jakarta.enterprise.context.RequestScoped
        //E SRVE0276E: Error while initializing Servlet [io.openliberty.restfulWS30.fat.appandresource.AppAndResource]: jakarta.servlet.ServletException: SRVE0207E: Uncaught initialization exception created by servlet
        server.stopServer("SRVE0271E", "SRVE0276E");
    }

}
