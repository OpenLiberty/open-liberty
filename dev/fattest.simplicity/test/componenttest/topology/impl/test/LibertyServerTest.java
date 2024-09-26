/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.impl.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class LibertyServerTest {

    @Test
    public void testGetInstalledFeaturesFromLogMessage() {
        Set<String> expected = new HashSet<>(Arrays.asList("cdi-4.1",
                                                           "componenttest-2.0",
                                                           "localConnector-1.0",
                                                           "mpConfig-3.1",
                                                           "mpReactiveMessaging-3.0",
                                                           "mpReactiveStreams-3.0",
                                                           "osgiConsole-1.0",
                                                           "servlet-6.1",
                                                           "timedexit-1.0"));

        String message = "[04/07/2024, 15:26:31:119 BST] 00000035 com.ibm.ws.kernel.feature.internal.FeatureManager            A CWWKF0012I: The server installed the following features: [cdi-4.1, componenttest-2.0, localConnector-1.0, mpConfig-3.1, mpReactiveMessaging-3.0, mpReactiveStreams-3.0, osgiConsole-1.0, servlet-6.1, timedexit-1.0].";
        Set<String> features = LibertyServer.getInstalledFeaturesFromLogMessage(message);

        assertEquals("Feature set did not contain expected features", expected, features);
    }

    @Test
    public void testGetInstalledFeaturesFromLogBadMessage() {
        Set<String> expected = new HashSet<>();

        String message = "[04/07/2024, 15:26:31:119 BST] 00000035 com.ibm.ws.kernel.feature.internal.FeatureManager            A CWWKF0012I: This is a fake message in an unexpected format: ]cdi-4.1, componenttest-2.0, localConnector-1.0, mpConfig-3.1, mpReactiveMessaging-3.0, mpReactiveStreams-3.0, osgiConsole-1.0, servlet-6.1, timedexit-1.0[.";
        Set<String> features = LibertyServer.getInstalledFeaturesFromLogMessage(message);

        assertEquals("Feature set should have been empty due to bad message", expected, features);
    }

    @Test
    public void testRemoveFeatureVersion() {
        String feature = "cdi-4.1"; //versioned feature short name
        String expected = "cdi";
        String actual = LibertyServer.removeFeatureVersion(feature);
        assertEquals(expected, actual);
    }

    @Test
    public void testRemoveFeatureVersionVersionless() {
        String feature = "cdi"; //versionless feature short name
        String expected = "cdi";
        String actual = LibertyServer.removeFeatureVersion(feature);
        assertEquals(expected, actual);
    }

    @Test
    public void testUnexpectedFeatures() {
        List<String> expectedFeatures = Arrays.asList("cdi-4.1",
                                                      "componenttest-2.0",
                                                      "localConnector-1.0",
                                                      "mpConfig-3.1",
                                                      "mpReactiveMessaging-3.0",
                                                      "mpReactiveStreams-3.0",
                                                      "osgiConsole-1.0",
                                                      "servlet-6.1",
                                                      "timedexit-1.0");

        List<String> installedFeatures = Arrays.asList("cdi-4.1",
                                                       "componenttest-2.0",
                                                       "localConnector-1.0",
                                                       "mpConfig-4.0", // This one is different... but...
                                                       "mpConfig-3.1", // This one is a match and so should negate the unexpected version above
                                                       "mpConfig-5.0", // Different again but should be ignored
                                                       "mpReactiveMessaging-3.0",
                                                       "mpReactiveStreams-4.0", // This one is different, unexpected version
                                                       "osgiConsole-1.0",
                                                       "servlet-6.1",
                                                       "timedexit-1.0");

        Map<String, String> expected = new HashMap<>();
        expected.put("mpReactiveStreams-3.0", "mpReactiveStreams-4.0");

        Map<String, String> actual = LibertyServer.getUnexpectedFeatures(expectedFeatures, installedFeatures);

        assertEquals(expected, actual);
    }

}
