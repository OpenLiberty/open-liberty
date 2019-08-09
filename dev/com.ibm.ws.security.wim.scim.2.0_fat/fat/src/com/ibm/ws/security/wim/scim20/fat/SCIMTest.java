/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.scim20.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.security.wim.scim20.model.ListResponse;
import com.ibm.websphere.security.wim.scim20.model.Resource;
import com.ibm.websphere.security.wim.scim20.model.groups.Group;
import com.ibm.websphere.security.wim.scim20.model.users.User;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.wim.scim20.SCIMUtil;
import com.ibm.ws.security.wim.scim20.model.ErrorImpl;
import com.ibm.ws.security.wim.scim20.model.ListResponseImpl;
import com.ibm.ws.security.wim.scim20.model.groups.GroupImpl;
import com.ibm.ws.security.wim.scim20.model.users.UserImpl;
import com.ibm.ws.security.wim.scim20.utils.SCIMFatUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

/**
 * This FAT test was written to do some simple testing of the SCIM 2.0 feature prior to any real implementation
 * having been done to connect to the back end. These tests will need to be replaced either in this FAT or
 * in another FAT as SCIM 2.0 implementation is added.
 */
@RunWith(FATRunner.class)
public class SCIMTest {
    private static final Class<?> c = SCIMTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("scim.test");
    private static final String ADMIN_USER_NAME = "administrator";
    private static final String ADMIN_USER_PASSWORD = "passw0rd";
    private static final String UNAUTH_USER_NAME = "unauthorized";
    private static final String UNAUTH_USER_PASSWORD = "passw0rd";
    private static final String READER_USER_NAME = "reader";
    private static final String READER_USER_PASSWORD = "passw0rd";

    private static void assertResponseEquals(Object expected, String actualJson) throws IOException {
        Object obj = SCIMUtil.deserialize(actualJson, expected.getClass());
        assertEquals(expected, obj);
    }

    @BeforeClass
    public static void setupClass() throws Exception {
        /*
         * Start the server.
         */
        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");
        server.startServer(c.getName() + ".log");

        /*
         * Make sure the application has come up before proceeding
         */
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I:.*"));
        assertNotNull("Rest service did not came up",
                      server.waitForStringInLog("CWWKT0016I:.*"));
        assertNotNull("Server did not came up",
                      server.waitForStringInLog("CWWKF0011I:.*"));
        assertNotNull("TCP non ssl Channel did not come up",
                      server.waitForStringInLog("CWWKO0219I:.*"));
        assertNotNull("TCP SSL Channel did not come up",
                      server.waitForStringInLog("CWWKO0219I:.*ssl.*"));
    }

    @AfterClass
    public static void teardownClass() throws Exception {
        Log.info(c, "teardownClass", "Stopping the server...");
        if (server != null) {
            try {
                server.stopServer();
            } catch (Exception e) {
                Log.error(c, "teardownClass", e, "Liberty server threw error while stopping. " + e.getMessage());
            }
        }
    }

    /**
     * Test deleting a group using the version-less URL.
     */
    @Test
    public void delete_Groups() throws Exception {
        final String METHOD_NAME = "delete_Groups";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups/cn=somegroup,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 204, null, HTTPRequestMethod.DELETE, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);
    }

    /**
     * Test attempting to delete a group resource using a user without any assigned roles.
     */
    @Test
    public void delete_Groups_Authorization_NoRoles() throws Exception {
        final String METHOD_NAME = "delete_Groups_Authorization_NoRoles";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups/cn=somegroup,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 403, null, HTTPRequestMethod.DELETE, null, null, UNAUTH_USER_NAME, UNAUTH_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(403);
        expectedResponse.setDetail("The 'Administrator' role is required to create, delete, or update a resource.");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to delete a group resource using a user with only the reader role.
     */
    @Test
    public void delete_Groups_Authorization_ReaderRole() throws Exception {
        final String METHOD_NAME = "delete_Groups_Authorization_ReaderRole";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups/cn=somegroup,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 403, null, HTTPRequestMethod.DELETE, null, null, READER_USER_NAME, READER_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(403);
        expectedResponse.setDetail("The 'Administrator' role is required to create, delete, or update a resource.");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test deleting a group with an invalid SCIM version in the URL.
     */
    @Test
    public void delete_Groups_InvalidVersion() throws Exception {
        final String METHOD_NAME = "delete_Groups_InvalidVersion";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v22/Groups/cn=somegroup,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.DELETE, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("The URL contained an invalid SCIM version (v22)");
        expectedResponse.setScimType("invalidVers");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test deleting a group with no ID.
     */
    @Test
    public void delete_Groups_NoID() throws Exception {
        final String METHOD_NAME = "delete_Groups_NoID";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.DELETE, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("No resource ID was found for the DELETE operation.");
        expectedResponse.setScimType("invalidSyntax");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to delete a group resource using a user with bad credentials.
     */
    @Test
    public void delete_Groups_Unauthenticated() throws Exception {
        final String METHOD_NAME = "delete_Groups_Unauthenticated";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups/uid=somegroup,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 401, null, HTTPRequestMethod.DELETE, null, null, null, null);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         *
         * TODO The RESTHandler framework returns back a response before we can create a JSON error object.
         */
//        ErrorImpl expectedResponse = new ErrorImpl();
//        expectedResponse.setStatus(401);
//        expectedResponse.setDetail("???????");
//        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test deleting a group using a URL that specifies the SCIM 2.0 API.
     */
    @Test
    public void delete_Groups_V2() throws Exception {
        final String METHOD_NAME = "delete_Groups_V2";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v2/Groups/uid=someuser,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 204, null, HTTPRequestMethod.DELETE, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);
    }

    /**
     * Test deleting a user using the version-less URL.
     */
    @Test
    public void delete_Users() throws Exception {
        final String METHOD_NAME = "delete_Users";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users/uid=someuser,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 204, null, HTTPRequestMethod.DELETE, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);
    }

    /**
     * Test attempting to delete a user resource using a user without any assigned roles.
     */
    @Test
    public void delete_Users_Authorization_NoRoles() throws Exception {
        final String METHOD_NAME = "delete_Users_Authorization_NoRoles";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users/uid=someuser,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 403, null, HTTPRequestMethod.DELETE, null, null, UNAUTH_USER_NAME, UNAUTH_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(403);
        expectedResponse.setDetail("The 'Administrator' role is required to create, delete, or update a resource.");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to delete a user resource using a user with only the reader role.
     */
    @Test
    public void delete_Users_Authorization_ReaderRole() throws Exception {
        final String METHOD_NAME = "delete_Users_Authorization_ReaderRole";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users/uid=someuser,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 403, null, HTTPRequestMethod.DELETE, null, null, READER_USER_NAME, READER_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(403);
        expectedResponse.setDetail("The 'Administrator' role is required to create, delete, or update a resource.");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test deleting a user with an invalid SCIM version in the URL.
     */
    @Test
    public void delete_Users_InvalidVersion() throws Exception {
        final String METHOD_NAME = "delete_Users_InvalidVersion";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v22/Users/uid=someuser,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.DELETE, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("The URL contained an invalid SCIM version (v22)");
        expectedResponse.setScimType("invalidVers");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test deleting a user with no ID.
     */
    @Test
    public void delete_Users_NoID() throws Exception {
        final String METHOD_NAME = "delete_Users_NoID";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v22/Users";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.DELETE, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("The URL contained an invalid SCIM version (v22)");
        expectedResponse.setScimType("invalidVers");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to delete a user resource using a user with bad credentials.
     */
    @Test
    public void delete_Users_Unauthenticated() throws Exception {
        final String METHOD_NAME = "delete_Users_Unauthenticated";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users/uid=someuser,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 401, null, HTTPRequestMethod.DELETE, null, null, null, null);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         *
         * TODO The RESTHandler framework returns back a response before we can create a JSON error object.
         */
//        ErrorImpl expectedResponse = new ErrorImpl();
//        expectedResponse.setStatus(401);
//        expectedResponse.setDetail("???????");
//        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test deleting a user using a URL that specifies the SCIM 2.0 API.
     */
    @Test
    public void delete_Users_V2() throws Exception {
        final String METHOD_NAME = "delete_Users_V2";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v2/Users/uid=someuser,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 204, null, HTTPRequestMethod.DELETE, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);
    }

    /**
     * Test getting all groups using the version-less URL.
     */
    @Test
    public void get_Groups() throws Exception {
        final String METHOD_NAME = "get_Groups";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups";
        StringBuilder result = SCIMFatUtils.callURL(url, 200, null, HTTPRequestMethod.GET, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty ListResponse.
         */
        ListResponse<Group> expectedResponse = new ListResponseImpl<Group>();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to get a group resource using a user without any assigned roles.
     */
    @Test
    public void get_Groups_Authorization_NoRoles() throws Exception {
        final String METHOD_NAME = "get_Groups_Authorization_NoRoles";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups";
        StringBuilder result = SCIMFatUtils.callURL(url, 403, null, HTTPRequestMethod.GET, null, null, UNAUTH_USER_NAME, UNAUTH_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(403);
        expectedResponse.setDetail("The 'Administrator' or 'Reader' role is required to read a resource.");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to get a group resource using a user only the reader role.
     */
    @Test
    public void get_Groups_Authorization_ReaderRole() throws Exception {
        final String METHOD_NAME = "get_Groups_Authorization_ReaderRole";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups";
        StringBuilder result = SCIMFatUtils.callURL(url, 200, null, HTTPRequestMethod.GET, null, null, READER_USER_NAME, READER_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty ListResponse.
         */
        ListResponse<Group> expectedResponse = new ListResponseImpl<Group>();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test getting a group with an invalid SCIM version in the URL.
     */
    @Test
    public void get_Groups_InvalidVersion() throws Exception {
        final String METHOD_NAME = "get_Groups_InvalidVersion";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v22/Groups";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.GET, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("The URL contained an invalid SCIM version (v22)");
        expectedResponse.setScimType("invalidVers");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to get a group resource using a user with bad credentials.
     */
    @Test
    public void get_Groups_Unauthenticated() throws Exception {
        final String METHOD_NAME = "get_Groups_Unauthenticated";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups";
        StringBuilder result = SCIMFatUtils.callURL(url, 401, null, HTTPRequestMethod.GET, null, null, null, null);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         *
         * TODO The RESTHandler framework returns back a response before we can create a JSON error object.
         */
//        ErrorImpl expectedResponse = new ErrorImpl();
//        expectedResponse.setStatus(401);
//        expectedResponse.setDetail("???????");
//        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test getting a group using a URL that specifies the SCIM 2.0 API.
     */
    @Test
    public void get_Groups_V2() throws Exception {
        final String METHOD_NAME = "get_Groups_V2";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v2/Groups";
        StringBuilder result = SCIMFatUtils.callURL(url, 200, null, HTTPRequestMethod.GET, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty ListResponse.
         */
        ListResponse<Group> expectedResponse = new ListResponseImpl<Group>();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test getting a single group using the version-less URL.
     */
    @Test
    public void get_Groups_WithID() throws Exception {
        final String METHOD_NAME = "get_Groups";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups/cn=somegroup,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 200, null, HTTPRequestMethod.GET, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty group.
         */
        GroupImpl expectedResponse = new GroupImpl();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test trying to get a resource from an invalid endpoint.
     */
    @Test
    public void get_InvalidEndpoint() throws Exception {
        final String METHOD_NAME = "get_InvalidEndpoint";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Computers";
        StringBuilder result = SCIMFatUtils.callURL(url, 404, null, HTTPRequestMethod.GET, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(404);
        expectedResponse.setDetail("The endpoint 'Computers' does not exist.");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test getting all users using the version-less URL.
     */
    @Test
    public void get_Users() throws Exception {
        final String METHOD_NAME = "get_Users";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users";
        StringBuilder result = SCIMFatUtils.callURL(url, 200, null, HTTPRequestMethod.GET, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty ListResponse.
         */
        ListResponse<User> expectedResponse = new ListResponseImpl<User>();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to get a user resource using a user without any assigned roles.
     */
    @Test
    public void get_Users_Authorization_NoRoles() throws Exception {
        final String METHOD_NAME = "get_Users_Authorization_NoRoles";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users";
        StringBuilder result = SCIMFatUtils.callURL(url, 403, null, HTTPRequestMethod.GET, null, null, UNAUTH_USER_NAME, UNAUTH_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(403);
        expectedResponse.setDetail("The 'Administrator' or 'Reader' role is required to read a resource.");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to get a user resource using a user only the reader role.
     */
    @Test
    public void get_Users_Authorization_ReaderRole() throws Exception {
        final String METHOD_NAME = "get_Users_Authorization_ReaderRole";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users";
        StringBuilder result = SCIMFatUtils.callURL(url, 200, null, HTTPRequestMethod.GET, null, null, READER_USER_NAME, READER_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty ListResponse.
         */
        ListResponse<Group> expectedResponse = new ListResponseImpl<Group>();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test getting a user with an invalid SCIM version in the URL.
     */
    @Test
    public void get_Users_InvalidVersion() throws Exception {
        final String METHOD_NAME = "get_Users_InvalidVersion";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v22/Users";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.GET, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("The URL contained an invalid SCIM version (v22)");
        expectedResponse.setScimType("invalidVers");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to get a user resource using a user with bad credentials.
     */
    @Test
    public void get_Users_Unauthenticated() throws Exception {
        final String METHOD_NAME = "get_Users_Unauthenticated";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users";
        StringBuilder result = SCIMFatUtils.callURL(url, 401, null, HTTPRequestMethod.GET, null, null, null, null);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         *
         * TODO The RESTHandler framework returns back a response before we can create a JSON error object.
         */
//        ErrorImpl expectedResponse = new ErrorImpl();
//        expectedResponse.setStatus(401);
//        expectedResponse.setDetail("???????");
//        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test getting a user using a URL that specifies the SCIM 2.0 API.
     */
    @Test
    public void get_Users_V2() throws Exception {
        final String METHOD_NAME = "get_Users_V2";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v2/Users";
        StringBuilder result = SCIMFatUtils.callURL(url, 200, null, HTTPRequestMethod.GET, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty ListResponse.
         */
        ListResponse<User> expectedResponse = new ListResponseImpl<User>();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test getting a single user using the version-less URL.
     */
    @Test
    public void get_Users_WithID() throws Exception {
        final String METHOD_NAME = "get_Users";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users/uid=someuser,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 200, null, HTTPRequestMethod.GET, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty ListResponse.
         */
        ListResponse<User> expectedResponse = new ListResponseImpl<User>();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test creating a group using the version-less URL.
     */
    @Test
    public void post_Groups() throws Exception {
        final String METHOD_NAME = "post_Groups";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups";
        StringBuilder result = SCIMFatUtils.callURL(url, 201, null, HTTPRequestMethod.POST, requestContent, "application/json", ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty group.
         */
        GroupImpl expectedResponse = new GroupImpl();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to create a group resource using a user without any assigned roles.
     */
    @Test
    public void post_Groups_Authorization_NoRoles() throws Exception {
        final String METHOD_NAME = "post_Groups_Authorization_NoRoles";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups";
        StringBuilder result = SCIMFatUtils.callURL(url, 403, null, HTTPRequestMethod.POST, requestContent, "application/json", UNAUTH_USER_NAME, UNAUTH_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(403);
        expectedResponse.setDetail("The 'Administrator' role is required to create, delete, or update a resource.");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to create a group resource using a user only the reader role.
     */
    @Test
    public void post_Groups_Authorization_ReaderRole() throws Exception {
        final String METHOD_NAME = "post_Groups_Authorization_ReaderRole";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups";
        StringBuilder result = SCIMFatUtils.callURL(url, 403, null, HTTPRequestMethod.POST, requestContent, "application/json", READER_USER_NAME, READER_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(403);
        expectedResponse.setDetail("The 'Administrator' role is required to create, delete, or update a resource.");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test creating a group with an invalid SCIM version in the URL.
     */
    @Test
    public void post_Groups_InvalidVersion() throws Exception {
        final String METHOD_NAME = "post_Groups_InvalidVersion";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v22/Groups";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.POST, requestContent, "application/json", ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("The URL contained an invalid SCIM version (v22)");
        expectedResponse.setScimType("invalidVers");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test creating a group with no content.
     */
    @Test
    public void post_Groups_NoContent() throws Exception {
        final String METHOD_NAME = "post_Groups_NoContent";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.POST, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("It was expected that the request contained content, but no content was found on the request.");
        expectedResponse.setScimType("invalidSyntax");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test searching groups via POST.
     */
    @Test
    public void post_Groups_Search() throws Exception {
        final String METHOD_NAME = "post_Groups_Search";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups/.search";
        StringBuilder result = SCIMFatUtils.callURL(url, 200, null, HTTPRequestMethod.POST, requestContent, "application/json", ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty ListResponse.
         */
        ListResponse<Group> expectedResponse = new ListResponseImpl<Group>();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to search for groups via POST with a user with no assigned roles.
     */
    @Test
    public void post_Groups_Search_Authorization_NoRoles() throws Exception {
        final String METHOD_NAME = "post_Groups_Search_Authorization_NoRoles";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups/.search";
        StringBuilder result = SCIMFatUtils.callURL(url, 403, null, HTTPRequestMethod.POST, requestContent, "application/json", UNAUTH_USER_NAME, UNAUTH_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(403);
        expectedResponse.setDetail("The 'Administrator' or 'Reader' role is required to read a resource.");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to search for groups via POST with a user assigned only the reader role.
     */
    @Test
    public void post_Groups_Search_Authorization_ReaderRole() throws Exception {
        final String METHOD_NAME = "post_Groups_Search_Authorization_ReaderRole";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups/.search";
        StringBuilder result = SCIMFatUtils.callURL(url, 200, null, HTTPRequestMethod.POST, requestContent, "application/json", READER_USER_NAME, READER_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty ListResponse.
         */
        ListResponse<Group> expectedResponse = new ListResponseImpl<Group>();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test searching groups via POST with no content.
     */
    @Test
    public void post_Groups_Search_NoContent() throws Exception {
        final String METHOD_NAME = "post_Groups_Search_NoContent";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups/.search";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.POST, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("It was expected that the request contained content, but no content was found on the request.");
        expectedResponse.setScimType("invalidSyntax");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to create a group resource using a user with bad credentials.
     */
    @Test
    public void post_Groups_Unauthenticated() throws Exception {
        final String METHOD_NAME = "post_Groups_Unauthenticated";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups";
        StringBuilder result = SCIMFatUtils.callURL(url, 401, null, HTTPRequestMethod.POST, requestContent, "application/json", null, null);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         *
         * TODO The RESTHandler framework returns back a response before we can create a JSON error object.
         */
//        ErrorImpl expectedResponse = new ErrorImpl();
//        expectedResponse.setStatus(401);
//        expectedResponse.setDetail("???????");
//        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test creating a group using a URL that specifies the SCIM 2.0 API.
     */
    @Test
    public void post_Groups_V2() throws Exception {
        final String METHOD_NAME = "post_Groups_V2";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v2/Groups";
        StringBuilder result = SCIMFatUtils.callURL(url, 201, null, HTTPRequestMethod.POST, requestContent, "application/json", ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty group.
         */
        GroupImpl expectedResponse = new GroupImpl();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test searching all resources via POST.
     */
    @Test
    public void post_Root_Search() throws Exception {
        final String METHOD_NAME = "post_Root_Search";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/.search";
        StringBuilder result = SCIMFatUtils.callURL(url, 200, null, HTTPRequestMethod.POST, requestContent, "application/json", ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty ListResponse.
         */
        ListResponse<Resource> expectedResponse = new ListResponseImpl<Resource>();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test searching all resources via POST with no content.
     */
    @Test
    public void post_Root_Search_NoContent() throws Exception {
        final String METHOD_NAME = "post_Root_Search_NoContent";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/.search";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.POST, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("It was expected that the request contained content, but no content was found on the request.");
        expectedResponse.setScimType("invalidSyntax");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test creating a user using the version-less URL.
     */
    @Test
    public void post_Users() throws Exception {
        final String METHOD_NAME = "post_Users";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users";
        StringBuilder result = SCIMFatUtils.callURL(url, 201, null, HTTPRequestMethod.POST, requestContent, "application/json", ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty ListResponse.
         */
        UserImpl expectedResponse = new UserImpl();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to create a user resource using a user without any assigned roles.
     */
    @Test
    public void post_Users_Authorization_NoRoles() throws Exception {
        final String METHOD_NAME = "post_Users_Authorization_NoRoles";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users";
        StringBuilder result = SCIMFatUtils.callURL(url, 403, null, HTTPRequestMethod.POST, requestContent, "application/json", UNAUTH_USER_NAME, UNAUTH_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(403);
        expectedResponse.setDetail("The 'Administrator' role is required to create, delete, or update a resource.");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to create a user resource using a user only the reader role.
     */
    @Test
    public void post_Users_Authorization_ReaderRole() throws Exception {
        final String METHOD_NAME = "post_Users_Authorization_ReaderRole";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users";
        StringBuilder result = SCIMFatUtils.callURL(url, 403, null, HTTPRequestMethod.POST, requestContent, "application/json", READER_USER_NAME, READER_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(403);
        expectedResponse.setDetail("The 'Administrator' role is required to create, delete, or update a resource.");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test creating a user with an invalid SCIM version in the URL.
     */
    @Test
    public void post_Users_InvalidVersion() throws Exception {
        final String METHOD_NAME = "post_Users_InvalidVersion";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v22/Users";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.POST, requestContent, "application/json", ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("The URL contained an invalid SCIM version (v22)");
        expectedResponse.setScimType("invalidVers");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test creating a user with no content.
     */
    @Test
    public void post_Users_NoContent() throws Exception {
        final String METHOD_NAME = "post_Users_NoContent";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.POST, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("It was expected that the request contained content, but no content was found on the request.");
        expectedResponse.setScimType("invalidSyntax");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test searching users via POST.
     */
    @Test
    public void post_Users_Search() throws Exception {
        final String METHOD_NAME = "post_Users_Search";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users/.search";
        StringBuilder result = SCIMFatUtils.callURL(url, 200, null, HTTPRequestMethod.POST, requestContent, "application/json", ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty ListResponse.
         */
        ListResponse<User> expectedResponse = new ListResponseImpl<User>();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to search for users via POST with a user with no assigned roles.
     */
    @Test
    public void post_Users_Search_Authorization_NoRoles() throws Exception {
        final String METHOD_NAME = "post_Users_Search_Authorization_NoRoles";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users/.search";
        StringBuilder result = SCIMFatUtils.callURL(url, 403, null, HTTPRequestMethod.POST, requestContent, "application/json", UNAUTH_USER_NAME, UNAUTH_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(403);
        expectedResponse.setDetail("The 'Administrator' or 'Reader' role is required to read a resource.");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to search for users via POST with a user assigned only the reader role.
     */
    @Test
    public void post_Users_Search_Authorization_ReaderRole() throws Exception {
        final String METHOD_NAME = "post_Users_Search_Authorization_ReaderRole";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users/.search";
        StringBuilder result = SCIMFatUtils.callURL(url, 200, null, HTTPRequestMethod.POST, requestContent, "application/json", READER_USER_NAME, READER_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty ListResponse.
         */
        ListResponse<Group> expectedResponse = new ListResponseImpl<Group>();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test searching users via POST with no content.
     */
    @Test
    public void post_Users_Search_NoContent() throws Exception {
        final String METHOD_NAME = "post_Users_Search_NoContent";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users/.search";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.POST, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("It was expected that the request contained content, but no content was found on the request.");
        expectedResponse.setScimType("invalidSyntax");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to create a user resource using a user with bad credentials.
     */
    @Test
    public void post_Users_Unauthenticated() throws Exception {
        final String METHOD_NAME = "post_Users_Unauthenticated";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users";
        StringBuilder result = SCIMFatUtils.callURL(url, 401, null, HTTPRequestMethod.POST, requestContent, "application/json", null, null);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         *
         * TODO The RESTHandler framework returns back a response before we can create a JSON error object.
         */
//        ErrorImpl expectedResponse = new ErrorImpl();
//        expectedResponse.setStatus(401);
//        expectedResponse.setDetail("???????");
//        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test creating a user using a URL that specifies the SCIM 2.0 API.
     */
    @Test
    public void post_Users_V2() throws Exception {
        final String METHOD_NAME = "post_Users_V2";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v2/Users";
        StringBuilder result = SCIMFatUtils.callURL(url, 201, null, HTTPRequestMethod.POST, requestContent, "application/json", ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty user.
         */
        UserImpl expectedResponse = new UserImpl();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test updating a group using the version-less URL.
     */
    @Test
    public void put_Groups() throws Exception {
        final String METHOD_NAME = "put_Groups";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups/cn=somegroup,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 200, null, HTTPRequestMethod.PUT, requestContent, "application/json", ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty group.
         */
        GroupImpl expectedResponse = new GroupImpl();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to update a group resource using a user without any assigned roles.
     */
    @Test
    public void put_Groups_Authorization_NoRoles() throws Exception {
        final String METHOD_NAME = "put_Groups_Authorization_NoRoles";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups/cn=somegroup,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 403, null, HTTPRequestMethod.PUT, requestContent, "application/json", UNAUTH_USER_NAME, UNAUTH_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(403);
        expectedResponse.setDetail("The 'Administrator' role is required to create, delete, or update a resource.");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to update a group resource using a user only the reader role.
     */
    @Test
    public void put_Groups_Authorization_ReaderRole() throws Exception {
        final String METHOD_NAME = "put_Groups_Authorization_ReaderRole";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups/cn=somegroup,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 403, null, HTTPRequestMethod.PUT, requestContent, "application/json", READER_USER_NAME, READER_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(403);
        expectedResponse.setDetail("The 'Administrator' role is required to create, delete, or update a resource.");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test updating a group with no ID in the URL.
     */
    @Test
    public void put_Groups_NoID() throws Exception {
        final String METHOD_NAME = "put_Groups_NoID";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.PUT, requestContent, "application/json", ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("The SCIM PUT operation requires a resource ID in the URL.");
        expectedResponse.setScimType("invalidSyntax");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test updating a group with an invalid SCIM version in the URL.
     */
    @Test
    public void put_Groups_InvalidVersion() throws Exception {
        final String METHOD_NAME = "put_Groups_InvalidVersion";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v22/Groups/cn=somegroup,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.PUT, requestContent, "application/json", ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("The URL contained an invalid SCIM version (v22)");
        expectedResponse.setScimType("invalidVers");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test updating a group with no content.
     */
    @Test
    public void put_Groups_NoContent() throws Exception {
        final String METHOD_NAME = "put_Groups_NoContent";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Groups/cn=somegroup,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.PUT, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("It was expected that the request contained content, but no content was found on the request.");
        expectedResponse.setScimType("invalidSyntax");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to update a group resource using a user with bad credentials.
     */
    @Test
    public void put_Groups_Unauthenticated() throws Exception {
        final String METHOD_NAME = "put_Groups_Unauthenticated";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v2/Groups/cn=somegroup,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 401, null, HTTPRequestMethod.PUT, requestContent, "application/json", null, null);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         *
         * TODO The RESTHandler framework returns back a response before we can create a JSON error object.
         */
//        ErrorImpl expectedResponse = new ErrorImpl();
//        expectedResponse.setStatus(401);
//        expectedResponse.setDetail("???????");
//        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test updating a group using a URL that specifies the SCIM 2.0 API.
     */
    @Test
    public void put_Groups_V2() throws Exception {
        final String METHOD_NAME = "put_Groups_V2";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v2/Groups/cn=somegroup,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 200, null, HTTPRequestMethod.PUT, requestContent, "application/json", ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty group.
         */
        GroupImpl expectedResponse = new GroupImpl();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test updating a user using the version-less URL.
     */
    @Test
    public void put_Users() throws Exception {
        final String METHOD_NAME = "put_Users";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users/uid=someuser,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 200, null, HTTPRequestMethod.PUT, requestContent, "application/json", ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty ListResponse.
         */
        UserImpl expectedResponse = new UserImpl();
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to update a user resource using a user without any assigned roles.
     */
    @Test
    public void put_Users_Authorization_NoRoles() throws Exception {
        final String METHOD_NAME = "put_Users_Authorization_NoRoles";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users/uid=someuser,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 403, null, HTTPRequestMethod.PUT, requestContent, "application/json", UNAUTH_USER_NAME, UNAUTH_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(403);
        expectedResponse.setDetail("The 'Administrator' role is required to create, delete, or update a resource.");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to update a user resource using a user only the reader role.
     */
    @Test
    public void put_Users_Authorization_ReaderRole() throws Exception {
        final String METHOD_NAME = "put_Users_Authorization_ReaderRole";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users/uid=someuser,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 403, null, HTTPRequestMethod.PUT, requestContent, "application/json", READER_USER_NAME, READER_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(403);
        expectedResponse.setDetail("The 'Administrator' role is required to create, delete, or update a resource.");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test updating a user with no ID in the URL.
     */
    @Test
    public void put_Users_NoID() throws Exception {
        final String METHOD_NAME = "put_Users_NoID";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.PUT, requestContent, "application/json", ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("The SCIM PUT operation requires a resource ID in the URL.");
        expectedResponse.setScimType("invalidSyntax");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test updating a user with an invalid SCIM version in the URL.
     */
    @Test
    public void put_Users_InvalidVersion() throws Exception {
        final String METHOD_NAME = "put_Users_InvalidVersion";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v22/Users/uid=someuser,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.PUT, requestContent, "application/json", ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("The URL contained an invalid SCIM version (v22)");
        expectedResponse.setScimType("invalidVers");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test updating a user with no content.
     */
    @Test
    public void put_Users_NoContent() throws Exception {
        final String METHOD_NAME = "put_Groups_NoContent";

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users/uid=someuser,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 400, null, HTTPRequestMethod.PUT, null, null, ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         */
        ErrorImpl expectedResponse = new ErrorImpl();
        expectedResponse.setStatus(400);
        expectedResponse.setDetail("It was expected that the request contained content, but no content was found on the request.");
        expectedResponse.setScimType("invalidSyntax");
        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test attempting to update a user resource using a user with bad credentials.
     */
    @Test
    public void put_Users_Unauthenticated() throws Exception {
        final String METHOD_NAME = "put_Users_Unauthenticated";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/Users/uid=someuser,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 401, null, HTTPRequestMethod.PUT, requestContent, "application/json", null, null);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Compare the response with our expected response.
         *
         * TODO The RESTHandler framework returns back a response before we can create a JSON error object.
         */
//        ErrorImpl expectedResponse = new ErrorImpl();
//        expectedResponse.setStatus(401);
//        expectedResponse.setDetail("???????");
//        assertResponseEquals(expectedResponse, result.toString());
    }

    /**
     * Test updating a user using a URL that specifies the SCIM 2.0 API.
     */
    @Test
    public void put_Users_V2() throws Exception {
        final String METHOD_NAME = "put_Users_V2";

        String content = "{}";
        InputStream requestContent = new ByteArrayInputStream(content.getBytes());

        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/ibm/api/scim/v2/Users/uid=someuser,o=ibm.com";
        StringBuilder result = SCIMFatUtils.callURL(url, 200, null, HTTPRequestMethod.PUT, requestContent, "application/json", ADMIN_USER_NAME, ADMIN_USER_PASSWORD);
        Log.info(c, METHOD_NAME, "Result: " + result);

        /*
         * Currently hard-coded to empty ListResponse.
         */
        UserImpl expectedResponse = new UserImpl();
        assertResponseEquals(expectedResponse, result.toString());
    }
}
