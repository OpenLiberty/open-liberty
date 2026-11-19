/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.rest.jwt.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.net.HttpURLConnection;
import java.util.Collections;

import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Key rotation: the JSON Web Key Set that the jwkEndpointUrl of the JMXConnectorRESTConsumer names is replaced
 * while the server is running. Every key has its own kid, and the tokens are signed with RS256 and carry the kid
 * of their signing key, as the tokens of a token issuer do.
 *
 * The jwtConsumer keeps the public keys it read from the file until its configuration is reloaded, so overwriting
 * the file is not enough on its own. Each rotation in the first tests replaces the file and then changes the
 * rotation.clockSkew variable in server.xml, which reloads the jwtConsumer. After the reload, tokens signed with
 * a key that is in the key set must be accepted and tokens signed with a key that is not in it must be rejected,
 * without restarting the server.
 *
 * The testLiveRotation tests replace the key set and do NOT reload the jwtConsumer, like a node agent that
 * rotates its key while the server runs. The keys and the tokens must carry a kid: a kid that is not in the
 * cached key set makes the server read the key file again. A token without a kid is verified with the single key
 * that is cached, so the new key is never picked up.
 *
 * Every request uses a token with a new jti claim because the jwtConsumer accepts a given jti only once.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class RestJwtKeyRotationFATTest {

    private static final Class<?> c = RestJwtKeyRotationFATTest.class;

    private static final String ROTATION_VARIABLE = "rotation.clockSkew";
    private static final String CLOCK_SKEW_A = "60s";
    private static final String CLOCK_SKEW_B = "61s";

    private static final String ORIGINAL_KID = "original-key";
    private static final String NEW_KID = "new-key";

    // JWKSet.Stale is 1 minute; wait a little longer so the removed key is stale
    private static final long STALE_WAIT_MS = 62 * 1000;

    @Server("RestJwtKeyRotationServer")
    public static LibertyServer server;

    private static boolean useClockSkewB;

    // Every key of the reload tests gets its own kid
    private static int keyCount;

    // The key whose public key is the only one in the key set after the last rotation
    private static RsaJsonWebKey currentKey;

    @BeforeClass
    public static void setUp() throws Exception {
        String methodName = "setUp";

        // Generate the key pairs at run time so that no private key is checked in.
        currentKey = newKey();
        FATSuite.writeJwks(server, currentKey);

        Log.info(c, methodName, "Starting RestJwtKeyRotationServer with the JWKS file " + FATSuite.getJwksFile(server).getAbsolutePath());
        server.startServer();
        assertNotNull("IBMJMXConnectorREST application was not ready",
                      server.waitForStringInLog("SRVE0242I.*IBMJMXConnectorREST"));
        assertNotNull("Default HTTPS endpoint did not start", server.waitForDefaultHTTPEndpointSSLStart());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer();
        } finally {
            FATSuite.getJwksFile(server).delete();
        }
    }

    /**
     * Each test starts from a freshly rotated key and a reloaded jwtConsumer, so that the tests do not depend on
     * their order and no key from an earlier test is cached.
     */
    @Before
    public void startWithNewKey() throws Exception {
        rotateKey();
    }

    /**
     * After the rotation the token signed with the new private key is accepted and the token signed with the
     * previous private key is rejected.
     */
    @Test
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    public void testRotatedKey_NewKeyAccepted_OldKeyRejected() throws Exception {
        RsaJsonWebKey oldKey = currentKey;
        assertAccepted(oldKey, "a token signed with the current key before the rotation");

        RsaJsonWebKey newKey = rotateKey();

        assertAccepted(newKey, "a token signed with the new key after the rotation");
        assertRejected(oldKey, "a token signed with the previous key");
    }

    /**
     * The key can be rotated more than once. Only the key that is in the key set at the moment of the request is
     * trusted, so neither of the two earlier keys is accepted any more.
     */
    @Test
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    public void testRotatedTwice_OnlyLatestKeyAccepted() throws Exception {
        RsaJsonWebKey firstKey = currentKey;
        RsaJsonWebKey secondKey = rotateKey();
        RsaJsonWebKey thirdKey = rotateKey();

        assertAccepted(thirdKey, "a token signed with the latest key");
        assertRejected(secondKey, "a token signed with the key before the latest");
        assertRejected(firstKey, "a token signed with the oldest key");
    }

    /**
     * A rejected token is a 401 because of the signature, not because the server is unable to
     * authenticate anyone: a token signed with the current key still works afterwards.
     */
    @Test
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    public void testRotatedKey_RejectedTokenDoesNotAffectValidTokens() throws Exception {
        RsaJsonWebKey oldKey = currentKey;
        RsaJsonWebKey newKey = rotateKey();

        assertRejected(oldKey, "a token signed with the previous key");
        assertAccepted(newKey, "a token signed with the new key after a rejected request");
    }

    /**
     * Rotation with a grace period. While the grace period lasts the key set has both keys, so a token that was
     * signed with the old key before the rotation is still accepted, and so is a token signed with the new key.
     * When the grace period is over the old key is removed from the key set, and only the token signed with the
     * new key is accepted. The jwtConsumer is reloaded at each step, so the result does not depend on how long the
     * jwtConsumer caches a key.
     */
    @Test
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    public void testRotationWithGracePeriod_BothKeysAcceptedThenOnlyNewKey() throws Exception {
        RsaJsonWebKey oldKey = currentKey;
        RsaJsonWebKey newKey = newKey();

        // Before the rotation only the old key is trusted
        assertAccepted(oldKey, "a token signed with the old key before the rotation");
        assertRejected(newKey, "a token signed with the new key before it is in the key set");

        // Start of the grace period: the new key is added to the key set and the old key stays
        FATSuite.writeJwks(server, oldKey, newKey);
        reloadJwtConsumer();
        assertAccepted(oldKey, "a token signed with the old key during the grace period");
        assertAccepted(newKey, "a token signed with the new key during the grace period");

        // End of the grace period: the old key is removed from the key set
        FATSuite.writeJwks(server, newKey);
        currentKey = newKey;
        reloadJwtConsumer();
        assertAccepted(newKey, "a token signed with the new key after the grace period");
        assertRejected(oldKey, "a token signed with the old key after the grace period");
    }

    /**
     * Live rotation of a JWKS file with a grace period, without any configuration change. The file holds the
     * original key, then the original and the new key (grace period), then only the new key. The keys and the
     * tokens carry a kid. A token with a kid that is not in the cached key set is a cache miss, so the file is
     * read again and the new key is picked up.
     * <p>
     * A key that was removed from the file stays in the cache until it is older than JWKSet.Stale (1 minute) and
     * the file is read again, so this test does not expect the removed key to be rejected. See
     * testLiveRotation_JwksWithKid_RemovedKeyRejectedAfterStaleTime.
     */
    @Test
    public void testLiveRotation_JwksWithKid_NewKeyAcceptedWithoutConfigChange() throws Exception {
        RsaJsonWebKey originalKey = FATSuite.generateRsaKey(ORIGINAL_KID);
        RsaJsonWebKey newKey = FATSuite.generateRsaKey(NEW_KID);

        FATSuite.writeJwks(server, originalKey);
        assertAccepted(originalKey, "a token with kid " + ORIGINAL_KID + " when it is the only key in the JWKS file");

        FATSuite.writeJwks(server, originalKey, newKey);
        assertAccepted(originalKey, "a token with kid " + ORIGINAL_KID + " during the grace period");

        FATSuite.writeJwks(server, newKey);
        assertAccepted(newKey, "a token with kid " + NEW_KID + " after the original key was removed from the JWKS file");
    }

    /**
     * The original key is replaced by a new key in the JWKS file while the server runs and the configuration is
     * NOT changed. The removed key stays cached until it is older than JWKSet.Stale (1 minute) and the key file is
     * read again. A request with the new kid is a cache miss, so it reads the file and drops the stale original
     * key. After that a token with the original kid must be rejected.
     */
    @Test
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    public void testLiveRotation_JwksWithKid_RemovedKeyRejectedAfterStaleTime() throws Exception {
        RsaJsonWebKey originalKey = FATSuite.generateRsaKey(ORIGINAL_KID);
        RsaJsonWebKey newKey = FATSuite.generateRsaKey(NEW_KID);

        // Only the original key is cached by the first request, so the request with the new kid is a cache miss
        FATSuite.writeJwks(server, originalKey);
        assertAccepted(originalKey, "a token with kid " + ORIGINAL_KID + " when it is the only key in the JWKS file");

        FATSuite.writeJwks(server, newKey);
        Thread.sleep(STALE_WAIT_MS);

        assertAccepted(newKey, "a token with kid " + NEW_KID + " after the original key was removed from the JWKS file");
        assertRejected(originalKey, "a token with kid " + ORIGINAL_KID + " more than a minute after it was removed from the JWKS file");
    }

    /**
     * Negative test: the same live rotation, but the keys and the tokens have no kid. The first request caches
     * the single key from the file. A token without a kid is verified with that cached key and never causes the
     * file to be read again, so a token signed with the new key is rejected until the jwtConsumer is reloaded.
     * This is why the keys of a rotating key file must have a kid.
     */
    @Test
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    public void testLiveRotation_JwksWithoutKid_NewKeyRejectedWithoutConfigChange() throws Exception {
        // A generated key has no kid until one is set, and a token that is signed with it has no kid in the header
        RsaJsonWebKey originalKey = RsaJwkGenerator.generateJwk(2048);
        RsaJsonWebKey newKey = RsaJwkGenerator.generateJwk(2048);

        FATSuite.writeJwks(server, originalKey);
        assertAccepted(originalKey, "a token without a kid when the original key is the only key in the JWKS file");

        FATSuite.writeJwks(server, newKey);
        assertRejected(newKey, "a token without a kid, signed with the new key, because the old key is still cached and the key file is not read again");
    }

    /**
     * Replaces the key set with one that only has a new key and reloads the jwtConsumer.
     *
     * @return the key that is now the only key in the key set
     */
    private static RsaJsonWebKey rotateKey() throws Exception {
        currentKey = newKey();
        FATSuite.writeJwks(server, currentKey);
        reloadJwtConsumer();
        return currentKey;
    }

    private static RsaJsonWebKey newKey() throws Exception {
        return FATSuite.generateRsaKey("rotationKey" + (++keyCount));
    }

    /**
     * Changes the variable that the clockSkew of the jwtConsumer refers to, which makes the server reload the
     * jwtConsumer and drop the public keys that it cached from the key set file.
     */
    private static void reloadJwtConsumer() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        Variable variable = config.getVariables().getBy("name", ROTATION_VARIABLE);
        assertNotNull("The " + ROTATION_VARIABLE + " variable is missing from the server.xml", variable);
        useClockSkewB = !useClockSkewB;
        variable.setValue(useClockSkewB ? CLOCK_SKEW_B : CLOCK_SKEW_A);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        assertFalse("The server configuration update was not completed",
                    server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet()).isEmpty());
    }

    /** Sends a token that is signed with the key, with the kid of the key in the header if the key has one */
    private static void assertAccepted(RsaJsonWebKey key, String description) throws Exception {
        String token = FATSuite.createRs256Jwt(FATSuite.createClaims(FATSuite.SERVER_ID, FATSuite.ADMIN_GROUPS), key);
        assertEquals("Expected 200 OK for " + description,
                     HttpURLConnection.HTTP_OK, FATSuite.getResponseCode(server, token));
    }

    private static void assertRejected(RsaJsonWebKey key, String description) throws Exception {
        String token = FATSuite.createRs256Jwt(FATSuite.createClaims(FATSuite.SERVER_ID, FATSuite.ADMIN_GROUPS), key);
        server.setMarkToEndOfLog();
        assertEquals("Expected 401 Unauthorized for " + description,
                     HttpURLConnection.HTTP_UNAUTHORIZED, FATSuite.getResponseCode(server, token));
        assertNotNull("RestJwtTAI did not report an error validating " + description,
                      server.waitForStringInTraceUsingMark("Error validating JWT token"));
    }
}
