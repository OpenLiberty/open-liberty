/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.fat.utils;

import java.util.Set;

import org.junit.Assert;

import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class MetricsAuthTestUtil {

    /**
     * @param server - Liberty server
     * @param name - The name of a feature to remove e.g. mpMetrics-1.0
     */
    public static void removeFeature(LibertyServer server, String name) {
        try {
            ServerConfiguration config = server.getServerConfiguration();
            Set<String> features = config.getFeatureManager().getFeatures();
            if (features.isEmpty()) {
                return;
            } else {
                if (!features.contains(name))
                    return;
                features.remove(name);
                server.updateServerConfiguration(config);
//                assertNotNull("Config wasn't updated successfully",
//                              server.waitForStringInLogUsingMark("CWWKG0017I.* | CWWKG0018I.*"));
            }
        } catch (Exception e) {
            Assert.fail("Unable to remove feature:" + name);
        }
    }

    /**
     * @param server - Liberty server
     * @param name - The name of a feature to add e.g. mpMetrics-1.0
     */
    public static void addFeature(LibertyServer server, String name) {
        try {
            ServerConfiguration config = server.getServerConfiguration();
            Set<String> features = config.getFeatureManager().getFeatures();
            if (features.contains(name))
                return;
            features.add(name);
            server.updateServerConfiguration(config);
//            assertNotNull("Config wasn't updated successfully",
//                          server.waitForStringInLogUsingMark("CWWKG0017I.* | CWWKG0018I.*"));
        } catch (Exception e) {
            Assert.fail("Unable to add feature:" + name);
        }
    }

}
