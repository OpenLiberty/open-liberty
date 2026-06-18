/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.security;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Base64;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jws.JsonWebSignature;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;

/**
 * FAT test for JWT Builder functionality and RFC 9728 signed metadata.
 */
@RunWith(FATRunner.class)
public class JwtBuilderTest {

    private static final int JWT_PART_HEADER = 0;
    private static final int JWT_PART_PAYLOAD = 1;

    @Server("mcp-server-jwt")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "jwtBuilderTest.war")
                                   .addClass(JwtBuilderServlet.class);
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        server.startServer();
        assertNotNull(server.waitForStringInLog("CWWKZ0001I.*jwtBuilderTest"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWWKE0912W", "CWWKE0921W", "CWNEN0047W", "CWNEN0049W");
    }

    // ==================== Signed Metadata Tests (single request) ====================

    /**
     * Fetches metadata once and validates all aspects of the signed metadata response.
     * Tests covered: metadata structure, scopes, bearer methods, JWT header fields,
     * issuer, resource claim, and registered claims (iss, iat, exp).
     */
    @Test
    public void testSignedMetadata() throws Exception {
        JSONObject metadata = httpRequest("/jwtBuilderTest/jwtBuilder?mock=signed-metadata");

        verifyMetadataStructure(metadata);
        verifyScopesInMetadataAndJwt(metadata);
        verifyBearerMethodsInMetadataAndJwt(metadata);
        verifyJwtHeaderFields(metadata);
        verifyIssuerClaim(metadata);
        verifyResourceClaim(metadata);
        verifyRegisteredClaims(metadata);
    }

    // ==================== JWK Endpoint and Signature Verification ====================

    @Test
    public void testJwkEndpointAndSignatureVerification() throws Exception {
        JSONObject jwkSet = httpRequest("/jwt/ibm/api/testJwtBuilder/jwk");
        verifyJwkEndpoint(jwkSet);

        JSONObject metadata = httpRequest("/jwtBuilderTest/jwtBuilder");
        verifySignatureAgainstJwk(metadata, jwkSet);
    }

    @Test
    public void testUnsignedMetadataWhenJwtBuilderRefMissing() throws Exception {
        JSONObject metadata = httpRequest("/jwtBuilderTest/jwtBuilder?mock=unsigned-metadata");

        assertTrue("Metadata should have 'resource' field", metadata.has("resource"));
        assertTrue("Metadata should have 'authorization_servers' field", metadata.has("authorization_servers"));
        assertTrue("Metadata should have 'scopes_supported' field", metadata.has("scopes_supported"));
        assertTrue("Metadata should have 'bearer_methods_supported' field", metadata.has("bearer_methods_supported"));
        assertFalse("Metadata should not have 'signed_metadata' field when jwtBuilderRef is missing", metadata.has("signed_metadata"));
        assertFalse("Metadata should not have 'jwks_uri' field when jwtBuilderRef is missing", metadata.has("jwks_uri"));
    }

    // ==================== Assertion Methods ====================

    private void verifyMetadataStructure(JSONObject metadata) throws Exception {
        assertTrue("Metadata should have 'resource' field", metadata.has("resource"));
        assertTrue("Metadata should have 'authorization_servers' field", metadata.has("authorization_servers"));
        assertTrue("Metadata should have 'jwks_uri' field", metadata.has("jwks_uri"));
        assertTrue("Metadata should have 'signed_metadata' field", metadata.has("signed_metadata"));

        JSONArray authServers = verifyIsJSONArray(metadata, "authorization_servers");

        String jwksUri = metadata.getString("jwks_uri");
        assertNotNull("jwks_uri should not be null", jwksUri);
        assertFalse("jwks_uri should not be empty", jwksUri.isEmpty());

        String signedMetadata = metadata.getString("signed_metadata");
        assertNotNull("signed_metadata should not be null", signedMetadata);

        System.out.println("Validated RFC 9728 Signed Metadata:");
        System.out.println("  resource: " + metadata.getString("resource"));
        System.out.println("  authorization_servers: " + authServers.toString());
        System.out.println("  scopes_supported present: " + metadata.has("scopes_supported"));
        System.out.println("  bearer_methods_supported present: " + metadata.has("bearer_methods_supported"));
        System.out.println("  jwks_uri: " + jwksUri);
        System.out.println("  signed_metadata (JWT): " + signedMetadata.substring(0, Math.min(50, signedMetadata.length())) + "...");
    }

    private void verifyScopesInMetadataAndJwt(JSONObject metadata) throws Exception {
        assertTrue("scopes_supported must be present in JSON when SCOPES is configured",
                   metadata.has("scopes_supported"));

        JSONArray scopes = metadata.getJSONArray("scopes_supported");
        assertEquals("Expected 4 scopes", 4, scopes.length());
        assertEquals("toys_browse", scopes.getString(0));
        assertEquals("toys_search", scopes.getString(1));
        assertEquals("cart_read", scopes.getString(2));
        assertEquals("cart_write", scopes.getString(3));

        JSONObject jwtPayload = decodeJwtPayload(metadata.getString("signed_metadata"));

        assertTrue("scopes_supported must be present in JWT payload when SCOPES is configured",
                   jwtPayload.has("scopes_supported"));

        JSONArray jwtScopes = jwtPayload.getJSONArray("scopes_supported");
        assertEquals("JWT scope count must match JSON scope count", scopes.length(), jwtScopes.length());
        assertEquals("toys_browse", jwtScopes.getString(0));
        assertEquals("toys_search", jwtScopes.getString(1));
        assertEquals("cart_read", jwtScopes.getString(2));
        assertEquals("cart_write", jwtScopes.getString(3));
    }

    private void verifyBearerMethodsInMetadataAndJwt(JSONObject metadata) throws Exception {
        assertTrue("bearer_methods_supported must be present in JSON when BEARER_METHODS is configured",
                   metadata.has("bearer_methods_supported"));

        JSONArray bearerMethods = metadata.getJSONArray("bearer_methods_supported");
        assertEquals("Expected 1 bearer method", 1, bearerMethods.length());
        assertEquals("header", bearerMethods.getString(0));

        JSONObject jwtPayload = decodeJwtPayload(metadata.getString("signed_metadata"));

        assertTrue("bearer_methods_supported must be present in JWT payload when BEARER_METHODS is configured",
                   jwtPayload.has("bearer_methods_supported"));

        JSONArray jwtBearerMethods = jwtPayload.getJSONArray("bearer_methods_supported");
        assertEquals("JWT bearer method count must match JSON count", bearerMethods.length(), jwtBearerMethods.length());
        assertEquals("header", jwtBearerMethods.getString(0));
    }

    /** JWT header contains required fields: alg, kid, and typ */
    private void verifyJwtHeaderFields(JSONObject metadata) throws Exception {
        JSONObject header = decodeJwtHeader(metadata.getString("signed_metadata"));

        assertTrue("Header must contain 'alg' field", header.has("alg"));
        assertTrue("Header must contain 'kid' field", header.has("kid"));
        assertTrue("Header must contain 'typ' field", header.has("typ"));

        String alg = header.getString("alg");
        String kid = header.getString("kid");
        String typ = header.getString("typ");

        assertNotNull("alg must not be null", alg);
        assertFalse("alg must not be empty", alg.isEmpty());
        assertNotNull("kid must not be null", kid);
        assertFalse("kid must not be empty", kid.isEmpty());
        assertEquals("typ must be 'JWT'", "JWT", typ);

        System.out.println("JWT Header fields: alg=" + alg + ", kid=" + kid + ", typ=" + typ);
    }

    /** Issuer claim is present and non-empty */
    private void verifyIssuerClaim(JSONObject metadata) throws Exception {
        JSONObject payload = decodeJwtPayload(metadata.getString("signed_metadata"));

        assertTrue("JWT payload must contain 'iss' claim", payload.has("iss"));
        String issuer = payload.getString("iss");
        assertNotNull("iss claim must not be null", issuer);
        assertFalse("iss claim must not be empty", issuer.isEmpty());

        System.out.println("JWT iss claim: " + issuer);
    }

    /** Resource claim is present and matches the metadata resource field */
    private void verifyResourceClaim(JSONObject metadata) throws Exception {
        JSONObject payload = decodeJwtPayload(metadata.getString("signed_metadata"));

        assertTrue("JWT payload must contain 'resource' claim", payload.has("resource"));
        String resource = payload.getString("resource");
        assertNotNull("resource claim must not be null", resource);
        assertFalse("resource claim must not be empty", resource.isEmpty());

        String metadataResource = metadata.getString("resource");
        assertEquals("JWT resource claim must match metadata resource field", metadataResource, resource);

        System.out.println("JWT resource claim: " + resource);
    }

    /** iat and exp are present, positive, and exp is after iat */
    private void verifyRegisteredClaims(JSONObject metadata) throws Exception {
        JSONObject payload = decodeJwtPayload(metadata.getString("signed_metadata"));

        // iat (Issued At) - must be present
        assertTrue("JWT must contain 'iat' (Issued At) claim", payload.has("iat"));
        long iat = payload.getLong("iat");
        assertTrue("iat must be a positive Unix timestamp", iat > 0);

        // exp (Expiration Time) - must be present
        assertTrue("JWT must contain 'exp' (Expiration Time) claim", payload.has("exp"));
        long exp = payload.getLong("exp");
        assertTrue("exp must be a positive Unix timestamp", exp > 0);
        assertTrue("exp must be after iat", exp > iat);

        System.out.println("Registered claims: iat=" + iat + ", exp=" + exp);
    }

    /** Cryptographic signature verification — finds the matching JWK by kid and verifies the JWT signature. */
    private void verifySignatureAgainstJwk(JSONObject metadata, JSONObject jwkSet) throws Exception {
        String signedMetadata = metadata.getString("signed_metadata");
        JSONObject header = decodeJwtHeader(signedMetadata);

        String kid = header.getString("kid");
        assertNotNull("JWT kid must not be null", kid);

        JSONArray keys = jwkSet.getJSONArray("keys");
        assertTrue("JWK Set must contain at least one key", keys.length() > 0);

        // Find the JWK entry whose kid matches the JWT header.
        JSONObject matchedKey = null;
        for (int i = 0; i < keys.length(); i++) {
            JSONObject key = keys.getJSONObject(i);
            if (key.has("kid") && key.getString("kid").equals(kid)) {
                matchedKey = key;
                break;
            }
        }
        assertNotNull("JWT kid '" + kid + "' must match a key in the JWK Set", matchedKey);
        System.out.println("Matched JWK key: kid=" + matchedKey.getString("kid") + ", kty=" + matchedKey.getString("kty"));

        // Reconstruct the public key from the JWK and verify the JWT signature.
        PublicJsonWebKey publicJwk = PublicJsonWebKey.Factory.newPublicJwk(matchedKey.toString());
        PublicKey publicKey = publicJwk.getPublicKey();

        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(signedMetadata);
        jws.setKey(publicKey);
        assertTrue("JWT signature must be valid against the published JWK", jws.verifySignature());

        System.out.println("JWT signature verified successfully against kid=" + kid);
    }

    private void verifyJwkEndpoint(JSONObject jwkSet) throws Exception {
        assertTrue("JWK Set should have 'keys' field", jwkSet.has("keys"));

        JSONArray keys = jwkSet.getJSONArray("keys");
        assertTrue("JWK Set 'keys' array should not be empty", keys.length() > 0);

        JSONObject firstKey = keys.getJSONObject(0);
        assertTrue("JWK should have 'kty' field", firstKey.has("kty"));
        assertTrue("JWK should have 'use' field", firstKey.has("use"));
        assertTrue("JWK should have 'kid' field", firstKey.has("kid"));
        assertTrue("JWK should have 'n' field", firstKey.has("n"));
        assertTrue("JWK should have 'e' field", firstKey.has("e"));

        System.out.println("Validated JWK Set:");
        System.out.println("  key count: " + keys.length());
        System.out.println("  kid: " + firstKey.getString("kid"));
        System.out.println("  kty: " + firstKey.getString("kty"));
        System.out.println("  use: " + firstKey.getString("use"));
    }

    // ==================== Helper Methods ====================

    private JSONObject httpRequest(String path) throws Exception {
        HttpRequest request = new HttpRequest(server, path)
                                                           .method("GET")
                                                           .expectCode(200);
        String response = request.run(String.class);
        showOutput(response);
        return new JSONObject(response);
    }

    private void showOutput(String string) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("JWT Builder Test Output");
        System.out.println("=".repeat(80));
        System.out.println(string);
        System.out.println("=".repeat(80) + "\n");
    }

    private JSONObject decodeJwtHeader(String jwt) throws JSONException {
        String[] parts = jwt.split("\\.");
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[JWT_PART_HEADER]), StandardCharsets.UTF_8);
        return new JSONObject(headerJson);
    }

    private JSONObject decodeJwtPayload(String jwt) throws JSONException {
        String[] parts = jwt.split("\\.");
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[JWT_PART_PAYLOAD]), StandardCharsets.UTF_8);
        return new JSONObject(payloadJson);
    }

    private JSONArray verifyIsJSONArray(JSONObject metadata, String arrayName) throws JSONException {
        JSONArray jsonArray = metadata.getJSONArray(arrayName);
        assertTrue(arrayName + " should not be empty", jsonArray.length() > 0);
        return jsonArray;
    }
}
