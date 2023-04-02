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
import static org.junit.Assert.assertFalse;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ui.fat.APIConstants;
import com.ibm.ws.ui.fat.FATSuite;
import com.ibm.ws.ui.fat.rest.CommonRESTTest;
import com.ibm.ws.ui.fat.Bookmark;
import com.ibm.ws.ui.fat.ToolEntry;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class ToolboxAPIv1Test extends CommonRESTTest implements APIConstants {
    private static final Class<?> c = ToolboxAPIv1Test.class;

    private final String noSuchToolEntryURL = API_V1_TOOLBOX + "/toolEntries/idontexist-0.0";
    private final String toolEntryURL = API_V1_TOOLBOX + "/toolEntries/com.ibm.websphere.appserver.adminCenter.tool.explore-1.0-1.0.0";
    private final String noSuchBookmarkURL = API_V1_TOOLBOX + "/bookmarks/idontexist-0.0";
    private final String bookmarkURL = API_V1_TOOLBOX + "/bookmarks/Test+Bookmark";

    public ToolboxAPIv1Test() {
        super(c);
        url = API_V1_TOOLBOX;
    }

    /**
     * Reset the catalog and the toolbox to the default state.
     */
    public void resetCatalogAndToolbox() throws Exception {
        delete(RESET_CATALOG_URL, adminUser, adminPassword, 200);

        delete(RESET_TOOLBOX_URL, adminUser, adminPassword, 200);

        delete(RESET_TOOLBOX_URL, readerUser, readerPassword, 200);
    }

    /**
     * Attempts to find a ToolEntry with the matching id and type.
     *
     * @param toolEntries
     * @param id
     * @param type
     */
    private void findToolEntry(JsonArray toolEntries, String id, String type) {
        boolean foundEntry = false;
        // for (Map<?, ?> tool : toolEntries) {
        for (int i = 0; i < toolEntries.size(); i++) {
            JsonObject tool = toolEntries.getJsonObject(i);
            if (id.equals(tool.getString("id")) && type.equals(tool.getString("type"))) {
                foundEntry = true;
                break;
            }
        }
        assertTrue("FAIL: could not find tool entry for " + id + " with type " + type,
                   foundEntry);
    }

    /**
     * Tests the default toolbox is the same as the default toolbox when customer log in first time.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getToolbox() throws Exception {
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        assertSize(response, 6);
        assertContains(response, "ownerId", "admin");
        assertContains(response, "ownerDisplayName", "admin");

        // Confirm the '_metadata' field is correct
        assertContains(response, "_metadata");
        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertTrue("FAIL: Should have gotten back a default toolbox",
                    metadata.getBoolean("isDefault"));
        assertContains(metadata, "lastModified");

        // Confirm the 'preferences' field is correct
        assertContains(response, "preferences");
        JsonObject preferences = response.getJsonObject("preferences");
        // Default preferences should be empty
        assertSize(preferences, 0);

        // Confirm the tools are correct
        assertContains(response, "bookmarks");
        assertContains(response, "toolEntries");

        JsonArray bookmarks = response.getJsonArray("bookmarks");
        // Default toolbox should have no tools
        assertSize(bookmarks, 0);

        JsonArray toolEntries = response.getJsonArray("toolEntries");

        // Because we could have a bunch of feature tools being added, we can't
        // afford to look for each and every tool, or even guess at the size.
        // Better to just look for known things and fail if we don't find them.
        findToolEntry(toolEntries, "com.ibm.websphere.appserver.adminCenter.tool.explore-1.0-1.0.0", "featureTool");
    }

    /**
     * Tests GET /ibm/api/adminCenter/v1/toolbox for reader-role
     *
     * @throws Exception
     */
    @Test
    public void getToolbox_reader() throws Exception {
        get(url, readerUser, readerPassword, 200);
    }

    /**
     * Access the toolbox resource with a GET request filtering on just the
     * top-level metadata.
     *
     * Test flow:
     * 1. Request GET the toolbox resource filtering for just metadata
     * 2. Response HTTP Status Code: 200
     * 3. The JSON object contains only the metadata
     */
    @Test
    public void getToolbox_filtered() throws Exception {
        response = get(url + "?fields=_metadata", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertSize(response, 1);

        // Confirm the '_metadata' field is correct
        assertContains(response, "_metadata");
        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertTrue("FAIL: Should have gotten back a default toolbox",
                    metadata.getBoolean("isDefault"));
        assertContains(metadata, "lastModified");
    }

    @Test
    public void getToolbox_filtered_reader() throws Exception {
        response = get(url + "?fields=_metadata", readerUser, readerPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertSize(response, 1);

        // Confirm the '_metadata' field is correct
        assertContains(response, "_metadata");
        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertTrue("FAIL: Should have gotten back a default toolbox",
                    metadata.getBoolean("isDefault"));
        assertContains(metadata, "lastModified");
    }

    /**
     * Attempts to find a ToolEntry with the matching id and type.
     *
     * @param toolEntries
     * @param id
     * @param type
     */
    private void findToolEntry(JsonArray toolEntries, String id) {
        boolean foundEntry = false;
        for (int i = 0; i < toolEntries.size(); i++) {
            JsonObject tool = toolEntries.getJsonObject(i);
            if (id.equals(tool.getString("id"))) {
                foundEntry = true;
                break;
            }
        }
        assertTrue("FAIL: could not find tool entry for " + id,
                   foundEntry);
    }

    /**
     * Access the toolbox resource with a GET request filtering on tool fields.
     *
     * Test flow:
     * 1. Request GET the toolbox resource filtering for specific tool fields
     * 2. Response HTTP Status Code: 200
     * 3. The JSON object contains tools with only the specified fields
     */
    @Test
    public void getToolbox_filteredToolEntries() throws Exception {
        response = get(url + "?fields=toolEntries.id", adminUser, adminPassword, 200);

        // Map<?, ?> obj = response.getEntity(Map.class);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertSize(response, 1);

        // Confirm the tools are correct
        assertContains(response, "toolEntries");
        JsonArray toolEntries = response.getJsonArray("toolEntries");

        // Because we could have a bunch of feature tools being added, we can't
        // afford to look for each and every tool, or even guess at the size.
        // Better to just look for known things and fail if we don't find them.
        findToolEntry(toolEntries, "com.ibm.websphere.appserver.adminCenter.tool.explore-1.0-1.0.0");
    }

    @Test
    public void postToolbox_unsupported() throws Exception {
        post(url, adminUser, adminPassword, null, 405);
    }

    @Test
    public void putToolbox_unsupported() throws Exception {
        put(url, adminUser, adminPassword, null, 405);
    }

    /**
     * Access the toolbox resource with a DELETE request.
     * The toolbox resource will be reset when the correct request parameter
     * 'resetCatalog=true' is provided. This scenario does not provide the
     * request parameter.
     *
     * Test flow:
     * 1. Request DELETE the toolbox resource w/ no confirmation param
     * 2. Response HTTP Status Code: 400
     * 3. Response Entity: CWWKX1024E
     */
    @Test
    public void deleteToolbox_resetNotConfirmed() throws Exception {
        response = delete(url, adminUser, adminPassword, 400);
        assertContains(response, "status");
        int status = response.getInt("status");
        assertTrue("FAIL: error message response did not have the correct status code of 400",
                    (400 == status));
        assertContains(response, "message");
        String msg = response.getString("message");
        assertTrue("FAIL: The toolbox should respond with message CWWKX1028E but got: " + msg,
                    msg.startsWith("CWWKX1028E"));

        resetCatalogAndToolbox();
    }

    /**
     * Access the toolbox resource with a DELETE request.
     * The toolbox resource will be reset when the correct request parameter
     * 'resetToolbox=true' is provided. This scenario does not provide the
     * request parameter.
     *
     * Test flow:
     * 1. Request DELETE the toolbox resource w/ confirmation param
     * 2. Response HTTP Status Code: 200
     * 3. Response Entity: CWWKX1025I
     */
    @Test
    public void deleteToolbox_resetConfirmed() throws Exception {
        response = delete(url + "?resetToolbox=true", adminUser, adminPassword, 200);
        assertContains(response, "status");
        int status = response.getInt("status");
        assertTrue("FAIL: The message should report 200",
                    (200 == status));
        assertContains(response, "message");
        String msg = response.getString("message");
        assertTrue("FAIL: The toolbox should respond with message CWWKX1027I but got: " + msg,
                    msg.startsWith("CWWKX1027I"));

        resetCatalogAndToolbox();
    }

    @Test
    public void deleteToolbox_resetConfirmed_reader() throws Exception {
        response = delete(url + "?resetToolbox=true", readerUser, readerPassword, 200);
        assertContains(response, "status");
        int status = response.getInt("status");
        assertTrue("FAIL: The message should report 200",
                    (200 == status));
        assertContains(response, "message");
        String msg = response.getString("message");
        assertTrue("FAIL: The toolbox should respond with message CWWKX1027I but got: " + msg,
                    msg.startsWith("CWWKX1027I"));

        resetCatalogAndToolbox();
    }

    /**
     * Test that when the Toolbox is reset, isDefault returns to true.
     */
    @Test
    public void deleteToolbox_resetReturnsToDefault() throws Exception {
        // Add a new tool to the toolbox
        String name = "New York Times";
        String toolUrl = "http://nytimes.com";
        String icon = "http://i1.nyt.com/images/misc/nytlogo379x64.gif";
        Bookmark bookmark = new Bookmark(name, toolUrl, icon);
        String bookmarkString = bookmark.toString().substring(9); // take out "Bookmark "
        Log.info(c, method.getMethodName(), "bookmark to string is " + bookmarkString);

        // Add a tool
        response = post(url + "/bookmarks", adminUser, adminPassword, bookmarkString, 201);

        // Confirm no longer default
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        assertContains(response, "_metadata");
        JsonObject metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertFalse("FAIL: Should have gotten back a non-default toolbox",
                    metadata.getBoolean("isDefault"));

        // Reset the toolbox
        response = delete(url + "?resetToolbox=true", adminUser, adminPassword, 200);

        // Confirm reset returns to default
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        assertContains(response, "_metadata");
        metadata = response.getJsonObject("_metadata");
        assertContains(metadata, "isDefault");
        assertTrue("FAIL: Should have gotten back a default toolbox",
                    metadata.getBoolean("isDefault"));

        resetCatalogAndToolbox();
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

    /**
     * Gets the preferences via the preferences resource URL.
     */
    @Test
    public void getPreferences() throws Exception {
        response = get(url + "/preferences", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Default preferences should be empty
        assertSize(response, 0);
    }

    /**
     * Gets the preferences via the preferences resource URL for reader-role.
     */
    @Test
    public void getPreferences_reader() throws Exception {
        response = get(url + "/preferences", readerUser, readerPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        // Default preferences should be empty
        assertSize(response, 0);
    }

    /**
     * Attempt to get preferences with no credentials.
     */
    @Test
    public void getPreferences_nonadmin() throws Exception {
        get(url + "/preferences", nonadminUser, nonadminPassword, 403);
    }

    @Test
    public void postPreferences_unsupported() throws Exception {
        post(url + "/preferences", adminUser, adminPassword, null, 405);
    }

    /**
     * Gets the preferences via the preferences resource URL.
     */
    @Test
    public void putPreferences() throws Exception {
        // test invalid key
        String newPreferencesString = "{\"testPref\": \"itWorks!\"}";

        response = put(url + "/preferences", adminUser, adminPassword, newPreferencesString, 200);
        Log.info(c, method.getMethodName(), "Got JSON object (previous preferences): " + response);

        // Previous preferences should be empty
        assertSize(response, 0);

        response = get(url + "/preferences", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object (new preferences): " + response);

        assertSize(response, 0); // invalid key is removed

        // test valid key and valid value, valid key with invalid value
        newPreferencesString = "{\"bidiEnabled\": \"true\", \"bidiTextDirection\": \"wrongValue\"}";
        response = put(url + "/preferences", adminUser, adminPassword, newPreferencesString, 200);
        Log.info(c, method.getMethodName(), "Got JSON object (previous preferences): " + response);

        // Previous preferences should still be empty
        assertSize(response, 0);

        response = get(url + "/preferences", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object (new preferences): " + response);
        assertSize(response, 2);
        assertContains(response, "bidiEnabled");
        assertTrue("FAIL: bidiEnabled should be enabled",
                    response.getBoolean("bidiEnabled"));
        assertContains(response, "bidiTextDirection", "ltr"); // default to ltr when invalid value is used
        resetCatalogAndToolbox();
    }

    /**
     * PUT and GET new preference via the preferences resource URL for reader-role.
     */
    @Test
    public void putPreferences_reader() throws Exception {
        // test invalid key, valid key and valid value, valid key with invalid value
        String newPreferencesString = "{\"testPref\": \"itWorks!\", \"bidiEnabled\": \"wrongValue\", \"bidiTextDirection\": \"contextual\"}";

        response = put(url + "/preferences", readerUser, readerPassword, newPreferencesString, 200);
        Log.info(c, method.getMethodName(), "Got JSON object (previous preferences): " + response);

        // Previous preferences should be empty
        assertSize(response, 0);

        response = get(url + "/preferences", readerUser, readerPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object (new preferences): " + response);

        // Current preferences should have new preference
        assertSize(response, 2); // invalid testPref key should be removed
        assertContains(response, "bidiEnabled");
        // default to false when invalid value is used
        assertFalse("FAIL: bidiEnabled should be enabled",
                    response.getBoolean("bidiEnabled"));
        assertContains(response, "bidiTextDirection", "contextual");
        resetCatalogAndToolbox();
    }

    /**
     * Attempt to PUT preference with no credentials.
     */
    @Test
    public void putPreferences_nonadmin() throws Exception {
        String newPreferencesString = "{\"testPref\": \"itWorks!\"}";
        put(url + "/preferences", nonadminUser, nonadminPassword, newPreferencesString, 403);
    }

    @Test
    public void deletePreferences_unsupported() throws Exception {
        delete(url + "/preferences", adminUser, adminPassword, 405);
    }

    @Test
    public void getBookmarks_ok() throws Exception {
        getWithJsonArrayResponse(url + "/bookmarks", adminUser, adminPassword, 200);
    }

    @Test
    public void getBookmarks_ok_readerrole() throws Exception {
        getWithJsonArrayResponse(url + "/bookmarks", readerUser, readerPassword, 200);
    }

    /**
     * Attempt to get bookmarks with no credentials.
     */
    @Test
    public void getBookmarks_nonadmin() throws Exception {
        getWithJsonArrayResponse(url + "/bookmarks", nonadminUser, nonadminPassword, 403);
    }

    /**
     * Test the add tool to toolbox then access the toolbox, the toolbox should have 3 tools, since the tool to be added is invalid.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void postBookmarks_withInvalidBookmark() throws Exception {
        // Get the toolbox before we try to add the bad tool
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        JsonArray toolEntries = response.getJsonArray("toolEntries");
        int toolEntriesSize = toolEntries.size();

        // Add a new tool to the toolbox
        String name = "New York Times";
        String toolUrl = "htt://nytimes.com";
        String icon = "http://i1.nyt.com/images/misc/nytlogo379x64.gif";
        Bookmark bookmark = new Bookmark(name, toolUrl, icon);
        String bookmarkString = bookmark.toString().substring(9); // take out "Bookmark "

        response = post(url + "/bookmarks", adminUser, adminPassword, bookmarkString, 400);
        assertContains(response, "status");
        int status = response.getInt("status");
        assertTrue("FAIL: error message response did not have the correct status code of 400",
                    (400 == status));
        assertContains(response, "message");
        String msg = response.getString("message");
        assertTrue("FAIL: The toolbox should respond with message CWWKX1026E but got: " + msg,
                    msg.startsWith("CWWKX1026E"));
                

        // Get the toolbox to verify that the new tool was not added
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        toolEntries = response.getJsonArray("toolEntries");
        assertEquals("FAIL: The toolEntries size should not have changed when adding a duplicate tool",
                     toolEntriesSize, toolEntries.size());

        JsonArray bookmarks = response.getJsonArray("bookmarks");
        assertSize(bookmarks, 0);
    }

    /**
     * Test sending valid JSON, but with fields that
     * the server should not understand and therefore send back a
     * 400
     */
    @Test
    public void postBookmarks_withInvalidObjectJSON() throws Exception {
        String badJson = "{'schlemiel': true, 'schlimazel': false, 'Hasenpfeffer': 'Incorporated'}";
        post(url + "/bookmarks", adminUser, adminPassword, badJson, 400);
    }

    /**
     * Test sending invalid JSON syntax to the toolbox,
     * expect a 400 bad request in return.
     */
    @Test
    public void postBookmarks_withBadSyntaxJSON() throws Exception {
        String badJson = "{";
        post(url + "/bookmarks", adminUser, adminPassword, badJson, 400);
    }

    /**
     * Test the add tool to toolbox then access the toolbox, the toolbox should have 4 tools
     */
    @Test
    public void postBookmarks() throws Exception {
        // Add a new bookmark to the toolbox
        String name = "New York Times";
        String toolUrl = "http://nytimes.com";
        String icon = "http://i1.nyt.com/images/misc/nytlogo379x64.gif";
        Bookmark bookmark = new Bookmark(name, toolUrl, icon);
        String id = bookmark.getId();
        String type = bookmark.getType();
        String bookmarkString = bookmark.toString().substring(9); // take out "Bookmark "

        response = post(url + "/bookmarks", adminUser, adminPassword, bookmarkString, 201);
        Log.info(c, method.getMethodName(), "response from POST: " + response);
        assertContains(response, "id", id);
        assertContains(response, "name", name);
        assertContains(response, "url", toolUrl);
        assertContains(response, "icon", icon);

        // Get the toolbox to verify that the new tool was added
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        JsonArray toolEntries = response.getJsonArray("toolEntries");
        findToolEntry(toolEntries, id, type);
        resetCatalogAndToolbox();
    }

    /**
     * Reader-role:
     * Add tool to toolbox then access the toolbox, the toolbox should have 4 tools.
     * This tests GET and POST for bookmarks/{id}
     */
    @Test
    public void postBookmarks_reader() throws Exception {
        // Add a new bookmark to the toolbox
        String name = "New York Times";
        String toolUrl = "http://nytimes.com";
        String icon = "http://i1.nyt.com/images/misc/nytlogo379x64.gif";
        Bookmark bookmark = new Bookmark(name, toolUrl, icon);
        String id = bookmark.getId();
        String type = bookmark.getType();
        String bookmarkString = bookmark.toString().substring(9); // take out "Bookmark "

        response = post(url + "/bookmarks", readerUser, readerPassword, bookmarkString, 201);
        Log.info(c, method.getMethodName(), "response from POST: " + response);
        assertContains(response, "id", id);
        assertContains(response, "name", name);
        assertContains(response, "url", toolUrl);
        assertContains(response, "icon", icon);
        
        // Get the toolbox to verify that the new tool was added
        response = get(url, readerUser, readerPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        JsonArray toolEntries = response.getJsonArray("toolEntries");
        findToolEntry(toolEntries, id, type);

        //Get bookmark by ID
        get(url + "/bookmarks/" + id, readerUser, readerPassword, 200);
        resetCatalogAndToolbox();
    }

    /**
     * Attempt to POST a bookmark with no credentials.
     */
    @Test
    public void postBookmarks_nonadmin() throws Exception {
        // Add a new bookmark to the toolbox
        String name = "New York Times";
        String toolUrl = "http://nytimes.com";
        String icon = "http://i1.nyt.com/images/misc/nytlogo379x64.gif";
        Bookmark bookmark = new Bookmark(name, toolUrl, icon);
        String bookmarkString = bookmark.toString().substring(9); // take out "Bookmark "

        post(url + "/bookmarks", nonadminUser, nonadminPassword, bookmarkString, 403);
    }

    /**
     * Test the add tool to toolbox then access the toolbox, the toolbox should have 4 tools,
     * then add the same tool again, it should return 400.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void postBookmarks_duplicate() throws Exception {
        // Add a new tool to the toolbox
        String name = "New York Times";
        String toolUrl = "http://nytimes.com";
        String icon = "http://i1.nyt.com/images/misc/nytlogo379x64.gif";
        Bookmark bookmark = new Bookmark(name, toolUrl, icon);
        String id = bookmark.getId();
        String type = bookmark.getType();
        String bookmarkString = bookmark.toString().substring(9); // take out "Bookmark "

        response = post(url + "/bookmarks", adminUser, adminPassword, bookmarkString, 201);
        Log.info(c, method.getMethodName(), "response from POST: " + response);
        assertContains(response, "id", id);
        assertContains(response, "name", name);
        assertContains(response, "url", toolUrl);
        assertContains(response, "icon", icon);

        // // Get the toolbox to verify that the new tool was added
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        JsonArray toolEntries = response.getJsonArray("toolEntries");
        int toolEntriesSize = toolEntries.size();
        findToolEntry(toolEntries, id, type);

        JsonArray bookmarks = response.getJsonArray("bookmarks");
        assertSize(bookmarks, 1);
        findToolEntry(bookmarks, id, type);

        response = post(url + "/bookmarks", adminUser, adminPassword, bookmarkString, 409);
        Log.info(c, method.getMethodName(), "JSON object from POST: " + response);
        assertContains(response, "status");
        int status = response.getInt("status");
        assertTrue("FAIL: error message response did not have the correct status code of 409",
                    (409 == status));
        assertContains(response, "message");
        String msg = response.getString("message");
        assertTrue("FAIL: error should respond with message CWWKX1025E but got: " + msg,
                    msg.startsWith("CWWKX1025E"));

        // Get the toolbox to verify that the new tool was added
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        toolEntries = response.getJsonArray("toolEntries");
        assertEquals("FAIL: The toolEntries size should not have changed when adding a duplicate tool",
                     toolEntriesSize, toolEntries.size());
        findToolEntry(toolEntries, id, type);

        bookmarks = response.getJsonArray("bookmarks");
        assertSize(bookmarks, 1);
        findToolEntry(bookmarks, id, type);
        resetCatalogAndToolbox();
    }

    @Test
    public void putBookmarks_unsupported() throws Exception {
        put(url + "/bookmarks", adminUser, adminPassword, null, 405);
    }

    @Test
    public void deleteBookmarks_unsupported() throws Exception {
        delete(url + "/bookmarks", adminUser, adminPassword, 405);
    }

    @Test
    public void getToolEntries_ok() throws Exception {
        getWithJsonArrayResponse(url + "/toolEntries", adminUser, adminPassword, 200);
    }

    @Test
    public void getToolEntries_ok_reader() throws Exception {
        getWithJsonArrayResponse(url + "/toolEntries", readerUser, readerPassword, 200);
    }

    /**
     * Attempt to get toolEntries with no credentials.
     */
    @Test
    public void getToolEntries_nonadmin() throws Exception {
        getWithJsonArrayResponse(url + "/toolEntries", nonadminUser, nonadminPassword, 403);
    }

    /**
     * Access the toolbox resource with a non-Administrator/non-reader user.
     * The toolbox resource requires the user be an Administrator or reader.
     *
     * Test flow:
     * 1. GET the toolbox resource with non-Administrator/non-reader credentials
     * 2. Response HTTP Status Code: 403
     */
    @Test
    public void toolboxNonAdminCredentials() throws Exception {
        getWithJsonArrayResponse(url, nonadminUser, nonadminPassword, 403);
    }

    /**
     * Attempt to POST toolEntries with no credentials.
     */
    @Test
    public void postToolEntries_nonadmin() throws Exception {
        ToolEntry toolEntry = new ToolEntry("com.ibm.websphere.appserver.adminCenter.tool.explore-1.0-1.0.0", "featureTool");
        post(url + "/toolEntries", nonadminUser, nonadminPassword, toolEntry.toString().substring(10), 403);
    }

    /**
     * Test the add tool to toolbox then access the toolbox, the toolbox should have 4 tools,
     * then add the same tool again, it should return 400.
     *
     * @throws Exception
     */
    @Test
    public void postToolEntries_duplicate() throws Exception {
        ToolEntry toolEntry = new ToolEntry("com.ibm.websphere.appserver.adminCenter.tool.explore-1.0-1.0.0", "featureTool");
        response = post(url + "/toolEntries", adminUser, adminPassword, toolEntry.toString().substring(10), 409);
        assertContains(response, "status");
        int status = response.getInt("status");
        assertTrue("FAIL: error message response did not have the correct status code of 409",
                    (409 == status));
        assertContains(response, "message");
        String msg = response.getString("message");
        assertTrue("FAIL: error should respond with message CWWKX1025E but got: " + msg,
                    msg.startsWith("CWWKX1025E"));       
    }

    private String convertToolEntriesToString(JsonArray toolEntriesJson, boolean reverseOrder) {
        StringBuilder toolEntries = new StringBuilder("[");
        int startingIndex = 0;
        if (reverseOrder) {
            for (int i = toolEntriesJson.size() - 1; i >= 0; i--) {
                if (i != (toolEntriesJson.size() - 1)) {
                    toolEntries.append(",");
                }
                JsonObject toolEntry = toolEntriesJson.getJsonObject(i);
                toolEntries.append(toolEntry.toString());
            }
        } else {
            for (int i = 0; i < toolEntriesJson.size(); i++) {
                if (i != 0) {
                    toolEntries.append(",");
                }
                JsonObject toolEntry = toolEntriesJson.getJsonObject(i);
                toolEntries.append(toolEntry.toString());
            }
        }
        toolEntries.append("]");

        return toolEntries.toString();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void putToolEntries_changeToolOrder() throws Exception {
        JsonArray toolEntriesJson = getWithJsonArrayResponse(url + "/toolEntries", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON list: " + toolEntriesJson);

        // Flip toolEntries
        String reversedToolEntries = convertToolEntriesToString(toolEntriesJson, true);
        Log.info(c, method.getMethodName(), "After reversing the tool entries: " + reversedToolEntries);

        response = put(url + "/toolEntries", adminUser, adminPassword, reversedToolEntries, 200);

        toolEntriesJson = getWithJsonArrayResponse(url + "/toolEntries", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON list: " + toolEntriesJson);

        assertEquals("FAIL: server did not store reversed list",
                     reversedToolEntries, convertToolEntriesToString(toolEntriesJson, false));

        resetCatalogAndToolbox();
    }

    @SuppressWarnings("unchecked")
    @Test
    // TBD
    public void putToolEntries_changeToolOrder_reader() throws Exception {
        JsonArray toolEntriesJson = getWithJsonArrayResponse(url + "/toolEntries", readerUser, readerPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON list: " + toolEntriesJson);

        // Flip toolEntries
        String reversedToolEntries = convertToolEntriesToString(toolEntriesJson, true);
        Log.info(c, method.getMethodName(), "After reversing the tool entries: " + reversedToolEntries);

        response = put(url + "/toolEntries", readerUser, readerPassword, reversedToolEntries, 200);

        toolEntriesJson = getWithJsonArrayResponse(url + "/toolEntries", readerUser, readerPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON list: " + toolEntriesJson);

        assertEquals("FAIL: server did not store reversed list",
                     reversedToolEntries, convertToolEntriesToString(toolEntriesJson, false));

        resetCatalogAndToolbox();
    }

    /**
     * Attempt to PUT toolEntries with no credentials.
     */
    @Test
    public void putToolEntries_nonadmin() throws Exception {
        put(url + "/toolEntries", nonadminUser, nonadminPassword, new Object(), 403);
    }

    /**
     * This was a problem we hit in development. Please do not remove this scenario.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void putToolEntries_changeAndAddBookmark() throws Exception {
        putToolEntries_changeToolOrder();

        // Add a new bookmark to the toolbox
        Bookmark bookmark = addTestBookmark();

        // Confirm it was added to the end of toolEntries
        JsonArray toolEntries = getWithJsonArrayResponse(url + "/toolEntries", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON list: " + toolEntries);
        JsonObject lastEntry = toolEntries.getJsonObject(toolEntries.size() - 1);
        assertContains(lastEntry, "id", bookmark.getId());
        assertContains(lastEntry, "type", bookmark.getType());
        resetCatalogAndToolbox();
    }

    @Test
    public void deleteToolEntries_unsupported() throws Exception {
        delete(url + "/toolEntries", adminUser, adminPassword, 405);
    }

    private Bookmark addTestBookmark() throws Exception {
        return addTestBookmark(adminUser, adminPassword);
    }

    /**
     * Creates the 'Test Bookmark'
     */
    // private Bookmark addTestBookmark(BasicAuthSecurityHandler credentials) {
    private Bookmark addTestBookmark(String user, String password) throws Exception {
        // Add a test bookmark to the toolbox
        String name = "Test Bookmark";
        String toolUrl = "http://ibm.com";
        String icon = "http://www.ibm.com/favicon.ico";
        Bookmark bookmark = new Bookmark(name, toolUrl, icon);
        post(url + "/bookmarks", user, password, bookmark.toString().substring(9), 201);
        return bookmark;
    }

    /**
     * Access the version 1 API root page with a GET request.
     * A toolbox tool resource only supports GET requests, and returns a JSON
     * object which contains links to the available version 1 API resources.
     *
     * Test flow:
     * 1. Request GET the version 1 API root page
     * 2. Response HTTP Status Code: 200
     * 3. Response Content-Type: JSON
     * 4. Response body contains JSON object
     * 5. The JSON object contains the available resources
     */
    @Test
    public void getBookmark() throws Exception {
        Bookmark bookmark = addTestBookmark();

        response = get(bookmarkURL, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertSize(response, 6);
        assertContains(response, "id", bookmark.getId());
        assertContains(response, "type", bookmark.getType());
        assertContains(response, "name", bookmark.getName());
        assertContains(response, "url", bookmark.getURL());
        assertContains(response, "icon", bookmark.getIcon());
        resetCatalogAndToolbox();
    }

    /**
     * Attempt to GET a bookmark with no credentials.
     */
    @Test
    public void getBookmark_nonadmin() throws Exception {
        get(bookmarkURL, nonadminUser, nonadminPassword, 403);
    }

    @Test
    public void getBookmark_filtered() throws Exception {
        Bookmark bookmark = addTestBookmark();

        response = get(bookmarkURL + "?fields=name,icon", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertSize(response, 2);
        assertContains(response, "name", bookmark.getName());
        assertContains(response, "icon", bookmark.getIcon());

        resetCatalogAndToolbox();
    }

    /**
     * Access a toolbox tool resource which does not actually exist.
     *
     * Test flow:
     * 1. GET a toolbox tool resource which does not exist
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
        assertTrue("FAIL: error message response did not have the correct status code of 404",
                    (404 == status));
        assertContains(response, "message");
        String msg = response.getString("message");
        assertTrue("FAIL: The returned message did not contain the expected message",
                    msg.matches("CWWKX1022E:.*idontexist-0.0.*"));       
    }

    /**
     * Access a toolbox tool resource with a POST request.
     * Toolbox tool resources do not support POST requests.
     *
     * Test flow:
     * 1. Request POST a toolbox tool resource
     * 2. Response HTTP Status Code: 405
     */
    @Test
    public void postBookmark_unsupported() throws Exception {
        addTestBookmark();
        post(bookmarkURL, adminUser, adminPassword, null, 405);

        resetCatalogAndToolbox();
    }

    @Test
    public void postBookmark_doesntExist() throws Exception {
        post(noSuchBookmarkURL, adminUser, adminPassword, null, 404);
    }

    /**
     * Access a toolbox tool resource with a PUT request.
     * Toolbox tool resources do not support PUT requests.
     *
     * Test flow:
     * 1. Request PUT a toolbox tool resource
     * 2. Response HTTP Status Code: 405
     */
    @Test
    public void putBookmark_unsupported() throws Exception {
        addTestBookmark();
        put(bookmarkURL, adminUser, adminPassword, null, 405);

        resetCatalogAndToolbox();
    }

    @Test
    public void putBookmark_doesntExist() throws Exception {
        put(noSuchBookmarkURL, adminUser, adminPassword, null, 404);
    }

    @Test
    public void deleteBookmark_ok() throws Exception {
        addTestBookmark();
        delete(bookmarkURL, adminUser, adminPassword, 200);

        resetCatalogAndToolbox();
    }

    @Test
    public void deleteBookmark_ok_reader() throws Exception {
        addTestBookmark(readerUser, readerPassword);
        delete(bookmarkURL, readerUser, readerPassword, 200);

        resetCatalogAndToolbox();
    }

    /**
     * Attempt to DELETE a bookmark with no credentials.
     */
    @Test
    public void deleteBookmark_nonadmin() throws Exception {
        delete(bookmarkURL, nonadminUser, nonadminPassword, 403);
    }

    /**
     * Delete a non-existing tool from the toolbox with a DELETE request.
     *
     * Test flow:
     * 1. Request DELETE the toolbox resource
     * 2. Response HTTP Status Code: 404
     */
    @Test
    public void deleteBookmark_doesntExist() throws Exception {
        response = delete(noSuchBookmarkURL, adminUser, adminPassword, 404);
        assertContains(response, "status");
        int status = response.getInt("status");
        assertTrue("FAIL: error message response did not have the correct status code of 404",
                    (404 == status));
        assertContains(response, "message");
        String msg = response.getString("message");
        assertTrue("FAIL: The returned message did not contain the expected message",
                    msg.matches("CWWKX1022E:.*idontexist-0.0.*"));     
    }

    /**
     * Access the version 1 API root page with a GET request.
     * A toolbox tool resource only supports GET requests, and returns a JSON
     * object which contains links to the available version 1 API resources.
     *
     * Test flow:
     * 1. Request GET the version 1 API root page
     * 2. Response HTTP Status Code: 200
     * 3. Response Content-Type: JSON
     * 4. Response body contains JSON object
     * 5. The JSON object contains the available resources
     */
    @Test
    public void getToolEntry() throws Exception {
        response = get(toolEntryURL, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        assertSize(response, 9);
        assertContains(response, "id", "com.ibm.websphere.appserver.adminCenter.tool.explore-1.0-1.0.0");
        assertContains(response, "type", "featureTool");
        assertContains(response, "featureName", "com.ibm.websphere.appserver.adminCenter.tool.explore-1.0");
        assertContains(response, "featureVersion", "1.0.0");
        assertContains(response, "featureShortName", "explore-1.0");
        assertContains(response, "name", "Explore");
        assertContains(response, "url", "/ibm/adminCenter/explore-1.0");
        assertContains(response, "icon", "/ibm/api/adminCenter/v1/icons/com.ibm.websphere.appserver.adminCenter.tool.explore-1.0");
        assertContains(response, "description",
                       "Use the Explore tool to browse and operate on elements of the topology. Discover and manage the available clusters, servers, hosts and applications. ");
    }

    /**
     * Attempt to get a toolEntry with no credentials.
     */
    @Test
    public void getToolEntry_nonadmin() throws Exception {
        get(toolEntryURL, nonadminUser, nonadminPassword, 403);
    }

    @Test
    public void getToolEntry_filtered() throws Exception {
        response = get(toolEntryURL + "?fields=name,icon", adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertSize(response, 2);
        assertContains(response, "name", "Explore");
        assertContains(response, "icon", "/ibm/api/adminCenter/v1/icons/com.ibm.websphere.appserver.adminCenter.tool.explore-1.0");
    }

    /**
     * Access a toolbox tool resource which does not actually exist.
     *
     * Test flow:
     * 1. GET a toolbox tool resource which does not exist
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
    public void getToolEntry_doesntExist() throws Exception {
        response = get(noSuchToolEntryURL, adminUser, adminPassword, 404);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertContains(response, "status");
        int status = response.getInt("status");
        assertTrue("FAIL: error message response did not have the correct status code of 404",
                    (404 == status));
        assertContains(response, "message");
        String msg = response.getString("message");
        assertTrue("FAIL: The returned message did not contain the expected message",
                    msg.matches("CWWKX1022E:.*idontexist-0.0.*"));   
    }

    /**
     * Access a toolbox tool resource with a POST request.
     * Toolbox tool resources do not support POST requests.
     *
     * Test flow:
     * 1. Request POST a toolbox tool resource
     * 2. Response HTTP Status Code: 405
     */
    @Test
    public void postToolEntry_unsupported() throws Exception {
        post(toolEntryURL, adminUser, adminPassword, null, 405);
    }

    @Test
    public void postToolEntry_doesntExist() throws Exception {
        post(noSuchToolEntryURL, adminUser, adminPassword, null, 404);
    }

    /**
     * Access a toolbox tool resource with a PUT request.
     * Toolbox tool resources do not support PUT requests.
     *
     * Test flow:
     * 1. Request PUT a toolbox tool resource
     * 2. Response HTTP Status Code: 405
     */
    @Test
    public void putToolEntry_unsupported() throws Exception {
        put(toolEntryURL, adminUser, adminPassword, null, 405);
    }

    @Test
    public void putToolEntry_doesntExist() throws Exception {
        put(noSuchToolEntryURL, adminUser, adminPassword, null, 404);
    }

    @Test
    public void deleteToolEntry_ok() throws Exception {
        delete(toolEntryURL, adminUser, adminPassword, 200);
        resetCatalogAndToolbox();
    }

    /**
     * Attempt to DELETE a toolEntry with no credentials.
     */
    @Test
    public void deleteToolEntry_nonadmin() throws Exception {
        delete(toolEntryURL, nonadminUser, nonadminPassword, 403);
    }

    /**
     * Test POST and DELETE on a toolEntry in the toolbox with reader role.
     *
     * Test flow:
     * 1. Request DELETE the toolbox resource
     * 2. Response HTTP Status Code: 200
     * 3. Request POST the toolbox resource
     * 4. Response HTTP Status Code: 201
     * 5. Request GET the version 1 API root page
     * 6. Response HTTP Status Code: 200
     * 7. Response Content-Type: JSON
     * 8. Response body contains JSON object
     * 9. The JSON object contains the available resources
     */
    @Test
    public void deletePostGetToolEntry_ok_reader() throws Exception {
        String urlToolEntry = url + "/toolEntries/com.ibm.websphere.appserver.adminCenter.tool.serverConfig-1.0-1.0.0";

        response = delete(urlToolEntry, readerUser, readerPassword, 200);
        ToolEntry newToolEntry = new ToolEntry("com.ibm.websphere.appserver.adminCenter.tool.serverConfig-1.0-1.0.0", "featureTool");
        response = post(url + "/toolEntries", readerUser, readerPassword, newToolEntry.toString().substring(10), 201);

        response = get(urlToolEntry, readerUser, readerPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);
        assertSize(response, 9);
        assertContains(response, "id", "com.ibm.websphere.appserver.adminCenter.tool.serverConfig-1.0-1.0.0");
        assertContains(response, "type", "featureTool");
        assertContains(response, "featureName", "com.ibm.websphere.appserver.adminCenter.tool.serverConfig-1.0");
        assertContains(response, "featureVersion", "1.0.0");
        assertContains(response, "featureShortName", "serverConfig-1.0");
        assertContains(response, "name", "Server Config");
        assertContains(response, "url", "/ibm/adminCenter/serverConfig-1.0");
        assertContains(response, "icon", "/ibm/api/adminCenter/v1/icons/com.ibm.websphere.appserver.adminCenter.tool.serverConfig-1.0");
        assertContains(response, "description",
                       "Use the Server Config tool to view and modify Liberty server configuration files.");

        resetCatalogAndToolbox();
    }

    /**
     * Delete a non-existing tool from the toolbox with a DELETE request.
     *
     * Test flow:
     * 1. Request DELETE the toolbox resource
     * 2. Response HTTP Status Code: 404
     */
    @Test
    public void deleteToolEntry_doesntExist() throws Exception {
        response = delete(noSuchToolEntryURL, adminUser, adminPassword, 404);
        assertContains(response, "status");
        int status = response.getInt("status");
        assertTrue("FAIL: error message response did not have the correct status code of 404",
                    (404 == status));
        assertContains(response, "message");
        String msg = response.getString("message");
        assertTrue("FAIL: The returned message did not contain the expected message",
                    msg.matches("CWWKX1022E:.*idontexist-0.0.*"));   
    }

    /**
     * When a tool is removed from the Catalog, it should also be removed from
     * the Toolbox.
     *
     * Test flow:
     * 1. Confirm Simple+Clock exists in the Toolbox
     * 2. Delete Simple+Clock from the Catalog
     * 3. Confirm Simple+Clock no longer exists in the Toolbox
     *
     * @throws Exception
     */
    @Test
    public void catalogRemoveCausesToolboxRemove() throws Exception {
        // 1. Add a new bookmark to the catalog
        String name = "New York Times";
        String toolUrl = "http://nytimes.com";
        String icon = "http://i1.nyt.com/images/misc/nytlogo379x64.gif";
        Bookmark bookmark = new Bookmark(name, toolUrl, icon);
        String bookmarkString = bookmark.toString().substring(9);

        final String catalogURL = API_V1_CATALOG;
        final String toolURL = API_V1_TOOLBOX + "/toolEntries/" + bookmark.getId();

        // make sure the toolbox for admin is loaded, otherwise post will fail if running as
        // the first test.
        get(toolURL, adminUser, adminPassword, 404);

        post(catalogURL + "/bookmarks", adminUser, adminPassword, bookmarkString, 201);

        ToolEntry newToolEntry = new ToolEntry(bookmark.getId(), bookmark.getType());
        String newToolEntryString = newToolEntry.toString().substring(10);
        post(url + "/toolEntries", adminUser, adminPassword, newToolEntryString, 201);

        // 2. Confirm added catalog bookmark exists in the Toolbox
        get(toolURL, adminUser, adminPassword, 200);

        // 3. Delete added catalog bookmark from the Catalog
        delete(catalogURL + "/bookmarks/" + bookmark.getId(), adminUser, adminPassword, 200);

        // 4. Confirm added catalog bookmark no longer exists in the Toolbox
        get(toolURL, adminUser, adminPassword, 404);

        resetCatalogAndToolbox();
    }

    /**
     * When a tool is removed from the Catalog, it should also be removed from
     * the Toolbox.
     *
     * Test flow:
     * 1. Confirm Simple+Clock exists in the Toolbox
     * 2. Delete Simple+Clock from the Catalog
     * 3. Confirm Simple+Clock no longer exists in the Toolbox
     *
     * @throws Exception
     *
     * Note: if this test happens to be run as the first test in the testsuite, it would
     * fail with 409 when doing the POST with reader credential. This is likely caused by
     * some timing issue. To work around the problem, perform a get on both admin and reader
     * credentials to force both toolboxes to be loaded.
     */
    @Test
    public void catalogRemoveCausesToolboxRemove_reader() throws Exception {
        // 1. Add a new bookmark to the catalog
        String name = "New York Times";
        String toolUrl = "http://nytimes.com";
        String icon = "http://i1.nyt.com/images/misc/nytlogo379x64.gif";
        Bookmark bookmark = new Bookmark(name, toolUrl, icon);
        String bookmarkString = bookmark.toString().substring(9);

        final String catalogURL = API_V1_CATALOG;
        final String toolURL = API_V1_TOOLBOX + "/toolEntries/" + bookmark.getId();

        // make sure the toolboxes for admin and reader are loaded, otherwise post will fail if running
        // as the first test.
        get(toolURL, adminUser, adminPassword, 404);
        get(toolURL, readerUser, readerPassword, 404);

        post(catalogURL + "/bookmarks", adminUser, adminPassword, bookmarkString, 201);
        // confirm that the bookmark (New York Times) is not in either toolbox
        get(toolURL, adminUser, adminPassword, 404);
        get(toolURL, readerUser, readerPassword, 404);

        ToolEntry newToolEntry = new ToolEntry(bookmark.getId(), bookmark.getType());
        String newToolEntryString = newToolEntry.toString().substring(10);
        post(url + "/toolEntries", readerUser, readerPassword, newToolEntryString, 201);

        // 2. Confirm added catalog bookmark exists in the Toolbox
        get(toolURL, readerUser, readerPassword, 200);

        // 3. Delete added catalog bookmark from the Catalog
        delete(catalogURL + "/bookmarks/" + bookmark.getId(), adminUser, adminPassword, 200);

        // 4. Confirm added catalog bookmark no longer exists in the Toolbox
        get(toolURL, readerUser, readerPassword, 404);

        resetCatalogAndToolbox();
    }

    /**
     * When a tool is removed from the Catalog, it should also be removed from
     * the Toolbox.
     *
     * Test flow:
     * 1. Add a Tool to the toolbox
     * 2. Confirm the tool exists in the Toolbox
     * 3. Reset the Catalog
     * 4. Confirm the tool still exists in the Toolbox
     *
     * @throws Exception
     */
    @Test
    public void catalogRemoveDoesNotRemoveBookmark() throws Exception {
        // 1. Add a new bookmark to the toolbox
        String name = "New York Times";
        String toolUrl = "http://nytimes.com";
        String icon = "http://i1.nyt.com/images/misc/nytlogo379x64.gif";
        Bookmark bookmark = new Bookmark(name, toolUrl, icon);
        String bookmarkString = bookmark.toString().substring(9);

        post(url + "/bookmarks", adminUser, adminPassword, bookmarkString, 201);

        // 2. Confirm created bookmark exists in the Toolbox
        String newToolURL = API_V1_TOOLBOX + "/bookmarks/" + bookmark.getId();
        get(newToolURL, adminUser, adminPassword, 200);

        // 3. Reset the Catalog
        delete(API_V1_CATALOG + "?resetCatalog=true", adminUser, adminPassword, 200);

        // 4. Confirm created bookmark still exists in the Toolbox
        get(newToolURL, adminUser, adminPassword, 200);

        resetCatalogAndToolbox();
    }

}
