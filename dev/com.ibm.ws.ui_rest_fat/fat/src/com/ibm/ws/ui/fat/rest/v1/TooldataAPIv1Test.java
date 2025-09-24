/*******************************************************************************
 * Copyright (c) 2013, 2025 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.net.HttpURLConnection;

import javax.ws.rs.core.MultivaluedMap;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ui.fat.APIConstants;
import com.ibm.ws.ui.fat.FATSuite;
import com.ibm.ws.ui.fat.rest.CommonRESTTest;
import com.ibm.ws.ui.fat.rest.CommonHttpsRequest;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * The algorithm assessment of FIPS 140-3 by updating SHA512 checksum is based on slack discussion with component SMEs.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class TooldataAPIv1Test extends CommonRESTTest implements APIConstants {
    private static final Class<?> c = TooldataAPIv1Test.class;

    private final String noSuchTooldataEntryURL = API_V1_TOOLDATA + "/toolnamedoesnotexist";
    private final String tooldataExploreEntryURL = API_V1_TOOLDATA + "/com.ibm.websphere.appserver.adminCenter.tool.explore";
    private final String dataString = "{\"key\":\"value\"}";
    private final String dataString1 = "{\"key\":\"value1\"}";
    private final String etagCheckSum1 = "0213f898602b6a489de25f20d8d32c3dbf3d6fee0fbe468f89ac803874ac846f8cd239294a78c2bddf3e0988acb8cd3d00e067a981c9c7933b43a42d3b273eae";
    private final String etagCheckSum2 = "30a9c1549c94169bc284e21ea24d49084b1d661d01a8f6c194b7823bcad429da2aafae945b6a1573994234806fb6000ca092718e8b92c02b062223650e6817e1";

    public TooldataAPIv1Test() {
        super(c);
        url = API_V1_TOOLDATA;
    }

    /**
     * Delete the tool data as pre-condition
     */
    @Before
    public void cleanToolData() throws Exception {
        // ignore any error
        deleteWithStringResponse(tooldataExploreEntryURL, adminUser, adminPassword, -1);

        delete(RESET_CATALOG_URL, adminUser, adminPassword, 200);

        delete(RESET_TOOLBOX_URL, adminUser, adminPassword, 200);

        response = get(API_V1_TOOLBOX, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Confirm the tools are correct
        assertContains(response, "bookmarks");
        assertContains(response, "toolEntries");

        JsonArray toolEntries = response.getJsonArray("toolEntries");
        boolean foundEntry = false;
        for (int i = 0; i < toolEntries.size(); i++) {
            JsonObject tool = toolEntries.getJsonObject(i);
            if ("com.ibm.websphere.appserver.adminCenter.tool.explore-1.0-1.0.0".equals(tool.getString("id")) && 
                "featureTool".equals(tool.getString("type"))) {
                foundEntry = true;
                break;
            }
        }
        assertTrue("FAIL: could not find explore tool in toolbox - this is a pre-condition",
                   foundEntry);

    }

    /**
     * Tests the get tooldata when the tool name is not found.
     *
     * @throws Exception
     */
    @Test
    public void getTooldataWithNonExistToolname() throws Exception {
        get(noSuchTooldataEntryURL, adminUser, adminPassword, 404);
    }

    /**
     * Tests the post tooldata when the tool name is not found.
     *
     * @throws Exception
     */
    @Test
    public void postTooldataWithNonExistToolname() throws Exception {
        getHTTPRequestWithPostPlainText(noSuchTooldataEntryURL, adminUser, adminPassword, dataString, 404);
    }

    /**
     * Tests the put tooldata when the tool name is not found.
     *
     * @throws Exception
     */
    @Test
    public void putTooldataWithNonExistToolname() throws Exception {
        getHTTPRequestWithPutPlainText(noSuchTooldataEntryURL, adminUser, adminPassword, dataString, 404);
    }

    /**
     * Tests the delete tooldata when the tool name is not found.
     *
     * @throws Exception
     */
    @Test
    public void deleteTooldataWithNonExistToolname() throws Exception {
        delete(noSuchTooldataEntryURL, adminUser, adminPassword, 404);
    }

    /**
     * Tests the get/post/put/delete tooldata when the tool name is found.
     *
     * @throws Exception
     */
    @Test
    public void exploreToolDataTest() throws Exception {
        // first try a get, it should return 204 (No content)
        String strResponse = getWithStringResponse(tooldataExploreEntryURL, adminUser, adminPassword, 204);

        // now try a post
        CommonHttpsRequest connection = getHTTPRequestWithPostPlainText(tooldataExploreEntryURL, adminUser, adminPassword, dataString, 201);

        // check etag
        checkETag(connection.getResponseHeaders(), etagCheckSum1);

        // post again should return 409 conflict
        getHTTPRequestWithPostPlainText(tooldataExploreEntryURL, adminUser, adminPassword, dataString, 409);

        // put with no etag should return 412
        getHTTPRequestWithPutPlainText(tooldataExploreEntryURL, adminUser, adminPassword, dataString, 412);

        // put with wrong etag should return 400
        getHTTPRequestWithPutPlainTextWithHeaderWithStringResponse(tooldataExploreEntryURL, adminUser, adminPassword, dataString, "If-Match", "fakemd5", 412);

        // put with correct etag should return 200
        connection = getHTTPRequestWithPutPlainTextWithHeader(tooldataExploreEntryURL, adminUser, adminPassword, dataString1, "If-Match", etagCheckSum1, 200);

        // check returned etag
        checkETag(connection.getResponseHeaders(), etagCheckSum2);

        // delete should return 200
        deleteWithStringResponse(tooldataExploreEntryURL, adminUser, adminPassword, 200);
    }

    /**
     * Tests the get/post/put/delete tooldata when the tool name is found.
     *
     * @throws Exception
     */
    @Test
    public void exploreToolDataTest_reader() throws Exception {
        // first try a get, it should return 204 (No content)
        String strResponse = getWithStringResponse(tooldataExploreEntryURL, readerUser, readerPassword, 204);

        // now try a post
        CommonHttpsRequest connection = getHTTPRequestWithPostPlainText(tooldataExploreEntryURL, readerUser, readerPassword, dataString, 201);

        // check etag
        checkETag(connection.getResponseHeaders(), etagCheckSum1);

        // post again should return 409 conflict
        getHTTPRequestWithPostPlainText(tooldataExploreEntryURL, readerUser, readerPassword, dataString, 409);
        // assertStatusCode("post tool data for existing tool when tool data pre-exists should return 409", 409, response);

        // put with no etag should return 412
        getHTTPRequestWithPutPlainText(tooldataExploreEntryURL, readerUser, readerPassword, dataString, 412);

        // put with wrong etag should return 400
        getHTTPRequestWithPutPlainTextWithHeaderWithStringResponse(tooldataExploreEntryURL, readerUser, readerPassword, dataString, "If-Match", "fakemd5", 412);

        // put with correct etag should return 200
        connection = getHTTPRequestWithPutPlainTextWithHeader(tooldataExploreEntryURL, readerUser, readerPassword, dataString1, "If-Match", etagCheckSum1, 200);

        // check returned etag
        checkETag(connection.getResponseHeaders(), etagCheckSum2);

        //delete should return 200
        strResponse = deleteWithStringResponse(tooldataExploreEntryURL, readerUser, readerPassword, 200);
    }

    /**
     * Access the tooldata/{toolid} resource with a non-Administrator/non-reader user.
     * The tooldata resource requires the user be an Administrator or reader.
     *
     * Test flow:
     * 1. GET the tooldata/{toolid} resource with non-Administrator/non-reader credentials
     * 2. Response HTTP Status Code: 403
     */
    @Test
    public void getTooldataWithNonAdminCredentials() throws Exception {
        get(tooldataExploreEntryURL, nonadminUser, nonadminPassword, 403);
    }

    /**
     * POST to the tooldata/{toolid} resource with a non-Administrator/non-reader user.
     * The tooldata resource requires the user be an Administrator or reader.
     *
     * Test flow:
     * 1. POST the tooldata/{toolid} resource with non-Administrator/non-reader credentials
     * 2. Response HTTP Status Code: 403
     */
    @Test
    public void postTooldataWithNonAdminCredentials() throws Exception {
        getHTTPRequestWithPostPlainText(tooldataExploreEntryURL, nonadminUser, nonadminPassword, dataString, 403);
    }

    /**
     * PUT the tooldata/{toolid} resource with a non-Administrator/non-reader user.
     * The tooldata resource requires the user be an Administrator or reader.
     *
     * Test flow:
     * 1. PUT the tooldata/{toolid} resource with non-Administrator/non-reader credentials
     * 2. Response HTTP Status Code: 403
     */
    @Test
    public void putTooldataWithNonAdminCredentials() throws Exception {
        getHTTPRequestWithPutPlainTextWithHeader(tooldataExploreEntryURL, nonadminUser, nonadminPassword, dataString1, "If-Match", etagCheckSum1, 403);
    }

    /**
     * DELETE a tooldata/{toolid} resource with a non-Administrator/non-reader user.
     * The tooldata resource requires the user be an Administrator or reader.
     *
     * Test flow:
     * 1. DELETE the tooldata/{toolid} resource with non-Administrator/non-reader credentials
     * 2. Response HTTP Status Code: 403
     */
    @Test
    public void deleteTooldataWithNonAdminCredentials() throws Exception {
        delete(tooldataExploreEntryURL, nonadminUser, nonadminPassword, 403);
    }

    private void checkETag(Map<String,List<String>> headers, String expectedETagValue) {
        // make sure etag is set
        assertTrue("The response should contain header 'ETag'.", headers.containsKey("ETag"));

        List<String> list = headers.get("ETag");
        // make sure only 1 etag
        assertEquals("Should return only 1 etag header.", 1, list.size());

        // make sure etag is correct
        assertEquals("Unexpected etag value.", expectedETagValue, list.get(0));
    }

}
