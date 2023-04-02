/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.restfulWS30.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.restfulWS30.fat.restfulwsprototype.RestfulWsPrototypeClientTestServlet;

/*
 * The purpose of this test is to provide an empty canvas for rapid/easy test experimentation,
 * as well as providing and example of FAT best practices.
 *
 * This Test should never have any real tests, if you use this Test to create a test that should
 * be added permanently, create a new FAT Test using this test as a template.
 */
@RunWith(FATRunner.class)
public class RestfulWsPrototypeTest extends FATServletClient {

    public static final String APP_NAME = "restfulwsprototype";
    public static final String SERVER_NAME = APP_NAME;
    
    // If needed, third party libs are copied to ${buildDir}/autoFVT/appLibs/prototype in build.gradle
//    private static final String libs = "appLibs/prototype";


    @Server(SERVER_NAME)
    @TestServlet(servlet = RestfulWsPrototypeClientTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Build an application
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, RestfulWsPrototypeClientTestServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);
        
        // Build an application, add third party libs, and manually export to the dropins directory
//      WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "restfulwsprototype");
//      app.addAsLibraries(new File(libs).listFiles());
//      ShrinkHelper.exportDropinAppToServer(server, app);
//      server.addInstalledAppForValidation(APP_NAME);


        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer("CWWKE1102W");  //ignore server quiesce timeouts due to slow test machines
        }
    }
    
    @Before
    public void beforeTest() {}

    @After
    public void afterTest() {}

}
