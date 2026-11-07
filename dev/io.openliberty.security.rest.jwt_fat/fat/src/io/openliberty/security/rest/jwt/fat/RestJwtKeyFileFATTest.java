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
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * RestJwtTAI still validates the token with the jwtConsumer, but the RS256 public key is read from
 * the file named by the jwkEndpointUrl of the JMXConnectorRESTConsumer in server.xml:
 * jwkEndpointUrl="file:${server.config.dir}resources/security/jwtPublic.pem"
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class RestJwtKeyFileFATTest {

    private static final Class<?> c = RestJwtKeyFileFATTest.class;

    @Server("RestJwtKeyFileServer")
    public static LibertyServer server;

    private static PrivateKey signingKey;
    private static PrivateKey otherKey;

    @BeforeClass
    public static void setUp() throws Exception {
        String methodName = "setUp";

        // Generate the key pairs at run time so that no private key is checked in.
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        signingKey = pair.getPrivate();
        otherKey = generator.generateKeyPair().getPrivate();

        File keyFile = new File(server.getServerRoot() + "/resources/security/jwtPublic.pem");
        keyFile.getParentFile().mkdirs();
        String pem = "-----BEGIN PUBLIC KEY-----\n"
                     + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(pair.getPublic().getEncoded())
                     + "\n-----END PUBLIC KEY-----\n";
        Files.write(keyFile.toPath(), pem.getBytes(StandardCharsets.US_ASCII));

        Log.info(c, methodName, "Starting RestJwtKeyFileServer with the jwkEndpointUrl file " + keyFile.getAbsolutePath());
        server.startServer();
        assertNotNull("IBMJMXConnectorREST application was not ready",
                      server.waitForStringInLog("SRVE0242I.*IBMJMXConnectorREST"));
        assertNotNull("Default HTTPS endpoint did not start", server.waitForDefaultHTTPEndpointSSLStart());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testTokenSignedWithKeyFromFile_Success() throws Exception {
        String token = FATSuite.createRs256Jwt(FATSuite.createClaims(FATSuite.SERVER_ID, FATSuite.ADMIN_GROUPS), signingKey);
        assertEquals("Expected 200 OK when the signature matches the public key file",
                     HttpURLConnection.HTTP_OK, FATSuite.getResponseCode(server, token));
    }

    @Test
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    public void testTokenSignedWithOtherKey_Unauthorized() throws Exception {
        String token = FATSuite.createRs256Jwt(FATSuite.createClaims(FATSuite.SERVER_ID, FATSuite.ADMIN_GROUPS), otherKey);
        assertEquals("Expected 401 Unauthorized when the signature does not match the public key file",
                     HttpURLConnection.HTTP_UNAUTHORIZED, FATSuite.getResponseCode(server, token));
    }
}
