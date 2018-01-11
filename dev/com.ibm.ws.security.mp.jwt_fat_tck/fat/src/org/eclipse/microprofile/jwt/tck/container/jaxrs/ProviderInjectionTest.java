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

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.tck.TCKConstants;
import org.eclipse.microprofile.jwt.tck.util.TokenUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
 * Tests of injection JsonWebToken claims using the {@linkplain javax.inject.Provider} interface.
 */
@RunWith(FATRunner.class)
public class ProviderInjectionTest extends FATServletClient {

    /**
     * The test generated JWT token string
     */
    private static String token;
    // Time claims in the token
    private static Long iatClaim;
    private static Long authTimeClaim;
    private static Long expClaim;

    //testng to junit adapters
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

    /**
     * Create a CDI aware base web application archive
     *
     * @return the base base web application archive
     * @throws IOException - on resource failure
     */
    @Server("mpjwt")
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {

        URL publicKey = ProviderInjectionTest.class.getResource("/publicKey.pem");
        WebArchive webArchive = ShrinkWrap
                        .create(WebArchive.class, "ProviderInjectionTest.war")
                        .addAsResource(publicKey, "/publicKey.pem")
                        .addClass(ProviderInjectionEndpoint.class)
                        .addClass(TCKApplication.class)
                        .addAsWebInfResource("beans.xml", "beans.xml")
                        .addAsWebInfResource("web.xml", "web.xml");
        System.out.printf("WebArchive: %s\n", webArchive.toString(true));
        ShrinkHelper.exportToServer(server1, "dropins", webArchive);

        baseURL = "http://localhost:" + server1.getHttpDefaultPort() + "/ProviderInjectionTest";
        server1.startServer();
    }

    @BeforeClass
    public static void generateToken() throws Exception {
        HashMap<String, Long> timeClaims = new HashMap<>();
        token = TokenUtils.generateTokenString("/Token1.json", null, timeClaims);
        iatClaim = timeClaims.get(Claims.iat.name());
        authTimeClaim = timeClaims.get(Claims.auth_time.name());
        expClaim = timeClaims.get(Claims.exp.name());
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected token issuer claim is as expected")
    public void verifyIssuerClaim() throws Exception {
        Reporter.log("Begin verifyIssuerClaim");
        String uri = baseURL + "/endp/verifyInjectedIssuer";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam(Claims.iss.name(), TCKConstants.TEST_ISSUER)
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected raw token claim is as expected")
    public void verifyInjectedRawToken() throws Exception {
        Reporter.log("Begin verifyInjectedRawToken\n");
        String uri = baseURL + "/endp/verifyInjectedRawToken";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam(Claims.raw_token.name(), token)
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected jti claim is as expected")
    public void verifyInjectedJTI() throws Exception {
        Reporter.log("Begin verifyInjectedJTI\n");
        String uri = baseURL + "/endp/verifyInjectedJTI";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam(Claims.jti.name(), "a-123")
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected aud claim is as expected")
    public void verifyInjectedAudience() throws Exception {
        Reporter.log("Begin verifyInjectedAudience\n");
        String uri = baseURL + "/endp/verifyInjectedAudience";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam(Claims.aud.name(), "s6BhdRkqt3")
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected iat claim is as expected")
    public void verifyInjectedIssuedAt() throws Exception {
        Reporter.log("Begin verifyInjectedIssuedAt\n");
        String uri = baseURL + "/endp/verifyInjectedIssuedAt";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam(Claims.iat.name(), iatClaim)
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected sub claim is as expected")
    public void verifyInjectedOptionalSubject() throws Exception {
        Reporter.log("Begin verifyInjectedOptionalSubject\n");
        String uri = baseURL + "/endp/verifyInjectedOptionalSubject";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam(Claims.sub.name(), "24400320")
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected raw token claim is as expected")
    public void verifyInjectedOptionalAuthTime() throws Exception {
        Reporter.log("Begin verifyInjectedOptionalAuthTime\n");
        String uri = baseURL + "/endp/verifyInjectedOptionalAuthTime";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected custom claim is missing as expected")
    public void verifyInjectedOptionalCustomMissing() throws Exception {
        Reporter.log("Begin verifyInjectedOptionalCustomMissing\n");
        String uri = baseURL + "/endp/verifyInjectedOptionalCustomMissing";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected customString claim is as expected")
    public void verifyInjectedCustomString() throws Exception {
        Reporter.log("Begin verifyInjectedCustomString\n");
        String uri = baseURL + "/endp/verifyInjectedCustomString";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("value", "customStringValue")
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected customInteger claim is as expected")
    public void verifyInjectedCustomInteger() throws Exception {
        Reporter.log("Begin verifyInjectedCustomInteger\n");
        String uri = baseURL + "/endp/verifyInjectedCustomInteger";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("value", 123456789)
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected customDouble claim is as expected")
    public void verifyInjectedCustomDouble() throws Exception {
        Reporter.log("Begin verifyInjectedCustomDouble\n");
        String uri = baseURL + "/endp/verifyInjectedCustomDouble";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("value", 3.141592653589793)
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    // Duplicate tests that use Token2.json to verify that @RequestScope or @Dependent scoping is in use

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected token issuer claim is as expected")
    public void verifyIssuerClaim2() throws Exception {
        Reporter.log("Begin verifyIssuerClaim");
        String uri = baseURL + "/endp/verifyInjectedIssuer";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam(Claims.iss.name(), TCKConstants.TEST_ISSUER)
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected raw token claim is as expected")
    public void verifyInjectedRawToken2() throws Exception {
        Reporter.log("Begin verifyInjectedRawToken\n");
        String uri = baseURL + "/endp/verifyInjectedRawToken";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam(Claims.raw_token.name(), token)
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected jti claim is as expected")
    public void verifyInjectedJTI2() throws Exception {
        Reporter.log("Begin verifyInjectedJTI\n");
        String uri = baseURL + "/endp/verifyInjectedJTI";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam(Claims.jti.name(), "a-123")
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected aud claim is as expected")
    public void verifyInjectedAudience2() throws Exception {
        Reporter.log("Begin verifyInjectedAudience\n");
        String uri = baseURL + "/endp/verifyInjectedAudience";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam(Claims.aud.name(), "s6BhdRkqt3")
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected iat claim is as expected")
    public void verifyInjectedIssuedAt2() throws Exception {
        Reporter.log("Begin verifyInjectedIssuedAt\n");
        String uri = baseURL + "/endp/verifyInjectedIssuedAt";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam(Claims.iat.name(), iatClaim)
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected sub claim is as expected")
    public void verifyInjectedOptionalSubject2() throws Exception {
        Reporter.log("Begin verifyInjectedOptionalSubject\n");
        String uri = baseURL + "/endp/verifyInjectedOptionalSubject";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam(Claims.sub.name(), "24400320")
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected raw token claim is as expected")
    public void verifyInjectedOptionalAuthTime2() throws Exception {
        Reporter.log("Begin verifyInjectedOptionalAuthTime\n");
        String uri = baseURL + "/endp/verifyInjectedOptionalAuthTime";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected customString claim is as expected")
    public void verifyInjectedCustomString2() throws Exception {
        Reporter.log("Begin verifyInjectedCustomString\n");
        String uri = baseURL + "/endp/verifyInjectedCustomString";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("value", "customStringValue")
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected customInteger claim is as expected")
    public void verifyInjectedCustomInteger2() throws Exception {
        Reporter.log("Begin verifyInjectedCustomInteger\n");
        String uri = baseURL + "/endp/verifyInjectedCustomInteger";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("value", 123456789)
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @Test //@Test(groups = TEST_GROUP_CDI_PROVIDER,         description = "Verify that the injected customDouble claim is as expected")
    public void verifyInjectedCustomDouble2() throws Exception {
        Reporter.log("Begin verifyInjectedCustomDouble\n");
        String uri = baseURL + "/endp/verifyInjectedCustomDouble";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                        .target(uri)
                        .queryParam("value", 3.141592653589793)
                        .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert2.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert2.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }
}
