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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class LibertyServerTest {

    @Test
    public void testGetInstalledFeaturesFromLogMessage() {
        List<String> expected = Arrays.asList("cdi-4.1", "componenttest-2.0", "localConnector-1.0", "mpConfig-3.1", "mpReactiveMessaging-3.0", "mpReactiveStreams-3.0",
                                              "osgiConsole-1.0", "servlet-6.1", "timedexit-1.0");

        String message = "[04/07/2024, 15:26:31:119 BST] 00000035 com.ibm.ws.kernel.feature.internal.FeatureManager            A CWWKF0012I: The server installed the following features: [cdi-4.1, componenttest-2.0, localConnector-1.0, mpConfig-3.1, mpReactiveMessaging-3.0, mpReactiveStreams-3.0, osgiConsole-1.0, servlet-6.1, timedexit-1.0].";
        Set<String> features = LibertyServer.getInstalledFeaturesFromLogMessage(message);

        assertTrue("Feature set did not contain all expected features", features.containsAll(expected));
        assertEquals("Feature set contained extra features", features.size(), expected.size());
    }

    @Test
    public void testGetVersionlessFeatureName() {
        String feature = "cdi-4.1"; //versioned feature short name
        String expected = "cdi";
        String actual = LibertyServer.getVersionlessFeatureName(feature);
        assertEquals(expected, actual);

        feature = "cdi"; //already a versionless feature
        expected = "cdi";
        actual = LibertyServer.getVersionlessFeatureName(feature);
        assertEquals(expected, actual);
    }

}
