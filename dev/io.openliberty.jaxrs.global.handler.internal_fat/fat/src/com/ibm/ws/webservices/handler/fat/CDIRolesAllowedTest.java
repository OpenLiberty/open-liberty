/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.webservices.handler.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;
import org.apache.wink.client.handlers.BasicAuthSecurityHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class CDIRolesAllowedTest {

    @Server("RSCDITestServer")
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("RSCDITestServer");

    private static final String CONTEXT_ROOT = "cdi-rolesallowed";

    @BeforeClass
    public static void setup() throws Exception {

        //server.installUserBundle("RSHandler1_1.0.0");
        ShrinkHelper.defaultUserFeatureArchive(server, "rsUserBundle1", "com.ibm.ws.rsuserbundle1.myhandler");
        TestUtils.installUserFeature(server, "RSHandler1Feature");
        //server.installUserBundle("RSHandler2_1.0.0");
        ShrinkHelper.defaultUserFeatureArchive(server, "rsUserBundle2", "com.ibm.ws.rsuserbundle2.myhandler");
        TestUtils.installUserFeature(server, "RSHandler2Feature");

        ShrinkHelper.defaultApp(server, CONTEXT_ROOT, "com.ibm.ws.jaxrs.fat.cdi.rolesallowed");
        server.addInstalledAppForValidation(CONTEXT_ROOT);
        server.startServer("RolesAllowedTest.log", true);

        // Wait for security service to start
        // CWWKS0008I: The security service is ready.
        assertNotNull("Security service did not report it was ready", server.waitForStringInLog("CWWKS0008I"));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKW1001W");
        server.uninstallUserBundle("rsUserBundle1");
        server.uninstallUserFeature("RSHandler1Feature");
        server.uninstallUserBundle("rsUserBundle2");
        server.uninstallUserFeature("RSHandler2Feature");
    }

    /**
     * Tests that an authenticated and authorized user can invoke the JAX-RS
     * resource methods.
     * 
     * @throws Exception
     */
    @Test
    public void testPostiveManagersAccess() throws Exception {
        ClientConfig config = new ClientConfig();
        BasicAuthSecurityHandler secHandler = new BasicAuthSecurityHandler();
        //secHandler.setSSLRequired(false);
        secHandler.setUserName("user2");
        secHandler.setPassword("user2pwd");
        config.handlers(secHandler);
        RestClient client = new RestClient(config);
        //final URI uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(CONTEXT_ROOT, "rolesallowed")).path(OnlyManagersResource.class).build();
        final URI uri = URI.create(TestUtils.getBaseTestUri(CONTEXT_ROOT, "rolesallowed") + "/managersonly");

        Resource res = client.resource(uri);

        ClientResponse response = res.delete();
        assertEquals(Status.NO_CONTENT, response.getStatusCode());

        response = res.get();
        assertEquals(Status.NO_CONTENT, response.getStatusCode());

        String data = "My Hello World String!";
        response = res.post(data);
        assertEquals(Status.OK, response.getStatusCode());
        assertEquals(data, response.getEntity(String.class));
        assertNotNull("RSInHandler1 not invoked", server.waitForStringInLog("in RSInHandler1 handleMessage method"));
        assertNotNull("RSInHandler2 not invoked", server.waitForStringInLog("in RSInHandler2 handleMessage method"));

        response = res.get();
        assertEquals(Status.OK, response.getStatusCode());
        assertEquals(data, response.getEntity(String.class));

        data = "Goodbye String!";
        response = res.post(data);
        assertEquals(Status.OK, response.getStatusCode());
        assertEquals(data, response.getEntity(String.class));

        response = res.get();
        assertEquals(Status.OK, response.getStatusCode());
        assertEquals(data, response.getEntity(String.class));
    }

    /**
     * Tests that an unauthenticated user 1ill get a 401 Unauthorized.
     * 
     * @throws Exception
     */
    @Test
    public void testNegativeNoCredentialsManagersAccess() throws Exception {
        RestClient client = new RestClient();
        //final URI uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(CONTEXT_ROOT, "rolesallowed")).path(OnlyManagersResource.class).build();
        final URI uri = URI.create(TestUtils.getBaseTestUri(CONTEXT_ROOT, "rolesallowed") + "/managersonly");

        Resource res = client.resource(uri);

        ClientResponse response = res.delete();
        assertEquals(Status.UNAUTHORIZED, response.getStatusCode());
        assertEquals(null, response.getEntity(String.class));

        response = res.get();
        assertEquals(Status.UNAUTHORIZED, response.getStatusCode());
        assertEquals(null, response.getEntity(String.class));

        String data = "My Hello World String!";
        response = res.post(data);
        assertEquals(Status.UNAUTHORIZED, response.getStatusCode());
        assertEquals(null, response.getEntity(String.class));

        response = res.get();
        assertEquals(Status.UNAUTHORIZED, response.getStatusCode());
        assertEquals(null, response.getEntity(String.class));

        data = "Goodbye String!";
        response = res.post(data);
        assertEquals(Status.UNAUTHORIZED, response.getStatusCode());
        assertEquals(null, response.getEntity(String.class));

        response = res.get();
        assertEquals(Status.UNAUTHORIZED, response.getStatusCode());
        assertEquals(null, response.getEntity(String.class));
    }

    /**
     * Tests that an authenticated user (but not authorized) will get a 403
     * Forbidden.
     * 
     * @throws Exception
     */
    @Test
    public void testNegativeWrongUserCredentialsManagersAccess() throws Exception {
        ClientConfig config = new ClientConfig();
        BasicAuthSecurityHandler secHandler = new BasicAuthSecurityHandler();
        //secHandler.setSSLRequired(false);
        secHandler.setUserName("user1");
        secHandler.setPassword("user1pwd");
        config.handlers(secHandler);
        RestClient client = new RestClient(config);
        //final URI uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(CONTEXT_ROOT, "rolesallowed")).path(OnlyManagersResource.class).build();
        final URI uri = URI.create(TestUtils.getBaseTestUri(CONTEXT_ROOT, "rolesallowed") + "/managersonly");

        Resource res = client.resource(uri);

        ClientResponse response = res.delete();
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getEntity(String.class), response.getEntity(String.class)
                        .contains("Error 403: AuthorizationFailed"));

        response = res.get();
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getEntity(String.class), response.getEntity(String.class)
                        .contains("Error 403: AuthorizationFailed"));

        String data = "My Hello World String!";
        response = res.post(data);
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getEntity(String.class), response.getEntity(String.class)
                        .contains("Error 403: AuthorizationFailed"));

        response = res.get();
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getEntity(String.class), response.getEntity(String.class)
                        .contains("Error 403: AuthorizationFailed"));

        data = "Goodbye String!";
        response = res.post(data);
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getEntity(String.class), response.getEntity(String.class)
                        .contains("Error 403: AuthorizationFailed"));

        response = res.get();
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getEntity(String.class), response.getEntity(String.class)
                        .contains("Error 403: AuthorizationFailed"));
    }

    /**
     * Tests that an authenticated and authorized user can invoke the JAX-RS
     * resource methods.
     * 
     * @throws Exception
     */
    @Test
    @SkipForRepeat(JakartaEE9Action.ID) // needs more investigation
    public void testPostiveEmployeeeAccess() throws Exception {
        ClientConfig config = new ClientConfig();
        BasicAuthSecurityHandler secHandler = new BasicAuthSecurityHandler();
        //secHandler.setSSLRequired(false);
        secHandler.setUserName("user4");
        secHandler.setPassword("user4pwd");
        config.handlers(secHandler);
        RestClient client = new RestClient(config);
        //final URI uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(CONTEXT_ROOT, "rolesallowed")).path(EmployeeResource.class).build("1234");
        final URI uri = URI.create(TestUtils.getBaseTestUri(CONTEXT_ROOT, "rolesallowed") + "/employee/1234");

        Resource res = client.resource(uri);

        ClientResponse response = res.delete();
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.get();
        assertEquals(Status.NOT_FOUND, response.getStatusCode());

        response = res.post("random data");
        assertEquals(Status.OK, response.getStatusCode());
        assertEquals("user4:1234", response.getEntity(String.class));

        response = res.get();
        assertEquals(Status.OK, response.getStatusCode());
        assertEquals("user4:1234", response.getEntity(String.class));

        response = res.put("my name");
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.delete();
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.get();
        assertEquals(Status.OK, response.getStatusCode());
        assertEquals("user4:1234", response.getEntity(String.class));
    }

    /**
     * Tests that an unauthenticated user cannot invoke the JAX-RS resource
     * methods.
     * 
     * @throws Exception
     */
    @Test
    public void testNegativeNoCredentialEmployeeeAccess() throws Exception {
        RestClient client = new RestClient();
        //final URI uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(CONTEXT_ROOT, "rolesallowed")).path(EmployeeResource.class).build("1234");
        final URI uri = URI.create(TestUtils.getBaseTestUri(CONTEXT_ROOT, "rolesallowed") + "/employee/1234");

        Resource res = client.resource(uri);

        ClientResponse response = res.delete();
        assertEquals(Status.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.get();
        assertEquals(Status.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.post("random data");
        assertEquals(Status.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.get();
        assertEquals(Status.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.put("my name");
        assertEquals(Status.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.delete();
        assertEquals(Status.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.get();
        assertEquals(Status.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getEntity(String.class));
    }

    /**
     * Tests that an authenticated but unauthorized user cannot invoke the
     * JAX-RS resource methods.
     * 
     * @throws Exception
     */
    @Test
    @SkipForRepeat(JakartaEE9Action.ID) // needs more investigation
    public void testNegativeWrongUserCredentialsEmployeeeAccess() throws Exception {
        ClientConfig config = new ClientConfig();
        BasicAuthSecurityHandler secHandler = new BasicAuthSecurityHandler();
        //secHandler.setSSLRequired(false);
        secHandler.setUserName("user2");
        secHandler.setPassword("user2pwd");
        config.handlers(secHandler);
        RestClient client = new RestClient(config);
        //final URI uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(CONTEXT_ROOT, "rolesallowed")).path(EmployeeResource.class).build("1234");
        final URI uri = URI.create(TestUtils.getBaseTestUri(CONTEXT_ROOT, "rolesallowed") + "/employee/1234");

        Resource res = client.resource(uri);

        ClientResponse response = res.delete();
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.get();
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.post("random data");
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.get();
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.put("my name");
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.delete();
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.get();
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertNull(response.getEntity(String.class));
    }

    /**
     * Tests that an authenticated and authorized user can invoke the JAX-RS
     * resource methods.
     * 
     * @throws Exception
     */
    @Test
    @SkipForRepeat(JakartaEE9Action.ID) // needs more investigation
    public void testPostiveHRAccess() throws Exception {
        ClientConfig config = new ClientConfig();
        BasicAuthSecurityHandler secHandler = new BasicAuthSecurityHandler();
        //secHandler.setSSLRequired(false);
        secHandler.setUserName("user3");
        secHandler.setPassword("user3pwd");
        config.handlers(secHandler);
        RestClient client = new RestClient(config);
        //final URI uri = UriBuilder.fromUri(TestUtils.getBaseTestUri(CONTEXT_ROOT, "rolesallowed")).path(EmployeeResource.class).build("1234");
        final URI uri = URI.create(TestUtils.getBaseTestUri(CONTEXT_ROOT, "rolesallowed") + "/employee/1234");

        Resource res = client.resource(uri);

        ClientResponse response = res.delete();
        assertEquals(Status.NO_CONTENT, response.getStatusCode());

        response = res.get();
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.post("random data");
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.get();
        assertEquals(Status.FORBIDDEN, response.getStatusCode());
        assertNull(response.getEntity(String.class));

        response = res.put("my name");
        assertEquals(Status.OK, response.getStatusCode());
        assertEquals("my name:1234", response.getEntity(String.class));

        config = new ClientConfig();
        secHandler = new BasicAuthSecurityHandler();
        //secHandler.setSSLRequired(false);
        secHandler.setUserName("user4");
        secHandler.setPassword("user4pwd");
        config.handlers(secHandler);
        RestClient employeeClient = new RestClient(config);
        res = employeeClient.resource(uri);

        response = res.get();
        assertEquals(Status.OK, response.getStatusCode());
        assertEquals("my name:1234", response.getEntity(String.class));

        res = client.resource(uri);
        response = res.delete();
        assertEquals(Status.NO_CONTENT, response.getStatusCode());

        res = employeeClient.resource(uri);
        response = res.get();
        assertEquals(Status.NOT_FOUND, response.getStatusCode());
        assertNull(response.getEntity(String.class));
    }
}
