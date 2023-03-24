/*******************************************************************************
 * Copyright (c) 2013, 2023 IBM Corporation and others.
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
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
 * Drive all of the various persistence tests for the toolbox.
 * This test requires server restarts.
 * This can not be run via JUnit through Eclipse.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class ToolboxPersistenceTest extends CommonRESTTest implements APIConstants {
    private static final Class<?> c = ToolboxPersistenceTest.class;
    private static final String F_UI = "adminCenter-1.0";
    private static final String F_OSGI = "osgiConsole-1.0";
    private static final String RESOURCES_ADMIN_CENTER_1_0 = FATSuite.RESOURCES_ADMIN_CENTER_1_0;
    private static final String TOOLBOX_ADMIN_JSON = RESOURCES_ADMIN_CENTER_1_0 + "toolbox-admin.json";
    private static final String TOOLBOX_BOB_JSON = RESOURCES_ADMIN_CENTER_1_0 + "toolbox-bob.json";
    private static final String TOOLBOX_TEST_JSON = RESOURCES_ADMIN_CENTER_1_0 + "toolbox-test/test.json";
    private static final String TOOLBOX_ENCRYPTED_ADMIN_JSON = RESOURCES_ADMIN_CENTER_1_0 + "toolbox-YWRtaW4=.json";
    private static final String TOOLBOX_ENCRYPTED_BOB_JSON = RESOURCES_ADMIN_CENTER_1_0 + "toolbox-cmVhZGVy.json";
    private static final String TOOLBOX_ENCRYPTED_TEST_JSON = RESOURCES_ADMIN_CENTER_1_0 + "toolbox-dGVzdC90ZXN0.json";

    public ToolboxPersistenceTest() {
        super(c);
        url = API_V1_TOOLBOX;
    }

    /**
     * Ensure that the toolboxes starts from scratch before each test.
     */
    @Before
    @After
    public void cleanupPersistedToolboxes() throws Exception {
        if (FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ADMIN_JSON)) {
            FATSuite.server.deleteFileFromLibertyServerRoot(TOOLBOX_ADMIN_JSON);
            if (FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ADMIN_JSON)) {
                fail("The persisted toolbox for admin (file system) could not be deleted. Tests will fail if it exists. Aborting.");
            }
        }

        if (FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ENCRYPTED_ADMIN_JSON)) {
            FATSuite.server.deleteFileFromLibertyServerRoot(TOOLBOX_ENCRYPTED_ADMIN_JSON);
            if (FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ENCRYPTED_ADMIN_JSON)) {
                fail("The persisted toolbox for encrypted admin (file system) could not be deleted. Tests will fail if it exists. Aborting.");
            }
        }

        if (FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_BOB_JSON)) {
            FATSuite.server.deleteFileFromLibertyServerRoot(TOOLBOX_BOB_JSON);
            if (FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_BOB_JSON)) {
                fail("The persisted toolbox for bob (file system) could not be deleted. Tests will fail if it exists. Aborting.");
            }
        }

        if (FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ENCRYPTED_BOB_JSON)) {
            FATSuite.server.deleteFileFromLibertyServerRoot(TOOLBOX_ENCRYPTED_BOB_JSON);
            if (FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ENCRYPTED_BOB_JSON)) {
                fail("The persisted toolbox for encrypted bob (file system) could not be deleted. Tests will fail if it exists. Aborting.");
            }
        }

        if (FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_TEST_JSON)) {
            FATSuite.server.deleteFileFromLibertyServerRoot(TOOLBOX_TEST_JSON);
            if (FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_TEST_JSON)) {
                fail("The persisted toolbox for test (file system) could not be deleted. Tests will fail if it exists. Aborting.");
            }
        }

        if (FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ENCRYPTED_TEST_JSON)) {
            FATSuite.server.deleteFileFromLibertyServerRoot(TOOLBOX_ENCRYPTED_TEST_JSON);
            if (FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ENCRYPTED_TEST_JSON)) {
                fail("The persisted toolbox for test (file system) could not be deleted. Tests will fail if it exists. Aborting.");
            }
        }

    }

    /**
     * At the end of all of the testing, reset to default.
     */
    @AfterClass
    public static void tearDownClass() throws Exception {
        stopServerWithExpectedErrors();
        FATSuite.server.startServer();
    }

    private static void stopServerWithExpectedErrors() throws Exception {
        FATSuite.server.stopServer(
            "CWWKX1010E:.*",
            "CWWKX1031E:.*",
            "CWWKX1009E:.*",
            "CWWKX1030E:.*",
            "SRVE8094W: WARNING: Cannot set header. Response already committed.*" 
        );
    }

    /**
     * @param string
     */
    private void setAdminToolboxFileFromTestFile(String newPersistedFile, boolean useEncryptedName) throws Exception {
        FATSuite.server.copyFileToLibertyServerRoot(RESOURCES_ADMIN_CENTER_1_0, "persistence/toolbox/" + newPersistedFile);
        if (useEncryptedName) {
            FATSuite.server.renameLibertyServerRootFile(RESOURCES_ADMIN_CENTER_1_0 + newPersistedFile, TOOLBOX_ENCRYPTED_ADMIN_JSON);
        } else {
            FATSuite.server.renameLibertyServerRootFile(RESOURCES_ADMIN_CENTER_1_0 + newPersistedFile, TOOLBOX_ADMIN_JSON);
        }
    }

    /**
     * @param newPersistedFile
     * @param useEncryptedName
     * @throws Exception
     */
    private void setTestUserToolboxFileFromTestFile(String newPersistedFile, boolean useEncryptedName) throws Exception {
        File toolboxTestDir = new File (RESOURCES_ADMIN_CENTER_1_0 + "toolbox-test");
        if (!toolboxTestDir.exists()) {
            toolboxTestDir.mkdirs();
            assertTrue("Fail to setup for test/test user with toolbox-test/test.json",
                        toolboxTestDir.exists());
        }
        if (useEncryptedName) {
            FATSuite.server.copyFileToLibertyServerRoot(RESOURCES_ADMIN_CENTER_1_0, "persistence/toolbox/" + newPersistedFile);
            FATSuite.server.renameLibertyServerRootFile(RESOURCES_ADMIN_CENTER_1_0 + newPersistedFile, TOOLBOX_ENCRYPTED_TEST_JSON);
        } else {
            FATSuite.server.copyFileToLibertyServerRoot(RESOURCES_ADMIN_CENTER_1_0 + "toolbox-test", "persistence/toolbox/" + newPersistedFile);
        }
    }

    /**
     * Restart the server with the new persisted toolbox file. The stop and
     * start operations are validated so this operation will not return
     * until the server is fully restarted and ready for operations.
     * 
     * @param newPersistedFile
     * @throws Exception
     */
    private void restartWithNewPersistedAdminToolbox(String newPersistedFile) throws Exception {
        restartWithNewPersistedAdminToolbox(newPersistedFile, false);
    }

    private void restartWithNewPersistedAdminToolbox(String newPersistedFile, boolean useEncryptedName) throws Exception {
        stopServerAndValidate(FATSuite.server);

        setAdminToolboxFileFromTestFile(newPersistedFile, useEncryptedName);

        startServerAndValidate(FATSuite.server);
    }

    private void restartWithNewPersistedTestUserToolbox(String newPersistedFile, boolean useEncryptedName) throws Exception {
        stopServerAndValidate(FATSuite.server);

        setTestUserToolboxFileFromTestFile(newPersistedFile, useEncryptedName);

        startServerAndValidate(FATSuite.server);
    }

    @Override
    protected void stopServerAndValidate(LibertyServer server) throws Exception {
        Log.info(c, method.getMethodName(), "Using stopServerAndValidate with errors to ignore");
        stopServerWithExpectedErrors();
        assertFalse("FAIL: Server is not stopped.",
                    server.isStarted());
    }

    /**
     * Loads the toolbox from a json file using the non encrypted file name, make sure it is loaded correctly
     * 
     * Test flow:
     * 1. Stop the server
     * 2. Create toolbox.json in &lt;server-output-directory&gt;/resources/adminCenter-1.0
     * 3. Restart the server
     * 4. Request GET the toolbox resource
     * 5. Compare the returned JSON string with the original JSON string, make sure they are the same.
     */
    @Test
    public void toolboxLoadNoErrorTest() throws Exception {
        restartWithNewPersistedAdminToolbox("simpleToolbox.json");

        // Confirm the toolbox JSON contents
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertFalse("FAIL: Should have gotten back a non-default toolbox",
                    metadata.getBoolean("isDefault"));

        // Confirm the right message was printed after the toolbox was loaded
        assertNotNull("The server did not report that the persisted toolbox for admin was loaded",
                      FATSuite.server.waitForStringInLog("CWWKX1034I:.*admin"));

        assertTrue("FAIL: toolbox-YWRtaW4=.json file does not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ENCRYPTED_ADMIN_JSON));

        assertFalse("FAIL: toolbox-admin.json file should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ADMIN_JSON));
    }

    /**
     * Loads the toolbox from a json file with encrypted file name, make sure it is loaded correctly
     *
     * Test flow:
     * 1. Stop the server
     * 2. Create toolbox-<encryptedUserId>.json in &lt;server-output-directory&gt;/resources/adminCenter-1.0
     * 3. Restart the server
     * 4. Request GET the toolbox resource
     * 5. Compare the returned JSON string with the original JSON string, make sure they are the same.
     */
    @Test
    public void toolboxLoadWithEncyrptedFileNameTest() throws Exception {
        restartWithNewPersistedAdminToolbox("simpleToolbox.json", true);

        // Confirm the toolbox JSON contents
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertFalse("FAIL: Should have gotten back a non-default toolbox",
                    metadata.getBoolean("isDefault"));

        // Confirm the right message was printed after the toolbox was loaded
        assertNotNull("The server did not report that the persisted toolbox for admin was loaded",
                      FATSuite.server.waitForStringInLog("CWWKX1034I:.*admin"));

        assertTrue("FAIL: toolbox-YWRtaW4=.json file does not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ENCRYPTED_ADMIN_JSON));

        assertFalse("FAIL: toolbox-admin.json file should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ADMIN_JSON));
    }

    /**
     * Loads the toolbox from a json file with non encrypted file name using a user id with path traversal in it (eg. test/test),
     * make sure it is loaded correctly
     *
     * Test flow:
     * 1. Stop the server
     * 2. Create toolbox-test/test.json in &lt;server-output-directory&gt;/resources/adminCenter-1.0
     * 3. Restart the server
     * 4. Request GET the toolbox resource
     * 5. Compare the returned JSON string with the original JSON string, make sure they are the same.
     */
    @Test
    public void toolboxLoadWithPathInUserIdTest() throws Exception {
        restartWithNewPersistedTestUserToolbox("test.json", false);

        // Confirm the toolbox JSON contents
        response = get(url, testUser, testPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertFalse("FAIL: Should have gotten back a non-default toolbox",
                    metadata.getBoolean("isDefault"));

        // Confirm the right message was printed after the toolbox was loaded
        assertNotNull("The server did not report that the persisted toolbox for admin was loaded",
                      FATSuite.server.waitForStringInLog("CWWKX1034I:.*test/test"));

        assertTrue("FAIL: toolbox-dGVzdC90ZXN0.json file does not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ENCRYPTED_TEST_JSON));

        assertFalse("FAIL: toolbox-test/test.json file should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_TEST_JSON));
    }

    /**
     * Loads the toolbox from a json file with encrypted file name using a user id with path traversal in it (eg. test/test),
     * make sure it is loaded correctly
     *
     * Test flow:
     * 1. Stop the server
     * 2. Create toolbox-<encryptedUserId>.json in &lt;server-output-directory&gt;/resources/adminCenter-1.0
     * 3. Restart the server
     * 4. Request GET the toolbox resource
     * 5. Compare the returned JSON string with the original JSON string, make sure they are the same.
     */
    @Test
    public void toolboxLoadWithPathInUserIdAndEncryptedFileNameTest() throws Exception {
        restartWithNewPersistedTestUserToolbox("test.json", true);

        // Confirm the toolbox JSON contents
        response = get(url, testUser, testPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertFalse("FAIL: Should have gotten back a non-default toolbox",
                    metadata.getBoolean("isDefault"));

        // Confirm the right message was printed after the toolbox was loaded
        assertNotNull("The server did not report that the persisted toolbox for admin was loaded",
                      FATSuite.server.waitForStringInLog("CWWKX1034I:.*test/test"));

        assertTrue("FAIL: toolbox-dGVzdC90ZXN0.json file does not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ENCRYPTED_TEST_JSON));

        assertFalse("FAIL: toolbox-test/test.json file should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_TEST_JSON));
    }

    /**
     * Loads the toolbox from a json file with wrong field, make sure it is not loaded.
     *
     * Test flow:
     * 1. Stop the server
     * 2. Create toolbox.json in &lt;server-output-directory&gt;/resources/adminCenter-1.0
     * 3. Restart the server
     * 4. Request GET the toolbox resource
     * 5. Compare the returned JSON string with the original JSON string, make sure it is the default one.
     */
    @Test
    public void toolboxLoadJSONWrongFieldTest() throws Exception {
        restartWithNewPersistedAdminToolbox("simpleTool.json");

        // Confirm the toolbox JSON contents
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertTrue("FAIL: Should have gotten back a default toolbox",
                    metadata.getBoolean("isDefault"));

        // Confirm the bad toolbox json message and default toolbox message was printed
        // after the toolbox was loaded
        assertNotNull("The persistence layer did not report the cause for the load problem",
                      FATSuite.server.waitForStringInLog("CWWKX1010E:"));
        assertNotNull("The bad toolbox json should be reported and not be loaded",
                      FATSuite.server.waitForStringInLog("CWWKX1031E:.*admin"));
        assertNotNull("The default toolbox for admin should be created",
                      FATSuite.server.waitForStringInLog("CWWKX1029I:.*admin"));

        // if default toolbox is loaded, the encrypted presisted file should not exist
        assertFalse("FAIL: toolbox-YWRtaW4=.json file should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ENCRYPTED_ADMIN_JSON));
    }

    /**
     * Loads the toolbox from a json file with missing { (syntax error), make sure it is not loaded.
     * 
     * Test flow:
     * 1. Stop the server
     * 2. Create toolbox.json in &lt;server-output-directory&gt;/resources/adminCenter-1.0
     * 3. Restart the server
     * 4. Request GET the toolbox resource
     * 5. Compare the returned JSON string with the original JSON string, make sure it is the default one.
     */
    @Test
    public void toolboxLoadJSONSyntaxErrorTest() throws Exception {
        restartWithNewPersistedAdminToolbox("badSyntax.json");

        // Confirm the toolbox JSON contents
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertTrue("FAIL: Should have gotten back a default toolbox",
                    metadata.getBoolean("isDefault"));

        // Confirm the bad json syntax message and default toolbox message was printed
        // after the toolbox was loaded
        assertNotNull("The persistence layer did not report the cause for the load problem",
                      FATSuite.server.waitForStringInLog("CWWKX1009E:"));
        assertNotNull("The bad json syntax file should be reported and not be loaded",
                      FATSuite.server.waitForStringInLog("CWWKX1030E:.*admin"));
        assertNotNull("The default toolbox for admin should be created",
                      FATSuite.server.waitForStringInLog("CWWKX1029I:.*admin"));

        // if default toolbox is loaded, the encrypted presisted file should not exist
        assertFalse("FAIL: toolbox-YWRtaW4=.json file should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ENCRYPTED_ADMIN_JSON));
    }

    /**
     * Create default toolbox when there is nothing persisted.
     * 
     * Test flow:
     * 1. Stop the server
     * 2. delete toolbox.json
     * 3. Restart the server
     */
    @Test
    public void toolboxLoadDefaultNothingPersistedTest() throws Exception {
        stopServerAndValidate(FATSuite.server);
        // already clean up by the before and after method
        startServerAndValidate(FATSuite.server);

        // Confirm the toolbox JSON contents
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertTrue("FAIL: Should have gotten back a default toolbox",
                    metadata.getBoolean("isDefault"));

        // Confirm the default toolbox created
        assertNotNull("The default toolbox should be created",
                      FATSuite.server.waitForStringInLog("CWWKX1029I:.*admin"));

        // if default toolbox is loaded and the user has nothing persisted to begin with, both
        // the encrypted and non encrypted presisted file should not be there.
        assertFalse("FAIL: toolbox-YWRtaW4=.json file should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ENCRYPTED_ADMIN_JSON));
        assertFalse("FAIL: toolbox-test/test.json file should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_TEST_JSON));
    }

    /**
     * Add a tool to the toolbox with a POST request, then restart the server, make sure the tool is still available
     * 
     * Test flow:
     * 1. Request POST the toolbox resource
     * 2. Response HTTP Status Code: 201
     * 3. Request GET the toolbox resource of the newly added tool
     * 4. Response HTTP Status Code: 200
     * 5. Response body contains JSON object
     * 6. The JSON object contains the available resources
     * 7. Add the same tool again
     * 8. Response HTTP Status Code: 409
     */
    @SuppressWarnings("unchecked")
    @Test
    public void toolboxAddNewToolandRestartServer() throws Exception {
        // Add a new tool to the toolbox
        String name = "NewGoogle";
        String toolUrl = "http://www.google.com";
        String icon = "https://www.google.com/images/google_favicon_128.png";
        Bookmark bookmark = new Bookmark(name, toolUrl, icon);
        String id = bookmark.getId();
        String bookmarkString = bookmark.toString().substring(9);

        // Post to toolbox
        post(url + "/bookmarks", adminUser, adminPassword, bookmarkString, 201);

        // check how many tools are available
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the tools are correct
        assertContains(response, "bookmarks");
        JsonArray bookmarks = response.getJsonArray("bookmarks");
        assertSize(bookmarks, 1);

        // Once a post is performed, confirm toolbox is persisted with the encrypted file name only.
        assertTrue("FAIL: toolbox-YWRtaW4=.json file does not exist",
        FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ENCRYPTED_ADMIN_JSON));
        assertFalse("FAIL: toolbox-admin.json file should not exist",
        FATSuite.server.fileExistsInLibertyServerRoot(TOOLBOX_ADMIN_JSON));

        // restart server
        stopServerAndValidate(FATSuite.server);
        startServerAndValidate(FATSuite.server);

        // Get the (now persisted)
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        assertNotNull("The server did not report that the persisted toolbox for admin was loaded",
                      FATSuite.server.waitForStringInLog("CWWKX1034I:.*admin"));

        // Confirm the tools are correct
        assertContains(response, "bookmarks");
        bookmarks = response.getJsonArray("bookmarks");
        assertSize(bookmarks, 1);

        // Get the newly created tool from the toolbox
        response = get(url + "/bookmarks/" + id, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        assertContains(response, "id", id);
        assertContains(response, "type", bookmark.getType());
        assertContains(response, "name", name);
        assertContains(response, "url", toolUrl);
        assertContains(response, "icon", icon);

        // Delete the tool from the toolbox
        delete(url + "/bookmarks/" + id, adminUser, adminPassword, 200);
    }

    /**
     * Add a tool to the toolbox with a POST request, remove and restore the Admin Center feature,
     * make sure the tool is still available
     * 
     * Test flow:
     * 1. Request POST the toolbox resource
     * 2. Response HTTP Status Code: 201
     * 3. Request GET the toolbox resource of the newly added tool
     * 4. Response HTTP Status Code: 200
     * 5. Response body contains JSON object
     * 6. The JSON object contains the available resources
     * 7. Add the same tool again
     * 8. Response HTTP Status Code: 409
     */
    @SuppressWarnings("unchecked")
    @Test
    public void toolboxAddNewToolandRecycleAdminCenterFeature() throws Exception {
        // Add a new tool to the toolbox
        String name = "NewTwitter";
        String toolUrl = "http://www.twitter.com";
        String icon = "https://g.twimg.com/Twitter_logo_blue.png";
        Bookmark bookmark = new Bookmark(name, toolUrl, icon);
        String id = bookmark.getId();
        String bookmarkString = bookmark.toString().substring(9);

        // Post to toolbox
        post(url + "/bookmarks", adminUser, adminPassword, bookmarkString, 201);

        // check how many tools are available
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the tools are correct
        assertContains(response, "bookmarks");
        JsonArray bookmarks = response.getJsonArray("bookmarks");
        assertSize(bookmarks, 1);

        // Remove Admin Center feature from server.xml; wait for feature updates.
        List<String> newFeatures = new ArrayList<String>();
        newFeatures.add(F_OSGI);
        FATSuite.server.changeFeatures(newFeatures);
        setMarkAfterFeatureChange(FATSuite.server);
        newFeatures.clear();
        FATSuite.server.changeFeatures(newFeatures);
        setMarkAfterFeatureChange(FATSuite.server);

        // Add Admin Center feature to server.xml; validate that UI started and wait for feature update.
        newFeatures.add(F_UI);
        FATSuite.server.changeFeatures(newFeatures);
        validateUIStarted(FATSuite.server);
        setMarkAfterFeatureChange(FATSuite.server);

        // Get the (now persisted)
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        assertNotNull("The server did not report that the persisted toolbox for admin was loaded",
                      FATSuite.server.waitForStringInLog("CWWKX1034I:.*admin"));

        // Confirm the tools are correct
        assertContains(response, "bookmarks");
        bookmarks = response.getJsonArray("bookmarks");
        assertSize(bookmarks, 1);

        // Get the newly created tool from the toolbox
        response = get(url + "/bookmarks/" + id, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        assertContains(response, "id", id);
        assertContains(response, "type", bookmark.getType());
        assertContains(response, "name", name);
        assertContains(response, "url", toolUrl);
        assertContains(response, "icon", icon);

        // Delete the tool from the toolbox
        delete(url + "/bookmarks/" + id, adminUser, adminPassword, 200);

        // reset back to original features
        newFeatures.add("servlet-5.0");
        FATSuite.server.changeFeatures(newFeatures);
        setMarkAfterFeatureChange(FATSuite.server);
    }

    /**
     * 1. Set the admin toolbox to non-default
     * 2. Log in as admin
     * 3. Confirm toolbox is non-default
     * 4. Log in as bob (who has no persisted toolbox)
     * 5. Confirm bob's toolbox is default (and therefore different from admin)
     * 
     * @throws Exception
     */
    @Test
    public void multiUserTest() throws Exception {
        restartWithNewPersistedAdminToolbox("simpleToolbox.json");

        // Confirm the admin toolbox JSON contents
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertFalse("FAIL: Should have gotten back a non-default toolbox for admin",
                    metadata.getBoolean("isDefault"));

        // Confirm the right message was printed after the toolbox was loaded
        assertNotNull("The server did not report that the persisted toolbox for admin was loaded",
                      FATSuite.server.waitForStringInLog("CWWKX1034I:.*admin"));

        // Confirm the bob toolbox JSON contents
        response = get(url, "bob", "bobpwd", 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertTrue("FAIL: Should have gotten back a default toolbox for bob",
                    metadata.getBoolean("isDefault"));

        // Confirm the right message was printed after the toolbox was loaded
        assertNotNull("The server did not report that the persisted toolbox for admin was loaded",
                      FATSuite.server.waitForStringInLog("CWWKX1029I:.*bob"));
    }
}
