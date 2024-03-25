/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
import org.testcontainers.containers.MongoDBContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.data.nosql.web.DataNoSQLServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class DataNoSQLTest extends FATServletClient {

    public static MongoDBContainer mongoDBContainer = FATSuite.mongoDBContainer;

    @Server("io.openliberty.data.internal.fat.nosql")
    @TestServlet(servlet = DataNoSQLServlet.class, contextRoot = "DataNoSQLApp")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp("DataNoSQLApp", "test.jakarta.data.nosql.web");
        ShrinkHelper.exportAppToServer(server, war);

        server.addEnvVar("MONGO_DBNAME", "testdb");
        server.addEnvVar("MONGO_HOST", mongoDBContainer.getHost() + ":" + String.valueOf(mongoDBContainer.getMappedPort(27017)));

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
