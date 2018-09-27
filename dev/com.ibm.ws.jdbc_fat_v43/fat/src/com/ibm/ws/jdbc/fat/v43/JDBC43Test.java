/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.v43;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.test.d43.jdbc.D43Driver;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.fat.v43.web.JDBC43TestServlet;

@RunWith(FATRunner.class)
public class JDBC43Test extends FATServletClient {
    public static final String APP_NAME = "app43";

    @Server("com.ibm.ws.jdbc.fat.v43")
    @TestServlet(servlet = JDBC43TestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "jdbc.fat.v43.web");

        RemoteFile derby = server.getFileFromLibertySharedDir("resources/derby/derby.jar");

        JavaArchive derbyJar = ShrinkWrap.create(ZipImporter.class, "derby.jar")
                        .importFrom(new File(derby.getAbsolutePath()))
                        .as(JavaArchive.class);

        JavaArchive d43driver = ShrinkWrap.create(JavaArchive.class, "d43driver.jar")
                        .addPackage("org.test.d43.jdbc")
                        .merge(derbyJar)
                        .addAsServiceProvider(java.sql.Driver.class, D43Driver.class);

        ShrinkHelper.exportToServer(server, "drivers", d43driver);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("DSRA0302E.*XA_RBOTHER", // raised by mock JDBC driver for XA operations after abort
                          "DSRA8790W", // expected for begin/endRequest invoked by application being ignored
                          "J2CA0081E", // TODO why does Derby think a transaction is still active?
                          "WLTC0018E"); // TODO remove once transactions bug is fixed
    }
}
