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
package com.ibm.ws.jaxrs.fat.security.servlet;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

import componenttest.app.FATServlet;

public class SecurityAnnotationsParentTestServlet extends FATServlet {

    private static final long serialVersionUID = 4563456788769868446L;

    protected static String SECANNO_BASE_TEST_URI = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_secondary") + "/security/nomapper";
    protected static String SECANNO_NOWEBXML_BASE_TEST_URI = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_secondary") + "/securityNoWebXml/nomapper";

    private static final String clz = "SecurityAnnotationsParentTestServlet";
    private static final Logger LOG = Logger.getLogger(SecurityAnnotationsParentTestServlet.class.getName());

    protected void testClassLevelRolesAllowedUserInRole(final String baseUri, boolean userRoleFromAppBnd) throws Exception {
        LOG.entering(clz, "entered testClassLevelRolesAllowedUserInRole");
        String url = baseUri + "/ClassRolesAllowed";

        // create the resource instance to interact with
        LOG.info("testClassLevelRolesAllowedUserInRole about to invoke the resource: " + url);

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.register(new BasicAuthFilter(userRoleFromAppBnd ? "user1a" : "user1", "user1pwd"));
        Client c = cb.build();
        WebTarget t = c.target(url);

        Response response = t.request().accept("text/plain").get();
        assertEquals(200, response.getStatus());
        assertEquals("remotely accessible only to users in Role1", response.readEntity(String.class));
        c.close();

        LOG.info("testClassLevelRolesAllowedUserInRole SUCCEEDED");
        LOG.exiting(clz, "exiting testClassLevelRolesAllowedUserInRole exiting");
    }

    protected void testMethodLevelRolesAllowedUserInRole(final String baseUri, boolean userRoleFromAppBnd) throws Exception {
        LOG.entering(clz, "entered testMethodLevelRolesAllowedUserInRole");
        String url = baseUri + "/MethodRolesAllowed";

        // create the resource instance to interact with
        LOG.info("testMethodLevelRolesAllowedUserInRole about to invoke the resource: " + url);

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.register(new BasicAuthFilter(userRoleFromAppBnd ? "user2a" : "user2", "user2pwd"));
        Client c = cb.build();
        WebTarget t = c.target(url);
        Response response = t.request().accept("text/plain").get();
        assertEquals(200, response.getStatus());
        assertEquals("remotely accessible only to users in Role2", response.readEntity(String.class));
        c.close();

        LOG.info("testMethodLevelRolesAllowedUserInRole SUCCEEDED");
        LOG.exiting(clz, "exiting testMethodLevelRolesAllowedUserInRole exiting");
    }

    protected void testMethodLevelRolesAllowedUserInRoleMultipleRequests(final String baseUri, boolean userRoleFromAppBnd) throws Exception {
        LOG.entering(clz, "entered testMethodLevelRolesAllowedUserInRoleMultipleRequests");
        String url = baseUri + "/MethodRolesAllowed";

        // create the resource instance to interact with
        LOG.info("testMethodLevelRolesAllowedUserInRoleMultipleRequests about to invoke the resource: " + url);

        final String threadName1 = "jaxrsThread";
        ThreadFactory jaxrsThreadFactory1 = Executors.defaultThreadFactory();

        Thread jaxrsThread1 = jaxrsThreadFactory1.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName1))) {
                    throw new RuntimeException("testMethodLevelRolesAllowedUserInRoleMultipleRequests1: incorrect thread name");
                }
            }
        });

        jaxrsThread1.setName(threadName1);
        ClientBuilder cb1 = ClientBuilder.newBuilder();
        cb1.register(new BasicAuthFilter(userRoleFromAppBnd ? "user2a" : "user2", "user2pwd"));
        Client c1 = cb1.build();
        WebTarget t1 = c1.target(url);

        Response response1 = t1.request().accept("text/plain").get();
        assertEquals(200, response1.getStatus());
        assertEquals("remotely accessible only to users in Role2", response1.readEntity(String.class));

        final String threadName2 = "jaxrsThread";
        ThreadFactory jaxrsThreadFactory2 = Executors.defaultThreadFactory();

        Thread jaxrsThread2 = jaxrsThreadFactory2.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName2))) {
                    throw new RuntimeException("testMethodLevelRolesAllowedUserInRoleMultipleRequests2: incorrect thread name");
                }
            }
        });

        jaxrsThread2.setName(threadName2);
        ClientBuilder cb2 = ClientBuilder.newBuilder();
        cb2.register(new BasicAuthFilter(userRoleFromAppBnd ? "user2a" : "user2", "user2pwd"));
        Client c2 = cb2.build();
        WebTarget t2 = c2.target(url);

        Response response2 = t2.request().accept("text/plain").get();
        assertEquals(200, response2.getStatus());
        assertEquals("remotely accessible only to users in Role2", response2.readEntity(String.class));

        c1.close();
        c2.close();

        LOG.info("testMethodLevelRolesAllowedUserInRoleMultipleRequests SUCCEEDED");
        LOG.exiting(clz, "exiting testMethodLevelRolesAllowedUserInRoleMultipleRequests exiting");
    }

    public class BasicAuthFilter implements ClientRequestFilter {
//https://www.ibm.com/support/knowledgecenter/en/SSEQTP_8.5.5/com.ibm.websphere.wlp.doc/ae/cwlp_jaxrs_behavior.html
        private final String usr;
        private final String pwd;

        public BasicAuthFilter(String usr, String pwd) {
            this.usr = usr;
            this.pwd = pwd;
        }

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            MultivaluedMap<String, Object> headers = requestContext.getHeaders();

            String token = this.usr + ":" + this.pwd;
            final String basicAuthentication = "Basic " + DatatypeConverter.printBase64Binary(token.getBytes("UTF-8"));
            headers.add("Authorization", basicAuthentication);
        }
    }

}