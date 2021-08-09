/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;

/**
 *
 */
public class OidcOauth20UtilTest {

    @Test
    public void testIsNullEmptyCollection()
    {
        Enumeration<String> headers = null;
        assertTrue(OidcOAuth20Util.isNullEmpty(headers));

        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        headers = acceptTable.keys();

        assertTrue(OidcOAuth20Util.isNullEmpty(headers));

        acceptTable.put("application/rdf+xml", "");
        acceptTable.put("text/html", "");
        headers = acceptTable.keys();
        assertFalse(OidcOAuth20Util.isNullEmpty(headers));

    }

    @Test
    public void testIsNullEmptyString()
    {
        String headerValue = null;
        assertTrue(OidcOAuth20Util.isNullEmpty(headerValue));

        headerValue = "";
        assertTrue(OidcOAuth20Util.isNullEmpty(headerValue));

        headerValue = "Authorization:Basic Axut12==";
        assertFalse(OidcOAuth20Util.isNullEmpty(headerValue));

    }

    @Test
    public void testIsNullEmptyStringArray()
    {
        String[] headerValues = null;
        assertTrue(OidcOAuth20Util.isNullEmpty(headerValues));

        headerValues = new String[0];
        assertTrue(OidcOAuth20Util.isNullEmpty(headerValues));

        headerValues = new String[2];
        headerValues[0] = "Basic Axut12==";
        headerValues[0] = "Basic U3VqZWV0QW5kSm9lOk9uZUxhc3RQcm9qZWN0VG9nZXRoZXJJdFdhc0Z1bg==";
        assertFalse(OidcOAuth20Util.isNullEmpty(headerValues));

    }

    @Test
    public void testIsNullEmptyJSonArray()
    {
        JsonArray members = null;
        assertTrue(OidcOAuth20Util.isNullEmpty(members));

        members = new JsonArray();
        assertTrue(OidcOAuth20Util.isNullEmpty(members));

        members.add(new JsonPrimitive(("https://localhost/app1:5443")));
        members.add(new JsonPrimitive(("https://localhost/app1:5444")));
        members.add(new JsonPrimitive(("https://localhost/app2:8020")));
        members.add(new JsonPrimitive(("https://localhost/app2:8080")));
        assertFalse(OidcOAuth20Util.isNullEmpty(members));

    }

    @Test
    public void testJSonArrayContainsString()
    {
        JsonArray members = new JsonArray();
        members.add(new JsonPrimitive(("https://localhost/app1:5443")));
        members.add(new JsonPrimitive(("https://localhost/app1:5444")));
        members.add(new JsonPrimitive(("https://localhost/app2:8020")));
        members.add(new JsonPrimitive(("https://localhost/app2:8080")));
        assertFalse(OidcOAuth20Util.jsonArrayContainsString(members, "https://localhost/app2:9090"));
        assertTrue(OidcOAuth20Util.jsonArrayContainsString(members, "https://localhost/app2:8080"));

    }

    @Test
    public void testInitJSonArray()
    {
        JsonArray array = OidcOAuth20Util.initJsonArray("https://localhost/app1:5443");
        assertNotNull(array);
        assertEquals(array.size(), 1);

        String[] valueArray = { "https://localhost/app1:5443", "https://localhost/app1:5444", "https://localhost/app2:8020", "https://localhost/app2:8080" };
        JsonArray array2 = OidcOAuth20Util.initJsonArray(valueArray);
        assertNotNull(array2);
        assertEquals(array2.size(), 4);
        JsonArray members = new JsonArray();
        members.add(new JsonPrimitive(("https://localhost/app1:5443")));
        members.add(new JsonPrimitive(("https://localhost/app1:5444")));
        members.add(new JsonPrimitive(("https://localhost/app2:8020")));
        members.add(new JsonPrimitive(("https://localhost/app2:8080")));
        assertEquals(members, array2);

    }

    @Test
    public void testGetStringArray()
    {

        String[] valueArray = { "https://localhost/app1:5443", "https://localhost/app1:5444", "https://localhost/app2:8020", "https://localhost/app2:8080" };
        JsonArray members = new JsonArray();
        members.add(new JsonPrimitive(("https://localhost/app1:5443")));
        members.add(new JsonPrimitive(("https://localhost/app1:5444")));
        members.add(new JsonPrimitive(("https://localhost/app2:8020")));
        members.add(new JsonPrimitive(("https://localhost/app2:8080")));
        assertArrayEquals(valueArray, OidcOAuth20Util.getStringArray(members));

    }

    @Test
    public void testGetSpaceDelimitedString()
    {

        JsonArray members = new JsonArray();
        members.add(new JsonPrimitive(("https://localhost/app1:5443")));
        members.add(new JsonPrimitive(("https://localhost/app1:5444")));
        members.add(new JsonPrimitive(("https://localhost/app2:8020")));
        members.add(new JsonPrimitive(("https://localhost/app2:8080")));
        String expectedString = "https://localhost/app1:5443 https://localhost/app1:5444 https://localhost/app2:8020 https://localhost/app2:8080";
        assertEquals(expectedString, OidcOAuth20Util.getSpaceDelimitedString(members));

    }

    @Test
    public void testGetListOfJsonObjects()
    {

        JsonArray members = new JsonArray();
        members.add(OidcOAuth20Util.getJsonObj(new OidcBaseClient("clientId1", "clientSecret1", null, "clientName1", "componentId1", true)));
        members.add(OidcOAuth20Util.getJsonObj(new OidcBaseClient("clientId2", "clientSecret2", null, "clientName2", "componentId2", true)));
        members.add(OidcOAuth20Util.getJsonObj(new OidcBaseClient("clientId3", "clientSecret3", null, "clientName3", "componentId3", true)));
        members.add(OidcOAuth20Util.getJsonObj(new OidcBaseClient("clientId4", "clientSecret4", null, "clientName4", "componentId4", true)));
        List<JsonObject> jsonList = OidcOAuth20Util.getListOfJsonObjects(members);
        List<JsonObject> expectedList = new ArrayList<JsonObject>();
        expectedList.add(OidcOAuth20Util.getJsonObj(new OidcBaseClient("clientId1", "clientSecret1", null, "clientName1", "componentId1", true)));
        expectedList.add(OidcOAuth20Util.getJsonObj(new OidcBaseClient("clientId2", "clientSecret2", null, "clientName2", "componentId2", true)));
        expectedList.add(OidcOAuth20Util.getJsonObj(new OidcBaseClient("clientId3", "clientSecret3", null, "clientName3", "componentId3", true)));
        expectedList.add(OidcOAuth20Util.getJsonObj(new OidcBaseClient("clientId4", "clientSecret4", null, "clientName4", "componentId4", true)));
        assertEquals(expectedList, jsonList);

    }

}
