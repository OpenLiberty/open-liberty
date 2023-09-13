/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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

package com.ibm.ws.security.auth.data.fat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

/**
 * Some utility functions for FATs.
 */
public class AuthDataFatUtils {

    /**
     * JakartaEE9 transform a list of applications. The applications are the simple app names and they must exist at '<server>/apps/<appname>'.
     *
     * @param myServer The server to transform the applications on.
     * @param apps     The simple names of the applications to transform.
     */
    public static void transformApps(LibertyServer myServer, String... apps) {
        if (JakartaEEAction.isEE9OrLaterActive()) {
            for (String app : apps) {
                Path someArchive = Paths.get(myServer.getServerRoot() + File.separatorChar + "apps" + File.separatorChar + app);
                JakartaEEAction.transformApp(someArchive);
            }
        }
    }

    /**
     * Get the normalized test name. This will remove the test repeat pattern from the end of the test name.
     *
     * @param testName The test name.
     * @return The normalized test name.
     */
    public static String normalizeTestName(TestName testName) {
        return testName.getMethodName().replace("_EE9_FEATURES", "");
    }

    /**
     * Swap out the passwordUtilities-1.0 feature with the passwordUtilities-1.1 feature.
     *
     * @param server The server running with passwordUtilities-1.0.
     * @throws Exception if there was an issue swapping the features.
     */
    public static void runWithPasswordUtilities11(LibertyServer server) throws Exception {
        ServerConfiguration config = server.getServerConfiguration().clone();
        config.getFeatureManager().getFeatures().remove("passwordUtilities-1.0");
        config.getFeatureManager().getFeatures().remove("passwordutilities-1.0"); // JakartaEE9Action lower-cases feature names
        config.getFeatureManager().getFeatures().add("passwordUtilities-1.1");
        server.updateServerConfiguration(config);
    }
}
