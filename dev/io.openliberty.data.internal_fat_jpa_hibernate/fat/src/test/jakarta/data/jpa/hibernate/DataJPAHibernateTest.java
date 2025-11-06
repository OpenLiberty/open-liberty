/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.data.jpa.hibernate.web.DataJPAHibernateTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
@MaximumJavaLevel(javaLevel = 25) // TODO remove once Hibernate upgrades their ByteBuddy dependency to a version that supports java 26+
public class DataJPAHibernateTest extends FATServletClient {
    private static final String APP_NAME = "DataJPAHibernateTestApp";

    @Server("io.openliberty.data.internal.fat.jpa.hibernate")
    @TestServlet(servlet = DataJPAHibernateTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Set up server DataSource properties
        DatabaseContainerUtil.build(server, FATSuite.testContainer)
                        .withDatabaseProperties()
                        .withDriverVariable()
                        .modify();

        WebArchive war = ShrinkHelper.buildDefaultApp(APP_NAME, "test.jakarta.data.jpa.hibernate.web");
        ShrinkHelper.exportAppToServer(server, war);
        server.startServer();
    }

    //NOTE: Hibernate does not support the version of Derby we use during DatabaseRotation
    // HHH000511: The 10.11.1 version for [org.hibernate.community.dialect.DerbyDialect] is no longer supported, hence certain features may not work properly.
    // The minimum supported version is 10.15.2. Check the community dialects project for available legacy versions.
    // for now this test still works, but it might start failing with future Hibernate versions

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
