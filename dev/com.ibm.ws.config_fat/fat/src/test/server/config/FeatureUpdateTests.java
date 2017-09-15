/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class FeatureUpdateTests {
    private void test(LibertyServer server, String testUri) throws Exception {
        HttpURLConnection con = null;
        try {
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + testUri);
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            assertNotNull(is);

            String output = read(is);
            System.out.println(output);
            assertTrue(output, output.trim().startsWith("Test Passed"));
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private static String read(InputStream in) throws IOException {
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        return builder.toString();
    }

    private final Set<String> emptySet = Collections.emptySet();

    // Just make sure we don't get an FFDC when we remove a feature that something else has a reference to. 
    // In this test, the reference is also ibm:flat
    @Test
    public void testFeatureRemovalFlat() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.features");
        server.startServer("removeJDBCFeature.log");

        try {
            // switch to new configuration to remove jdbc-4.0 feature
            server.setServerConfigurationFile("featureRemove/serverRemoveJDBC.xml");
            assertNotNull("The server configuration update should complete",
                          server.waitForConfigUpdateInLogUsingMark(emptySet));
        } finally {
            server.stopServer();
        }

    }

    // Just make sure we don't get an FFDC when we remove a feature that something else has a reference to. 
    // In this test, the reference is not marked ibm:flat
    // We're also removing the configuration that the OpenID feature config points to. 
    @Test
    public void testFeatureRemoval() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.features");
        server.startServer("removeOpenIDFeature.log");

        try {
            // switch to new configuration to remove jdbc-4.0 feature
            server.setServerConfigurationFile("featureRemove/serverRemoveOpenID.xml");
            assertNotNull("The server configuration update should complete",
                          server.waitForConfigUpdateInLogUsingMark(emptySet));
        } finally {
            server.stopServer();
        }

    }
}
