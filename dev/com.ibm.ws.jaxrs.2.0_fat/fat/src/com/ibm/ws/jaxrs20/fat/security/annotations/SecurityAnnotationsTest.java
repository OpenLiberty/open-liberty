/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat.security.annotations;

import static com.ibm.ws.jaxrs20.fat.TestUtils.getBaseTestUri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;
import org.apache.wink.client.handlers.BasicAuthSecurityHandler;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class SecurityAnnotationsTest {

    @Server("com.ibm.ws.jaxrs.fat.security")
    public static LibertyServer server;

    private static final String secwar = "security";
    private static final String secNoWebXmlWar = "securityNoWebXml";
    private static WebArchive secwarapp;
    private static WebArchive secwarappnoxml;

    private static final String clz = "SecurityAnnotationsTest";
    private static final Logger LOG = Logger.getLogger(SecurityAnnotationsTest.class.getName());
    private RestClient client;

    private static LibertyServer swapToServer(LibertyServer newServer) throws Exception {
        LibertyServer oldServer = server;
        tearDown();
        server = newServer;
        startServer();
        return oldServer;
    }

    @Before
    public void setUp() {
        ClientConfig config = new ClientConfig();
        config.connectTimeout(120000);
        config.readTimeout(120000);
        client = new RestClient(config);
    }

    @BeforeClass
    public static void setupClass() throws Exception {
        secwarapp = ShrinkHelper.buildDefaultApp(secwar, "com.ibm.ws.jaxrs.fat.security.annotations",
                                "com.ibm.ws.jaxrs.fat.security.ssl",
                                "com.ibm.ws.jaxrs.fat.securitycontext",
                                "com.ibm.ws.jaxrs.fat.securitycontext.xml");
        secwarappnoxml = ShrinkHelper.buildDefaultApp(secNoWebXmlWar, "com.ibm.ws.jaxrs.fat.security.annotations",
                                "com.ibm.ws.jaxrs.fat.security.ssl",
                                "com.ibm.ws.jaxrs.fat.securitycontext",
                                "com.ibm.ws.jaxrs.fat.securitycontext.xml");
        startServer();
    }

    private static void startServer() throws Exception {
        ShrinkHelper.exportAppToServer(server, secwarapp);
        server.addInstalledAppForValidation(secwar);

        ShrinkHelper.exportAppToServer(server, secwarappnoxml);
        server.addInstalledAppForValidation(secNoWebXmlWar);
        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
            assertNotNull("The server did not start", server.waitForStringInLog("CWWKF0011I"));
            assertNotNull("The Security Service should be ready", server.waitForStringInLog("CWWKS0008I"));
            assertNotNull("FeatureManager did not report update was complete", server.waitForStringInLog("CWWKF0008I"));
            assertNotNull("LTPA configuration should report it is ready", server.waitForStringInLog("CWWKS4105I"));
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    private static String SECANNO_BASE_TEST_URI = getBaseTestUri(secwar, "nomapper");
    private static String SECANNO_NOWEBXML_BASE_TEST_URI = getBaseTestUri(secNoWebXmlWar, "nomapper");

    /**
     * tests DenyAll at class level
     */
    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testClassLevelDenyAll_withWebXml() throws Exception {
        testClassLevelDenyAll(SECANNO_BASE_TEST_URI);
    }

    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testClassLevelDenyAll_noWebXml() throws Exception {
        testClassLevelDenyAll(SECANNO_NOWEBXML_BASE_TEST_URI);
    }

    private void testClassLevelDenyAll(final String baseUri) throws Exception {
        LOG.entering(clz, "entered testClassLevelDenyAll");
        String url = baseUri + "/ClassDenyAll";

        // create the resource instance to interact with
        LOG.info("testClassLevelDenyAll about to invoke the resource: " + url);

        Resource resource = client.resource(url);

        ClientResponse response = resource.accept("text/plain").get();
        assertEquals(403, response.getStatusCode());

        LOG.info("testClassLevelDenyAll SUCCEEDED");
        LOG.exiting(clz, "exiting testClassLevelDenyAll exiting");
    }

    /**
     * tests DenyAll at method level
     */
    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testMethodLevelDenyAll_withWebXml() throws Exception {
        testMethodLevelDenyAll(SECANNO_BASE_TEST_URI);
    }

    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testMethodLevelDenyAll_noWebXml() throws Exception {
        testMethodLevelDenyAll(SECANNO_NOWEBXML_BASE_TEST_URI);
    }

    private void testMethodLevelDenyAll(final String baseUri) throws Exception {
        LOG.entering(clz, "entered testMethodLevelDenyAll");
        String url = baseUri + "/MethodDenyAll";

        // create the resource instance to interact with
        LOG.info("testMethodLevelDenyAll about to invoke the resource: " + url);

        Resource resource = client.resource(url);

        ClientResponse response = resource.accept("text/plain").get();
        assertEquals(403, response.getStatusCode());
        LOG.info("testMethodLevelDenyAll SUCCEEDED");
        LOG.exiting(clz, "exiting testMethodLevelDenyAll exiting");
    }

    /**
     * tests PermitAll at class level
     */
    @Test
    public void testClassLevelPermitAll_withWebXml() throws Exception {
        testClassLevelPermitAll(SECANNO_BASE_TEST_URI);
    }

    @Test
    public void testClassLevelPermitAll_noWebXml() throws Exception {
        testClassLevelPermitAll(SECANNO_NOWEBXML_BASE_TEST_URI);
    }

    private void testClassLevelPermitAll(final String baseUri) throws Exception {
        LOG.entering(clz, "entered testClassLevelPermitAll");
        String url = baseUri + "/ClassPermitAll";

        // create the resource instance to interact with
        LOG.info("testClassLevelPermitAll about to invoke the resource: " + url);

        Resource resource = client.resource(url);

        ClientResponse response = resource.accept("text/plain").get();
        assertEquals(200, response.getStatusCode());
        assertEquals("remotely accessible to all through class level PermitAll", response.getEntity(String.class));

        LOG.info("testClassLevelPermitAll SUCCEEDED");
        LOG.exiting(clz, "exiting testClassLevelPermitAll exiting");
    }

    /**
     * tests PermitAll at method level
     */
    @Test
    public void testMethodLevelPermitAll_withWebXml() throws Exception {
        testMethodLevelPermitAll(SECANNO_BASE_TEST_URI);
    }

    @Test
    public void testMethodLevelPermitAll_noWebXml() throws Exception {
        testMethodLevelPermitAll(SECANNO_NOWEBXML_BASE_TEST_URI);
    }

    private void testMethodLevelPermitAll(final String baseUri) throws Exception {
        LOG.entering(clz, "entered testMethodLevelPermitAll");
        String url = baseUri + "/MethodPermitAll";

        // create the resource instance to interact with
        LOG.info("testMethodLevelPermitAll about to invoke the resource: " + url);

        Resource resource = client.resource(url);

        ClientResponse response = resource.accept("text/plain").get();
        assertEquals(200, response.getStatusCode());
        assertEquals("remotely accessible to all through method level PermitAll", response.getEntity(String.class));

        LOG.info("testMethodLevelPermitAll SUCCEEDED");
        LOG.exiting(clz, "exiting testMethodLevelPermitAll SUCCESS");
    }

    /**
     * tests case of no security annotations
     */
    @Test
    public void testNoSecAnnotations_withWebXml() throws Exception {
        testNoSecAnnotations(SECANNO_BASE_TEST_URI);
    }

    @Test
    public void testNoSecAnnotations_noWebXml() throws Exception {
        testNoSecAnnotations(SECANNO_NOWEBXML_BASE_TEST_URI);
    }

    private void testNoSecAnnotations(final String baseUri) throws Exception {
        LOG.entering(clz, "entered testNoSecAnnotations");
        String url = baseUri + "/NoSecAnnotations";

        // create the resource instance to interact with
        LOG.info("testNoSecAnnotations about to invoke the resource: " + url);

        Resource resource = client.resource(url);

        ClientResponse response = resource.accept("text/plain").get();
        assertEquals(200, response.getStatusCode());
        assertEquals("remotely accessible because of No Security Annotations", response.getEntity(String.class));

        LOG.info("testNoSecAnnotations SUCCEEDED");
        LOG.exiting(clz, "exiting testNoSecAnnotations exiting");
    }

    /**
     * tests RolesAllowed at class level -- user in role defined in application-bnd element
     */
    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testClassLevelRolesAllowedUserInRole_withWebXml() throws Exception {
        testClassLevelRolesAllowedUserInRole(SECANNO_BASE_TEST_URI, true);
    }

    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testClassLevelRolesAllowedUserInRole_noWebXml() throws Exception {
        testClassLevelRolesAllowedUserInRole(SECANNO_NOWEBXML_BASE_TEST_URI, true);
    }

    /**
     * tests RolesAllowed at class level -- user in role same as user's group name
     */
    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testClassLevelRolesAllowedUserInRole_withWebXml_roleSameAsGroup() throws Exception {
        LibertyServer oldServer = null;
        try {
            oldServer = swapToServer(LibertyServerFactory.getLibertyServer("com.ibm.ws.jaxrs.fat.security_rolesAsGroups"));
            testClassLevelRolesAllowedUserInRole(SECANNO_BASE_TEST_URI, false);
        } finally {
            if (oldServer != null) {
                swapToServer(oldServer);
            }
        }
    }

    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testClassLevelRolesAllowedUserInRole_noWebXml_roleSameAsGroup() throws Exception {
        LibertyServer oldServer = null;
        try {
            oldServer = swapToServer(LibertyServerFactory.getLibertyServer("com.ibm.ws.jaxrs.fat.security_rolesAsGroups"));
            testClassLevelRolesAllowedUserInRole(SECANNO_NOWEBXML_BASE_TEST_URI, false);
        } finally {
            if (oldServer != null) {
                swapToServer(oldServer);
            }
        }
    }

    private void testClassLevelRolesAllowedUserInRole(final String baseUri, boolean userRoleFromAppBnd) throws Exception {
        LOG.entering(clz, "entered testClassLevelRolesAllowedUserInRole");
        ClientConfig config = new ClientConfig();
        BasicAuthSecurityHandler authSecHandler = new BasicAuthSecurityHandler();
        authSecHandler.setUserName(userRoleFromAppBnd ? "user1a" : "user1");
        authSecHandler.setPassword("user1pwd");
        config.handlers(authSecHandler);
        config.connectTimeout(120000);
        config.readTimeout(120000);
        RestClient client = new RestClient(config);
        String url = baseUri + "/ClassRolesAllowed";

        // create the resource instance to interact with
        LOG.info("testClassLevelRolesAllowedUserInRole about to invoke the resource: " + url);

        Resource resource = client.resource(url);

        ClientResponse response = resource.accept("text/plain").get();
        assertEquals(200, response.getStatusCode());
        assertEquals("remotely accessible only to users in Role1", response.getEntity(String.class));

        LOG.info("testClassLevelRolesAllowedUserInRole SUCCEEDED");
        LOG.exiting(clz, "exiting testClassLevelRolesAllowedUserInRole exiting");
    }

    /**
     * tests RolesAllowed at the class level -- user not in role
     */
    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testClassLevelRolesAllowedUserNotInRole_withWebXml() throws Exception {
        testClassLevelRolesAllowedUserNotInRole(SECANNO_BASE_TEST_URI);
    }

    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testClassLevelRolesAllowedUserNotInRole_noWebXml() throws Exception {
        testClassLevelRolesAllowedUserNotInRole(SECANNO_NOWEBXML_BASE_TEST_URI);
    }

    private void testClassLevelRolesAllowedUserNotInRole(final String baseUri) throws Exception {
        LOG.entering(clz, "entered testClassLevelRolesAllowedUserNotInRole");
        ClientConfig config = new ClientConfig();
        BasicAuthSecurityHandler authSecHandler = new BasicAuthSecurityHandler();
        authSecHandler.setUserName("user2");
        authSecHandler.setPassword("user2pwd");
        config.handlers(authSecHandler);
        config.connectTimeout(120000);
        config.readTimeout(120000);
        RestClient client = new RestClient(config);
        String url = baseUri + "/ClassRolesAllowed";

        // create the resource instance to interact with
        LOG.info("testClassLevelRolesAllowedUserNotInRole about to invoke the resource: " + url);

        Resource resource = client.resource(url);

        ClientResponse response = resource.accept("text/plain").get();
        assertEquals(403, response.getStatusCode());
        LOG.exiting(clz, "exiting testClassLevelRolesAllowedUserNotInRole SUCCEEDED");
        LOG.exiting(clz, "exiting testClassLevelRolesAllowedUserNotInRole exiting");
    }

    /**
     * tests RolesAllowed at method level -- user in role defined in application-bnd element
     */
    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testMethodLevelRolesAllowedUserInRole_withWebXml() throws Exception {
        testMethodLevelRolesAllowedUserInRole(SECANNO_BASE_TEST_URI, true);
    }

    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testMethodLevelRolesAllowedUserInRole_noWebXml() throws Exception {
        testMethodLevelRolesAllowedUserInRole(SECANNO_NOWEBXML_BASE_TEST_URI, true);
    }

    /**
     * tests RolesAllowed at method level -- user in role same as user's group name
     */
    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testMethodLevelRolesAllowedUserInRole_withWebXml_roleSameAsGroup() throws Exception {
        LibertyServer oldServer = null;
        try {
            oldServer = swapToServer(LibertyServerFactory.getLibertyServer("com.ibm.ws.jaxrs.fat.security_rolesAsGroups"));
            testMethodLevelRolesAllowedUserInRole(SECANNO_BASE_TEST_URI, false);
        } finally {
            if (oldServer != null) {
                swapToServer(oldServer);
            }
        }

    }

    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testMethodLevelRolesAllowedUserInRole_noWebXml_roleSameAsGroup() throws Exception {
        LibertyServer oldServer = null;
        try {
            oldServer = swapToServer(LibertyServerFactory.getLibertyServer("com.ibm.ws.jaxrs.fat.security_rolesAsGroups"));
            testMethodLevelRolesAllowedUserInRole(SECANNO_NOWEBXML_BASE_TEST_URI, false);
        } finally {
            if (oldServer != null) {
                swapToServer(oldServer);
            }
        }

    }

    private void testMethodLevelRolesAllowedUserInRole(final String baseUri, boolean userRoleFromAppBnd) throws Exception {
        LOG.entering(clz, "entered testMethodLevelRolesAllowedUserInRole");
        ClientConfig config = new ClientConfig();
        BasicAuthSecurityHandler authSecHandler = new BasicAuthSecurityHandler();
        authSecHandler.setUserName(userRoleFromAppBnd ? "user2a" : "user2");
        authSecHandler.setPassword("user2pwd");
        config.handlers(authSecHandler);
        config.connectTimeout(120000);
        config.readTimeout(120000);
        RestClient client = new RestClient(config);
        String url = baseUri + "/MethodRolesAllowed";

        // create the resource instance to interact with
        LOG.info("testMethodLevelRolesAllowedUserInRole about to invoke the resource: " + url);

        Resource resource = client.resource(url);

        ClientResponse response = resource.accept("text/plain").get();
        assertEquals(200, response.getStatusCode());
        assertEquals("remotely accessible only to users in Role2", response.getEntity(String.class));

        LOG.info("testMethodLevelRolesAllowedUserInRole SUCCEEDED");
        LOG.exiting(clz, "exiting testMethodLevelRolesAllowedUserInRole exiting");
    }

    /**
     * tests RolesAllowed at the method level -- user not in role
     */
    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testMethodLevelRolesAllowedUserNotInRole_withWebXml() throws Exception {
        testMethodLevelRolesAllowedUserNotInRole(SECANNO_BASE_TEST_URI);
    }

    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testMethodLevelRolesAllowedUserNotInRole_noWebXml() throws Exception {
        testMethodLevelRolesAllowedUserNotInRole(SECANNO_NOWEBXML_BASE_TEST_URI);
    }

    private void testMethodLevelRolesAllowedUserNotInRole(final String baseUri) throws Exception {
        LOG.entering(clz, "entered testMethodLevelRolesAllowedUserNotInRole");
        ClientConfig config = new ClientConfig();
        BasicAuthSecurityHandler authSecHandler = new BasicAuthSecurityHandler();
        authSecHandler.setUserName("user1");
        authSecHandler.setPassword("user1pwd");
        config.handlers(authSecHandler);
        config.connectTimeout(120000);
        config.readTimeout(120000);
        RestClient client = new RestClient(config);
        String url = baseUri + "/MethodRolesAllowed";

        // create the resource instance to interact with
        LOG.info("testMethodLevelRolesAllowedUserNotInRole about to invoke the resource: " + url);

        Resource resource = client.resource(url);

        ClientResponse response = resource.accept("text/plain").get();
        assertEquals(403, response.getStatusCode());
        LOG.exiting(clz, "exiting testMethodLevelRolesAllowedUserNotInRole SUCCESS");
    }

    /**
     * tests All security annotations used simultaneously at the class level
     */
    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testClassAllAnnotations_withWebXml() throws Exception {
        testClassAllAnnotations(SECANNO_BASE_TEST_URI);
    }

    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testClassAllAnnotations_noWebXml() throws Exception {
        testClassAllAnnotations(SECANNO_NOWEBXML_BASE_TEST_URI);
    }

    private void testClassAllAnnotations(final String baseUri) throws Exception {
        LOG.entering(clz, "entered testClassAllAnnotations");
        ClientConfig config = new ClientConfig();
        BasicAuthSecurityHandler authSecHandler = new BasicAuthSecurityHandler();
        authSecHandler.setUserName("user1");
        authSecHandler.setPassword("user1pwd");
        config.handlers(authSecHandler);
        config.connectTimeout(120000);
        config.readTimeout(120000);
        RestClient client = new RestClient(config);
        String url = baseUri + "/ClassAllAnnotations";

        // create the resource instance to interact with
        LOG.info("testClassAllAnnotations about to invoke the resource: " + url);

        Resource resource = client.resource(url);

        ClientResponse response = resource.accept("text/plain").get();
        assertEquals(403, response.getStatusCode());
        LOG.exiting(clz, "exiting testClassAllAnnotations SUCCESS");
    }

    /**
     * tests All security annotations used simultaneously at the method level
     */
    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testMethodAllAnnotations_withWebXml() throws Exception {
        testMethodAllAnnotations(SECANNO_BASE_TEST_URI);
    }

    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testMethodAllAnnotations_noWebXml() throws Exception {
        testMethodAllAnnotations(SECANNO_NOWEBXML_BASE_TEST_URI);
    }

    private void testMethodAllAnnotations(final String baseUri) throws Exception {
        ClientConfig config = new ClientConfig();
        BasicAuthSecurityHandler authSecHandler = new BasicAuthSecurityHandler();
        authSecHandler.setUserName("user1");
        authSecHandler.setPassword("user1pwd");
        config.handlers(authSecHandler);
        config.connectTimeout(120000);
        config.readTimeout(120000);
        RestClient client = new RestClient(config);
        String url = baseUri + "/MethodAllAnnotations";

        // create the resource instance to interact with
        LOG.info("testMethodAllAnnotations about to invoke the resource: " + url);

        Resource resource = client.resource(url);

        ClientResponse response = resource.accept("text/plain").get();
        assertEquals(403, response.getStatusCode());
        LOG.exiting(clz, "exiting testMethodAllAnnotations SUCCESS");
    }
}
