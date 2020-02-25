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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.tck.util.TokenUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests of the MP-JWT auth method authorization behavior as expected by the MP-JWT RBAC 1.0 spec
 */
@RunWith(FATRunner.class)
public class RolesAllowedTest extends FATServletClient {
    //test-ng to junit adapters
    static class Reporter {
        static void log(String in) {
            System.out.println("*** " + in);
        }
    }

    static class Assert2 {
        static void assertTrue(boolean b, String s) {
            Assert.assertTrue(s, b);
        }

        static void assertEquals(long actual, long expected) {
            Assert.assertEquals(expected, actual);
        }

        static void assertEquals(Object actual, Object expected) {
            Assert.assertEquals(expected, actual);
        }
    }

    /**
     * The test generated JWT token string
     */
    private static String token;
    // Time claims in the token
    private static Long iatClaim;
    private static Long authTimeClaim;
    private static Long expClaim;

    @Server("mpjwt_roles")
    public static LibertyServer server1;

    /**
     * The base URL for the container under test
     */
    //@ArquillianResource
    private static String baseURL;

    /**
     * Create a CDI aware base web application archive
     *
     * @return the base base web application archive
     * @throws IOException - on resource failure
     */
    @BeforeClass
    public static void setUp() throws Exception {
        URL publicKey = RolesAllowedTest.class.getResource("/publicKey.pem");
        WebArchive webArchive = ShrinkWrap
                        .create(WebArchive.class, "RolesAllowedTest.war")
                        .addAsResource(publicKey, "/publicKey.pem")
                        .addClass(RolesEndpoint.class)
                        .addClass(TCKApplication.class)
                        .addAsWebInfResource("beans.xml", "beans.xml")
                        .addAsWebInfResource("web.xml", "web.xml")
                        .addAsManifestResource("permissions.xml");
        System.out.printf("WebArchive: %s\n", webArchive.toString(true));
        ShrinkHelper.exportToServer(server1, "apps", webArchive);

        baseURL = "http://localhost:" + server1.getHttpDefaultPort() + "/RolesAllowedTest";
        server1.startServer();
        server1.waitForStringInLog("CWWKS4105I", 30000); // wait for ltpa keys to be created and service ready, which can happen after startup.
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer("CWWKS552[2-4]E");
    }

    @BeforeClass
    public static void generateToken() throws Exception {
        HashMap<String, Long> timeClaims = new HashMap<>();
        token = TokenUtils.generateTokenString("/Token1.json", null, timeClaims);
        iatClaim = timeClaims.get(Claims.iat.name());
        authTimeClaim = timeClaims.get(Claims.auth_time.name());
        expClaim = timeClaims.get(Claims.exp.name());
    }

    @Test //@Test(groups = TEST_GROUP_JAXRS, description = "Validate a request with no token fails with HTTP_UNAUTHORIZED")
    public void callEchoNoAuth() throws Exception {
        Reporter.log("callEchoNoAuth, expect HTTP_UNAUTHORIZED");
        String uri = baseURL + "/endp/echo";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("input", "hello");
        Response response = echoEndpointTarget.request(TEXT_PLAIN).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    @AllowedFFDC(value = { "com.ibm.websphere.security.jwt.InvalidTokenException", "org.jose4j.jwt.consumer.InvalidJwtException" })
    @Test //@Test(groups = TCKConstants.TEST_GROUP_JAXRS, description = "Attempting access with BASIC auth header should fail with HTTP_UNAUTHORIZED")
    public void callEchoBASIC() throws Exception {
        Reporter.log("callEchoBASIC, expect HTTP_UNAUTHORIZED");
        byte[] tokenb = Base64.getEncoder().encode("jdoe@example.com:password".getBytes());
        String token = new String(tokenb);
        System.out.printf("basic: %s\n", token);

        String uri = baseURL + "/endp/echo";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("input", "hello");
        Response response = echoEndpointTarget.request(TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, "BASIC " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_UNAUTHORIZED);
        String reply = response.readEntity(String.class);
        System.out.println(reply);
    }

    @Test //@Test(groups = TEST_GROUP_JAXRS, description = "Validate a request with MP-JWT succeeds with HTTP_OK, and replies with hello, user={token upn claim}")
    public void callEcho() throws Exception {
        Reporter.log("callEcho, expect HTTP_OK");

        String uri = baseURL + "/endp/echo";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("input", "hello");
        Response response = echoEndpointTarget.request(TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(HttpURLConnection.HTTP_OK, response.getStatus());
        String reply = response.readEntity(String.class);
        // Must return hello, user={token upn claim}
        Assert.assertEquals(reply, "hello, user=jdoe@example.com");
    }

    @Test //@Test(groups = TEST_GROUP_JAXRS, description = "Validate a request with MP-JWT but no associated role fails with HTTP_FORBIDDEN")
    public void callEcho2() throws Exception {
        Reporter.log("callEcho2, expect HTTP_FORBIDDEN");

        String uri = baseURL + "/endp/echo2";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("input", "hello");
        Response response = echoEndpointTarget.request(TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        String reply = response.readEntity(String.class);
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_FORBIDDEN);
    }

    @Test //@Test(groups = TEST_GROUP_JAXRS, description = "Validate a request with MP-JWT is able to access checkIsUserInRole with HTTP_OK")
    public void checkIsUserInRole() throws Exception {
        Reporter.log("checkIsUserInRole, expect HTTP_OK");

        String uri = baseURL + "/endp/checkIsUserInRole";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri);
        Response response = echoEndpointTarget.request(TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        String reply = response.readEntity(String.class);
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
    }

    @Test //@Test(groups = TEST_GROUP_JAXRS, description = "Validate a request with MP-JWT Token2 fails to access checkIsUserInRole with HTTP_FORBIDDEN")
    public void checkIsUserInRoleToken2() throws Exception {
        Reporter.log("checkIsUserInRoleToken2, expect HTTP_FORBIDDEN");
        String token2 = TokenUtils.generateTokenString("/Token2.json");

        String uri = baseURL + "/endp/checkIsUserInRole";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri);
        Response response = echoEndpointTarget.request(TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, "Bearer " + token2).get();
        String reply = response.readEntity(String.class);
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_FORBIDDEN);
    }

    @Test //@Test(groups = TEST_GROUP_JAXRS, description = "Validate a request with MP-JWT Token2 is able to access echoNeedsToken2Role with HTTP_OK")
    public void echoNeedsToken2Role() throws Exception {
        Reporter.log("echoNeedsToken2Role, expect HTTP_FORBIDDEN");
        String token2 = TokenUtils.generateTokenString("/Token2.json");

        String uri = baseURL + "/endp/echoNeedsToken2Role";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("input", "hello");
        Response response = echoEndpointTarget.request(TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, "Bearer " + token2).get();
        String reply = response.readEntity(String.class);
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
    }

    @Test //@Test(groups = TEST_GROUP_JAXRS, description = "Validate a request with MP-JWT Token2 calling echo fails with HTTP_FORBIDDEN")
    public void echoWithToken2() throws Exception {
        Reporter.log("echoWithToken2, expect HTTP_FORBIDDEN");
        String token2 = TokenUtils.generateTokenString("/Token2.json");

        String uri = baseURL + "/endp/echo";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("input", "hello");
        Response response = echoEndpointTarget.request(TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, "Bearer " + token2).get();
        String reply = response.readEntity(String.class);
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_FORBIDDEN);
    }

    @Test //@Test(groups = TEST_GROUP_JAXRS, description = "Validate a request with MP-JWT SecurityContext.getUserPrincipal() is a JsonWebToken")
    public void getPrincipalClass() throws Exception {
        Reporter.log("getPrincipalClass, expect HTTP_OK");
        String uri = baseURL + "/endp/getPrincipalClass";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri);
        Response response = echoEndpointTarget.request(TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String reply = response.readEntity(String.class);
        Assert2.assertEquals(reply, "isJsonWebToken:true");
    }

    /**
     * This test requires that the server provide a mapping from the group1 grant in the token to a Group1MappedRole
     * application declared role.
     */

    @Test //@Test(groups = TEST_GROUP_JAXRS, description = "Validate a request without an MP-JWT to endpoint requiring role mapping has HTTP_OK")
    public void testNeedsGroup1Mapping() {
        Reporter.log("testNeedsGroup1Mapping, expect HTTP_OK");
        String uri = baseURL + "/endp/needsGroup1Mapping";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri);
        Response response = echoEndpointTarget.request(TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String reply = response.readEntity(String.class);
        System.out.println(reply);
    }

    @Test //@Test(groups = TEST_GROUP_CDI, description = "Validate that accessing secured method has HTTP_OK and injected JsonWebToken principal")
    public void getInjectedPrincipal() throws Exception {
        Reporter.log("getInjectedPrincipal, expect HTTP_OK");
        String uri = baseURL + "/endp/getInjectedPrincipal";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri);
        Response response = echoEndpointTarget.request(TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(HttpURLConnection.HTTP_OK, response.getStatus());
        String reply = response.readEntity(String.class);
        Assert2.assertEquals(reply, "isJsonWebToken:true");
    }

    @Test //@Test(groups = TEST_GROUP_JAXRS, description = "Validate a request without an MP-JWT to unsecured endpoint has HTTP_OK with expected response")
    public void callHeartbeat() throws Exception {
        Reporter.log("callHeartbeat, expect HTTP_OK");
        String uri = baseURL + "/endp/heartbeat";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("input", "hello");
        Response response = echoEndpointTarget.request(TEXT_PLAIN).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String reply = response.readEntity(String.class);
        Assert2.assertTrue(reply.startsWith("Heartbeat:"), "Saw Heartbeat: ...");
    }
}
