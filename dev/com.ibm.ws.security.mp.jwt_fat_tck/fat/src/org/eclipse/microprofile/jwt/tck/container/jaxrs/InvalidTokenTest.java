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
import java.util.HashSet;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

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
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * These set of tests validate the validation expectations for JWTs
 */
@RunWith(FATRunner.class)
public class InvalidTokenTest extends FATServletClient {
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
    }

    /**
     * The base URL for the container under test
     */

    private static String baseURL;

    @Server("mpjwt_roles")
    public static LibertyServer server1;

    /**
     * Create a CDI aware base web application archive
     *
     * @return the base base web application archive
     * @throws IOException - on resource failure
     */
    @BeforeClass
    public static void setUp() throws Exception {
        URL publicKey = InvalidTokenTest.class.getResource("/publicKey.pem");
        WebArchive webArchive = ShrinkWrap
                        .create(WebArchive.class, "RolesAllowedTest.war")
                        .addAsResource(publicKey, "/publicKey.pem")
                        .addClass(RolesEndpoint.class)
                        .addClass(TCKApplication.class)
                        .addAsWebInfResource("beans.xml", "beans.xml")
                        .addAsWebInfResource("web.xml", "web.xml");
        System.out.printf("WebArchive: %s\n", webArchive.toString(true));
        ShrinkHelper.exportToServer(server1, "apps", webArchive);

        baseURL = "http://localhost:" + server1.getHttpDefaultPort() + "/RolesAllowedTest";
        server1.startServer();
        server1.waitForStringInLog("CWWKS4105I", 30000); // wait for ltpa keys to be created and service ready, which can happen after startup.
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer("CWWKS5523E", "CWWKS5524E");
    }

    @AllowedFFDC(value = { "com.ibm.websphere.security.jwt.InvalidClaimException",
                           "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test //@Test(groups = TEST_GROUP_JAXRS,         description = "Validate a request with expired token fails with HTTP_UNAUTHORIZED")
    public void callEchoExpiredToken() throws Exception {
        HashSet<TokenUtils.InvalidClaims> invalidFields = new HashSet<>();
        invalidFields.add(TokenUtils.InvalidClaims.EXP);
        String token = TokenUtils.generateTokenString("/Token1.json", invalidFields);
        System.out.printf("jwt: %s\n", token);

        String uri = baseURL + "/endp/echo";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("input", "hello");
        Response response = echoEndpointTarget.request(TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_UNAUTHORIZED);
        String reply = response.readEntity(String.class);
        System.out.printf("Reply: %s\n", reply);
    }

    @AllowedFFDC(value = { "com.ibm.websphere.security.jwt.InvalidClaimException",
                           "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test //@Test(groups = TEST_GROUP_JAXRS,         description = "Validate a request with an non-matching issuer fails with HTTP_UNAUTHORIZED")
    public void callEchoBadIssuer() throws Exception {
        HashSet<TokenUtils.InvalidClaims> invalidFields = new HashSet<>();
        invalidFields.add(TokenUtils.InvalidClaims.ISSUER);
        String token = TokenUtils.generateTokenString("/Token1.json", invalidFields);
        System.out.printf("jwt: %s\n", token);

        String uri = baseURL + "/endp/echo";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("input", "hello");
        Response response = echoEndpointTarget.request(TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_UNAUTHORIZED);
        String reply = response.readEntity(String.class);
        System.out.printf("Reply: %s\n", reply);
    }

    @ExpectedFFDC(value = { "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    @AllowedFFDC(value = { "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test //@Test(groups = TEST_GROUP_JAXRS,         description = "Validate a request with an incorrect signer fails with HTTP_UNAUTHORIZED")
    public void callEchoBadSigner() throws Exception {
        HashSet<TokenUtils.InvalidClaims> invalidFields = new HashSet<>();
        invalidFields.add(TokenUtils.InvalidClaims.SIGNER);
        String token = TokenUtils.generateTokenString("/Token1.json", invalidFields);
        System.out.printf("jwt: %s\n", token);

        String uri = baseURL + "/endp/echo";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("input", "hello");
        Response response = echoEndpointTarget.request(TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_UNAUTHORIZED);
        String reply = response.readEntity(String.class);
        System.out.printf("Reply: %s\n", reply);
    }

    @AllowedFFDC(value = { "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test //@Test(groups = TEST_GROUP_JAXRS,         description = "Validate a request with an incorrect signature algorithm fails with HTTP_UNAUTHORIZED")
    public void callEchoBadSignerAlg() throws Exception {
        HashSet<TokenUtils.InvalidClaims> invalidFields = new HashSet<>();
        invalidFields.add(TokenUtils.InvalidClaims.ALG);
        String token = TokenUtils.generateTokenString("/Token1.json", invalidFields);
        System.out.printf("jwt: %s\n", token);

        String uri = baseURL + "/endp/echo";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("input", "hello");
        Response response = echoEndpointTarget.request(TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_UNAUTHORIZED);
        String reply = response.readEntity(String.class);
        System.out.printf("Reply: %s\n", reply);
    }

}
