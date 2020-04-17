/*******************************************************************************
 * Copyright (c) 2011,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.context;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.context.app.ContextServiceTestServlet;

@RunWith(FATRunner.class)
public class ContextServiceTest extends FATServletClient {
    @Server("com.ibm.ws.context.fat.customproviders")
    @TestServlet(servlet = ContextServiceTestServlet.class, path = "contextbvt/ContextServiceTestServlet")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive contextbvtApp = ShrinkWrap.create(WebArchive.class, "contextbvt.war")
                        .addPackage("test.context.app") //
                        .addAsWebInfResource(new File("test-applications/contextbvt/resources/WEB-INF/web.xml"));
        ShrinkHelper.exportDropinAppToServer(server, contextbvtApp);

        server.addInstalledAppForValidation("contextbvt");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
