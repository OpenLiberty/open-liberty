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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ui.fat.APIConstants;
import com.ibm.ws.ui.fat.FATSuite;
import com.ibm.ws.ui.fat.rest.CommonRESTTest;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Persistence test on tool data
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class TooldataPersistenceTest extends CommonRESTTest implements APIConstants {
    private static final Class<?> c = TooldataPersistenceTest.class;

    private static final String EXPLORE_DIR = "com.ibm.websphere.appserver.adminCenter.tool.explore";
    private final String tooldataExploreEntryURL = API_V1_TOOLDATA + "/" + EXPLORE_DIR;
    private static final String dataString = "{\"key\":\"value\"}";

    private static final String EXPLORE_TOOL_PERSISTENCE_DIR = FATSuite.RESOURCES_ADMIN_CENTER_1_0 + EXPLORE_DIR + "/";
    private static final String EXPLORE_TOOL_DATA_ADMIN_JSON = EXPLORE_TOOL_PERSISTENCE_DIR + "admin.json";
    private static final String EXPLORE_TOOL_DATA_ENCRYPTED_ADMIN_JSON = EXPLORE_TOOL_PERSISTENCE_DIR + "YWRtaW4=.json";
    private static final String EXPLORE_TOOL_DATA_TEST_JSON = EXPLORE_TOOL_PERSISTENCE_DIR + "test/test.json";
    private static final String EXPLORE_TOOL_DATA_ENCRYPTED_TEST_JSON = EXPLORE_TOOL_PERSISTENCE_DIR + "dGVzdC90ZXN0.json";

    public TooldataPersistenceTest() {
        super(c);
        url = API_V1_TOOLDATA;
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        Log.info(c, "tearDownClass", "Removing persistence directory: " + RESOURCES_ADMIN_CENTER_1_0);
        FATSuite.server.deleteDirectoryFromLibertyServerRoot(RESOURCES_ADMIN_CENTER_1_0);
        FATSuite.server.restartServer();
    }

    /**
     * Delete the tool data as pre-condition
     */
    @Before
    public void cleanAllToolData() throws Exception {
        Log.info(c, "cleanAllToolData", "Cleaning up data and restarting server before start of each test");
        cleanToolData(adminUser, adminPassword);
        cleanToolData(testUser, testPassword);
    }

    private void cleanToolData(String user, String password) throws Exception {
        // ignore error
        deleteWithStringResponse(tooldataExploreEntryURL, user, password, -1);
    }

    /**
     * @param useEncryptedName
     * @throws Exception
     */
    private void setAdminToolDataFileFromTestFile(boolean useEncryptedName) throws Exception {
        File toolDir = new File (EXPLORE_TOOL_PERSISTENCE_DIR);
        if (!toolDir.exists()) {
            toolDir.mkdirs();
            assertTrue("Fail to setup Explore tool persistence directory for admin user", 
                        toolDir.exists());
        }
        String fileToBeCopied = "admin.json";
        if (useEncryptedName) {
            fileToBeCopied = "YWRtaW4=.json";
        }
        FATSuite.server.copyFileToLibertyServerRoot(EXPLORE_TOOL_PERSISTENCE_DIR, "persistence/tool/" + fileToBeCopied);

        if (useEncryptedName) {
            assertFalse("FAIL: pre test Explore tool data file for admin should not exist",
                        FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ADMIN_JSON));
            assertTrue("FAIL: pre test Explore tool encrypted data file name for admin should exist",
                        FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ENCRYPTED_ADMIN_JSON));
        } else {
            assertTrue("FAIL: pre test Explore tool data file for admin should exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ADMIN_JSON));
            assertFalse("FAIL: pre test Explore tool encrypted data file name for admin should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ENCRYPTED_ADMIN_JSON));
        }
    }

    /**
     * @param useEncryptedName
     * @throws Exception
     */
    private void setTestUserToolDataFileFromTestFile(boolean useEncryptedName) throws Exception {
        String toolDirPath = EXPLORE_TOOL_PERSISTENCE_DIR;
        String fileToBeCopied = "dGVzdC90ZXN0.json";
        if (!useEncryptedName) {
            toolDirPath += "test";
            fileToBeCopied = "test.json";
        }
        File toolDir = new File (toolDirPath);
        if (!toolDir.exists()) {
            toolDir.mkdirs();
            assertTrue("Fail to setup Explore tool persistence directory for test user", 
                        toolDir.exists());
        }

        FATSuite.server.copyFileToLibertyServerRoot(toolDirPath, "persistence/tool/" + fileToBeCopied);

        if (useEncryptedName) {
            assertFalse("FAIL: pre test Explore tool data file for test/test should not exist",
                        FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_TEST_JSON));
            assertTrue("FAIL: pre test Explore tool encrypted data file name for test/test should exist",
                        FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ENCRYPTED_TEST_JSON));
        } else {
            assertTrue("FAIL: pre test Explore tool data file for test/test should exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_TEST_JSON));
            assertFalse("FAIL: pre test Explore tool encrypted data file name for test/test should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ENCRYPTED_TEST_JSON));
        }
    }

    /**
     * Test with no persistence tool data using admin user
     *
     * @throws Exception
     */
    @Test
    public void exploreToolDataAdminWithNoPersistenceData() throws Exception {
        get(API_V1_TOOLBOX, adminUser, adminPassword, 200);

        // first try a get, it should return 204 (No content)
        getWithStringResponse(tooldataExploreEntryURL, adminUser, adminPassword, 204);

        assertFalse("FAIL: pre test Explore tool data file for admin should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ADMIN_JSON));

        assertFalse("FAIL: pre test Explore tool encrypted data file name for admin should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ENCRYPTED_ADMIN_JSON));

        // now try a post
        getHTTPRequestWithPostPlainText(tooldataExploreEntryURL, adminUser, adminPassword, dataString, 201);
        assertFalse("FAIL: Explore tool data file for admin should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ADMIN_JSON));
        assertTrue("FAIL: Explore tool encrypted data file name for admin should exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ENCRYPTED_ADMIN_JSON));
    }

    /**
     * Test with persistence tool data in encrypted file name using admin user
     *
     * @throws Exception
     */
    @Test
    public void exploreToolDataAdminWithPersistenceDataInEncryptedFileName() throws Exception {
        setAdminToolDataFileFromTestFile(true);
        get(API_V1_TOOLBOX, adminUser, adminPassword, 200);

        get(tooldataExploreEntryURL, adminUser, adminPassword, 200);

        assertFalse("FAIL: Explore tool data file for admin should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ADMIN_JSON));
        assertTrue("FAIL: Explore tool encrypted data file name for admin should exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ENCRYPTED_ADMIN_JSON));
    }

    /**
     * Test with persistence tool data in non-encrypted file name using admin user.
     * The non-encrypted file name will be prompted to encrypted file name.
     *
     * @throws Exception
     */
    @Test
    public void exploreToolDataAdminWithPersistenceDataInNonEncryptedFileName() throws Exception {
        setAdminToolDataFileFromTestFile(false);
        get(API_V1_TOOLBOX, adminUser, adminPassword, 200);

        get(tooldataExploreEntryURL, adminUser, adminPassword, 200);

        assertFalse("FAIL: Explore tool data file for admin should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ADMIN_JSON));
        assertTrue("FAIL: Explore tool encrypted data file name for admin should exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ENCRYPTED_ADMIN_JSON));
    }

    /**
     * Test with no persistence tool data using test/test user
     *
     * @throws Exception
     */
    @Test
    public void exploreToolDataTestWithNoPersistenceData() throws Exception {
        get(API_V1_TOOLBOX, testUser, testPassword, 200);

        // first try a get, it should return 204 (No content)
        getWithStringResponse(tooldataExploreEntryURL, testUser, testPassword, 204);

        assertFalse("FAIL: pre test Explore tool data file for test/test should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_TEST_JSON));

        assertFalse("FAIL: pre test Explore tool encrypted data file name for test/test should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ENCRYPTED_TEST_JSON));

        // now try a post
        getHTTPRequestWithPostPlainText(tooldataExploreEntryURL, testUser, testPassword, dataString, 201);
        assertFalse("FAIL: Explore tool data file for test/test should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_TEST_JSON));
        assertTrue("FAIL: Explore tool encrypted data file name for test/test should exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ENCRYPTED_TEST_JSON));
    }

    /**
     * Test with persistence tool data in encrypted file name using test/test user
     *
     * @throws Exception
     */
    @Test
    public void exploreToolDataTestWithPersistenceDataInEncryptedFileName() throws Exception {
        setTestUserToolDataFileFromTestFile(true);
        get(API_V1_TOOLBOX, testUser, testPassword, 200);

        get(tooldataExploreEntryURL, testUser, testPassword, 200);

        assertFalse("FAIL: Explore tool data file for test/test should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_TEST_JSON));
        assertTrue("FAIL: Explore tool encrypted data file name for test/test should exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ENCRYPTED_TEST_JSON));
    }

    /**
     * Test with persistence tool data in non-encrypted file name using test/test user.
     * The non-encrypted file name will be prompted to encrypted file name.
     *
     * @throws Exception
     */
    @Test
    public void exploreToolDataTestWithPersistenceDataInNonEncryptedFileName() throws Exception {
        setTestUserToolDataFileFromTestFile(false);
        get(API_V1_TOOLBOX, testUser, testPassword, 200);

        get(tooldataExploreEntryURL, testUser, testPassword, 200);

        assertFalse("FAIL: Explore tool data file for test/test should not exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_TEST_JSON));
        assertTrue("FAIL: Explore tool encrypted data file name for test/test should exist",
                    FATSuite.server.fileExistsInLibertyServerRoot(EXPLORE_TOOL_DATA_ENCRYPTED_TEST_JSON));
    }
}