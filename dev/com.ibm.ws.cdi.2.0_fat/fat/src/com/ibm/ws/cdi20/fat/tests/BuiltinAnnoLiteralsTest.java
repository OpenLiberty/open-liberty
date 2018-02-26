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
package com.ibm.ws.cdi20.fat.tests;

import java.io.File;

import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import builtinAnnoApp.web.BuiltinAnnoServlet;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * These tests verify the use of Built-in annotation literals in CDI2.0 as per http://docs.jboss.org/cdi/spec/2.0/cdi-spec-with-assertions.html#built_in_annotation_literals
 */
@RunWith(FATRunner.class)
public class BuiltinAnnoLiteralsTest extends FATServletClient {
    public static final String APP_NAME = "builtinAnnoLiteralsApp";

    // We'll create an app with a Servlet.
    @Server("cdi20BuiltinAnnoServer")
    @TestServlets({ @TestServlet(servlet = BuiltinAnnoServlet.class, path = APP_NAME + "/builtin") })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'builtinAnnoLiteralsApp.war' once it's written to a file
        // Include the 'beanManagerLookupApp.web' package and all of it's java classes and sub-packages
        // Include a simple index.jsp static file in the root of the WebArchive
        WebArchive app1 = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "builtinAnnoApp.web")
                        .add(new FileAsset(new File("test-applications/" + APP_NAME + "/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + "/resources/META-INF/services/javax.enterprise.inject.spi.Extension"),
                                               "services/javax.enterprise.inject.spi.Extension")
                        .addAsWebInfResource(new File("test-applications/" + APP_NAME + "/resources/META-INF/beans.xml"),
                                             "beans.xml") // NEEDS TO GO IN WEB-INF in a war
                        .addAsWebInfResource(new File("test-applications/" + APP_NAME + "/resources/index.jsp"));
        // Write the WebArchive to 'publish/servers/cdi20BuiltinAnnoServer/apps/builtinAnnoLiteralsApp.war' and print the contents
        ShrinkHelper.exportAppToServer(server1, app1);

        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer();
    }
}
