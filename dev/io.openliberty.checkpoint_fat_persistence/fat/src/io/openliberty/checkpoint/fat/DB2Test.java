/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Collections;
import java.util.function.Consumer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatActions.SEVersion;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.db2.web.DB2TestServlet;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class DB2Test extends FATServletClient {

    // Updated docker image to use TLS1.2 for secure communication
    static final DockerImageName db2Image = DockerImageName.parse("kyleaure/db2-ssl:3.0")
                    .asCompatibleSubstituteFor("ibmcom/db2"); //TODO update .asCompatibleSubstituteFor("icr.io/db2_community/db2")

    @ClassRule
    public static Db2Container db2 = new Db2Container(db2Image)
                    .acceptLicense()
                    .withUsername("db2inst1") // set in Dockerfile
                    .withPassword("password") // set in Dockerfile
                    .withDatabaseName("testdb") // set in Dockerfile
                    .withExposedPorts(50000, 50001) // 50k is regular 50001 is secure
                    // Use 5m timeout for local runs, 35m timeout for remote runs (extra time since the DB2 container can be slow to start)
                    .waitingFor(new LogMessageWaitStrategy()
                                    .withRegEx(".*DB2 SSH SETUP DONE.*")
                                    .withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN && !FATRunner.ARM_ARCHITECTURE ? 5 : 35)))
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "db2-ssl"))
                    .withReuse(true);

    final static String SERVER_NAME = "io.openliberty.checkpoint.jdbc.fat.db2";
    @ClassRule
    public static RepeatTests rt = RepeatTests
                    .with(new FeatureReplacementAction().forServers(SERVER_NAME).removeFeatures(Collections.singleton("jdbc-*")).addFeature("jdbc-4.1").withID("JDBC4.1"))
                    .andWith(new FeatureReplacementAction().forServers(SERVER_NAME)
                                    .removeFeatures(Collections.singleton("jdbc-*"))
                                    .addFeature("jdbc-4.2")
                                    .withID("JDBC4.2")
                                    .fullFATOnly())
                    .andWith(new FeatureReplacementAction().forServers(SERVER_NAME)
                                    .removeFeatures(Collections.singleton("jdbc-*"))
                                    .addFeature("jdbc-4.3")
                                    .withID("JDBC4.3")
                                    .withMinJavaLevel(SEVersion.JAVA11)
                                    .fullFATOnly());

    public static final String APP_NAME = "db2fat";
    public static final String SERVLET_NAME = "DB2TestServlet";

    @Server(SERVER_NAME)
    @TestServlet(servlet = DB2TestServlet.class, path = APP_NAME + '/' + SERVLET_NAME)
    public static LibertyServer server;

    static private void initDB() throws Exception {
        Connection con = db2.createConnection("");

        try {
            // Create tables
            Statement stmt = con.createStatement();
            try {
                stmt.execute("DROP TABLE MYTABLE");
            } catch (SQLException x) {
                if (!"42704".equals(x.getSQLState()))
                    throw x;
            }
            stmt.execute("CREATE TABLE MYTABLE (ID SMALLINT NOT NULL PRIMARY KEY, STRVAL NVARCHAR(40))");
            stmt.close();
        } finally {
            con.close();
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "io.openliberty.checkpoint.db2.web");

        initDB();
        Consumer<LibertyServer> preRestoreLogic = checkpointServer -> {
            File serverEnvFile = new File(checkpointServer.getServerRoot() + "/server.env");
            try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                serverEnvWriter.println("DB2_DBNAME=" + db2.getDatabaseName());
                serverEnvWriter.println("DB2_HOSTNAME=" + db2.getHost());
                serverEnvWriter.println("DB2_PORT=" + String.valueOf(db2.getMappedPort(50000)));
                serverEnvWriter.println("DB2_PORT_SECURE=" + String.valueOf(db2.getMappedPort(50001)));
                serverEnvWriter.println("DB2_USER=" + db2.getUsername());
                serverEnvWriter.println("DB2_PASS=" + db2.getPassword());
            } catch (FileNotFoundException e) {
                throw new UncheckedIOException(e);
            }
        };

        // at this point the server no longer has the env set; we set them just before restore
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true, preRestoreLogic);

        // Launch servlet with embedded junit tests,
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWPKI0063W");
    }
}
