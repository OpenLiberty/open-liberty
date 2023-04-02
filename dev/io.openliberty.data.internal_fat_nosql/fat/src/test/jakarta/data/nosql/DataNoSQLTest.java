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
package test.jakarta.data.nosql;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.data.nosql.web.DataNoSQLServlet;

/**
 * This test bucket will eventually be used to test Jakarta Data running with a Jakarta NoSQL provider.
 * To start with, it is only being used to verify that the Jakarta NoSQL classes can be loaded from a
 * currently nonship nosql-1.0 feature.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11) // TODO 17
public class DataNoSQLTest extends FATServletClient {

    @Server("io.openliberty.data.internal.fat.nosql")
    @TestServlet(servlet = DataNoSQLServlet.class, contextRoot = "DataNoSQLApp")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp("DataNoSQLApp", "test.jakarta.data.nosql.web");
        ShrinkHelper.exportAppToServer(server, war);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
