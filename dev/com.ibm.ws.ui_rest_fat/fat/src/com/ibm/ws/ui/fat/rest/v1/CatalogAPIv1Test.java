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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ui.fat.APIConstants;
import com.ibm.ws.ui.fat.FATSuite;
import com.ibm.ws.ui.fat.rest.CommonRESTTest;
import com.ibm.ws.ui.fat.Bookmark;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Validate the version 1 catalog resource. This resource only supports
 * GET requests and requires that the user be an authenticated and authorized
 * user.
 * <p>
 * This is not an exhaustive set of tests as we can rely on unit tests in
 * many cases. The amount of variants for accessing this API is limited in
 * the FAT.
 * </p>
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class CatalogAPIv1Test extends CommonRESTTest implements APIConstants {
    private static final Class<?> c = CatalogAPIv1Test.class;
    private final String bookmarksURL = API_V1_CATALOG + "/bookmarks/";
    private final String noSuchFeatureToolURL = API_V1_CATALOG + "/featureTools/idontexist-0.0";
    private final String featureToolURL = API_V1_CATALOG + "/featureTools/com.ibm.websphere.appserver.adminCenter.tool.explore-1.0-1.0.0";
    private final String noSuchBookmarkURL = API_V1_CATALOG + "/bookmarks/idontexist-0.0";
    private final String bookmarkURL = API_V1_CATALOG + "/bookmarks/openliberty.io";

    public CatalogAPIv1Test() {
        super(c);
        url = API_V1_CATALOG;
    }

    /**
     * Reset the catalog to the default state before each test.
     */
    @Before
    public void resetCatalog() throws Exception {
        delete(RESET_CATALOG_URL, adminUser, adminPassword, 200);
    }

    /**
     * Access the catalog resource with a GET request.
     * The catalog resource supports GET requests, and returns a JSON
     * object which contains all of the tools within the catalog.
     *
     * Test flow:
     * 1. Request GET the catalog resource
     * 2. Response HTTP Status Code: 200
     * 3. The JSON object contains the available tools
     */
    @Test
    public void getCatalog() throws Exception {
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertSize(response, 3);

        // Confirm the '_metadata' field is correct
        assertContains(response, "_metadata");

        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault", true, true);
        assertContains(metadata, "lastModified");

        // Confirm the tools are correct
        assertContains(response, "featureTools");
        assertContains(response, "bookmarks");

        JsonArray bookmarks = response.getJsonArray("bookmarks");
        assertSize(bookmarks, 1);

        JsonObject openlibertyio = bookmarks.getJsonObject(0);
        assertSize(openlibertyio, 6);
        assertContains(openlibertyio, "id", "openliberty.io");
        assertContains(openlibertyio, "type", "bookmark");
        assertContains(openlibertyio, "name", "openliberty.io");
        assertContains(openlibertyio, "url", "https://openliberty.io");
        assertContains(openlibertyio, "icon", "images/tools/Open_Liberty_square_142x142.png");
        assertContains(openlibertyio, "description", "Open Liberty website.");
    }

    /**
     * Access the catalog resource with a GET request as a reader-role user.
     * The catalog resource supports GET requests, and returns a JSON
     * object which contains all of the tools within the catalog.
     *
     * Test flow:
     * 1. Request GET the catalog resource
     * 2. Response HTTP Status Code: 200
     * 3. The JSON object contains the available tools
     */
    @Test
    public void getCatalog_reader() throws Exception {
        response = get(url, readerUser, readerPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertSize(response, 3);

        // Confirm the '_metadata' field is correct
        assertContains(response, "_metadata");

        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault", true, true);
        assertContains(metadata, "lastModified");

        // Confirm the tools are correct
        assertContains(response, "featureTools");
        assertContains(response, "bookmarks");

        JsonArray bookmarks = response.getJsonArray("bookmarks");
        assertSize(bookmarks, 1);

        JsonObject openlibertyio = bookmarks.getJsonObject(0);
        assertSize(openlibertyio, 6);
        assertContains(openlibertyio, "id", "openliberty.io");
        assertContains(openlibertyio, "type", "bookmark");
        assertContains(openlibertyio, "name", "openliberty.io");
        assertContains(openlibertyio, "url", "https://openliberty.io");
        assertContains(openlibertyio, "icon", "images/tools/Open_Liberty_square_142x142.png");
        assertContains(openlibertyio, "description", "Open Liberty website.");
    }

    /**
     * Attempt to GET the catalog with no role
     */
    @Test
    public void getCatalog_nonAdmin() throws Exception {
        get(url, nonadminUser, nonadminPassword, 403);
    }

    /**
     * Access the catalog resource with a GET request filtering on just the
     * top-level metadata.
     *
     * Test flow:
     * 1. Request GET the catalog resource filtering for just metadata
     * 2. Response HTTP Status Code: 200
     * 3. The JSON object contains only the metadata
     */
    @Test
    public void getCatalog_filtered() throws Exception {
        response = get(url + "?fields=_metadata", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertSize(response, 1);

        // Confirm the '_metadata' field is correct
        assertContains(response, "_metadata");

        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault", true, true);
        assertContains(metadata, "lastModified");
    }

    /**
     * Access the catalog resource with a GET request filtering on tool fields.
     *
     * Test flow:
     * 1. Request GET the catalog resource filtering for specific tool fields
     * 2. Response HTTP Status Code: 200
     * 3. The JSON object contains tools with only the specified fields
     */
    @Test
    public void getCatalog_filteredFeatureTools() throws Exception {
        response = get(url + "?fields=featureTools.id,featureTools.name,featureTools.icon", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertSize(response, 1);

        // Confirm the tools are correct
        assertContains(response, "featureTools");

        @SuppressWarnings("unchecked")
        List<Map<?, ?>> featureTools = (List<Map<?, ?>>) response.get("featureTools");
        for (Map<?, ?> tool : featureTools) {
            assertSize(tool, 3);
            assertContains(tool, "id");
            assertContains(tool, "name");
            assertContains(tool, "icon");
        }
    }

    /**
     * Access the catalog resource with a POST request.
     * The catalog resource does not support POST requests.
     *
     * Test flow:
     * 1. Request POST the catalog resource
     * 2. Response HTTP Status Code: 405
     */
    @Test
    public void postCatalog_unsupported() throws Exception {
        post(url, adminUser, adminPassword, null, 405);
    }

    /**
     * Access the catalog resource with a PUT request.
     * The catalog resource does not support PUT requests.
     *
     * Test flow:
     * 1. Request PUT the catalog resource
     * 2. Response HTTP Status Code: 405
     */
    @Test
    public void putCatalog_unsupported() throws Exception {
        put(url, adminUser, adminPassword, null, 405);
    }

    /**
     * Access the catalog resource with a DELETE request.
     * The catalog resource will be reset when the correct request parameter
     * 'resetCatalog=true' is provided. This scenario does not provide the
     * request parameter.
     *
     * Test flow:
     * 1. Request DELETE the catalog resource w/ no confirmation param
     * 2. Response HTTP Status Code: 400
     * 3. Response Entity: CWWKX1024E
     */
    @Test
    public void deleteCatalog_resetNotConfirmed() throws Exception {
        response = delete(url, adminUser, adminPassword, 400);
        Log.info(c, method.getMethodName(), "delete catalog returns with response: " + response);
        assertContains(response, "message");
        String msg = response.getString("message");
        assertTrue("FAIL: The catalog should respond with message CWWKX1024E but got: " + msg,
                    msg.startsWith("CWWKX1024E"));
    }

    /**
     * Access the catalog resource with a DELETE request.
     * The catalog resource will be reset when the correct request parameter
     * 'resetCatalog=true' is provided. This scenario provides the
     * request parameter.
     *
     * Test flow:
     * 1. Request DELETE the catalog resource w/ confirmation param
     * 2. Response HTTP Status Code: 200
     * 3. Response Entity: CWWKX1023I
     */
    @Test
    public void deleteCatalog_resetConfirmed() throws Exception {
        response = delete(url + "?resetCatalog=true", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "delete catalog returns with response: " + response);
        assertContains(response, "message");
        String msg = response.getString("message");
        assertTrue("FAIL: The catalog should respond with message CWWKX1023I but got: " + msg,
                    msg.startsWith("CWWKX1023I"));
    }

    /**
     * Access the catalog resource with a DELETE request as reader.
     * The catalog resource requires the user be an Administrator.
     *
     * Test flow:
     * 1. GET the catalog resource with non-Administrator credentials
     * 2. Response HTTP Status Code: 403
     */
    @Test
    public void deleteCatalog_reader() throws Exception {
        delete(url + "?resetCatalog=true", readerUser, readerPassword, 403);
    }

    /**
     * Access the catalog resource with a DELETE request as nonreader/nonadmin.
     * The catalog resource requires the user be an Administrator.
     *
     * Test flow:
     * 1. GET the catalog resource with non-Administrator credentials
     * 2. Response HTTP Status Code: 403
     */
    @Test
    public void deleteCatalog_nonadmin() throws Exception {                      
        delete(url + "?resetCatalog=true", nonadminUser, nonadminPassword, 403);
    }

    @Test
    public void getMetadata_ok() throws Exception {
        get(url + "/_metadata", adminUser, adminPassword, 200);
    }

    @Test
    public void getMetadata_ok_reader() throws Exception {
        get(url + "/_metadata", readerUser, readerPassword, 200);
    }

    @Test
    public void getMetadata_nonadmin() throws Exception {
        get(url + "/_metadata", nonadminUser, nonadminPassword, 403);
    }

    @Test
    public void postMetadata_unsupported() throws Exception {
        post(url + "/_metadata", adminUser, adminPassword, null, 405);
    }

    @Test
    public void putMetadata_unsupported() throws Exception {
        put(url + "/_metadata", adminUser, adminPassword, null, 405);
    }

    @Test
    public void deleteMetadata_unsupported() throws Exception {
        delete(url + "/_metadata", adminUser, adminPassword, 405);
    }

    @Test
    public void getFeatureTools_ok() throws Exception {
        getWithJsonArrayResponse(url + "/featureTools", adminUser, adminPassword, 200);
    }

    @Test
    public void getFeatureTools_ok_reader() throws Exception {
        getWithJsonArrayResponse(url + "/featureTools", readerUser, readerPassword, 200);
    }

    @Test
    public void getFeatureTools_nonadmin() throws Exception {
        get(url + "/featureTools", nonadminUser, nonadminPassword, 403);
    }

    @Test
    public void postFeatureTools_unsupported() throws Exception {
        post(url + "/featureTools", adminUser, adminPassword, null, 405);
    }

    @Test
    public void putFeatureTools_unsupported() throws Exception {
        put(url + "/featureTools", adminUser, adminPassword, null, 405);
    }

    @Test
    public void deleteFeatureTools_unsupported() throws Exception {
        delete(url + "/featureTools", adminUser, adminPassword, 405);
    }

    @Test
    public void getBookmarks_ok() throws Exception {
        getWithJsonArrayResponse(bookmarksURL, adminUser, adminPassword, 200);
    }

    @Test
    public void getBookmarks_ok_reader() throws Exception {
        getWithJsonArrayResponse(bookmarksURL, readerUser, readerPassword, 200);
    }

    /**
     * Attempt to GET bookmarks without role
     */
    @Test
    public void getBookmarks_nonadmin() throws Exception {
        get(bookmarksURL, nonadminUser, nonadminPassword, 403);
    }

    /**
     * Attempt to GET a bookmark without role
     */
    @Test
    public void getBookmark_nonadmin() throws Exception {
        get(bookmarkURL, nonadminUser, nonadminPassword, 403);
    }

    /**
     * Add a null tool to the catalog with a POST request.
     *
     * Test flow:
     * 1. Request POST a catalog tool resource
     * 2. Response HTTP Status Code: 400
     */
    @Test
    public void postBookmarks_nullPayload() throws Exception {
        post(bookmarksURL, adminUser, adminPassword, null, 400);
    }

    /**
     * Add a tool with bad JSON syntax to the catalog with a POST request.
     *
     * Test flow:
     * 1. Request POST the catalog resource
     * 2. Response HTTP Status Code: 400
     */
    @Test
    public void postBookmarks_badJson() throws Exception {
        String badJson = "{";
        post(bookmarksURL, adminUser, adminPassword, badJson, 400);
    }

    /**
     * Add a tool with wrong JSON object to the catalog with a POST request.
     *
     * Test flow:
     * 1. Request POST the catalog resource
     * 2. Response HTTP Status Code: 400
     */
    @Test
    public void postBookmarks_wrongObjectJSON() throws Exception {
        String badJson = "{'badField': true}";
        post(bookmarksURL, adminUser, adminPassword, badJson, 400);
    }

    /**
     * Attempt to POST a bookmark without role
     */
    @Test
    public void postBookmarks_nonadmin() throws Exception {
        String name = "Google";
        String toolUrl = "http://google.com";
        String icon = "https://www.google.com/images/google_favicon_128.png";
        String description = "Google front page";
        Bookmark bookmark = new Bookmark(name, toolUrl, icon, description);

        post(bookmarksURL, nonadminUser, nonadminPassword, bookmark, 403);
    }

    /**
     * Add a bookmark to the catalog with a POST request.
     *
     * Test flow:
     * 1. Request POST the catalog resource as reader
     * 2. Response HTTP Status Code: 403
     * 1. Request POST the catalog resource as admin
     * 2. Response HTTP Status Code: 201
     * 3. Request GET the catalog resource of the newly added tool
     * 4. Response HTTP Status Code: 200
     * 5. Response body contains JSON object
     * 6. The JSON object contains the available resources
     * 7. Add the same tool again
     * 8. Response HTTP Status Code: 409
     */
    @Test
    public void postBookmarks_addNewBookmark() throws Exception {
        // Add a new tool to the catalog
        String name = "Google";
        String toolUrl = "http://google.com";
        String icon = "https://www.google.com/images/google_favicon_128.png";
        String description = "Google front page";
        Bookmark Bookmark = new Bookmark(name, toolUrl, icon, description);
        String id = Bookmark.getId();
        String type = Bookmark.getType();
        String bookmarkString = Bookmark.toString().substring(9); // take out "Bookmark "

        Log.info(c, method.getMethodName(), "bookmarkString is " + bookmarkString);

        response = post(bookmarksURL, readerUser, readerPassword, bookmarkString, 403);

        response = post(bookmarksURL, adminUser, adminPassword, bookmarkString, 201);
        Log.info(c, method.getMethodName(), "response: from POST " + response);

        // Bookmark createdTool = response.getEntity(Bookmark.class);
        assertSize(response, 6);
        assertContains(response, "id", id);
        assertContains(response, "type", type);
        assertContains(response, "name", name);
        assertContains(response, "url", toolUrl);
        assertContains(response, "icon", icon);
        assertContains(response, "description", description);

        // Get the newly created tool from the catalog
        response = get(bookmarksURL + id, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "response: from GET " + response);
        assertSize(response, 6);
        assertContains(response, "id", id);
        assertContains(response, "type", type);
        assertContains(response, "name", name);
        assertContains(response, "url", toolUrl);
        assertContains(response, "icon", icon);
        assertContains(response, "description", description);

        // Add the same tool again and expect to see an exception
        response = post(bookmarksURL, adminUser, adminPassword, bookmarkString, 409);
    }

    /**
     * Add a tool with missing icon field JSON object to the catalog with a POST request.
     *
     * Test flow:
     * 1. Request POST the catalog resource
     * 2. Response HTTP Status Code: 400
     */
    @Test
    public void postBookmarks_withMissingIconField() throws Exception {
        String badJson = "{" +
                         "\"name\": \"Test Bookmark\"," +
                         "\"url\": \"http://ibm.com\"," +
                         "\"description\": \"A simple test bookmark\"" +
                         "}\"";

        response = post(bookmarksURL, adminUser, adminPassword, badJson, 400);
        Log.info(c, method.getMethodName(), "response: from POST " + response);

        assertContains(response, "status");
        int status = response.getInt("status");
        assertTrue("FAIL: error message response did not have the correct status code of 400",
                    (400 == status));
        assertContains(response, "message");
        String msg = response.getString("message");
        assertTrue("FAIL: error message response did not have the correct message",
                    msg.startsWith("CWWKX1008E"));
    }

    @Test
    public void putBookmarks_unsupported() throws Exception {
        put(bookmarksURL, adminUser, adminPassword, null, 405);
    }

    @Test
    public void deleteBookmarks_unsupported() throws Exception {
        delete(bookmarksURL, adminUser, adminPassword, 405);
    }

    @Test
    public void getFeatureTool_ok() throws Exception {
        get(featureToolURL, adminUser, adminPassword, 200);
    }

    @Test
    public void getFeatureTool_ok_reader() throws Exception {
        get(featureToolURL, readerUser, readerPassword, 200);
    }

    /**
     * Attempt to GET a featureTool without a role
     */
    @Test
    public void getFeatureTool_nonadmin() throws Exception {
        get(featureToolURL, nonadminUser, nonadminPassword, 403);
    }

    @Test
    public void getFeatureTool_doesntExist() throws Exception {
        get(noSuchFeatureToolURL, adminUser, adminPassword, 404);
    }

    @Test
    public void postFeatureTool_unsupported() throws Exception {
        post(featureToolURL, adminUser, adminPassword, null, 405);
    }

    @Test
    public void postFeatureTool_doesntExist() throws Exception {
        post(noSuchFeatureToolURL, adminUser, adminPassword, null, 404);
    }

    @Test
    public void putFeatureTool_unsupported() throws Exception {
        put(featureToolURL, adminUser, adminPassword, null, 405);
    }

    @Test
    public void putFeatureTool_doesntExist() throws Exception {
        put(noSuchFeatureToolURL, adminUser, adminPassword, null, 404);
    }

    @Test
    public void deleteFeatureTool_unsupported() throws Exception {
        delete(featureToolURL, adminUser, adminPassword, 405);
    }

    @Test
    public void deleteFeatureTool_doesntExist() throws Exception {
        delete(noSuchFeatureToolURL, adminUser, adminPassword, 404);
    }

    /**
     * Access the version 1 API root page with a GET request.
     * A catalog tool resource only supports GET requests, and returns a JSON
     * object which contains links to the available version 1 API resources.
     *
     * Test flow:
     * 1. Request GET the version 1 API root page
     * 2. Response HTTP Status Code: 200
     * 3. The JSON object contains the available resources
     */
    @Test
    public void getBookmark() throws Exception {
        response = get(bookmarkURL, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertSize(response, 6);
        assertContains(response, "id", "openliberty.io");
        assertContains(response, "type", "bookmark");
        assertContains(response, "name", "openliberty.io");
        assertContains(response, "url", "https://openliberty.io");
        assertContains(response, "icon", "images/tools/Open_Liberty_square_142x142.png");
        assertContains(response, "description", "Open Liberty website.");
    }

    /**
     * Access the version 1 API root page with a GET request as reader.
     * A catalog tool resource only supports GET requests, and returns a JSON
     * object which contains links to the available version 1 API resources.
     *
     * Test flow:
     * 1. Request GET the version 1 API root page
     * 2. Response HTTP Status Code: 200
     * 3. The JSON object contains the available resources
     */
    @Test
    public void getBookmark_reader() throws Exception {
        response = get(bookmarkURL, readerUser, readerPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertSize(response, 6);
        assertContains(response, "id", "openliberty.io");
        assertContains(response, "type", "bookmark");
        assertContains(response, "name", "openliberty.io");
        assertContains(response, "url", "https://openliberty.io");
        assertContains(response, "icon", "images/tools/Open_Liberty_square_142x142.png");
        assertContains(response, "description", "Open Liberty website.");
    }

    @Test
    public void getBookmark_filtered() throws Exception {
        response = get(bookmarkURL + "?fields=name,icon", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertSize(response, 2);
        assertContains(response, "name", "openliberty.io");
        assertContains(response, "icon", "images/tools/Open_Liberty_square_142x142.png");
    }

    /**
     * Access a catalog tool resource which does not actually exist.
     *
     * Test flow:
     * 1. GET a catalog tool resource which does not exist
     * 2. Response HTTP Status Code: 404
     * 3. Validate returned JSON payload
     *
     * Example JSON payload:
     *
     * <pre>
     * HTTP Status Code (404)
     * {
     * "status": Int (HTTP status code)
     * "message": String (Translated message w/ PII ID)
     * "userMessage": Actions available to the user (if applicable)
     * "developerMessage": Actions available to the developer (if applicable)
     * }
     * </pre>
     */
    @Test
    public void getBookmark_doesntExist() throws Exception {
        response = get(noSuchBookmarkURL, adminUser, adminPassword, 404);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertContains(response, "status");
        int status = response.getInt("status");
        assertTrue("FAIL: The returned message did not have the correct status code of 404",
                    (404 == status));
        assertContains(response, "message");
        String msg = response.getString("message");
        assertTrue("FAIL: The returned message did not contain the expected message",
                    msg.matches("CWWKX1001E:.*idontexist-0.0.*"));
    }

    /**
     * Access a catalog tool resource with a POST request.
     * Catalog tool resources do not support POST requests.
     *
     * Test flow:
     * 1. Request POST a catalog tool resource
     * 2. Response HTTP Status Code: 405
     */
    @Test
    public void postBookmark_unsupported() throws Exception {
        post(bookmarkURL, adminUser, adminPassword, null, 405);
    }

    @Test
    public void postBookmark_doesntExist() throws Exception {
        post(noSuchBookmarkURL, adminUser, adminPassword, null, 404);
    }

    /**
     * Access a catalog tool resource with a PUT request.
     * Catalog tool resources do not support PUT requests.
     *
     * Test flow:
     * 1. Request PUT a catalog tool resource
     * 2. Response HTTP Status Code: 405
     */
    @Test
    public void putBookmark_unsupported() throws Exception {
        put(bookmarkURL, adminUser, adminPassword, null, 405);
    }

    @Test
    public void putBookmark_doesntExist() throws Exception {
        put(noSuchBookmarkURL, adminUser, adminPassword, null, 404);
    }

    /**
     * Delete an existing bookmark from the catalog with a DELETE request.
     *
     * Test flow:
     * 1. Request DELETE the catalog resource as reader
     * 2. Response HTTP Status Code: 403
     * 3. Request DELETE the catalog resource as admin
     * 4. Response HTTP Status Code: 200
     */
    @Test
    public void deleteBookmark() throws Exception {
        // setup: create a new tool to be deleted
        String name = "Delete";
        String toolUrl = "http://google.com";
        String description = "Google front page";
        String icon = "https://www.google.com/images/google_favicon_128.png";
        Bookmark featureTool = new Bookmark(name, toolUrl, icon, description);
        String toolId = featureTool.getId();
        String featureToolString = featureTool.toString().substring(9);

        response = post(bookmarksURL, adminUser, adminPassword, featureToolString, 201);
        Log.info(c, method.getMethodName(), "response: from POST " + response);

        assertContains(response, "id", toolId);

        response = delete(bookmarksURL + toolId, readerUser, readerPassword, 403);
        response = delete(bookmarksURL + toolId, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "response: from DELETE " + response);

        assertContains(response, "id", toolId);

        // verify the tool is actually deleted from the catalog by deleting it again
        response = delete(bookmarksURL + toolId, adminUser, adminPassword, 404);
        Log.info(c, method.getMethodName(), "response: from DELETE " + response);

        assertContains(response, "message");
        String msg = response.getString("message");
        assertTrue("FAIL: The returned message did not contain the expected message",
                    msg.matches("CWWKX1001E:.*" + toolId + ".*"));
    }

    /**
     * Delete a non-existing tool from the catalog with a DELETE request.
     *
     * Test flow:
     * 1. Request DELETE the catalog resource
     * 2. Response HTTP Status Code: 404
     */
    @Test
    public void deleteBookmark_doesntExist() throws Exception {
        response = delete(noSuchBookmarkURL, adminUser, adminPassword, 404);
        Log.info(c, method.getMethodName(), "response: from DELETE " + response);

        assertContains(response, "status");
        int status = response.getInt("status");
        assertTrue("FAIL: The returned message did not have the correct status code of 404",
                    (404 == status));
        assertContains(response, "message");
        String msg = response.getString("message");
        assertTrue("FAIL: The returned message did not contain the expected message",
                    msg.matches("CWWKX1001E:.*idontexist-0.0.*"));
    }

    /**
     * Attempt to DELETE a bookmark with no role
     */
    @Test
    public void deleteBookmark_nonadmin() throws Exception {
        delete(bookmarkURL, nonadminUser, nonadminPassword, 403);
    }

    /**
     * Access the catalog resource with a non-Administrator user.
     * The catalog resource requires the user be an Administrator.
     *
     * Test flow:
     * 1. GET the catalog resource with non-Administrator credentials
     * 2. Response HTTP Status Code: 403
     */
    @Test
    public void catalogNonAdminCredentials() throws Exception {
        get(url, nonadminUser, nonadminPassword, 403);
    }

}
