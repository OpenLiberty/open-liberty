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
package test.jakarta.data.jpa;

import java.util.HashMap;
import java.util.Map;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import test.jakarta.data.jpa.web.DataJPATestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
@CheckpointTest
public class DataJPATestCheckpoint extends FATServletClient {

    @ClassRule
    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    @Server("io.openliberty.data.internal.checkpoint.fat.jpa")
    @TestServlet(servlet = DataJPATestServlet.class, contextRoot = "DataJPATestApp")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Set up server DataSource properties
        DatabaseContainerUtil.build(server, testContainer).withDriverReplacement().withDatabaseProperties().modify();

        WebArchive war = ShrinkHelper.buildDefaultApp("DataJPATestApp", "test.jakarta.data.jpa.web");
        ShrinkHelper.exportAppToServer(server, war);

        Map<String, String> envVars = new HashMap<>();
        envVars.put("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer();

        //Server started, application started, checkpoint taken, server is now stopped.
        //Configure environment variable used by servlet
        server.addEnvVarsForCheckpoint(envVars);

        server.checkpointRestore();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(DataJPATest.EXPECTED_ERROR_MESSAGES);
    }
}
