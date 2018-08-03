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
package com.ibm.ws.jdbc.fat.driver;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.fat.driver.derby.FATDriver;
import jdbc.fat.driver.web.JDBCDriverManagerServlet;

@RunWith(FATRunner.class)
public class JDBCDriverManagerTest extends FATServletClient {

    public static final String APP_NAME = "jdbcapp";

    @Server("com.ibm.ws.jdbc.fat.driver")
    @TestServlet(servlet = JDBCDriverManagerServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "jdbc.fat.driver.web");

        RemoteFile derby = server.getFileFromLibertySharedDir("resources/derby/derby.jar");

        JavaArchive derbyJar = ShrinkWrap.create(ZipImporter.class, "derby.jar")
                        .importFrom(new File(derby.getAbsolutePath()))
                        .as(JavaArchive.class);

        JavaArchive fatDriver = ShrinkWrap.create(JavaArchive.class, "FATDriver.jar")
                        .addPackage("jdbc.fat.driver.derby")
                        .addPackage("jdbc.fat.driver.derby.xa")
                        .merge(derbyJar)
                        .addAsServiceProvider(java.sql.Driver.class, FATDriver.class);

        ShrinkHelper.exportToServer(server, "derby", fatDriver);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("DSRA8020E.*internal.nonship.function"); // TODO remove once the capability becomes GA
    }
}
