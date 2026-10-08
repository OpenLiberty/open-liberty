/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.checkpoint.fat.security.oidc;

import static io.openliberty.checkpoint.fat.security.common.FATSuite.getTestMethod;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.PublicKey;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jws.JsonWebSignature;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.config.OpenidConnectClient;
import com.ibm.websphere.simplicity.config.ProtectedResourceMetadata;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ApacheJsonUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerWrapper;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@LibertyServerWrapper
@CheckpointTest
public class ProtectedResourceMetadataInstantOnTests extends CommonTest {

    private static final Class<?> thisClass = ProtectedResourceMetadataInstantOnTests.class;

    private static final String RSServerName = "com.ibm.ws.security.openidconnect.client-1.0_fat.protectedResourceMetadata";
    private static final String PROTECTED_RESOURCE_METADATA_PATH = "/.well-known/oauth-protected-resource";
    private static final String PROTECTED_RESOURCE_PATH = "/myApp/protectedExact";
    private static final String WITH_SCOPES_PATH = "/myApp/withScopes";
    private static final String SIGNED_PATH = "/myApp/withSigning";
    private static final String METADATA_ENABLED_PATH = "/myApp/metadataEnabled";

    private static LibertyServer genericServer;
    private static String serverHttpString;
    private static String serverHttpsString;
    private TestMethod testMethod;

    @BeforeClass
    public static void setUpServers() throws Exception {
        testSettings = new TestSettings();
        skipServerStart = true;
        genericTestServer = commonSetUp(RSServerName, "server.xml", Constants.GENERIC_SERVER, null, Constants.DO_NOT_USE_DERBY, null, null, null);
        genericServer = genericTestServer.getServer();
        serverHttpString = genericTestServer.getHttpString();
        serverHttpsString = genericTestServer.getHttpsString();
    }

    @Before
    public void setUp() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);

        genericServer.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        genericServer.startServer(testMethod + ".log");
        configureBeforeRestore();

        genericServer.checkpointRestore();

        genericServer.resetLogMarks();

        genericServer.waitForDefaultHTTPEndpointSSLStart();
    }

    private void configureBeforeRestore() {
        try {
            genericServer.saveServerConfiguration();
            Log.info(getClass(), testName.getMethodName(), "Configuring: " + testMethod);
            ServerConfiguration config = null;
            switch (testMethod) {
                case testAdvertisedScopesChange:
                    Log.info(getClass(), testName.getMethodName(), "UPDATING: " + testMethod);
                    config = genericServer.getServerConfiguration();
                    for (OpenidConnectClient client : config.getOpenidConnectClients()) {
                        if ("oidcClientScopes".equals(client.getId())) {
                            client.getProtectedResourceMetadata().setAdvertisedScopes("openid,profile,email");
                            break;
                        }
                    }
                    genericServer.updateServerConfiguration(config);
                    break;
                case testMetadataContainsSignedMetadataJwtChange:
                    Log.info(getClass(), testName.getMethodName(), "UPDATING:" + testMethod);
                    config = genericServer.getServerConfiguration();
                    for (OpenidConnectClient client : config.getOpenidConnectClients()) {
                        if ("oidcClientSigning".equals(client.getId())) {
                            client.getProtectedResourceMetadata().setJwtBuilderRef("metadataJwtBuilder");
                            break;
                        }
                    }
                    genericServer.updateServerConfiguration(config);
                    break;
                case testProtectedResourceMetadataReturns200:
                    Log.info(getClass(), testName.getMethodName(), "UPDATING:" + testMethod);
                    config = genericServer.getServerConfiguration();
                    for (OpenidConnectClient client : config.getOpenidConnectClients()) {
                        if ("oidcClientMetadataEnabled".equals(client.getId())) {
                            ProtectedResourceMetadata prm = new ProtectedResourceMetadata();
                            client.setProtectedResourceMetadata(prm);
                            break;
                        }
                    }
                    genericServer.updateServerConfiguration(config);
                    break;
                default:
                    Log.info(getClass(), testName.getMethodName(), "No configuration required: " + testMethod);
                    break;
            }
        } catch (Exception e) {
            Log.error(getClass(), testName.getMethodName(), e, "Failed to configure test: " + testMethod);
            throw new AssertionError("Unexpected error configuring test.", e);
        }

    }

    @Test
    public void testMetadataEndpointIsAccessible() throws Exception {
        JSONObject json = getMetadataJson(serverHttpString, PROTECTED_RESOURCE_PATH);

        assertEquals("Unexpected value for 'resource' field",
                     serverHttpString + PROTECTED_RESOURCE_PATH, json.get("resource"));
        assertNotNull("Response did not contain 'authorization_servers' field", json.get("authorization_servers"));
        assertTrue("'authorization_servers' should be a non-empty array",
                   ((JSONArray) json.get("authorization_servers")).size() > 0);
    }

    @Test
    public void testAdvertisedScopesChange() throws Exception {

        JSONObject json = getMetadataJson(serverHttpString, WITH_SCOPES_PATH);

        JSONArray scopesSupported = (JSONArray) json.get("scopes_supported");
        assertNotNull("Response did not contain 'scopes_supported' field", scopesSupported);
        assertTrue("Expected 'scopes_supported' to contain 'openid'", scopesSupported.contains("openid"));
        assertTrue("Expected 'scopes_supported' to contain 'profile'", scopesSupported.contains("profile"));
        assertTrue("Expected 'scopes_supported' to contain 'email'", scopesSupported.contains("email"));
        assertEquals("Expected exactly 3 scopes in 'scopes_supported'", 3, scopesSupported.size());

    }

    @Test
    @Ignore("See: https://github.com/OpenLiberty/open-liberty/issues/33381")
    public void testMetadataContainsSignedMetadataJwtChange() throws Exception {
        JSONObject json = getMetadataJson(serverHttpString, SIGNED_PATH);

        // Checks signature and consistency with unsigned metadata
        verifySignedMetadata(json, "metadataJwtBuilder");

        // issuer must be set to the value used in the config
        JSONObject signedPayload = decodeJwtPayload((String) json.get("signed_metadata"));
        String issuer = (String) signedPayload.get("iss");
        assertNotNull("Signed payload did not contain issuer claim", issuer);
    }

    /**
     * Test that requesting metadata for a resource whose OIDC client config initially does not have
     * protectedResourceMetadata configured returns 200 after the sub-element is added before restore.
     */
    @Test
    public void testProtectedResourceMetadataReturns200() throws Exception {
        String metadataUrl = buildMetadataUrl(serverHttpString, METADATA_ENABLED_PATH);
        getSuccessResponse(metadataUrl);
    }

    @After
    public void stopServers() throws Exception {
        try {
            genericServer.stopServer();
        } finally {
            Log.info(getClass(), "stopServer", "RESTORING ");
            genericServer.restoreServerConfiguration();

        }

    }

    static enum TestMethod {
        testMetadataEndpointIsAccessible,
        testAdvertisedScopesChange,
        testMetadataContainsSignedMetadataJwtChange,
        testProtectedResourceMetadataReturns200,
        unknown
    }

    /**
     * GETs the metadata endpoint for the given base URL and protected resource path,
     * asserts HTTP 200, and returns the parsed JSON response body.
     */
    private JSONObject getMetadataJson(String baseUrl, String protectedResourcePath) throws Exception {
        String metadataUrl = buildMetadataUrl(baseUrl, protectedResourcePath);
        Log.info(thisClass, "getMetadataJson", "GET " + metadataUrl);

        WebResponse response = getSuccessResponse(metadataUrl);

        return JSONObject.parse(response.getText());
    }

    /**
     * Builds the full metadata endpoint URL for a given base URL and protected resource path.
     */
    private static String buildMetadataUrl(String baseUrl, String protectedResourcePath) {
        String normalized;
        if (protectedResourcePath == null || protectedResourcePath.isEmpty() || "/".equals(protectedResourcePath)) {
            normalized = "/";
        } else if (protectedResourcePath.startsWith("/")) {
            normalized = protectedResourcePath;
        } else {
            normalized = "/" + protectedResourcePath;
        }
        return baseUrl + PROTECTED_RESOURCE_METADATA_PATH + normalized;
    }

    private WebResponse getSuccessResponse(String url) throws Exception {
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setProtectedResourceMetadataUrl(url);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addResponseExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE_METADATA_ENDPOINT,
                                                    "Did not find protected resource metadata in the response.", "\"resource\":");

        WebResponse response = genericInvokeEndpoint(_testName, wc, null, updatedTestSettings.getProtectedResourceMetadataUrl(),
                                                     Constants.GETMETHOD, Constants.INVOKE_PROTECTED_RESOURCE_METADATA_ENDPOINT, null, null, expectations);

        return response;
    }

    /**
     * Validates that the metadata is signed and internally consistent.
     * <p>
     * Checks the metadata includes signed_metadata, that the signature is valid and that the claims match the metadata
     *
     * @param metadata     the protected resource metadata to verify
     * @param jwtBuilderId the jwtBuilder ID, used to find the key to verify the signature
     */
    private void verifySignedMetadata(JSONObject metadata, String jwtBuilderId) throws Exception {

        // Check signed metadata present
        String signedMetadata = (String) metadata.get("signed_metadata");
        assertNotNull("Response did not contain 'signed_metadata' field", signedMetadata);

        // Parse signed_metadata as JWT
        JSONObject payload = decodeJwtPayload(signedMetadata);

        // Check JWT includes all metadata entries as claims, except signed_metadata
        for (Entry<String, Object> entry : (Set<Entry<String, Object>>) metadata.entrySet()) {
            String key = entry.getKey();
            if (key.equals("signed_metadata")) {
                continue;
            }
            assertTrue("Signed metadata does not contain " + key, payload.containsKey(key));
            assertEquals("Signed metadata has a different value for " + key, entry.getValue(), payload.get(key));
        }

        // Check required claims
        assertTrue("Signed metadata must have an 'iss' (issuer) claim", payload.containsKey("iss"));

        // Check typ header
        JSONObject header = decodeJwtHeader(signedMetadata);
        assertEquals("Header 'typ' is incorrect", "JWT", header.get("typ"));

        // Check JWT signature
        verifySignatureAgainstJwk(signedMetadata, jwtBuilderId);
    }

    /**
     * Finds the JWK whose {@code kid} matches the JWT header, reconstructs the public key,
     * and asserts that the RS256 signature on the compact JWS is valid.
     */
    private void verifySignatureAgainstJwk(String compactJws, String jwtBuilderId) throws Exception {
        JSONObject header = decodeJwtHeader(compactJws);
        JSONObject jwkSet = getJwkFromJwtBuilderId(jwtBuilderId);

        String kid = (String) header.get("kid");
        assertNotNull("JWT header must contain a 'kid' claim", kid);

        JSONArray keys = (JSONArray) jwkSet.get("keys");
        assertNotNull("JWK Set must contain a 'keys' array", keys);
        assertTrue("JWK Set must contain at least one key", keys.size() > 0);

        // Find the JWK entry whose kid matches the JWT header.
        JSONObject matchedKey = null;
        for (Object entry : keys) {
            JSONObject key = (JSONObject) entry;
            if (kid.equals(key.get("kid"))) {
                matchedKey = key;
                break;
            }
        }
        assertNotNull("JWT kid '" + kid + "' must match a key in the JWK Set", matchedKey);
        Log.info(thisClass, "verifySignatureAgainstJwk",
                 "Matched JWK key: kid=" + matchedKey.get("kid") + ", kty=" + matchedKey.get("kty"));

        // Reconstruct the public key from the JWK and verify the JWT signature.
        PublicJsonWebKey publicJwk = PublicJsonWebKey.Factory.newPublicJwk(matchedKey.toString());
        PublicKey publicKey = publicJwk.getPublicKey();

        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(compactJws);
        jws.setKey(publicKey);
        assertTrue("JWT signature must be valid against the published JWK", jws.verifySignature());

        Log.info(thisClass, "verifySignatureAgainstJwk",
                 "JWT signature verified successfully against kid=" + kid);
    }

    /**
     * Decodes the payload part of a JWT and returns it as a JSONObject
     */
    private static JSONObject decodeJwtPayload(String compactJws) throws Exception {
        return decodeJwtPart(compactJws, 1);
    }

    /**
     * Decodes a JWT part and returns it as a JSONObject.
     */
    private static JSONObject decodeJwtPart(String compactJws, int part) throws Exception {
        String[] parts = compactJws.split("\\.");
        assertEquals("JWT has wrong number of parts", 3, parts.length);
        String segmentB64 = parts[part];
        String segment = ApacheJsonUtils.fromBase64StringToJsonString(segmentB64);
        return JSONObject.parse(segment);
    }

    /**
     * Decodes the header part of a JWT and returns it as a JSONObject
     */
    private static JSONObject decodeJwtHeader(String compactJws) throws Exception {
        return decodeJwtPart(compactJws, 0);
    }

    /**
     * Fetches and parses a JSON object from the given URL, asserting HTTP 200.
     */
    private JSONObject getJwkFromJwtBuilderId(String builderId) throws Exception {

        String url = serverHttpsString + "/jwt/ibm/api/" + builderId + "/jwk";
        Log.info(thisClass, "getJwkFromUrl", "GET " + url);

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(new String[] { Constants.INVOKE_JWK_ENDPOINT });
        WebConversation wc = new WebConversation();
        WebResponse response = genericInvokeEndpoint(_testName, wc, null, url, Constants.GETMETHOD, Constants.INVOKE_JWK_ENDPOINT, null, null, expectations);

        return JSONObject.parse(response.getText());
    }
}
