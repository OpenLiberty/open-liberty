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
package io.openliberty.checkpoint.fat.springboot.data;

import static componenttest.topology.utils.FATServletClient.getTestMethodSimpleName;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.SpringBootApplication;
import com.ibm.websphere.simplicity.config.WebApplication;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.SimpleLogConsumer;
import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                SpringBootDeployDataTests.class,
                WarDeployDataTests.class
})
public class FATSuite extends TestContainerSuite {
    private static final String APP_NAME = "io.openliberty.checkpoint.springboot.fat30.data.app-1.0.0.war";
    private static final String POSTGRES_DB = "database";
    private static final String POSTGRES_USER = "productionuser";
    private static final String POSTGRES_PASS = "productionpw";

    @ClassRule
    public static JdbcDatabaseContainer<?> postgre = DatabaseContainerFactory.createType(DatabaseContainerType.Postgres)
                    .withDatabaseName(POSTGRES_DB)
                    .withUsername(POSTGRES_USER)
                    .withPassword(POSTGRES_PASS)
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "postgre"));

    @BeforeClass
    public static void createTables() throws Exception {
        // Create tables for the DB and insert some test rows
        try (Connection conn = postgre.createConnection("")) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE users( id BIGSERIAL NOT NULL, email VARCHAR (255), name VARCHAR (255), PRIMARY KEY (id) );");
            stmt.execute("CREATE SCHEMA premadeschema");
            stmt.close();
            PreparedStatement ps = conn.prepareStatement("INSERT INTO users (email,name) values (?,?)");
            ps.setString(1, "jo@email.com");
            ps.setString(2, "jo");
            ps.executeUpdate();
            ps = conn.prepareStatement("INSERT INTO users (email,name) values (?,?)");
            ps.setString(1, "james@email.com");
            ps.setString(2, "james");
            ps.executeUpdate();
        }
    }

    public static void setUp(LibertyServer server, boolean asWar, CheckpointPhase phase, String testMethod) throws Exception {
        configureApplication(server, APP_NAME, asWar);
        server.setCheckpoint(phase, true,
                             s -> {
                                 if (phase == CheckpointPhase.AFTER_APP_START) {
                                     assertNotNull("'CWWKZ0001I: Application started' message not found in log.",
                                                   s.waitForStringInLogUsingMark("CWWKZ0001I", 0));
                                     assertNotNull("JVM checkpoint/restore callback not registered",
                                                   s.waitForStringInLogUsingMark("Registering JVM checkpoint/restore callback for Spring-managed lifecycle beans", 0));
                                     // make sure the web app URL is not logged on checkpoint side
                                     assertNull("'CWWKT0016I: Web application available' found in log.",
                                                s.waitForStringInLogUsingMark("CWWKT0016I", 0));
                                     assertNotNull("Spring-managed lifecycle beans not stopped",
                                                   s.waitForStringInLogUsingMark("Stopping Spring-managed lifecycle beans before JVM checkpoint", 0));
                                 }

                                 Map<String, String> envMap = new HashMap<>();
                                 envMap.put("SPRING_DATASOURCE_URL", postgre.getJdbcUrl().replace("\\", ""));
                                 envMap.put("SPRING_DATASOURCE_USERNAME", postgre.getUsername());
                                 envMap.put("SPRING_DATASOURCE_PASSWORD", postgre.getPassword());
                                 configureEnvVariable(s, envMap);
                             });

        server.setArchiveMarker("SpringBootDataTest." + (asWar ? "asWar." : "asJar.") + testMethod);
        server.startServer();

        if (phase == CheckpointPhase.AFTER_APP_START) {
            assertNotNull("Spring-managed lifecycle beans not restarted after JVM restore",
                          server.waitForStringInLogUsingMark("Restarting Spring-managed lifecycle beans after JVM restore"));
        }

        // make sure the web app URL is logged on restore side
        assertNotNull("'CWWKT0016I: Web application available' not found in log.",
                      server.waitForStringInLogUsingMark("CWWKT0016I"));
    }

    public static void doSpringBootDataTest(LibertyServer server) throws Exception {
        String response = HttpUtils.getHttpResponseAsString(server, "users");
        assertTrue("Response does not have jo:" + response, response.contains("jo@email.com"));
        assertTrue("Response does not have james:" + response, response.contains("james@email.com"));
    }

    public static String getTestMethodNameOnly(TestName testName) {
        String testMethodSimpleName = getTestMethodSimpleName(testName);
        // Sometimes the method name includes the class name; remove the class name.
        int dot = testMethodSimpleName.indexOf('.');
        if (dot != -1) {
            testMethodSimpleName = testMethodSimpleName.substring(dot + 1);
        }
        return testMethodSimpleName;
    }

    static public <T extends Enum<T>> T getTestMethod(Class<T> type, TestName testName) {
        String simpleName = getTestMethodNameOnly(testName);
        try {
            T t = Enum.valueOf(type, simpleName);
            Log.info(FATSuite.class, testName.getMethodName(), "got test method: " + t);
            return t;
        } catch (IllegalArgumentException e) {
            Log.info(type, simpleName, "No configuration enum: " + testName);
            fail("Unknown test name: " + testName.getMethodName());
            return null;
        }
    }

    static void configureEnvVariable(LibertyServer server, Map<String, String> newEnv) {
        try {
            File serverEnvFile = new File(server.getFileFromLibertyServerRoot("server.env").getAbsolutePath());
            try (PrintWriter out = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                newEnv.entrySet().forEach(e -> out.println(e.getKey() + '=' + e.getValue()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static void configureApplication(LibertyServer server, String appName, boolean asWAR) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        RemoteFile appFile = server.getFileFromLibertyServerRoot("apps/" + appName);
        ConfigElementList<Application> apps;
        Application app;
        if (asWAR) {
            app = new WebApplication();
            ((WebApplication) app).setContextRoot("/");
            apps = (ConfigElementList) config.getWebApplications();
        } else {
            app = new SpringBootApplication();
            apps = (ConfigElementList) config.getSpringBootApplications();
        }
        apps.clear();
        apps.add(app);
        app.setLocation(appFile.getName());
        server.updateServerConfiguration(config);
    }
}
