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
package test.jakarta.data.jpa.hibernate;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.data.jpa.hibernate.web.DataJPAHibernateTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
@SkipIfSysProp(SkipIfSysProp.DB_Oracle) //TODO Hibernate fails to load oracle.jdbc.OracleConnection class
public class DataJPAHibernateTest extends FATServletClient {
    private static final String APP_NAME = "DataJPAHibernateTestApp";

    @ClassRule
    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    @Server("io.openliberty.data.internal.fat.jpa.hibernate")
    @TestServlet(servlet = DataJPAHibernateTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Get driver type
        DatabaseContainerType type = DatabaseContainerType.valueOf(testContainer);
        server.addEnvVar("DB_DRIVER", type.getDriverName());

        // Set up server DataSource properties
        DatabaseContainerUtil.setupDataSourceDatabaseProperties(server, testContainer);

        WebArchive war = ShrinkHelper.buildDefaultApp(APP_NAME, "test.jakarta.data.jpa.hibernate.web");
        ShrinkHelper.exportAppToServer(server, war);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
