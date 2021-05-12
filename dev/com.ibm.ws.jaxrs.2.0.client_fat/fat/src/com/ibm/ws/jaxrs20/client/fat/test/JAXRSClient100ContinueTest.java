/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.fat.test;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.HashMap;
import java.util.Map;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.integration.ClientAndServer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * These tests are used to determine whether a JAX-RS 2.X client can handle
 * 100-Continue responses.  100 Continue responses are used when a client
 * wants to check whether a server will handle the entity content or not -
 * i.e. why bother sending a large amount of data if the server is just
 * going to disregard it?
 * So, the way it works is that the client sends the request (sans-entity)
 * with the HTTP header, "Expect: 100-continue".  If the server will handle
 * the entity, then it responds with a 100 Continue response (with no
 * entity). Then the client re-sends the request, this time with the entity
 * to the server.
 */
@RunWith(FATRunner.class)
public class JAXRSClient100ContinueTest extends AbstractTest {

    private final static Class<?> c = JAXRSClient100ContinueTest.class;

    @Server("jaxrs20.client.100ContinueTest")
    public static LibertyServer server;

    private final static String appname = "jaxrsclient100Continue";
    private final static String target = appname + "/ClientTestServlet";

    private static int mockServerPort;
    private static ClientAndServer mockServerClient;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname,
                                                       "com.ibm.ws.jaxrs20.client.jaxrsclient100Continue.client");

        mockServerPort = Integer.getInteger("member_4.http");
        //mockServerPort = 10080; //DIAG: tcptunnel --local-port=9080 --remote-host=localhost --remote-port=10080 --stay-alive --log 
        mockServerClient = startClientAndServer(mockServerPort);

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        mockServerClient.stop();
        if (server != null) {
            server.stopServer();
        }
    }

    @Before
    public void preTest() {
        serverRef = server;
        Log.info(c, "preTest", "Mock Server listening on port " + mockServerPort);
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    @Test
    public void testClientHandles100Continue() throws Exception {
        mockServerClient.when(request().withMethod("POST")
                                       .withPath("/mockContextRoot/mockResource")
                                       .withBody("This is a really big body"))
                        .respond(response().withStatusCode(200)
                                           .withBody("We got it!"));
        
        Map<String, String> p = new HashMap<String, String>();
        p.put("mockServerIP", "localhost");
        p.put("mockServerPort", "" + mockServerPort);
        //p.put("mockServerPort", "" + (mockServerPort - 1000)); //DIAG
        this.runTestOnServer(target, "testClientHandles100Continue", p, "200 We got it!");
    }
}
