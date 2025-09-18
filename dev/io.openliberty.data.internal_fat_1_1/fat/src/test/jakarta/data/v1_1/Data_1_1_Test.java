/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.v1_1;

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
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.data.v1_1.web.Data_1_1_Servlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 21)
public class Data_1_1_Test extends FATServletClient {
    /**
     * Error messages, typically for invalid repository methods, that are
     * intentionally caused by tests to cover error paths.
     * These are ignored when checking the messages.log file for errors.
     */
    static final String[] EXPECTED_ERROR_MESSAGES = //
                    new String[] {
                                   "CWWKD1054E.*findByIsControlTrueAndNumericValueBetween",
                                   "CWWKD1091E.*countBySurgePriceGreaterThanEqual",
                    };

    @ClassRule
    public static final JdbcDatabaseContainer<?> testContainer = //
                    DatabaseContainerFactory.create();

    @Server("io.openliberty.data.internal.fat.1.1")
    @TestServlets({ @TestServlet(servlet = Data_1_1_Servlet.class,
                                 contextRoot = "Data_1_1_App")
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        DatabaseContainerUtil.build(server, testContainer)
                        .withDriverVariable()
                        .withDatabaseProperties()
                        .modify();

        WebArchive war = ShrinkHelper
                        .buildDefaultApp("Data_1_1_App",
                                         "test.jakarta.data.v1_1.web");
        ShrinkHelper.exportAppToServer(server, war);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(EXPECTED_ERROR_MESSAGES);
    }
}
