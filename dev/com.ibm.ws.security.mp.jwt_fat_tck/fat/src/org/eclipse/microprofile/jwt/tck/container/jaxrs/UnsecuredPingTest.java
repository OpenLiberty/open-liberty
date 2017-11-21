/*
 * Copyright (c) 2016-2017 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.eclipse.microprofile.jwt.tck.container.jaxrs;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
//import static org.eclipse.microprofile.jwt.tck.TCKConstants.TEST_GROUP_JAXRS;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * A basic test of an unsecured JAX-RS endpoint to validate the test harness
 * without including JWT authentication.
 */
@RunWith(FATRunner.class)
public class UnsecuredPingTest extends FATServletClient {
    /**
     * The base URL for the container under test
     */

    private static String baseURL = null;

    /**
     * Create a CDI aware base web application archive
     *
     * @return the base base web application archive
     * @throws IOException - on resource failure
     */

    @Server("mpjwt")
    //@TestServlet(servlet = TestServletA.class, path = APP_NAME + "/TestServletA")
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {

        URL publicKey = UnsecuredPingTest.class.getResource("/publicKey.pem");
        WebArchive webArchive = ShrinkWrap
                        .create(WebArchive.class, "PingTest.war")
                        .addAsResource(publicKey, "/publicKey.pem")
                        .addClass(UnsecuredPingEndpoint.class)
                        .addClass(UnsecureTCKApplication.class)
                        .addAsWebInfResource("beans.xml", "beans.xml");
        //   .addAsWebInfResource("web.xml", "web.xml");
        // Arquillian adds web.xml automatically, but here we have to ask it.
        System.out.printf("WebArchive: %s\n", webArchive.toString(true));
        ShrinkHelper.exportToServer(server1, "dropins", webArchive);

        baseURL = "http://localhost:" + server1.getHttpDefaultPort();
        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer();
    }

    @Test
    //description = "Basic test of an unsecured JAX-RS endpoint")
    public void callEchoNoAuth() throws Exception {

        String uri = baseURL + "/PingTest/ping/echo";
        System.out.println("*** accessing URL: " + uri);
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("input", "hello");
        Response response = echoEndpointTarget.request(TEXT_PLAIN).get();
        System.out.println("*** response body: " + response.readEntity(String.class));
        Assert.assertEquals(HttpURLConnection.HTTP_OK, response.getStatus());
    }

}
