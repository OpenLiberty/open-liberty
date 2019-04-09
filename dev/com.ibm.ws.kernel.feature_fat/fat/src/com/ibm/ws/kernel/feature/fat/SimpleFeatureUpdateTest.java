/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.kernel.feature.fat;

import org.junit.Test;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class SimpleFeatureUpdateTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.simple");

    @Test
    public void testFeatureRemoval() throws Exception {
        // Start the server with a set of features installed, stop the server, remove a feature, start the server. 

        server.startServer();

        server.stopServer();

        ServerConfiguration config = server.getServerConfiguration();

        config.getFeatureManager().getFeatures().remove("osgiConsole-1.0");

        server.updateServerConfiguration(config);

        server.startServer(false);

        server.stopServer("");
    }
}
