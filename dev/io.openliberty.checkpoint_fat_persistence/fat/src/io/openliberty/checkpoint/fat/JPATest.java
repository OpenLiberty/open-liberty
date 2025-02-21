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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.function.Consumer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.CheckpointTest;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class JPATest {

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
                                    .withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 5 : 35)))
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "db2-ssl"))
                    .withReuse(true);

    public static final String APP_NAME = "jpafat";
    public static final String SERVLET_NAME = "DB2TestServlet";

    public static LibertyServer server = LibertyServerFactory.getLibertyServer("checkpointfat.jpa");;

    static private void initDB() throws Exception {
        Connection con = db2.createConnection("");

        try {
            // Create tables
            Statement stmt = con.createStatement();
            for (String table : new String[] { "EVENT", "SEQUENCE" }) {
                try {
                    stmt.execute("DROP TABLE " + table);
                } catch (SQLException x) {
                    //ignore Object does not exist errors
                    if (!"42704".equals(x.getSQLState())) {
                        throw x;
                    }
                }
            } ;
            stmt.execute("CREATE TABLE Event (eventId INTEGER NOT NULL, eventLocation VARCHAR(255), "
                         + "eventName VARCHAR(255), eventTime VARCHAR(255), PRIMARY KEY (eventId))");
            stmt.execute("CREATE TABLE SEQUENCE (SEQ_NAME VARCHAR(50) NOT NULL, SEQ_COUNT DECIMAL(15), "
                         + "PRIMARY KEY (SEQ_NAME))");
            stmt.execute("INSERT INTO SEQUENCE(SEQ_NAME, SEQ_COUNT) values ('SEQ_GEN', 3)");
            stmt.execute("INSERT INTO Event(eventId, eventLocation, eventName, eventTime) values"
                         + " (1, 'Africa', 'event1', '12:00 AM, January 1 2023')");
            stmt.execute("INSERT INTO Event(eventId, eventLocation, eventName, eventTime) values"
                         + " (2, 'Austin', 'event2', '12:00 AM, January 1 2023')");
            stmt.execute("INSERT INTO Event(eventId, eventLocation, eventName, eventTime) values"
                         + " (3, 'Lisbon', 'event3', '12:00 AM, January 1 2023')");
            stmt.close();
        } finally {
            con.close();
        }
    }

    private static void buildApp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "io.openliberty.guides.*");
    }

    @BeforeClass
    public static void setUp() throws Exception {
        buildApp();

        initDB();

        Consumer<LibertyServer> preRestoreLogic = checkpointServer -> {
            File serverEnvFile = new File(checkpointServer.getServerRoot() + "/server.env");
            try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                serverEnvWriter.println("DB2_DBNAME=" + db2.getDatabaseName());
                serverEnvWriter.println("DB2_HOSTNAME=" + db2.getHost());
                serverEnvWriter.println("DB2_PORT=" + String.valueOf(db2.getMappedPort(50000)));
                serverEnvWriter.println("DB2_PORT_SECURE=" + String.valueOf(db2.getMappedPort(50001)));
                serverEnvWriter.println("DB2_USER=" + "db2inst1");
                serverEnvWriter.println("DB2_PASS=" + "password");
                serverEnvWriter.flush();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true, preRestoreLogic);
        server.addCheckpointRegexIgnoreMessage("CWWKG0083W");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWJP9991W");
    }

    /**
     * Verify HTTP response based on return code, required content or excluded content.
     *
     * @param httpCon
     * @param expectedRC
     * @param checkedContent can be length 0, 1, 2. Empty strings are ignored.
     *                           <ul>
     *                           <li>Length 0 means no response content is checked </li>
     *                           <li> 1 means check that expectedContent[0] IS present in returned body content </li>
     *                           <li> 2 means also check that expectedContent[1] is NOT in returned content </li>
     *                           </ul>
     * @throws Exception
     */
    private void checkHTTResponse(HttpURLConnection httpCon, int expectedRC, String... checkedContent) throws Exception {
        if (httpCon.getResponseCode() != expectedRC) {
            try (InputStream is = httpCon.getErrorStream()) {
                if (is != null) {
                    try (BufferedReader ESbufferedReader = new BufferedReader(new InputStreamReader(is))) {
                        String errLine;
                        while ((errLine = ESbufferedReader.readLine()) != null) {
                            Log.info(JPATest.class, "create()", "ERROR : " + errLine);
                        }
                    }
                } else {
                    Log.info(JPATest.class, "checkHTTResponse()", "Unexpected HTTP response code. No errorStream provided.");
                }
            }
            assertEquals("Did not get expected HTTP response code", expectedRC, httpCon.getResponseCode());
        }

        //we may need to verify the content in the body of the HTTP response.
        if (checkedContent.length > 0) {
            String requiredContent = checkedContent[0];
            String excludedContent = checkedContent.length < 2 ? "" : checkedContent[1];
            if (requiredContent.length() > 0 || excludedContent.length() > 0) {
                boolean foundrequiredContent = requiredContent.equals("") ? true : false;
                try (BufferedReader reader = HttpUtils.getConnectionStream(httpCon)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Log.info(JPATest.class, "checkHTTResponse()", "HTTPResponse Connection stream: " + line);
                        if (!foundrequiredContent) {
                            if (line.contains(requiredContent)) {
                                foundrequiredContent = true;
                                if (excludedContent.equals("")) {
                                    break;
                                }
                            }
                        }
                        if (excludedContent != "") {
                            if (line.contains(excludedContent)) {
                                fail("Excluded content was found: " + excludedContent);
                            }
                        }
                    }
                    if (!foundrequiredContent) {
                        fail("Expected content missing from HTTP response: " + requiredContent);
                    }
                }
            }
        }
    }

    // Make JPA ORM working with a few basic CRUD operations.

    @Test
    public void create() throws Exception {
        HttpURLConnection hc = HttpUtils.getHttpConnection(HttpUtils.createURL(server, "events"),
                                                           componenttest.topology.utils.HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.POST);
        // Add  a record to the DB
        String xwwwFormUrleEncoded = "name=event41&" +
                                     "time=" + URLEncoder.encode("12:00 AM, January 1 2023", StandardCharsets.UTF_8.toString()) + "&" +
                                     "location=Texas";
        OutputStream os = hc.getOutputStream();
        os.write(xwwwFormUrleEncoded.getBytes());
        os.flush();
        checkHTTResponse(hc, HttpURLConnection.HTTP_NO_CONTENT);

        // Check if record we just created is there.
        hc = HttpUtils.getHttpConnection(HttpUtils.createURL(server, "events/4"),
                                         componenttest.topology.utils.HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.GET);
        checkHTTResponse(hc, HttpURLConnection.HTTP_OK, "Texas");

    }

    @Test
    public void read() throws Exception {
        HttpURLConnection hc = HttpUtils.getHttpConnection(HttpUtils.createURL(server, "events/1"),
                                                           componenttest.topology.utils.HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.GET);

        checkHTTResponse(hc, HttpURLConnection.HTTP_OK, "Africa");
    }

    @Test
    public void delete() throws Exception {
        HttpURLConnection hc = HttpUtils.getHttpConnection(HttpUtils.createURL(server, "events/2"),
                                                           componenttest.topology.utils.HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.DELETE);
        checkHTTResponse(hc, HttpURLConnection.HTTP_NO_CONTENT);

        hc = HttpUtils.getHttpConnection(HttpUtils.createURL(server, "events/2"),
                                         componenttest.topology.utils.HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.GET);
        checkHTTResponse(hc, HttpURLConnection.HTTP_OK, "", "Austin"); // check deleted record not found
    }
}
