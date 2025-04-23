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
                                          "message": "Do this first."
                                        }""")
                        .run(JsonObject.class);

        saved2 = new HttpRequest(server, "/DataGlobalRestApp/data/reminder/save")
                        .method("POST")
                        .jsonBody("""
                                        {
                                          "id": 2,
                                          "message": "Do this second."
                                        }""")
                        .run(JsonObject.class);

        saved3 = new HttpRequest(server, "/DataGlobalRestApp/data/reminder/save")
                        .method("POST")
                        .jsonBody("""
                                        {
                                          "id": 3,
                                          "message": "Do this third."
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
        } finally {
            server.stopServer(EXPECTED_ERROR_MESSAGES);
        }
    }

    /**
     * Verify that an entity can be found in the database by querying on its Id.
     */
    @Test
    public void testFindById() throws Exception {
        String path = "/DataGlobalRestApp/data/reminder/id/1";
        JsonObject json = new HttpRequest(server, path).run(JsonObject.class);

        String found = "found: " + json;

        assertEquals(found, 1, json.getInt("id"));
        assertEquals(found, "Do this first.", json.getString("message"));
    }

    /**
     * Verify that a non-matching entity Id gets a 404 error indicating that an
     * entity is not found in the database.
     */
    @Test
    public void testNotFound() throws Exception {
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
}
