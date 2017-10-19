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

package com.ibm.ws.jpa;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import cdi.web.ELIServlet;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class JPAELInjectionFATTest {
    public static final String APP_NAME = "cdi";
    public static final String SERVLET = "eli";

    @Server("JPA22FATServer")
    @TestServlet(servlet = ELIServlet.class, path = APP_NAME + "/" + SERVLET)
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Include a simple index.jsp static file in the root of the WebArchive
        WebArchive app1 = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "cdi.web")
                        .addPackages(true, "cdi.model")
                        .addAsWebInfResource(new File("test-applications/" + APP_NAME + "/resources/META-INF/persistence.xml"), "classes/META-INF/persistence.xml")
                        .addAsWebInfResource(new File("test-applications/" + APP_NAME + "/resources/WEB-INF/beans.xml"))
                        .addAsWebInfResource(new File("test-applications/" + APP_NAME + "/resources/index.jsp"));
        // Write the WebArchive to 'publish/servers/FATServer/apps/app1.war' and print the contents
        ShrinkHelper.exportDropinAppToServer(server1, app1);
//        ShrinkHelper.exportAppToServer(server1, app1);

        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getFeatureManager().getFeatures().add("cdi-2.0");
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();
        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer();
    }
}
