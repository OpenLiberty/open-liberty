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
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.util.List;

import org.junit.AfterClass;
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
 * Negative test: with only jwt-1.0 enabled (no restConnector-2.0) the restJwt-1.0 auto-feature must not be
 * provisioned, so RestJwtTAI must not be created and must not process any request.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class RestJwtNoConnectorFATTest {

    private static final Class<?> c = RestJwtNoConnectorFATTest.class;

    // Anything that would only be logged if the restJwt bundle was started or the TAI was invoked
    private static final String TAI_TRACE_REGEX = "RestJwtTAI|restJwtTAI|io\\.openliberty\\.security\\.rest\\.jwt\\.tai";

    @Server("RestJwtNoConnectorServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(c, "setUp", "Starting RestJwtNoConnectorServer (jwt-1.0 without restConnector-2.0)...");
        server.startServer();
        assertNotNull("Default HTTPS endpoint did not start", server.waitForDefaultHTTPEndpointSSLStart());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Without restConnector-2.0 the auto-feature's provision capability is not satisfied, so the bundle is never
     * started and the TAI component is never registered.
     */
    @Test
    public void testRestJwtTaiNotActivated() throws Exception {
        List<String> found = server.findStringsInTrace(TAI_TRACE_REGEX);
        assertTrue("RestJwtTAI should not be activated without restConnector-2.0, but found: " + found, found.isEmpty());
    }

    /**
     * A request carrying a valid Bearer JWT for the REST admin path must not be handled by RestJwtTAI. The
     * IBMJMXConnectorREST application does not exist, so the server answers 404 and the TAI is never invoked.
     */
    @Test
    public void testBearerRequestNotProcessedByRestJwtTai() throws Exception {
        String token = FATSuite.createHs256Jwt(FATSuite.createClaims(FATSuite.SERVER_ID, FATSuite.ADMIN_GROUPS), FATSuite.SHARED_KEY);

        assertEquals("Expected 404 because the REST connector application is not present", HttpURLConnection.HTTP_NOT_FOUND,
                     FATSuite.getResponseCode(server, token));

        List<String> found = server.findStringsInTrace(TAI_TRACE_REGEX);
        assertTrue("RestJwtTAI should not process the request, but found: " + found, found.isEmpty());
    }
}
