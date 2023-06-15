/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
package test.jakarta.data.jpa;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
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
import test.jakarta.data.jpa.web.DataJPATestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11) // TODO 17
public class DataJPATest extends FATServletClient {
    private static String jdbcJarName;

    @ClassRule
    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    @Server("io.openliberty.data.internal.fat.jpa")
    @TestServlet(servlet = DataJPATestServlet.class, contextRoot = "DataJPATestApp")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Get driver type
        DatabaseContainerType type = DatabaseContainerType.valueOf(testContainer);
        server.addEnvVar("DB_DRIVER", jdbcJarName = type.getDriverName());
        server.addEnvVar("DB_USER", testContainer.getUsername());
        server.addEnvVar("DB_PASSWORD", testContainer.getPassword());

        // Set up server DataSource properties
        DatabaseContainerUtil.setupDataSourceDatabaseProperties(server, testContainer);

        WebArchive war = ShrinkHelper.buildDefaultApp("DataJPATestApp", "test.jakarta.data.jpa.web");
        ShrinkHelper.exportAppToServer(server, war);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // TODO if we decide to add the ability to put Jakarta Data properties onto DataSourceDefinition properties,
        // then an update will be needed to com.ibm.ws.jdbc.internal.JDBCDriverService.create to ignore them for the data source:
        // W DSRA8020E: Warning: The property 'data.createTables' does not exist on the DataSource class ...
        server.stopServer("DSRA8020E.*data.createTables",
                          "DSRA8020E.*data.dropTables",
                          "DSRA8020E.*data.tablePrefix");
    }

    /**
     * This test has conditional logic based on the JDBC driver/database.
     */
    @Test
    public void testFindAndDeleteEntityThatHasAnIdClass() throws Exception {
        runTest(server, "DataJPATestApp", "testFindAndDeleteEntityThatHasAnIdClass&jdbcJarName=" + jdbcJarName);
    }

    /**
     * This test has conditional logic based on the JDBC driver/database.
     */
    @Test
    public void testUnannotatedCollection() throws Exception {
        runTest(server, "DataJPATestApp", "testUnannotatedCollection&jdbcJarName=" + jdbcJarName);
    }
}
