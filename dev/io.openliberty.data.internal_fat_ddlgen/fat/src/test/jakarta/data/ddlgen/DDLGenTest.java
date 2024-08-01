/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.jakarta.data.ddlgen;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.data.ddlgen.web.DDLGenTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class DDLGenTest extends FATServletClient {
    // TODO uncomment testcontainers when it isn't down
    //@ClassRule
    //public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    @Server("io.openliberty.data.internal.fat.ddlgen")
    @TestServlet(servlet = DDLGenTestServlet.class, contextRoot = "DDLGenTestApp")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Get driver type
        //DatabaseContainerType type = DatabaseContainerType.valueOf(testContainer);
        //server.addEnvVar("DB_DRIVER", type.getDriverName());

        // Set up server DataSource properties
        //DatabaseContainerUtil.setupDataSourceDatabaseProperties(server, testContainer);

        WebArchive war = ShrinkHelper.buildDefaultApp("DDLGenTestApp", "test.jakarta.data.ddlgen.web");
        ShrinkHelper.exportAppToServer(server, war);

        // TODO run ddlgen and save its output to a file

        server.startServer();

        runTest(server, "DDLGenTestApp/DDLGenTestServlet", "executeDDL");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
