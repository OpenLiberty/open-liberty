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

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
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

    protected static String SECANNO_BASE_TEST_URI = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_secondary") + "/jaxrs21security/nomapper";
    protected static String SECANNO_NOWEBXML_BASE_TEST_URI = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_secondary") + "/jaxrs21securityNoWebXml/nomapper";

    private static final String clz = "SecurityAnnotationsParentTestServlet";
    private static final Logger LOG = Logger.getLogger(SecurityAnnotationsParentTestServlet.class.getName());

    protected void testClassLevelRolesAllowedUserInRole(final String baseUri, boolean userRoleFromAppBnd) throws Exception {
        LOG.entering(clz, "entered testClassLevelRolesAllowedUserInRole");
        String url = baseUri + "/ClassRolesAllowed";

        // create the resource instance to interact with
        LOG.info("testClassLevelRolesAllowedUserInRole about to invoke the resource: " + url);

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.connectTimeout(120000, TimeUnit.MILLISECONDS);
        cb.readTimeout(120000, TimeUnit.MILLISECONDS);
        cb.register(new BasicAuthFilter(userRoleFromAppBnd ? "user1a" : "user1", "user1pwd"));
        Client c = cb.build();
        WebTarget t = c.target(url);
        CompletableFuture<Response> completableFuture = t.request().accept("text/plain").rx().get().toCompletableFuture();
        try {
            Response response = completableFuture.get();
            assertEquals(200, response.getStatus());
            assertEquals("remotely accessible only to users in Role1", response.readEntity(String.class));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
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
        cb.connectTimeout(120000, TimeUnit.MILLISECONDS);
        cb.readTimeout(120000, TimeUnit.MILLISECONDS);
        cb.register(new BasicAuthFilter(userRoleFromAppBnd ? "user2a" : "user2", "user2pwd"));
        Client c = cb.build();
        WebTarget t = c.target(url);
        CompletableFuture<Response> completableFuture = t.request().accept("text/plain").rx().get().toCompletableFuture();
        try {
            Response response = completableFuture.get();
            assertEquals(200, response.getStatus());
            assertEquals("remotely accessible only to users in Role2", response.readEntity(String.class));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        c.close();

        LOG.info("testMethodLevelRolesAllowedUserInRole SUCCEEDED");
        LOG.exiting(clz, "exiting testMethodLevelRolesAllowedUserInRole exiting");
    }

    protected void testMethodLevelRolesAllowedUserInRoleMultipleRequests(final String baseUri, boolean userRoleFromAppBnd) throws Exception {
        LOG.entering(clz, "entered testMethodLevelRolesAllowedUserInRoleMultipleRequests");
        String url = baseUri + "/MethodRolesAllowed";

        // create the resource instance to interact with
        LOG.info("testMethodLevelRolesAllowedUserInRoleMultipleRequests about to invoke the resource: " + url);

        final String threadName1 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory1 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread1 = jaxrs21ThreadFactory1.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName1))) {
                    throw new RuntimeException("testMethodLevelRolesAllowedUserInRoleMultipleRequests1: incorrect thread name");
                }
            }
        });

        jaxrs21Thread1.setName(threadName1);
        ExecutorService executorService1 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory1);
        ClientBuilder cb1 = ClientBuilder.newBuilder().executorService(executorService1);
        cb1.connectTimeout(120000, TimeUnit.MILLISECONDS);
        cb1.readTimeout(120000, TimeUnit.MILLISECONDS);
        cb1.register(new BasicAuthFilter(userRoleFromAppBnd ? "user2a" : "user2", "user2pwd"));
        Client c1 = cb1.build();
        WebTarget t1 = c1.target(url);
        CompletionStage<Response> completionStage1 = t1.request().accept("text/plain").rx().get();
        CompletableFuture<Response> completableFuture1 = completionStage1.toCompletableFuture();
        Response response1 = null;
        try {
            response1 = completableFuture1.get();
            assertEquals(200, response1.getStatus());
            assertEquals("remotely accessible only to users in Role2", response1.readEntity(String.class));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        final String threadName2 = "jaxrs21Thread";
        ThreadFactory jaxrs21ThreadFactory2 = Executors.defaultThreadFactory();

        Thread jaxrs21Thread2 = jaxrs21ThreadFactory2.newThread(new Runnable() {
            @Override
            public void run() {
                String runThreadName = Thread.currentThread().getName();
                if (!(runThreadName.equals(threadName2))) {
                    throw new RuntimeException("testMethodLevelRolesAllowedUserInRoleMultipleRequests2: incorrect thread name");
                }
            }
        });

        jaxrs21Thread2.setName(threadName2);
        ExecutorService executorService2 = Executors.newSingleThreadExecutor(jaxrs21ThreadFactory2);
        ClientBuilder cb2 = ClientBuilder.newBuilder().executorService(executorService2);
        cb2.connectTimeout(120000, TimeUnit.MILLISECONDS);
        cb2.readTimeout(120000, TimeUnit.MILLISECONDS);
        cb2.register(new BasicAuthFilter(userRoleFromAppBnd ? "user2a" : "user2", "user2pwd"));
        Client c2 = cb2.build();
        WebTarget t2 = c2.target(url);
        CompletionStage<Response> completionStage2 = t2.request().accept("text/plain").rx().get();
        CompletableFuture<Response> completableFuture2 = completionStage2.toCompletableFuture();
        Response response2 = null;
        try {
            response2 = completableFuture2.get();
            assertEquals(200, response2.getStatus());
            assertEquals("remotely accessible only to users in Role2", response2.readEntity(String.class));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        BiFunction<? super Response, ? super Response, ? extends Object> myFunction = (a, b) -> {
            return a;
        };

        CompletionStage<Object> completionStage = completionStage1.thenCombine(completionStage2, myFunction);
        CompletableFuture<Object> completableFuture = completionStage.toCompletableFuture();

        try {
            Response response = (Response) completableFuture.get();
            assertEquals(200, (response).getStatus());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
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