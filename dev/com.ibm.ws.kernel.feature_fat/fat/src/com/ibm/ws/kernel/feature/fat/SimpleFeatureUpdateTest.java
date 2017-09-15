/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
