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
package test.jakarta.data.global;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import test.jakarta.data.global.webapp.DataGlobalWebAppServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class DataJavaGlobalTest extends FATServletClient {
    /**
     * Error messages, typically for invalid repository methods, that are
     * intentionally caused by tests to cover error paths.
     * These are ignored when checking the messages.log file for errors.
     */
    private static final String[] EXPECTED_ERROR_MESSAGES = //
                    new String[] {
                                   "CWWKD1064E.*getDataSource" // other app stopped
                    };

    @Server("io.openliberty.data.internal.fat.global")
    @TestServlet(servlet = DataGlobalWebAppServlet.class, contextRoot = "DataGlobalWebApp")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive DataGlobalRestApp = ShrinkWrap
                        .create(WebArchive.class, "DataGlobalRestApp.war")
                        .addPackage("test.jakarta.data.global.rest");
        ShrinkHelper.exportAppToServer(server, DataGlobalRestApp);

        WebArchive DataGlobalWebApp = ShrinkWrap
                        .create(WebArchive.class, "DataGlobalWebApp.war")
                        .addPackage("test.jakarta.data.global.webapp");
        ShrinkHelper.exportAppToServer(server, DataGlobalWebApp);

        server.startServer();

        // initialize some data for tests to use

        JsonObject saved1, saved2, saved3;
        saved1 = new HttpRequest(server, "/DataGlobalRestApp/data/reminder/save")
                        .method("POST")
                        .jsonBody("""
                                        {
                                          "id": 1,
                                          "expiresAt": "2025-10-01T13:30:00.133-05:00[America/Chicago]",
                                          "forDayOfWeek": "MONDAY",
                                          "message": "Do this first.",
                                          "monthDayCreated": "--04-25",
                                          "yearCreated": 2025
                                        }""")
                        .run(JsonObject.class);

        saved2 = new HttpRequest(server, "/DataGlobalRestApp/data/reminder/save")
                        .method("POST")
                        .jsonBody("""
                                        {
                                          "id": 2,
                                          "expiresAt": "2025-12-01T12:24:15.000-07:00[America/Los_Angeles]",
                                          "forDayOfWeek": "TUESDAY",
                                          "message": "Do this second.",
                                          "monthDayCreated": "--12-31",
                                          "yearCreated": 2024
                                        }""")
                        .run(JsonObject.class);

        saved3 = new HttpRequest(server, "/DataGlobalRestApp/data/reminder/save")
                        .method("POST")
                        .jsonBody("""
                                        {
                                          "id": 3,
                                          "expiresAt": "2026-12-31T15:45:59.999-06:00[America/Phoenix]",
                                          "forDayOfWeek": "WEDNESDAY",
                                          "message": "Do this third.",
                                          "monthDayCreated": "--04-25",
                                          "yearCreated": 2025
                                        }""")
                        .run(JsonObject.class);

        assertEquals(1, saved1.getInt("id"));
        assertEquals(2, saved2.getInt("id"));
        assertEquals(3, saved3.getInt("id"));
        assertEquals("Do this first.", saved1.getString("message"));
        assertEquals("Do this second.", saved2.getString("message"));
        assertEquals("Do this third.", saved3.getString("message"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            // Stop an application that has a repository that uses a DataSource
            // that is defined in a different application
            ServerConfiguration config = server.getServerConfiguration();
            config.getApplications().removeBy("location", "DataGlobalWebApp.war");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Set.of(),
                                                     "CWWKZ0009I.*DataGlobalWebApp");

            // Verify that the repository in the remaining application can continue
            // to be used.
            String path = "/DataGlobalRestApp/data/reminder/id/3";
            JsonObject json = new HttpRequest(server, path).run(JsonObject.class);

            String found = "found: " + json;
            assertEquals(found, 3, json.getInt("id"));
            assertEquals(found, "Do this third.", json.getString("message"));

            // Attempt to access a repository that depends on resource reference and
            // DataSource in java:global from the other application which has already
            // been stopped.
            path = "/DataGlobalRestApp/data/referral/datasource";
            try {
                json = new HttpRequest(server, path).run(JsonObject.class);
                fail("Should not find " + json);
            } catch (Exception x) {
                if (x.getMessage() != null && x.getMessage().contains("500"))
                    ; // expected
                else
                    throw x;
            }
        } finally {
            server.stopServer(EXPECTED_ERROR_MESSAGES);
        }
    }

    @Test
    public void testExtractMonthAndDayFromMonthDay() throws Exception {
        String path = "/DataGlobalRestApp/data/reminder/created/month/4/day/25";
        JsonArray array = new HttpRequest(server, path).run(JsonArray.class);

        String found = "found: " + array;

        assertEquals(found, 2, array.size());
        assertEquals(found, "Do this first.", array.getString(0));
        assertEquals(found, "Do this third.", array.getString(1));
    }

    /**
     * Verify that an entity can be found in the database by querying on its Id.
     * The DataSource used by the repository has a java:global name and is located
     * in the same application as the repository.
     */
    @Test
    public void testFindByIdGlobalDataSourceSameApp() throws Exception {
        String path = "/DataGlobalRestApp/data/reminder/id/1";
        JsonObject json = new HttpRequest(server, path).run(JsonObject.class);

        String found = "found: " + json;

        assertEquals(found, 1, json.getInt("id"));
        assertEquals(found, "2025-10-01T13:30:00.133-05:00[America/Chicago]",
                     json.getString("expiresAt"));
        assertEquals(found, "MONDAY", json.getString("forDayOfWeek"));
        assertEquals(found, "Do this first.", json.getString("message"));
        assertEquals(found, "--04-25", json.getString("monthDayCreated"));
        assertEquals(found, 2025, json.getInt("yearCreated"));
    }

    /**
     * Verify that an entity can be found in the database by querying on its Id.
     * The DataSource resource reference used by the repository has a
     * java:global/env name and is located in a different application than the
     * repository.
     */
    @Test
    public void testGlobalResRefFromDifferentApp() throws Exception {
        JsonObject saved = new HttpRequest(server, "/DataGlobalRestApp/data/referral/save")
                        .method("POST")
                        .jsonBody("""
                                        {
                                          "email": "testGlobalResRef1@openliberty.io",
                                          "name": "TestGlobalResRefFromDifferentApp",
                                          "phone": 5075554321
                                        }""")
                        .run(JsonObject.class);

        String response = saved.toString();

        assertEquals(response,
                     "testGlobalResRef1@openliberty.io",
                     saved.getString("email"));

        assertEquals(response,
                     "TestGlobalResRefFromDifferentApp",
                     saved.getString("name"));

        assertEquals(response,
                     5075554321L,
                     saved.getJsonNumber("phone").longValue());

        String path = "/DataGlobalRestApp/data/referral/email/" +
                      "testGlobalResRef1@openliberty.io";
        JsonObject json = new HttpRequest(server, path).run(JsonObject.class);

        String found = "found: " + json;

        assertEquals(found,
                     "testGlobalResRef1@openliberty.io",
                     json.getString("email"));

        assertEquals(found,
                     "TestGlobalResRefFromDifferentApp",
                     json.getString("name"));

        assertEquals(found,
                     5075554321L,
                     json.getJsonNumber("phone").longValue());

        path = "/DataGlobalRestApp/data/referral/datasource";
        json = new HttpRequest(server, path).run(JsonObject.class);

        found = "found: " + json;

        assertEquals(found,
                     "Apache Derby",
                     json.getString("DatabaseProductName"));

        assertEquals(found,
                     "Apache Derby Embedded JDBC Driver",
                     json.getString("DriverName"));

        assertEquals(found,
                     "dbuser2",
                     json.getString("UserName"));
    }

    /**
     * Verify that a non-matching entity Id gets a 404 error indicating that an
     * entity is not found in the database.
     */
    @Test
    public void testNotFoundGlobalDataSourceSameApp() throws Exception {
        String path = "/DataGlobalRestApp/data/reminder/id/97531";
        try {
            JsonObject json = new HttpRequest(server, path).run(JsonObject.class);
            fail("Should not find " + json);
        } catch (Exception x) {
            if (x.getMessage() != null && x.getMessage().contains("404"))
                ; // expected
            else
                throw x;
        }
    }

    /**
     * Verify that a REST Application method that observes the CDI Startup event
     * has access to a Jakarta Data repository and can use it to populate data.
     */
    @Test
    public void testStartupEventInRestApplication() throws Exception {
        String path = "/DataGlobalRestApp/data/referral/email/" +
                      "startup@openliberty.io";
        JsonObject json = new HttpRequest(server, path).run(JsonObject.class);

        String found = "found: " + json;

        assertEquals(found,
                     "startup@openliberty.io",
                     json.getString("email"));

        assertEquals(found,
                     "Startup Event",
                     json.getString("name"));

        assertEquals(found,
                     5075556789L,
                     json.getJsonNumber("phone").longValue());
    }
}
