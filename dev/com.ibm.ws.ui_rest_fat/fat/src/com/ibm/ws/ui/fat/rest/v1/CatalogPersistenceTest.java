/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package com.ibm.ws.ui.fat.rest.v1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ui.fat.APIConstants;
import com.ibm.ws.ui.fat.Bookmark;
import com.ibm.ws.ui.fat.FATSuite;
import com.ibm.ws.ui.fat.rest.CommonRESTTest;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Drive all of the various persistence tests for the catalog.
 * This test requires server restarts.
 * This can not be run via JUnit through Eclipse.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class CatalogPersistenceTest extends CommonRESTTest implements APIConstants {
    private static final Class<?> c = CatalogPersistenceTest.class;
    private static final LibertyServer server = FATSuite.server;

    public CatalogPersistenceTest() {
        super(c);
        url = API_V1_CATALOG;
    }

    /**
     * Ensure that the catalog starts from scratch before each test.
     */
    @Before
    @After
    public void cleanupPersistedCatalog() throws Exception {
        if (server.fileExistsInLibertyServerRoot(CATALOG_JSON)) {
            server.deleteFileFromLibertyServerRoot(CATALOG_JSON);
            if (server.fileExistsInLibertyServerRoot(CATALOG_JSON)) {
                fail("The persisted catalog (file system) could not be deleted. Tests will fail if it exists. Aborting.");
            }
        }
    }

    // We added this beforeclass to make sure that the isStarted for the server
    // is correctly set to the current state
    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        server.resetStarted();
    }

    /**
     * At the end of all of the testing, reset to default.
     */
    @AfterClass
    public static void tearDownClass() throws Exception {
        stopServerWithExpectedErrors();
        server.startServer();
    }

    private static void stopServerWithExpectedErrors() throws Exception {
        server.stopServer(
            "CWWKX1002E:.*",
            "CWWKX1003E:.*",
            "CWWKX1009E:.*",
            "CWWKX1010E:.*",
            "SRVE8094W:.*");
    }

    /**
     * Sets the new Catalog persisted into the server's persisted storage.
     *
     * @param newPersistedFile
     */
    private void setCatalogFileFromTestFile(String newPersistedFile) throws Exception {
        server.copyFileToLibertyServerRoot(RESOURCES_ADMIN_CENTER_1_0, "persistence/catalog/" + newPersistedFile);
        server.renameLibertyServerRootFile(RESOURCES_ADMIN_CENTER_1_0 + newPersistedFile, CATALOG_JSON);
    }

    /**
     * Restart the server with the new persisted catalog file. The stop and
     * start operations are validated so this operation will not return
     * until the server is fully restarted and ready for operations.
     *
     * @param newPersistedFile
     * @throws Exception
     */
    private void restartWithNewPersistedCatalog(String newPersistedFile) throws Exception {
        stopServerAndValidate(server);
        setCatalogFileFromTestFile(newPersistedFile);
        startServerAndValidate(server);
    }

    private void ignoreErrorAndStopServerWithValidate() throws Exception {
        stopServerWithExpectedErrors();
        assertFalse("FAIL: Server is not stopped.",
                    server.isStarted());
    }

    private void ignoreErrorAndRestartWithNewPersistedCatalog(String newPersistedFile) throws Exception {
        ignoreErrorAndStopServerWithValidate();
        setCatalogFileFromTestFile(newPersistedFile);
        startServerAndValidate(server);
    }

    /**
     * Loads the catalog from a json file, make sure it is loaded correctly
     *
     * Test flow:
     * 1. Stop the server
     * 2. Create catalog.json in &lt;server-output-directory&gt;/resources/adminCenter-1.0
     * 3. Restart the server
     * 4. Request GET the catalog resource
     * 5. Compare the returned JSON string with the original JSON string, make sure they are the same.
     */
    @Test
    public void catalogLoadNoErrorTest() throws Exception {
        ignoreErrorAndRestartWithNewPersistedCatalog("simpleCatalog.json");

        // Confirm the catalog JSON contents
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertContains(response, "_metadata");
        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertFalse("FAIL: Should have gotten back a non-default Catalog",
                    metadata.getBoolean("isDefault"));

        // Confirm the right message was printed after the catalog was loaded
        assertNotNull("The server did not report that the persisted catalog was loaded",
                      server.waitForStringInLog("CWWKX1006I:"));
    }

    /**
     * Loads the catalog from a json file with wrong field, make sure it is not loaded.
     *
     * Test flow:
     * 1. Stop the server
     * 2. Create catalog.json in &lt;server-output-directory&gt;/resources/adminCenter-1.0
     * 3. Restart the server
     * 4. Request GET the catalog resource
     * 5. Compare the returned JSON string with the original JSON string, make sure it is the default one.
     */
    @Test
    public void catalogLoadJSONWrongFieldTest() throws Exception {
        ignoreErrorAndRestartWithNewPersistedCatalog("simpleTool.json");

        // Confirm catalog contents
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        assertContains(response, "_metadata");
        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertTrue("FAIL: Should have gotten back a default Catalog",
                    metadata.getBoolean("isDefault"));

        // Confirm the bad catalog json message and default catlaog message was printed
        // after the catalog was loaded
        assertNotNull("The persistence layer did not report the cause for the load problem",
                      server.waitForStringInLog("CWWKX1010E:"));
        assertNotNull("The bad catalog json should be reported and not be loaded",
                      server.waitForStringInLog("CWWKX1003E:"));
        assertNotNull("The default catalog should be loaded",
                      server.waitForStringInLog("CWWKX1000I:"));
    }

    /**
     * Loads the catalog from a json file with missing { (syntax error), make sure it is not loaded.
     *
     * Test flow:
     * 1. Stop the server
     * 2. Create catalog.json in &lt;server-output-directory&gt;/resources/adminCenter-1.0
     * 3. Restart the server
     * 4. Request GET the catalog resource
     * 5. Compare the returned JSON string with the original JSON string, make sure it is the default one.
     */
    @Test
    public void catalogLoadJSONSyntaxErrorTest() throws Exception {
        ignoreErrorAndRestartWithNewPersistedCatalog("badSyntax.json");

        // Confirm catalog contents
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        assertContains(response, "_metadata");
        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertTrue("FAIL: Should have gotten back a default Catalog",
                    metadata.getBoolean("isDefault"));

        // Confirm the bad json syntax message and default catlaog message was printed
        // after the catalog was loaded
        assertNotNull("The persistence layer did not report the cause for the load problem",
                      server.waitForStringInLog("CWWKX1009E:.*\\{thisisbadsyntaxjson\\}"));
        assertNotNull("The bad json syntax file should be reported and not be loaded",
                      server.waitForStringInLog("CWWKX1002E:"));
        assertNotNull("The default catalog should be loaded",
                      server.waitForStringInLog("CWWKX1000I:"));
    }

    /**
     * Create default catalog when there is nothing persisted.
     *
     * Test flow:
     * 1. Stop the server
     * 2. delete catalog.json
     * 3. Restart the server
     */
    @Test
    public void catalogLoadDefaultNothingPersistedTest() throws Exception {
        ignoreErrorAndStopServerWithValidate();
        server.deleteFileFromLibertyServerRoot(RESOURCES_ADMIN_CENTER_1_0 + CATALOG_JSON);
        startServerAndValidate(server);

        // Confirm catalog contents
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        assertContains(response, "_metadata");
        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertTrue("FAIL: Should have gotten back a default Catalog",
                    metadata.getBoolean("isDefault"));

        // Confirm the default catalog created
        assertNotNull("The default catalog should be loaded",
                      server.waitForStringInLog("CWWKX1000I:"));
    }

    /**
     * Add a tool to the catalog with a POST request, then restart the server, make sure the tool is still available
     *
     * Test flow:
     * 1. Request POST the catalog resource
     * 2. Response HTTP Status Code: 201
     * 3. Request GET the catalog resource of the newly added tool
     * 4. Response HTTP Status Code: 200
     * 5. The JSON object contains the available resources
     * 6. Add the same tool again
     * 7. Response HTTP Status Code: 409
     */
    @SuppressWarnings("unchecked")
    @Test
    public void catalogAddNewToolandRestartServer() throws Exception {
        // Add a new tool to the catalog
        String name = "NewGoogle";
        String toolUrl = "http://www.google.com";
        String icon = "https://www.google.com/images/google_favicon_128.png";
        String description = "Google front page new version";
        Bookmark bookmark = new Bookmark(name, toolUrl, icon, description);
        String id = bookmark.getId();
        String bookmarkString = bookmark.toString().substring(9); // take out "Bookmark "

        // Post to catalog
        response = post(url + "/bookmarks", adminUser, adminPassword, bookmarkString, 201);

        // check how many tools are available
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "response: from GET " + response);

        // Confirm the tools are correct
        assertContains(response, "bookmarks");
        JsonArray bookmarks = response.getJsonArray("bookmarks");
        assertSize(bookmarks, 2); // The default (openliberty.io) + newly added

        // restart server
        ignoreErrorAndStopServerWithValidate();
        startServerAndValidate(server);

        // Get the (now persisted)
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        assertNotNull("The server did not report that the persisted catalog was loaded",
                      server.waitForStringInLog("CWWKX1006I:"));

        // Confirm the tools are correct
        assertContains(response, "bookmarks");
        bookmarks = response.getJsonArray("bookmarks");
        assertSize(bookmarks, 2); // The default (openliberty.io) + newly added

        // Get the previously created tool from the catalog
        response = get(url + "/bookmarks/" + id, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        assertSize(response, 6);
        assertContains(response, "id", id);
        assertContains(response, "type", bookmark.getType());
        assertContains(response, "name", name);
        assertContains(response, "url", toolUrl);
        assertContains(response, "description", description);
        assertContains(response, "icon", icon);
    }

}
