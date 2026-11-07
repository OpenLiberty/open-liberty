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
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * The jwkEndpointUrl of the JMXConnectorRESTConsumer (file:${server.config.dir}resources/security/jwtPublic.pem
 * in server.xml) points at a file that is missing, unreadable or does not hold a public key. The
 * jwtConsumer cannot get a key, so RestJwtTAI must reject the request.
 *
 * The file URL is fixed for the life of the server, but RestJwtTAI reads the file on
 * every request, so each test changes the state of the file instead of restarting the server. A
 * valid key is never served, so no key can be cached from an earlier request.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class RestJwtKeyFileInvalidFATTest {

    private static final Class<?> c = RestJwtKeyFileInvalidFATTest.class;

    @Server("RestJwtKeyFileServer")
    public static LibertyServer server;

    private static File keyFile;
    private static String token;

    @BeforeClass
    public static void setUp() throws Exception {
        String methodName = "setUp";

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        token = FATSuite.createRs256Jwt(FATSuite.createClaims(FATSuite.SERVER_ID, FATSuite.ADMIN_GROUPS), pair.getPrivate());

        // Must be the file named by jwkEndpointUrl in the RestJwtKeyFileServer server.xml
        keyFile = new File(server.getServerRoot() + "/resources/security/jwtPublic.pem");
        keyFile.getParentFile().mkdirs();
        deleteKeyFile();

        Log.info(c, methodName, "Starting RestJwtKeyFileServer with the jwkEndpointUrl file " + keyFile.getAbsolutePath());
        server.startServer();
        assertNotNull("IBMJMXConnectorREST application was not ready",
                      server.waitForStringInLog("SRVE0242I.*IBMJMXConnectorREST"));
        assertNotNull("Default HTTPS endpoint did not start", server.waitForDefaultHTTPEndpointSSLStart());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer("CWWKS6\\d+E", "CWWKS5\\d+E", "CWWKG0\\d+E");
        } finally {
            deleteKeyFile();
        }
    }

    @Before
    public void resetKeyFile() throws Exception {
        deleteKeyFile();
    }

    @Test
    public void testKeyFileMissing_Unauthorized() throws Exception {
        assertEquals("The key file must not exist", false, keyFile.exists());
        assertUnauthorized();
    }

    @Test
    public void testKeyFileUnreadable_Unauthorized() throws Exception {
        Files.write(keyFile.toPath(), "-----BEGIN PUBLIC KEY-----\nAAAA\n-----END PUBLIC KEY-----\n".getBytes(StandardCharsets.US_ASCII));
        keyFile.setReadable(false, false);
        // root can read a file whatever its permissions, so the case cannot be created then
        assumeTrue(!keyFile.canRead());
        assertUnauthorized();
    }

    @Test
    public void testKeyFileWithoutPublicKey_Unauthorized() throws Exception {
        Files.write(keyFile.toPath(), "this is not a public key".getBytes(StandardCharsets.US_ASCII));
        assertUnauthorized();
    }

    @Test
    public void testKeyFileEmpty_Unauthorized() throws Exception {
        Files.write(keyFile.toPath(), new byte[0]);
        assertUnauthorized();
    }

    private void assertUnauthorized() throws Exception {
        server.setMarkToEndOfLog();
        assertEquals("Expected 401 Unauthorized when the public key cannot be read from " + keyFile,
                     HttpURLConnection.HTTP_UNAUTHORIZED, FATSuite.getResponseCode(server, token));
        assertNotNull("RestJwtTAI did not report an error validating the JWT",
                      server.waitForStringInTraceUsingMark("Error validating JWT token"));
    }

    private static void deleteKeyFile() {
        if (keyFile != null && keyFile.exists()) {
            keyFile.setReadable(true, false);
            keyFile.delete();
        }
    }
}
