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
package com.ibm.ws.jaxrs21.fat.security.servlet;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;

@WebServlet(urlPatterns = "/SecurityAnnotationsTestServlet")
public class SecurityAnnotationsTestServlet extends SecurityAnnotationsParentTestServlet {

    private static final long serialVersionUID = 4563456788769868446L;

    private static final String clz = "SecurityAnnotationsTestServlet";
    private static final Logger LOG = Logger.getLogger(SecurityAnnotationsTestServlet.class.getName());

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

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.connectTimeout(120000, TimeUnit.MILLISECONDS);
        cb.readTimeout(120000, TimeUnit.MILLISECONDS);
        Client c = cb.build();
        WebTarget t = c.target(url);
        CompletableFuture<Response> completableFuture = t.request().accept("text/plain").rx().get().toCompletableFuture();
        try {
            Response response = completableFuture.get();
            assertEquals(403, response.getStatus());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();

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

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.connectTimeout(120000, TimeUnit.MILLISECONDS);
        cb.readTimeout(120000, TimeUnit.MILLISECONDS);
        Client c = cb.build();
        WebTarget t = c.target(url);
        CompletableFuture<Response> completableFuture = t.request().accept("text/plain").rx().get().toCompletableFuture();
        try {
            Response response = completableFuture.get();
            assertEquals(403, response.getStatus());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();

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

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.connectTimeout(120000, TimeUnit.MILLISECONDS);
        cb.readTimeout(120000, TimeUnit.MILLISECONDS);
        Client c = cb.build();
        WebTarget t = c.target(url);
        CompletableFuture<Response> completableFuture = t.request().accept("text/plain").rx().get().toCompletableFuture();
        try {
            Response response = completableFuture.get();
            assertEquals(200, response.getStatus());
            assertEquals("remotely accessible to all through class level PermitAll", response.readEntity(String.class));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();

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

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.connectTimeout(120000, TimeUnit.MILLISECONDS);
        cb.readTimeout(120000, TimeUnit.MILLISECONDS);
        Client c = cb.build();
        WebTarget t = c.target(url);
        CompletableFuture<Response> completableFuture = t.request().accept("text/plain").rx().get().toCompletableFuture();
        try {
            Response response = completableFuture.get();
            assertEquals(200, response.getStatus());
            assertEquals("remotely accessible to all through method level PermitAll", response.readEntity(String.class));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();

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

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.connectTimeout(120000, TimeUnit.MILLISECONDS);
        cb.readTimeout(120000, TimeUnit.MILLISECONDS);
        Client c = cb.build();
        WebTarget t = c.target(url);
        CompletableFuture<Response> completableFuture = t.request().accept("text/plain").rx().get().toCompletableFuture();
        try {
            Response response = completableFuture.get();
            assertEquals(200, response.getStatus());
            assertEquals("remotely accessible because of No Security Annotations", response.readEntity(String.class));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();

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
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testClassLevelRolesAllowedUserInRole_withWebXml_roleSameAsGroup() throws Exception {
        testClassLevelRolesAllowedUserInRole(SECANNO_BASE_TEST_URI, false);
    }

    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testClassLevelRolesAllowedUserInRole_noWebXml_roleSameAsGroup() throws Exception {
        testClassLevelRolesAllowedUserInRole(SECANNO_NOWEBXML_BASE_TEST_URI, false);
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
        String url = baseUri + "/ClassRolesAllowed";

        // create the resource instance to interact with
        LOG.info("testClassLevelRolesAllowedUserNotInRole about to invoke the resource: " + url);

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.connectTimeout(120000, TimeUnit.MILLISECONDS);
        cb.readTimeout(120000, TimeUnit.MILLISECONDS);
        cb.register(new BasicAuthFilter("user2", "user2pwd"));
        Client c = cb.build();
        WebTarget t = c.target(url);
        CompletableFuture<Response> completableFuture = t.request().accept("text/plain").rx().get().toCompletableFuture();
        try {
            Response response = completableFuture.get();
            assertEquals(403, response.getStatus());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();

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

    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testMethodLevelRolesAllowedUserInRoleMultipleRequests_noWebXml() throws Exception {
        testMethodLevelRolesAllowedUserInRoleMultipleRequests(SECANNO_NOWEBXML_BASE_TEST_URI, true);
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

        String url = baseUri + "/MethodRolesAllowed";

        // create the resource instance to interact with
        LOG.info("testMethodLevelRolesAllowedUserNotInRole about to invoke the resource: " + url);

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.connectTimeout(120000, TimeUnit.MILLISECONDS);
        cb.readTimeout(120000, TimeUnit.MILLISECONDS);
        cb.register(new BasicAuthFilter("user1", "user1pwd"));
        Client c = cb.build();
        WebTarget t = c.target(url);
        CompletableFuture<Response> completableFuture = t.request().accept("text/plain").rx().get().toCompletableFuture();
        try {
            Response response = completableFuture.get();
            assertEquals(403, response.getStatus());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();

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
        String url = baseUri + "/ClassAllAnnotations";

        // create the resource instance to interact with
        LOG.info("testClassAllAnnotations about to invoke the resource: " + url);

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.connectTimeout(120000, TimeUnit.MILLISECONDS);
        cb.readTimeout(120000, TimeUnit.MILLISECONDS);
        cb.register(new BasicAuthFilter("user1", "user1pwd"));
        Client c = cb.build();
        WebTarget t = c.target(url);
        CompletableFuture<Response> completableFuture = t.request().accept("text/plain").rx().get().toCompletableFuture();
        try {
            Response response = completableFuture.get();
            assertEquals(403, response.getStatus());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();

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
        String url = baseUri + "/MethodAllAnnotations";

        // create the resource instance to interact with
        LOG.info("testMethodAllAnnotations about to invoke the resource: " + url);

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.connectTimeout(120000, TimeUnit.MILLISECONDS);
        cb.readTimeout(120000, TimeUnit.MILLISECONDS);
        cb.register(new BasicAuthFilter("user1", "user1pwd"));
        Client c = cb.build();
        WebTarget t = c.target(url);
        CompletableFuture<Response> completableFuture = t.request().accept("text/plain").rx().get().toCompletableFuture();
        try {
            Response response = completableFuture.get();
            assertEquals(403, response.getStatus());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();

        LOG.exiting(clz, "exiting testMethodAllAnnotations SUCCESS");
    }

}