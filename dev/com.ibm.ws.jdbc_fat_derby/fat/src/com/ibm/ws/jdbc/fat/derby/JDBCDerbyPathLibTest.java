/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.derby;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.Path;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.fat.derby.web.JDBCDerbyServlet;

@RunWith(FATRunner.class)
public class JDBCDerbyPathLibTest extends FATServletClient {

    @Server(FATSuite.SERVER)
    @TestServlet(servlet = JDBCDerbyServlet.class, contextRoot = FATSuite.jdbcapp)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Library> libraries = config.getLibraries();
        Library library = libraries.get(0);

        // switch from using filesets to using path config to the jars
        library.getFilesets().clear();

        ConfigElementList<Path> paths = library.getPaths();
        Path derbyPath = new Path();
        derbyPath.setName("${shared.resource.dir}/derby/derby.jar");
        paths.add(derbyPath);

        Path trandriverPath = new Path();
        trandriverPath.setName("${shared.resource.dir}/derby/trandriver.jar");
        paths.add(trandriverPath);

        server.updateServerConfiguration(config);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKE0701E", //expected by testTransactionalSetting
                          "DSRA4011E", //expected by testTNConfigIsoLvlReverse
                          "SRVE0319E"); //expected by testTNConfigTnsl
    }
}
